package com.example.bodegalider.model

data class TaskItem(
    val id: String,
    val userId: String,
    val userName: String,
    val storeCode: String,
    val title: String,
    val isCompleted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
