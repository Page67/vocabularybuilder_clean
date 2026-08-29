package com.shiki.vocabulary.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shiki.vocabulary.data.LearningProgress
import com.shiki.vocabulary.data.ProgressStore
import com.shiki.vocabulary.data.SentenceWork
import com.shiki.vocabulary.data.UnitContentRepository
import com.shiki.vocabulary.model.BilingualText
import com.shiki.vocabulary.model.CatalogUnit
import com.shiki.vocabulary.model.ContentCatalog
import com.shiki.vocabulary.model.ContentLibrary
import com.shiki.vocabulary.model.CourseStage
import com.shiki.vocabulary.model.FixedQuiz
import com.shiki.vocabulary.model.PracticeMode
import com.shiki.vocabulary.model.PracticeQuestion
import com.shiki.vocabulary.model.Pronunciation
import com.shiki.vocabulary.model.QuizChoice
import com.shiki.vocabulary.model.QuizQuestion
import com.shiki.vocabulary.model.QuizSection
import com.shiki.vocabulary.model.UnitContent
import com.shiki.vocabulary.model.WordEntry
import com.shiki.vocabulary.model.WordGroup
import com.shiki.vocabulary.model.buildSentenceReviewPrompt
import com.shiki.vocabulary.model.checkSentenceWorkshop
import com.shiki.vocabulary.model.gradeQuiz
import com.shiki.vocabulary.model.makePracticeQuestion
import com.shiki.vocabulary.model.sentenceUsesTarget
import com.shiki.vocabulary.ui.theme.Card as CardColor
import com.shiki.vocabulary.ui.theme.Correct
import com.shiki.vocabulary.ui.theme.CorrectBackground
import com.shiki.vocabulary.ui.theme.Forest
import com.shiki.vocabulary.ui.theme.ForestSecondary
import com.shiki.vocabulary.ui.theme.Ink
import com.shiki.vocabulary.ui.theme.Mint
import com.shiki.vocabulary.ui.theme.Muted
import com.shiki.vocabulary.ui.theme.Paper
import com.shiki.vocabulary.ui.theme.StageColors
import com.shiki.vocabulary.ui.theme.Sun
import com.shiki.vocabulary.ui.theme.VocabularyTheme
import com.shiki.vocabulary.ui.theme.Wrong
import com.shiki.vocabulary.ui.theme.WrongBackground
import kotlin.math.roundToInt

private enum class MainView(val label: String, val symbol: String, val testTag: String) {
    Bookshelf("书架", "▦", "nav-bookshelf"),
    Home("概览", "⌂", "nav-home"),
    Learn("课程", "▤", "nav-learn"),
    Quiz("测验", "✓", "nav-quiz"),
    Practice("练习", "✦", "nav-practice"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyApp(
    library: ContentLibrary,
    initialUnitId: String = library.catalog.units.first().unitId,
    progressStoreFactory: (String) -> ProgressStore? = { null },
    onUnitChanged: (String) -> Unit = {},
    onSpeak: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val safeInitialUnitId = initialUnitId.takeIf { it in library.units } ?: library.catalog.units.first().unitId
    var currentUnitId by remember(library.catalog.catalogVersion) { mutableStateOf(safeInitialUnitId) }
    val content = library.unit(currentUnitId)
    val progressStore = remember(currentUnitId) { progressStoreFactory(currentUnitId) }
    var currentView by remember { mutableStateOf(MainView.Home) }
    var activeStage by remember(currentUnitId) { mutableStateOf(content.courseStages.first()) }
    var activeQuizId by remember(currentUnitId) { mutableStateOf(content.fixedQuizzes.first().id) }
    var globalChinese by remember { mutableStateOf(false) }
    var progressByUnit by remember(library.catalog.catalogVersion) {
        mutableStateOf(
            library.catalog.units.associate { entry ->
                entry.unitId to (progressStoreFactory(entry.unitId)?.load() ?: LearningProgress())
            },
        )
    }
    val progress = progressByUnit[currentUnitId] ?: LearningProgress()

    fun updateProgress(next: LearningProgress) {
        progressByUnit = progressByUnit + (currentUnitId to next)
        progressStore?.save(next)
    }

    fun selectUnit(unitId: String) {
        if (unitId == currentUnitId) {
            currentView = MainView.Home
            return
        }
        currentUnitId = unitId
        globalChinese = false
        currentView = MainView.Home
        onUnitChanged(unitId)
    }

    Scaffold(
        containerColor = Paper,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Paper),
                title = {
                    Column {
                        Text("词根词汇训练", fontWeight = FontWeight.Bold)
                        Text(
                            "${content.title.en} · ROOTS & STORIES",
                            color = Muted,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                        )
                    }
                },
                actions = {
                    if (currentView != MainView.Bookshelf) {
                        TextButton(
                            onClick = { currentView = MainView.Bookshelf },
                            modifier = Modifier.testTag("open-bookshelf"),
                        ) { Text("书架") }
                    }
                    if (currentView == MainView.Learn) {
                        TextButton(
                            onClick = { globalChinese = !globalChinese },
                            modifier = Modifier.testTag("global-translation"),
                        ) {
                            Text(if (globalChinese) "隐藏中文" else "显示中文")
                        }
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFFFFFDF8)) {
                MainView.entries.forEach { view ->
                    NavigationBarItem(
                        selected = currentView == view,
                        onClick = { currentView = view },
                        icon = { Text(view.symbol, fontSize = 19.sp) },
                        label = { Text(view.label) },
                        modifier = Modifier.testTag(view.testTag),
                    )
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentView) {
                MainView.Bookshelf -> BookshelfScreen(
                    catalogUnits = library.catalog.units,
                    totalUnits = library.catalog.totalUnits,
                    units = library.units,
                    progressByUnit = progressByUnit,
                    currentUnitId = currentUnitId,
                    onUnit = ::selectUnit,
                )

                MainView.Home -> OverviewScreen(
                    content = content,
                    progress = progress,
                    onStage = {
                        activeStage = it
                        currentView = MainView.Learn
                    },
                    onPractice = { currentView = MainView.Practice },
                )

                MainView.Learn -> LearnScreen(
                    content = content,
                    activeStage = activeStage,
                    globalChinese = globalChinese,
                    onStageChange = { activeStage = it },
                    onWordViewed = { updateProgress(progress.markViewed(it)) },
                    onStartQuiz = {
                        activeQuizId = activeStage.quizId
                        currentView = MainView.Quiz
                    },
                    onSpeak = onSpeak,
                )

                MainView.Quiz -> QuizScreen(
                    content = content,
                    activeQuizId = activeQuizId,
                    progress = progress,
                    onQuizChange = { activeQuizId = it },
                    onComplete = { quizId, percent ->
                        updateProgress(progress.recordQuiz(quizId, percent))
                    },
                )

                MainView.Practice -> PracticeScreen(
                    content = content,
                    progress = progress,
                    progressStore = progressStore,
                    onResult = { updateProgress(progress.recordPractice(it)) },
                    onSentenceComplete = {
                        updateProgress(progress.completeSentencePractice(it))
                    },
                    onCopyPrompt = { copyReviewPrompt(context, it) },
                    onSharePrompt = { shareReviewPrompt(context, it) },
                )
            }
        }
    }
}

@Composable
fun ContentLoadError(message: String) {
    Box(
        modifier = Modifier.fillMaxSize().background(Paper).padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = WrongBackground)) {
            Column(Modifier.padding(22.dp)) {
                Text("无法载入词汇内容", style = MaterialTheme.typography.headlineSmall)
                Text(message, modifier = Modifier.padding(top = 10.dp), color = Wrong)
            }
        }
    }
}

