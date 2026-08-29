package com.shiki.vocabulary.data

import android.content.Context
import com.shiki.vocabulary.model.BilingualText
import com.shiki.vocabulary.model.CatalogUnit
import com.shiki.vocabulary.model.ContentCatalog
import com.shiki.vocabulary.model.ContentLibrary
import com.shiki.vocabulary.model.CourseStage
import com.shiki.vocabulary.model.FixedQuiz
import com.shiki.vocabulary.model.Pronunciation
import com.shiki.vocabulary.model.QuizChoice
import com.shiki.vocabulary.model.QuizQuestion
import com.shiki.vocabulary.model.QuizSection
import com.shiki.vocabulary.model.UnitContent
import com.shiki.vocabulary.model.WordEntry
import com.shiki.vocabulary.model.WordGroup
import org.json.JSONArray
import org.json.JSONObject

object UnitContentRepository {
    private const val CatalogAssetName = "catalog.json"

    fun load(context: Context): ContentLibrary {
        val catalog = context.assets.readJson(CatalogAssetName).let(::parseCatalog)
        val units = catalog.units.associate { entry ->
            val content = parse(context.assets.readJson(entry.payloadPath))
            require(content.unitId == entry.unitId) { "Unit id does not match catalog: ${entry.unitId}" }
            require(content.unit == entry.unit) { "Unit number does not match catalog: ${entry.unitId}" }
            require(content.title == entry.title) { "Unit title does not match catalog: ${entry.unitId}" }
            require(content.contentVersion == entry.contentVersion) {
                "Content version does not match catalog: ${entry.unitId}"
            }
            validateUnit(content)
            entry.unitId to content
        }
        return ContentLibrary(catalog, units)
    }

    internal fun parse(root: JSONObject): UnitContent {
        val roots = root.getJSONArray("roots").objects().map { groupJson ->
            val label = groupJson.getString("label")
            WordGroup(
                id = groupJson.getString("id"),
                label = label,
                title = "$label · ${groupJson.getJSONObject("meaning").getString("en")}",
                introduction = groupJson.getJSONObject("introduction").bilingual(),
                words = groupJson.getJSONArray("words").objects().map { it.word(label) },
            )
        }
        val thematicSections = root.getJSONArray("thematic_sections").objects().map { groupJson ->
            val title = groupJson.getJSONObject("title").bilingual()
            WordGroup(
                id = groupJson.getString("id"),
                label = "STORIES",
                title = title.en,
                introduction = title,
                words = groupJson.getJSONArray("words").objects().map { it.word("MYTH") },
            )
        }
        return UnitContent(
            unitId = root.getString("unit_id"),
            unit = root.getInt("unit"),
            contentVersion = root.getString("content_version"),
            title = root.getJSONObject("title").bilingual(),
            courseStages = root.getJSONArray("course_stages").objects().map { stageJson ->
                CourseStage(
                    id = stageJson.getString("id"),
                    sequence = stageJson.getInt("sequence"),
                    title = stageJson.getJSONObject("title").bilingual(),
                    groupIds = stageJson.getJSONArray("group_ids").strings(),
                    quizId = stageJson.getString("quiz_id"),
                    colorIndex = stageJson.getInt("color_index"),
                )
            },
            roots = roots,
            thematicSections = thematicSections,
            fixedQuizzes = root.getJSONArray("fixed_quizzes").objects().map { it.quiz() },
        )
    }
}

private fun JSONObject.bilingual() = BilingualText(
    en = getString("en"),
    zh = getString("zh"),
)

private fun JSONObject.word(group: String) = WordEntry(
    id = getString("id"),
    sequence = getInt("sequence"),
    headword = getString("headword"),
    pronunciation = Pronunciation(getJSONObject("pronunciation").getString("display")),
    definition = getJSONObject("definition").bilingual(),
    example = getJSONObject("example").bilingual(),
    note = getJSONObject("note").bilingual(),
    group = group,
)

private fun JSONObject.quiz() = FixedQuiz(
    id = getString("id"),
    title = getString("title"),
    sections = getJSONArray("sections").objects().map { sectionJson ->
        QuizSection(
            id = sectionJson.getString("id"),
            kind = sectionJson.getString("kind"),
            instruction = sectionJson.getString("instruction"),
            choiceBank = sectionJson.optJSONArray("choice_bank")?.choices().orEmpty(),
            questions = sectionJson.getJSONArray("questions").objects().map { questionJson ->
                QuizQuestion(
                    id = questionJson.getString("id"),
                    number = questionJson.getInt("number"),
                    prompt = questionJson.getString("prompt"),
                    answers = when (val answer = questionJson.get("answer")) {
                        is JSONArray -> answer.strings().toSet()
                        is String -> setOf(answer)
                        else -> error("Unsupported quiz answer in ${questionJson.getString("id")}")
                    },
                    choices = questionJson.optJSONArray("choices")?.choices().orEmpty(),
                )
            },
        )
    },
)

