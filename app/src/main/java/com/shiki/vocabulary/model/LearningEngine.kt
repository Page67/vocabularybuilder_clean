package com.shiki.vocabulary.model

import kotlin.math.roundToInt
import kotlin.random.Random

data class QuizGrade(
    val correct: Int,
    val total: Int,
) {
    val percent: Int = if (total == 0) 0 else ((correct * 100f) / total).roundToInt()
}

fun gradeQuiz(quiz: FixedQuiz, answers: Map<String, Set<String>>): QuizGrade {
    val questions = quiz.sections.flatMap { it.questions }
    return QuizGrade(
        correct = questions.count { answers[it.id].orEmpty() == it.answers },
        total = questions.size,
    )
}

enum class PracticeMode(val label: String) {
    Meaning("词义辨认"),
    Spelling("拼写"),
    Root("词根分类"),
    Cloze("例句填空"),
    Sentence("造句训练"),
}

data class PracticeOption(
    val label: String,
    val value: String,
)

data class PracticeQuestion(
    val mode: PracticeMode,
    val target: WordEntry,
    val prompt: String,
    val hint: String,
    val options: List<PracticeOption>,
    val answer: String,
) {
    fun isCorrect(value: String): Boolean = value.trim().equals(answer, ignoreCase = true)
}

fun makePracticeQuestion(
    mode: PracticeMode,
    content: UnitContent,
    random: Random = Random.Default,
): PracticeQuestion {
    require(content.words.size >= 4) { "At least four words are required for practice." }
    val target = content.words.random(random)
    return when (mode) {
        PracticeMode.Meaning -> {
            val distractors = content.words
                .filter { it.id != target.id }
                .shuffled(random)
                .take(3)
            PracticeQuestion(
                mode = mode,
                target = target,
                prompt = target.headword,
                hint = target.pronunciation.display,
                options = (distractors + target).shuffled(random).map {
                    PracticeOption(it.definition.en, it.id)
                },
                answer = target.id,
            )
        }

        PracticeMode.Root -> {
            val groups = content.words.map { it.group }.distinct()
            PracticeQuestion(
                mode = mode,
                target = target,
                prompt = target.headword,
                hint = "这个单词属于哪个词根或主题？",
                options = groups.shuffled(random).map { PracticeOption(it, it) },
                answer = target.group,
            )
        }

        PracticeMode.Cloze -> PracticeQuestion(
            mode = mode,
            target = target,
            prompt = target.example.en.replace(
                Regex(Regex.escape(target.headword), RegexOption.IGNORE_CASE),
                "_____",
            ),
            hint = target.definition.zh,
            options = emptyList(),
            answer = target.headword,
        )

        PracticeMode.Spelling -> PracticeQuestion(
            mode = mode,
            target = target,
            prompt = target.definition.zh,
            hint = target.pronunciation.display,
            options = emptyList(),
            answer = target.headword,
        )

        PracticeMode.Sentence -> PracticeQuestion(
            mode = mode,
            target = target,
            prompt = "用 ${target.headword} 造三个不同的英文句子。",
            hint = "${target.definition.en} · ${target.definition.zh}",
            options = emptyList(),
            answer = target.headword,
        )
    }
}

fun sentenceUsesTarget(sentence: String, headword: String): Boolean =
    Regex("\\b${Regex.escape(headword)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(sentence)

data class SentenceBasicCheck(
    val usesTarget: Boolean,
    val startsWithCapital: Boolean,
    val endsWithPunctuation: Boolean,
    val hasBasicLength: Boolean,
    val wordCount: Int,
) {
    val passed: Boolean = usesTarget && startsWithCapital && endsWithPunctuation && hasBasicLength
    val issueCount: Int = listOf(
        usesTarget,
        startsWithCapital,
        endsWithPunctuation,
        hasBasicLength,
    ).count { !it }
}

data class SentenceWorkshopCheck(
    val sentences: List<SentenceBasicCheck>,
    val sentencesAreDistinct: Boolean,
) {
    val issueCount: Int = sentences.sumOf { it.issueCount } + if (sentencesAreDistinct) 0 else 1
    val passed: Boolean = sentences.all { it.passed } && sentencesAreDistinct
}

fun checkSentenceWorkshop(sentences: List<String>, headword: String): SentenceWorkshopCheck {
    require(sentences.size == 3) { "Exactly three sentences are required." }
    val checks = sentences.map { sentence ->
        val trimmed = sentence.trim()
        val wordCount = EnglishWord.findAll(trimmed).count()
        SentenceBasicCheck(
            usesTarget = sentenceUsesTarget(trimmed, headword),
            startsWithCapital = trimmed.firstOrNull { it.isLetter() }?.isUpperCase() == true,
            endsWithPunctuation = trimmed
                .trimEnd('"', '\'', '”', '’', ')', ']', '}')
                .lastOrNull() in SentenceEndings,
            hasBasicLength = wordCount >= MinimumSentenceWords,
            wordCount = wordCount,
        )
    }
    val normalized = sentences.map(::normalizeSentenceForComparison)
    return SentenceWorkshopCheck(
        sentences = checks,
        sentencesAreDistinct = normalized.all { it.isNotBlank() } && normalized.distinct().size == sentences.size,
    )
}

private const val MinimumSentenceWords = 3
private val EnglishWord = Regex("[A-Za-z]+(?:['’-][A-Za-z]+)*")
private val SentenceEndings = setOf('.', '!', '?')

private fun normalizeSentenceForComparison(sentence: String): String = sentence
    .trim()
    .lowercase()
    .replace(Regex("\\s+"), " ")
    .trimEnd('.', '!', '?', '"', '\'', '”', '’')

fun buildSentenceReviewPrompt(word: WordEntry, sentences: List<String>): String {
    require(sentences.size == 3) { "Exactly three sentences are required." }
    require(sentences.all { it.isNotBlank() }) { "All three sentences must be present." }
    return """
        You are an English vocabulary writing tutor. Review three learner-written sentences that
        are intended to practise the target word below. Explain your feedback in Chinese, while
        keeping all corrected example sentences in English.

        Target word: ${word.headword}
        Pronunciation: ${word.pronunciation.display}
        English definition: ${word.definition.en}
        Chinese definition: ${word.definition.zh}

        Learner sentences:
        1. ${sentences[0].trim()}
        2. ${sentences[1].trim()}
        3. ${sentences[2].trim()}

        For each sentence:
        - Give a score from 0 to 10 for meaning, grammar, and naturalness.
        - State whether the target word is used with the intended meaning.
        - Identify grammar, collocation, register, or word-choice problems.
        - Provide a corrected, natural English sentence.
        - Briefly explain the correction in Chinese.

        Finish with an overall score out of 10 and one concise suggestion for mastering this word.
        Do not replace the target word with a different word unless you explain why its use is
        impossible in the learner's intended sentence.
    """.trimIndent()
}