@Composable
private fun BookshelfScreen(
    catalogUnits: List<CatalogUnit>,
    totalUnits: Int,
    units: Map<String, UnitContent>,
    progressByUnit: Map<String, LearningProgress>,
    currentUnitId: String,
    onUnit: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("screen-bookshelf"),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Eyebrow("LOCAL LIBRARY")
            Text(
                "单元书架",
                fontFamily = FontFamily.Serif,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "已收录 ${catalogUnits.size} / $totalUnits 个完整单元；只显示已审核并随应用提供的内容。",
                color = Muted,
            )
        }
        items(catalogUnits, key = { it.unitId }) { entry ->
            val unit = requireNotNull(units[entry.unitId])
            val progress = progressByUnit[entry.unitId] ?: LearningProgress()
            val visibleViewed = progress.viewedWords.count { wordId -> unit.words.any { it.id == wordId } }
            val percent = if (unit.words.isEmpty()) 0 else {
                ((visibleViewed * 100f) / unit.words.size).roundToInt()
            }
            val selected = entry.unitId == currentUnitId
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onUnit(entry.unitId) }
                    .testTag("unit-card-${entry.unitId}"),
                colors = CardDefaults.cardColors(containerColor = if (selected) Mint else CardColor),
                border = BorderStroke(1.dp, if (selected) Forest else Color(0xFFE1E4DC)),
                shape = RoundedCornerShape(22.dp),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Eyebrow("UNIT ${entry.unit.toString().padStart(2, '0')}")
                            Text(entry.title.zh, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                            Text(entry.title.en, color = Muted, fontSize = 13.sp)
                        }
                        Text(if (selected) "当前单元" else "打开 →", color = Forest, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "${unit.words.size} 个词 · ${unit.courseStages.size} 个阶段 · ${unit.fixedQuizzes.size} 组测验",
                        modifier = Modifier.padding(top = 14.dp),
                        color = Muted,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("阅读进度 $percent%", fontWeight = FontWeight.SemiBold)
                        Text("$visibleViewed / ${unit.words.size}", color = Muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewScreen(
    content: UnitContent,
    progress: LearningProgress,
    onStage: (CourseStage) -> Unit,
    onPractice: () -> Unit,
) {
    val visibleViewed = progress.viewedWords.count { wordId -> content.words.any { it.id == wordId } }
    val visibleQuizScores = progress.quizScores.keys.count { quizId -> content.fixedQuizzes.any { it.id == quizId } }
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("screen-home"),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card(
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFFDFCF8), Color(0xFFE4EEE7)),
                            ),
                        )
                        .padding(horizontal = 26.dp, vertical = 34.dp),
                ) {
                    Eyebrow("OFFLINE · UNIT ${content.unit}")
                    Text(
                        "从词根开始，\n把单词连成故事。",
                        modifier = Modifier.padding(top = 10.dp),
                        color = Ink,
                        fontFamily = FontFamily.Serif,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 41.sp,
                    )
                    Text(
                        "${content.words.size} 个核心词汇，按 ${content.courseStages.size} 个主题阶段循序学习。先读英文，在需要时展开中文，再用原书测验和本地动态练习巩固。",
                        modifier = Modifier.padding(top = 16.dp),
                        color = Muted,
                        lineHeight = 24.sp,
                    )
                    Row(
                        modifier = Modifier.padding(top = 22.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(onClick = { onStage(content.courseStages.first()) }) {
                            Text("继续学习")
                        }
                        OutlinedButton(onClick = onPractice) { Text("快速练习") }
                    }
                }
            }
        }
        item {
            val questionCount = content.fixedQuizzes.sumOf { it.questionCount }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard(content.words.size.toString(), "双语词条", Modifier.weight(1f))
                    MetricCard(content.roots.size.toString(), "拉丁词根", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard(questionCount.toString(), "固定测验题", Modifier.weight(1f))
                    MetricCard(
                        "$visibleQuizScores/${content.fixedQuizzes.size}",
                        "已完成测验",
                        Modifier.weight(1f),
                    )
                }
            }
        }
        item {
            val percent = if (content.words.isEmpty()) 0 else {
                ((visibleViewed * 100f) / content.words.size).roundToInt()
            }
            Card(colors = CardDefaults.cardColors(containerColor = Forest)) {
                Column(Modifier.padding(20.dp)) {
                    Text("${content.title.en} 学习进度", color = Color(0xFFB8D1C7), fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text("$percent%", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text("$visibleViewed / ${content.words.size}", color = Color.White)
                    }
                    Box(
                        Modifier.fillMaxWidth().padding(top = 10.dp).height(7.dp)
                            .background(Color.White.copy(alpha = .15f), CircleShape),
                    ) {
                        Box(
                            Modifier.fillMaxWidth(percent / 100f).height(7.dp)
                                .background(Sun, CircleShape),
                        )
                    }
                }
            }
        }
        item {
            Eyebrow("COURSE MAP")
            Text(
                "${content.courseStages.size} 阶段学习路径",
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text("全部开放，建议按顺序完成", color = Muted, fontSize = 13.sp)
        }
        items(content.courseStages, key = { it.id }) { stage ->
            val names = stage.groupIds.mapNotNull { id ->
                content.groups.find { it.id == id }?.label
            }.joinToString(" · ")
            StageCard(stage, names, onStage)
        }
        item {
            Text(
                "Content ${content.contentVersion} · 数据与进度仅保存在本机",
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                color = Muted,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun MetricCard(value: String, label: String, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = CardColor)) {
        Column(Modifier.padding(17.dp)) {
            Text(value, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Text(label, color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun StageCard(stage: CourseStage, names: String, onStage: (CourseStage) -> Unit) {
    val color = StageColors[stage.colorIndex]
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onStage(stage) },
        colors = CardDefaults.cardColors(containerColor = CardColor),
        border = BorderStroke(1.dp, color.copy(alpha = .2f)),
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).background(color, RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(stage.sequence.toString(), color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f).padding(horizontal = 15.dp)) {
                Text("STAGE ${stage.sequence}", color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(stage.title.zh, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(names, color = Muted, fontSize = 12.sp)
            }
            Text("→", color = color, fontSize = 22.sp)
        }
    }
}

@Composable
private fun LearnScreen(
    content: UnitContent,
    activeStage: CourseStage,
    globalChinese: Boolean,
    onStageChange: (CourseStage) -> Unit,
    onWordViewed: (String) -> Unit,
    onStartQuiz: () -> Unit,
    onSpeak: (String) -> Unit,
) {
    val groups = activeStage.groupIds.mapNotNull { id -> content.groups.find { it.id == id } }
    val stageColor = StageColors[activeStage.colorIndex]
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("screen-learn"),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Eyebrow("STAGE ${activeStage.sequence}")
            Text(
                activeStage.title.zh,
                fontFamily = FontFamily.Serif,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
            )
            Text("展开词条阅读例句与词源故事；中文默认隐藏，可按需查看。", color = Muted)
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(content.courseStages, key = { it.id }) { stage ->
                    val selected = stage.id == activeStage.id
                    if (selected) {
                        Button(onClick = { onStageChange(stage) }) { Text("${stage.sequence}. ${stage.title.zh}") }
                    } else {
                        OutlinedButton(onClick = { onStageChange(stage) }) { Text("${stage.sequence}. ${stage.title.zh}") }
                    }
                }
            }
        }
        items(groups, key = { it.id }) { group ->
            GroupCard(
                group = group,
                color = stageColor,
                globalChinese = globalChinese,
                onWordViewed = onWordViewed,
                onSpeak = onSpeak,
            )
        }
        item {
            Button(onClick = onStartQuiz, modifier = Modifier.fillMaxWidth()) {
                Text("开始 ${content.fixedQuizzes.first { it.id == activeStage.quizId }.title} →")
            }
        }
    }
}

@Composable
private fun GroupCard(
    group: WordGroup,
    color: Color,
    globalChinese: Boolean,
    onWordViewed: (String) -> Unit,
    onSpeak: (String) -> Unit,
) {
    var showGroupChinese by remember(group.id) { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = CardColor),
        shape = RoundedCornerShape(23.dp),
    ) {
        Column {
            Column(Modifier.fillMaxWidth().background(color).padding(22.dp)) {
                Text(
                    group.label,
                    color = Color.White,
                    fontFamily = FontFamily.Serif,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(group.title, color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    group.introduction.en,
                    modifier = Modifier.padding(top = 10.dp),
                    color = Color.White.copy(alpha = .86f),
                    lineHeight = 21.sp,
                )
                AnimatedVisibility(globalChinese || showGroupChinese) {
                    Text(
                        group.introduction.zh,
                        modifier = Modifier.padding(top = 12.dp),
                        color = Color.White,
                        lineHeight = 22.sp,
                    )
                }
                TextButton(
                    onClick = { showGroupChinese = !showGroupChinese },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                ) {
                    Text(if (globalChinese || showGroupChinese) "隐藏本段中文" else "显示本段中文")
                }
            }
            Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                group.words.forEachIndexed { index, word ->
                    WordCard(word, globalChinese, onWordViewed, onSpeak)
                    if (index != group.words.lastIndex) HorizontalDivider(color = Color(0xFFE1E5E1))
                }
            }
        }
    }
}

@Composable
private fun WordCard(
    word: WordEntry,
    globalChinese: Boolean,
    onWordViewed: (String) -> Unit,
    onSpeak: (String) -> Unit,
) {
    var expanded by remember(word.id) { mutableStateOf(false) }
    var showChinese by remember(word.id) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(vertical = 9.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable {
                expanded = !expanded
                if (expanded) onWordViewed(word.id)
            }.padding(vertical = 8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        word.headword,
                        fontFamily = FontFamily.Serif,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        word.pronunciation.display,
                        modifier = Modifier.padding(start = 10.dp, bottom = 2.dp),
                        color = Muted,
                        fontSize = 12.sp,
                    )
                }
                Text(word.definition.en, modifier = Modifier.padding(top = 7.dp), lineHeight = 21.sp)
                AnimatedVisibility(globalChinese || showChinese) {
                    Text(
                        word.definition.zh,
                        modifier = Modifier.padding(top = 8.dp),
                        color = ForestSecondary,
                    )
                }
            }
            TextButton(onClick = { onSpeak(word.headword) }) { Text("▶") }
            Text(if (expanded) "⌃" else "⌄", modifier = Modifier.padding(top = 9.dp), fontSize = 19.sp)
        }
        AnimatedVisibility(expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ContentBox("EXAMPLE · 例句", word.example, globalChinese || showChinese)
                OutlinedButton(onClick = { showChinese = !showChinese }) {
                    Text(if (globalChinese || showChinese) "隐藏本词全部中文" else "显示本词全部中文")
                }
                ContentBox("WORD STORY · 词源故事", word.note, globalChinese || showChinese)
            }
        }
    }
}

