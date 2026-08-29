package com.shiki.vocabulary.data

import android.content.Context
import android.content.SharedPreferences

data class LearningProgress(
    val viewedWords: Set<String> = emptySet(),
    val quizScores: Map<String, Int> = emptyMap(),
    val practiceCorrect: Int = 0,
    val practiceTotal: Int = 0,
    val completedSentenceWords: Set<String> = emptySet(),
) {
    fun markViewed(wordId: String): LearningProgress =
        copy(viewedWords = viewedWords + wordId)

    fun recordQuiz(quizId: String, percent: Int): LearningProgress = copy(
        quizScores = quizScores + (quizId to maxOf(percent, quizScores[quizId] ?: 0)),
    )

    fun recordPractice(correct: Boolean): LearningProgress = copy(
        practiceCorrect = practiceCorrect + if (correct) 1 else 0,
        practiceTotal = practiceTotal + 1,
    )

    fun completeSentencePractice(wordId: String): LearningProgress = copy(
        completedSentenceWords = completedSentenceWords + wordId,
    )
}

data class SentenceWork(
    val sentences: List<String> = List(3) { "" },
    val feedback: String = "",
)

object QuizScoreCodec {
    fun encode(scores: Map<String, Int>): String = scores.entries
        .sortedBy { it.key }
        .joinToString("|") { "${it.key}=${it.value}" }

    fun decode(value: String): Map<String, Int> = value
        .split('|')
        .mapNotNull { item ->
            val separator = item.lastIndexOf('=')
            if (separator <= 0) return@mapNotNull null
            val score = item.substring(separator + 1).toIntOrNull() ?: return@mapNotNull null
            item.substring(0, separator) to score.coerceIn(0, 100)
        }
        .toMap()
}

internal fun mergeProgress(current: LearningProgress, legacy: LearningProgress): LearningProgress = LearningProgress(
    viewedWords = current.viewedWords + legacy.viewedWords,
    quizScores = (current.quizScores.keys + legacy.quizScores.keys).associateWith { quizId ->
        maxOf(current.quizScores[quizId] ?: 0, legacy.quizScores[quizId] ?: 0)
    },
    practiceCorrect = maxOf(current.practiceCorrect, legacy.practiceCorrect),
    practiceTotal = maxOf(current.practiceTotal, legacy.practiceTotal),
    completedSentenceWords = current.completedSentenceWords + legacy.completedSentenceWords,
)

internal data class ProgressMigration(
    val progress: LearningProgress,
    val textValues: Map<String, String>,
)

internal fun planProgressMigration(
    current: LearningProgress,
    currentTextValues: Map<String, String>,
    legacy: LearningProgress,
    legacyTextValues: Map<String, String>,
    alreadyMigrated: Boolean,
): ProgressMigration? {
    if (alreadyMigrated) return null
    val additions = legacyTextValues.filter { (key, value) ->
        value.isNotBlank() && currentTextValues[key].isNullOrBlank()
    }
    return ProgressMigration(mergeProgress(current, legacy), additions)
}

internal fun decodeProgress(values: Map<String, *>): LearningProgress {
    fun stringSet(key: String): Set<String> = (values[key] as? Set<*>)
        .orEmpty()
        .filterIsInstance<String>()
        .toSet()
    return LearningProgress(
        viewedWords = stringSet("viewed_words"),
        quizScores = QuizScoreCodec.decode(values["quiz_scores"] as? String ?: ""),
        practiceCorrect = ((values["practice_correct"] as? Number)?.toInt() ?: 0).coerceAtLeast(0),
        practiceTotal = ((values["practice_total"] as? Number)?.toInt() ?: 0).coerceAtLeast(0),
        completedSentenceWords = stringSet("sentence_completed"),
    )
}

internal fun progressStoreName(unitId: String): String {
    require(Regex("unit-\\d{2}").matches(unitId)) { "Invalid stable unit id: $unitId" }
    return "vocabulary-progress-$unitId"
}

