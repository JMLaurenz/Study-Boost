package com.example.study_boost

data class TaskModel(
    val id: Int,
    val title: String,
    val subjectName: String,
    val status: String // "PENDING" or "COMPLETED"
)