private fun JSONArray.choices(): List<QuizChoice> = objects().map {
    QuizChoice(id = it.getString("id"), text = it.getString("text"))
}

private fun JSONArray.objects(): List<JSONObject> = List(length()) { getJSONObject(it) }

private fun JSONArray.strings(): List<String> = List(length()) { getString(it) }

private fun android.content.res.AssetManager.readJson(path: String): JSONObject {
    require(path.isNotBlank() && !path.startsWith('/') && !path.startsWith('\\')) {
        "Asset path must be relative: $path"
    }
    require(path.split('/', '\\').none { it == ".." || it.isBlank() }) {
        "Asset path escapes the content root: $path"
    }
    return open(path).bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) }
}

internal fun parseCatalog(root: JSONObject): ContentCatalog {
    val book = root.getJSONObject("book")
    val entries = root.getJSONArray("units").objects().map { unitJson ->
        require(
            unitJson.getString("approval_status") in
                setOf("approved", "pilot_test_authorized"),
        ) {
            "Only approved or explicitly authorized pilot units may be packaged."
        }
        CatalogUnit(
            unitId = unitJson.getString("unit_id"),
            unit = unitJson.getInt("unit"),
            title = unitJson.getJSONObject("title").bilingual(),
            payloadPath = unitJson.getString("payload_path"),
            contentVersion = unitJson.getString("content_version"),
        )
    }
    require(entries.isNotEmpty()) { "The content catalog is empty." }
    require(entries.map { it.unitId }.distinct().size == entries.size) { "Duplicate unit id in catalog." }
    require(entries.map { it.unit }.distinct().size == entries.size) { "Duplicate unit number in catalog." }
    require(entries.map { it.payloadPath }.distinct().size == entries.size) { "Duplicate payload path in catalog." }
    require(entries.map { it.unit } == entries.map { it.unit }.sorted()) { "Catalog units must be ordered." }
    entries.forEach { entry ->
        require(Regex("unit-\\d{2}").matches(entry.unitId)) { "Invalid stable unit id: ${entry.unitId}" }
        require(entry.payloadPath.startsWith("units/") && entry.payloadPath.endsWith(".json")) {
            "Unit payload must be under units/: ${entry.payloadPath}"
        }
        require(entry.payloadPath.split('/', '\\').none { it == ".." || it.isBlank() }) {
            "Unit payload path escapes the content root: ${entry.payloadPath}"
        }
    }
    return ContentCatalog(
        catalogVersion = root.getString("catalog_version"),
        bookTitle = book.getJSONObject("title").bilingual(),
        totalUnits = book.getInt("total_units"),
        units = entries,
    ).also { catalog ->
        require(catalog.totalUnits >= entries.size && entries.all { it.unit in 1..catalog.totalUnits }) {
            "Catalog unit number is outside the book range."
        }
    }
}

private fun validateUnit(content: UnitContent) {
    require(content.groups.isNotEmpty() && content.words.isNotEmpty()) { "${content.unitId} is empty." }
    require(content.courseStages.isNotEmpty() && content.fixedQuizzes.isNotEmpty()) {
        "${content.unitId} has no course stages or quizzes."
    }
    require(content.courseStages.map { it.id }.distinct().size == content.courseStages.size) {
        "Duplicate course stage id in ${content.unitId}."
    }
    require(content.courseStages.map { it.sequence } == (1..content.courseStages.size).toList()) {
        "Course stage sequence must be contiguous in ${content.unitId}."
    }
    require(content.groups.map { it.id }.distinct().size == content.groups.size) {
        "Duplicate group id in ${content.unitId}."
    }
    require(content.words.map { it.id }.distinct().size == content.words.size) {
        "Duplicate word id in ${content.unitId}."
    }
    val assignedGroups = content.courseStages.flatMap { it.groupIds }
    require(assignedGroups.size == assignedGroups.distinct().size && assignedGroups.toSet() == content.groups.map { it.id }.toSet()) {
        "Course stages must cover every group exactly once in ${content.unitId}."
    }
    val quizIds = content.fixedQuizzes.map { it.id }
    require(quizIds.distinct().size == quizIds.size) { "Duplicate quiz id in ${content.unitId}." }
    require(content.courseStages.all { it.quizId in quizIds }) { "Course stage references an unknown quiz." }
    val questions = content.fixedQuizzes.flatMap { quiz -> quiz.sections.flatMap { it.questions } }
    require(questions.map { it.id }.distinct().size == questions.size) { "Duplicate question id in ${content.unitId}." }
    questions.forEach { question ->
        val choices = content.fixedQuizzes.asSequence()
            .flatMap { it.sections.asSequence() }
            .first { question in it.questions }
            .choicesFor(question)
            .map { it.id }
            .toSet()
        require(question.answers.isNotEmpty() && question.answers.all { it in choices }) {
            "Question ${question.id} has an invalid answer."
        }
    }
}