@Composable
private fun ContentBox(label: String, text: BilingualText, showChinese: Boolean) {
    Column(
        Modifier.fillMaxWidth().background(Color(0xFFF6F7F3), RoundedCornerShape(16.dp)).padding(16.dp),
    ) {
        Text(label, color = ForestSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(text.en, modifier = Modifier.padding(top = 7.dp), lineHeight = 22.sp)
        AnimatedVisibility(showChinese) {
            Text(text.zh, modifier = Modifier.padding(top = 10.dp), color = Ink, lineHeight = 22.sp)
        }
    }
}

@Composable
private fun QuizScreen(
    content: UnitContent,
    activeQuizId: String,
    progress: LearningProgress,
    onQuizChange: (String) -> Unit,
    onComplete: (String, Int) -> Unit,
) {
    val quiz = content.fixedQuizzes.first { it.id == activeQuizId }
    var answers by remember(activeQuizId) { mutableStateOf<Map<String, Set<String>>>(emptyMap()) }
    var submitted by remember(activeQuizId) { mutableStateOf(false) }
    val grade = if (submitted) gradeQuiz(quiz, answers) else null
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("screen-quiz"),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Eyebrow("FIXED QUIZZES")
            Text(quiz.title, fontFamily = FontFamily.Serif, fontSize = 36.sp, fontWeight = FontWeight.Bold)
            Text("保留原书题目。提交后即时显示得分与正确答案。", color = Muted)
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(content.fixedQuizzes, key = { it.id }) { item ->
                    if (item.id == activeQuizId) {
                        Button(onClick = { onQuizChange(item.id) }) { Text(item.title) }
                    } else {
                        OutlinedButton(onClick = { onQuizChange(item.id) }) { Text(item.title) }
                    }
                }
            }
        }
        quiz.sections.forEach { section ->
            item(key = "heading-${section.id}") {
                Text(
                    if (section.id == "main") quiz.title else "Part ${section.id.uppercase()}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(section.instruction, color = Muted)
            }
            items(section.questions, key = { it.id }) { question ->
                QuestionCard(
                    question = question,
                    choices = section.choicesFor(question),
                    selected = answers[question.id].orEmpty(),
                    submitted = submitted,
                    onSelect = { choiceId ->
                        val current = answers[question.id].orEmpty()
                        val next = if (question.answers.size > 1) {
                            if (choiceId in current) current - choiceId else current + choiceId
                        } else {
                            setOf(choiceId)
                        }
                        answers = answers + (question.id to next)
                    },
                )
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(17.dp)) {
                    val historical = progress.quizScores[quiz.id]
                    Text(
                        when {
                            grade != null -> "本次 ${grade.correct}/${grade.total} · ${grade.percent}%"
                            historical != null -> "历史最好：$historical%"
                            else -> "尚未提交"
                        },
                        fontWeight = FontWeight.Bold,
                    )
                    if (grade != null) {
                        Text(
                            "正确选项已标绿；错选已标红。每题下方均显示正确答案。",
                            modifier = Modifier.padding(top = 5.dp),
                            color = ForestSecondary,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        OutlinedButton(onClick = {
                            answers = emptyMap()
                            submitted = false
                        }) { Text("重置") }
                        Spacer(Modifier.width(9.dp))
                        Button(
                            onClick = {
                                submitted = true
                                val result = gradeQuiz(quiz, answers)
                                onComplete(quiz.id, result.percent)
                            },
                            enabled = !submitted,
                            modifier = Modifier.testTag("submit-quiz"),
                        ) { Text("提交答案") }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionCard(
    question: QuizQuestion,
    choices: List<QuizChoice>,
    selected: Set<String>,
    submitted: Boolean,
    onSelect: (String) -> Unit,
) {
    val answeredCorrectly = selected == question.answers
    val allowsMultiple = question.answers.size > 1
    val container = when {
        !submitted -> Color(0xFFF7F7F3)
        answeredCorrectly -> Color(0xFFEFF8F2)
        else -> Color(0xFFFFF2EF)
    }
    val border = when {
        !submitted -> Color.Transparent
        answeredCorrectly -> Correct.copy(alpha = .65f)
        else -> Wrong.copy(alpha = .65f)
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        border = BorderStroke(1.dp, border),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier.size(26.dp).background(Forest, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(question.number.toString(), color = Color.White, fontSize = 12.sp)
                }
                Text(
                    question.prompt,
                    modifier = Modifier.weight(1f).padding(start = 9.dp),
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 21.sp,
                )
            }
            choices.forEach { choice ->
                val isSelected = choice.id in selected
                val isAnswer = choice.id in question.answers
                val choiceBackground = when {
                    submitted && isAnswer -> CorrectBackground
                    submitted && isSelected -> WrongBackground
                    isSelected -> Mint
                    else -> Color.White
                }
                val choiceBorder = when {
                    submitted && isAnswer -> Correct
                    submitted && isSelected -> Wrong
                    isSelected -> Forest
                    else -> MaterialTheme.colorScheme.outline
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        .background(choiceBackground, RoundedCornerShape(12.dp))
                        .border(1.dp, choiceBorder, RoundedCornerShape(12.dp))
                        .selectable(
                            selected = isSelected,
                            enabled = !submitted,
                            role = if (allowsMultiple) Role.Checkbox else Role.RadioButton,
                            onClick = { onSelect(choice.id) },
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.fillMaxWidth().background(
                            color = Color.Transparent,
                            shape = RoundedCornerShape(12.dp),
                        ),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (allowsMultiple) {
                                Checkbox(checked = isSelected, onCheckedChange = null, enabled = !submitted)
                            } else {
                                RadioButton(selected = isSelected, onClick = null, enabled = !submitted)
                            }
                            Text(
                                "${choice.id}. ${choice.text}",
                                modifier = Modifier.weight(1f),
                                color = when {
                                    submitted && isAnswer -> Correct
                                    submitted && isSelected -> Wrong
                                    else -> Ink
                                },
                            )
                            if (submitted && isAnswer) Text("✓ 正确", color = Correct, fontWeight = FontWeight.Bold)
                            if (submitted && isSelected && !isAnswer) Text("× 你的选择", color = Wrong, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            if (submitted) {
                val correct = choices.filter { it.id in question.answers }
                    .joinToString("；") { "${it.id}. ${it.text}" }
                val selectedText = selected.sorted().joinToString("、")
                Text(
                    if (selected.isEmpty()) {
                        "本题未作答 · 正确答案：$correct"
                    } else {
                        "你的选择：$selectedText · 正确答案：$correct"
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                        .background(Color.White.copy(alpha = .8f), RoundedCornerShape(10.dp)).padding(10.dp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PracticeScreen(
    content: UnitContent,
    progress: LearningProgress,
    progressStore: ProgressStore?,
    onResult: (Boolean) -> Unit,
    onSentenceComplete: (String) -> Unit,
    onCopyPrompt: (String) -> Boolean,
    onSharePrompt: (String) -> Boolean,
) {
    var mode by remember { mutableStateOf(PracticeMode.Meaning) }
    var question by remember { mutableStateOf(makePracticeQuestion(mode, content)) }
    var textAnswer by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<Boolean?>(null) }

    fun selectMode(next: PracticeMode) {
        mode = next
        question = makePracticeQuestion(next, content)
        textAnswer = ""
        result = null
    }

    fun check(value: String) {
        if (result != null) return
        val correct = question.isCorrect(value)
        result = correct
        onResult(correct)
    }

    fun nextQuestion() {
        question = makePracticeQuestion(mode, content)
        textAnswer = ""
        result = null
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("screen-practice"),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        item {
            Eyebrow("DYNAMIC PRACTICE")
            Text("快速练习", fontFamily = FontFamily.Serif, fontSize = 36.sp, fontWeight = FontWeight.Bold)
            Text("题目完全在本地从已批准的 ${content.title.en} 数据生成。", color = Muted)
        }
        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 3,
            ) {
                PracticeMode.entries.forEach { item ->
                    val modifier = if (item == PracticeMode.Sentence) {
                        Modifier.testTag("practice-sentence-mode")
                    } else {
                        Modifier
                    }
                    if (item == mode) {
                        Button(onClick = { selectMode(item) }, modifier = modifier) { Text(item.label) }
                    } else {
                        OutlinedButton(onClick = { selectMode(item) }, modifier = modifier) { Text(item.label) }
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Forest)) {
                Row(
                    Modifier.fillMaxWidth().padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (mode == PracticeMode.Sentence) {
                        val visibleCompleted = progress.completedSentenceWords.count { wordId ->
                            content.words.any { it.id == wordId }
                        }
                        Column {
                            Text("已完成造句的词", color = Color(0xFFB8D1C7), fontSize = 12.sp)
                            Text(
                                "$visibleCompleted",
                                color = Color.White,
                                fontSize = 27.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Text("/ ${content.words.size}", color = Color.White)
                    } else {
                        val accuracy = if (progress.practiceTotal == 0) 0 else {
                            ((progress.practiceCorrect * 100f) / progress.practiceTotal).roundToInt()
                        }
                        Column {
                            Text("本地练习正确率", color = Color(0xFFB8D1C7), fontSize = 12.sp)
                            Text("$accuracy%", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("${progress.practiceCorrect} / ${progress.practiceTotal}", color = Color.White)
                    }
                }
            }
        }
        item {
            if (mode == PracticeMode.Sentence) {
                SentenceWorkshop(
                    content = content,
                    initialWordId = question.target.id,
                    progressStore = progressStore,
                    onSentenceComplete = onSentenceComplete,
                    onCopyPrompt = onCopyPrompt,
                    onSharePrompt = onSharePrompt,
                )
            } else {
                PracticeCard(
                    question = question,
                    textAnswer = textAnswer,
                    result = result,
                    onTextAnswer = { textAnswer = it },
                    onCheck = ::check,
                    onNext = ::nextQuestion,
                )
            }
        }
    }
}

@Composable
private fun PracticeCard(
    question: PracticeQuestion,
    textAnswer: String,
    result: Boolean?,
    onTextAnswer: (String) -> Unit,
    onCheck: (String) -> Unit,
    onNext: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = CardColor), shape = RoundedCornerShape(23.dp)) {
        Column(Modifier.padding(22.dp)) {
            Eyebrow(question.mode.name.uppercase())
            Text(
                question.prompt,
                modifier = Modifier.padding(top = 8.dp),
                fontFamily = FontFamily.Serif,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 34.sp,
            )
            Text(question.hint, modifier = Modifier.padding(top = 9.dp), color = Muted, lineHeight = 21.sp)
            if (question.options.isNotEmpty()) {
                question.options.forEach { option ->
                    OutlinedButton(
                        onClick = { onCheck(option.value) },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        enabled = result == null,
                    ) {
                        Text(option.label, modifier = Modifier.fillMaxWidth())
                    }
                }
            } else {
                OutlinedTextField(
                    value = textAnswer,
                    onValueChange = onTextAnswer,
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                    label = { Text("输入英文单词") },
                    singleLine = true,
                    enabled = result == null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onCheck(textAnswer) }),
                )
                Button(
                    onClick = { onCheck(textAnswer) },
                    modifier = Modifier.padding(top = 10.dp),
                    enabled = result == null && textAnswer.isNotBlank(),
                ) { Text("检查答案") }
            }
            if (result != null) {
                Text(
                    if (result) "回答正确！" else "正确答案：${question.answer}",
                    modifier = Modifier.padding(top = 15.dp),
                    color = if (result) Correct else Wrong,
                    fontWeight = FontWeight.Bold,
                )
                OutlinedButton(onClick = onNext, modifier = Modifier.padding(top = 8.dp)) {
                    Text("下一题 →")
                }
            }
        }
    }
}

@Composable
private fun SentenceWorkshop(
    content: UnitContent,
    initialWordId: String,
    progressStore: ProgressStore?,
    onSentenceComplete: (String) -> Unit,
    onCopyPrompt: (String) -> Boolean,
    onSharePrompt: (String) -> Boolean,
) {
    val initialIndex = content.words.indexOfFirst { it.id == initialWordId }.coerceAtLeast(0)
    var wordIndex by remember(initialWordId) { mutableIntStateOf(initialIndex) }
    val word = content.words[wordIndex]
    var work by remember(word.id) {
        mutableStateOf(progressStore?.loadSentenceWork(word.id) ?: SentenceWork())
    }
    var actionMessage by remember(word.id) { mutableStateOf("") }
    var localCheckRun by remember(word.id) { mutableStateOf(false) }
    val usageChecks = work.sentences.map { sentenceUsesTarget(it, word.headword) }
    val localCheck = checkSentenceWorkshop(work.sentences, word.headword)
    val readyForLocalCheck = work.sentences.all { it.isNotBlank() }
    val readyForReview = localCheckRun && usageChecks.all { it } && localCheck.sentencesAreDistinct

    fun persistWork(message: String) {
        progressStore?.saveSentenceWork(word.id, work)
        actionMessage = message
    }

    fun changeWord(nextIndex: Int) {
        progressStore?.saveSentenceWork(word.id, work)
        wordIndex = nextIndex
    }

    fun prepareReview(action: (String) -> Boolean, successMessage: String) {
        if (!readyForReview) return
        progressStore?.saveSentenceWork(word.id, work)
        onSentenceComplete(word.id)
        val succeeded = action(buildSentenceReviewPrompt(word, work.sentences))
        actionMessage = if (succeeded) successMessage else "当前预览环境不支持此系统操作"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CardColor),
        shape = RoundedCornerShape(23.dp),
        modifier = Modifier.testTag("sentence-workshop"),
    ) {
        Column(Modifier.padding(22.dp)) {
            Eyebrow("SENTENCE WORKSHOP · ${word.sequence} / ${content.words.size}")
            Text(
                word.headword,
                modifier = Modifier.padding(top = 7.dp),
                fontFamily = FontFamily.Serif,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(word.pronunciation.display, color = Muted, fontSize = 12.sp)
            Text(word.definition.en, modifier = Modifier.padding(top = 10.dp), lineHeight = 21.sp)
            Text(word.definition.zh, modifier = Modifier.padding(top = 5.dp), color = ForestSecondary)
            Text(
                "请用这个词写三个不同的英文句子。先在本机完成可解释的基础检查，再把语法、搭配、语义和自然度交给外部 AI 深入批改。",
                modifier = Modifier.padding(top = 14.dp),
                color = Muted,
                lineHeight = 21.sp,
            )

            work.sentences.forEachIndexed { index, sentence ->
                val used = usageChecks[index]
                OutlinedTextField(
                    value = sentence,
                    onValueChange = { value ->
                        val updated = work.sentences.toMutableList().also { it[index] = value }
                        work = work.copy(sentences = updated)
                        localCheckRun = false
                        actionMessage = "草稿有未保存的修改，请重新运行本机检查"
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                        .testTag("sentence-input-${index + 1}"),
                    label = { Text("句子 ${index + 1}") },
                    supportingText = {
                        Text(
                            when {
                                sentence.isBlank() -> "请输入包含 ${word.headword} 的完整句子"
                                used -> "✓ 已使用 ${word.headword}"
                                else -> "尚未使用完整单词 ${word.headword}"
                            },
                        )
                    },
                    isError = sentence.isNotBlank() && !used,
                    minLines = 2,
                    maxLines = 5,
                )
            }

            OutlinedButton(
                onClick = { persistWork("草稿和反馈已保存在本机") },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                Text("保存草稿")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))
            Text(
                "第一层 · 本机基础检查",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "完全离线检查目标词、句首大写、结尾标点、基本长度和三句是否重复；这些是机械检查，不代表语法或语义已经正确。",
                modifier = Modifier.padding(top = 5.dp),
                color = Muted,
                lineHeight = 20.sp,
            )
            Button(
                onClick = {
                    progressStore?.saveSentenceWork(word.id, work)
                    localCheckRun = true
                    actionMessage = if (localCheck.passed) {
                        "本机基础检查全部通过"
                    } else {
                        "本机检查发现 ${localCheck.issueCount} 项基础问题"
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).testTag("run-local-check"),
                enabled = readyForLocalCheck,
            ) {
                Text("运行本机基础检查")
            }
            if (localCheckRun) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                        .testTag("local-check-report"),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    localCheck.sentences.forEachIndexed { index, check ->
                        Text("句子 ${index + 1}", fontWeight = FontWeight.SemiBold)
                        LocalCheckLine(check.usesTarget, "包含完整目标词 ${word.headword}")
                        LocalCheckLine(check.startsWithCapital, "句首字母大写")
                        LocalCheckLine(check.endsWithPunctuation, "以 .、! 或 ? 结尾")
                        LocalCheckLine(check.hasBasicLength, "至少 3 个英文单词（当前 ${check.wordCount} 个）")
                    }
                    LocalCheckLine(localCheck.sentencesAreDistinct, "三个句子内容不同")
                    Text(
                        if (localCheck.passed) {
                            "✓ 基础检查通过，可以进入第二层深入批改。"
                        } else {
                            "基础检查只提示可确定的问题；建议修正后重新运行，也可交给第二层深入批改。"
                        },
                        modifier = Modifier.padding(top = 4.dp),
                        color = if (localCheck.passed) Correct else Wrong,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))
            Text("第二层 · 外部 AI 深入批改", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "先运行本机检查；三个句子都包含目标词且内容不同后，可复制结构化请求，或一键分享到已安装的 AI 应用。本应用不会联网或自行发送数据。",
                modifier = Modifier.padding(top = 5.dp),
                color = Muted,
                lineHeight = 20.sp,
            )
            Button(
                onClick = { prepareReview(onCopyPrompt, "批改请求已复制") },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).testTag("copy-review-prompt"),
                enabled = readyForReview,
            ) {
                Text("复制批改请求")
            }
            OutlinedButton(
                onClick = { prepareReview(onSharePrompt, "已打开系统分享面板") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                enabled = readyForReview,
            ) {
                Text("分享到 AI 应用")
            }

            OutlinedTextField(
                value = work.feedback,
                onValueChange = {
                    work = work.copy(feedback = it)
                    actionMessage = "反馈有未保存的修改"
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).testTag("sentence-feedback"),
                label = { Text("粘贴 AI 反馈（可选）") },
                minLines = 5,
                maxLines = 12,
            )
            if (actionMessage.isNotBlank()) {
                Text(
                    actionMessage,
                    modifier = Modifier.padding(top = 9.dp),
                    color = ForestSecondary,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = { changeWord((wordIndex - 1).coerceAtLeast(0)) },
                    enabled = wordIndex > 0,
                ) { Text("← 上一词") }
                TextButton(onClick = {
                    val candidates = content.words.indices.filter { it != wordIndex }
                    changeWord(candidates.random())
                }) { Text("随机换词") }
                OutlinedButton(
                    onClick = { changeWord((wordIndex + 1).coerceAtMost(content.words.lastIndex)) },
                    enabled = wordIndex < content.words.lastIndex,
                ) { Text("下一词 →") }
            }
        }
    }
}

@Composable
private fun LocalCheckLine(passed: Boolean, label: String) {
    Text(
        "${if (passed) "✓" else "△"} $label",
        color = if (passed) Correct else Wrong,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    )
}

private fun copyReviewPrompt(context: Context, prompt: String): Boolean = runCatching {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("词汇造句批改请求", prompt))
    Toast.makeText(context, "批改请求已复制", Toast.LENGTH_SHORT).show()
}.isSuccess

private fun shareReviewPrompt(context: Context, prompt: String): Boolean = runCatching {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "词汇造句批改请求")
        putExtra(Intent.EXTRA_TEXT, prompt)
    }
    context.startActivity(Intent.createChooser(sendIntent, "选择用于批改的应用"))
}.isSuccess

@Composable
private fun Eyebrow(text: String) {
    Text(
        text,
        color = ForestSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.3.sp,
    )
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun VocabularyAppPreview() {
    val context = LocalContext.current
    val library = remember(context) {
        runCatching { UnitContentRepository.load(context) }.getOrDefault(PreviewContent.library)
    }
    VocabularyTheme { VocabularyApp(library) }
}

private object PreviewContent {
    private val words = listOf("sunlit", "glimmer", "radiant", "daybreak")
        .mapIndexed { index, headword ->
            WordEntry(
                id = "u1-light-$headword",
                sequence = index + 1,
                headword = headword,
                pronunciation = Pronunciation("sample pronunciation"),
                definition = BilingualText("An original demonstration definition.", "原创演示释义。"),
                example = BilingualText("This original sentence uses $headword.", "这是一条例句演示。"),
                note = BilingualText("Replace this demonstration with content you may distribute.", "请替换为你有权分发的内容。"),
                group = "LIGHT",
            )
        }
    private val group = WordGroup(
        id = "u1-light",
        label = "LIGHT",
        title = "LIGHT · brightness",
        introduction = BilingualText("An original group about light.", "一个关于光的原创演示词组。"),
        words = words,
    )
    private val question = QuizQuestion(
        id = "quiz-1-1-a-1",
        number = 1,
        prompt = "Which word suggests the beginning of a new day?",
        answers = setOf("a"),
        choices = listOf(QuizChoice("a", "daybreak"), QuizChoice("b", "midnight")),
    )
    val unit = UnitContent(
        unitId = "unit-01",
        unit = 1,
        contentVersion = "2026.08.29-demo.1",
        title = BilingualText("Demo Unit", "演示单元"),
        courseStages = listOf(
            CourseStage(
                id = "u1-stage-1",
                sequence = 1,
                title = BilingualText("Light", "光"),
                groupIds = listOf("u1-light"),
                quizId = "quiz-1-1",
                colorIndex = 0,
            ),
        ),
        roots = listOf(group),
        thematicSections = emptyList(),
        fixedQuizzes = listOf(
            FixedQuiz(
                id = "quiz-1-1",
                title = "Quiz 1-1",
                sections = listOf(QuizSection("a", "closest_synonym", "Choose:", emptyList(), listOf(question))),
            ),
        ),
    )
    val library = ContentLibrary(
        catalog = ContentCatalog(
            catalogVersion = "preview",
            bookTitle = BilingualText("Clean Vocabulary Demo", "纯净词汇演示"),
            totalUnits = 1,
            units = listOf(
                CatalogUnit("unit-01", 1, unit.title, "units/unit-01.json", unit.contentVersion),
            ),
        ),
        units = mapOf(unit.unitId to unit),
    )
}
