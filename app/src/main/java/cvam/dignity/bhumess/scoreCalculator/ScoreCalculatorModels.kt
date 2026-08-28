package cvam.dignity.bhumess

/**
 * Clean models for the CUET Score Calculator.
 * Focusing on IDs to ensure reliability.
 */

data class ScoreQuestionResult(
    val index: Int,
    val qId: String,
    val chosenOptionId: String,
    val correctAnswerId: String,
    val status: ScoreEvaluationStatus
)

enum class ScoreEvaluationStatus {
    CORRECT, WRONG, SKIPPED, DROPPED, KEY_MISSING
}

data class ScoreEvaluationReport(
    val subjectName: String = "",
    val score: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val skippedCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val questions: List<ScoreQuestionResult> = emptyList()
)