class ProgressStore(context: Context, unitId: String) {
    private val preferences = context.getSharedPreferences(
        progressStoreName(unitId),
        Context.MODE_PRIVATE,
    )

    init {
        if (unitId == UnitOneId) migrateLegacyUnitOne(context)
    }

    fun load(): LearningProgress = preferences.loadProgress()

    fun save(progress: LearningProgress) {
        preferences.edit().putProgress(progress).apply()
    }

    fun loadSentenceWork(wordId: String): SentenceWork = SentenceWork(
        sentences = List(3) { index ->
            preferences.all[sentenceKey(wordId, index)] as? String ?: ""
        },
        feedback = preferences.all[feedbackKey(wordId)] as? String ?: "",
    )

    fun saveSentenceWork(wordId: String, work: SentenceWork) {
        require(work.sentences.size == 3) { "Exactly three sentence drafts are required." }
        preferences.edit()
            .putString(sentenceKey(wordId, 0), work.sentences[0])
            .putString(sentenceKey(wordId, 1), work.sentences[1])
            .putString(sentenceKey(wordId, 2), work.sentences[2])
            .putString(feedbackKey(wordId), work.feedback)
            .apply()
    }

    private fun sentenceKey(wordId: String, index: Int) = "sentence_${wordId}_$index"
    private fun feedbackKey(wordId: String) = "sentence_feedback_$wordId"

    private fun migrateLegacyUnitOne(context: Context) {
        val legacy = context.getSharedPreferences(LegacyUnitOneStore, Context.MODE_PRIVATE)
        val currentValues = preferences.all
        val legacyValues = legacy.all
        val migration = planProgressMigration(
            current = decodeProgress(currentValues),
            currentTextValues = currentValues.sentenceTextValues(),
            legacy = decodeProgress(legacyValues),
            legacyTextValues = legacyValues.sentenceTextValues(),
            alreadyMigrated = currentValues[MigrationMarker] == true,
        ) ?: return
        val editor = preferences.edit().putProgress(migration.progress)
        migration.textValues.forEach { (key, value) -> editor.putString(key, value) }
        check(editor.putBoolean(MigrationMarker, true).commit()) { "Could not migrate Unit 1 progress." }
    }

    private companion object {
        const val UnitOneId = "unit-01"
        const val LegacyUnitOneStore = "vocabulary-progress-2026.08.26-unit1.1"
        const val MigrationMarker = "migrated_from_content_version_key_v1"
        const val ViewedWords = "viewed_words"
        const val QuizScores = "quiz_scores"
        const val PracticeCorrect = "practice_correct"
        const val PracticeTotal = "practice_total"
        const val SentenceCompleted = "sentence_completed"
    }
}

private fun SharedPreferences.loadProgress(): LearningProgress = decodeProgress(all)

private fun Map<String, *>.sentenceTextValues(): Map<String, String> = mapNotNull { (key, value) ->
    if ((key.startsWith("sentence_") || key.startsWith("sentence_feedback_")) && value is String) {
        key to value
    } else {
        null
    }
}.toMap()

private fun SharedPreferences.Editor.putProgress(progress: LearningProgress): SharedPreferences.Editor =
    putStringSet("viewed_words", progress.viewedWords)
        .putString("quiz_scores", QuizScoreCodec.encode(progress.quizScores))
        .putInt("practice_correct", progress.practiceCorrect)
        .putInt("practice_total", progress.practiceTotal)
        .putStringSet("sentence_completed", progress.completedSentenceWords)

class CurrentUnitStore(context: Context) {
    private val preferences = context.getSharedPreferences("vocabulary-app-state", Context.MODE_PRIVATE)

    fun load(availableUnitIds: Set<String>): String? =
        (preferences.all[CurrentUnitId] as? String)?.takeIf { it in availableUnitIds }

    fun save(unitId: String) {
        preferences.edit().putString(CurrentUnitId, unitId).apply()
    }

    private companion object {
        const val CurrentUnitId = "current_unit_id"
    }
}
