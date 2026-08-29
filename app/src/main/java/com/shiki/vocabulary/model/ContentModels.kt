package com.shiki.vocabulary.model

data class BilingualText(
    val en: String,
    val zh: String,
)

data class Pronunciation(
    val display: String,
)

data class WordEntry(
    val id: String,
    val sequence: Int,
    val headword: String,
    val pronunciation: Pronunciation,
    val definition: BilingualText,
    val example: BilingualText,
    val note: BilingualText,
    val group: String,
)

data class WordGroup(
    val id: String,
    val label: String,
    val title: String,
    val introduction: BilingualText,
    val words: List<WordEntry>,
)

data class QuizChoice(
    val id: String,
    val text: String,
)

data class QuizQuestion(
    val id: String,
    val number: Int,
    val prompt: String,
    val answers: Set<String>,
    val choices: List<QuizChoice>,
)

data class QuizSection(
    val id: String,
    val kind: String,
    val instruction: String,
    val choiceBank: List<QuizChoice>,
    val questions: List<QuizQuestion>,
) {
    fun choicesFor(question: QuizQuestion): List<QuizChoice> =
        question.choices.ifEmpty { choiceBank }
}

data class FixedQuiz(
    val id: String,
    val title: String,
    val sections: List<QuizSection>,
) {
    val questionCount: Int get() = sections.sumOf { it.questions.size }
}

data class UnitContent(
    val unitId: String,
    val unit: Int,
    val contentVersion: String,
    val title: BilingualText,
    val courseStages: List<CourseStage>,
    val roots: List<WordGroup>,
    val thematicSections: List<WordGroup>,
    val fixedQuizzes: List<FixedQuiz>,
) {
    val groups: List<WordGroup> get() = roots + thematicSections
    val words: List<WordEntry> get() = groups.flatMap { it.words }.sortedBy { it.sequence }
}

data class CourseStage(
    val id: String,
    val sequence: Int,
    val title: BilingualText,
    val groupIds: List<String>,
    val quizId: String,
    val colorIndex: Int,
)

data class CatalogUnit(
    val unitId: String,
    val unit: Int,
    val title: BilingualText,
    val payloadPath: String,
    val contentVersion: String,
)

data class ContentCatalog(
    val catalogVersion: String,
    val bookTitle: BilingualText,
    val totalUnits: Int,
    val units: List<CatalogUnit>,
)

data class ContentLibrary(
    val catalog: ContentCatalog,
    val units: Map<String, UnitContent>,
) {
    fun unit(unitId: String): UnitContent = requireNotNull(units[unitId]) {
        "Catalog unit is not loaded: $unitId"
    }
}
