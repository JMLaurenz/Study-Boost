package com.example.study_boost

data class TaskModel(
    val id: Int,
    val subjectId: Int,
    val title: String,
    val description: String,
    val dueDate: String,
    val hours: Double,
    val status: String,
    val subjectCode: String,
    val subjectName: String,
    val colorHex: String,
    val difficulty: Int,
    val studiedSeconds: Long = 0
) {
    companion object {
        const val PENDING = "PENDING"
        const val COMPLETED = "COMPLETED"
    }
    fun urgency(now: Long = System.currentTimeMillis()): Double =
        Priority.score(difficulty, hours, DateRules.endOfDay(dueDate), now)
}

data class StudySession(val id: Int, val taskId: Int, val seconds: Int,
                        val date: String, val notes: String)
