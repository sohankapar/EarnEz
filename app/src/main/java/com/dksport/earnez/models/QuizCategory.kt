package com.dksport.earnez.models

import com.google.firebase.firestore.DocumentId

data class QuizCategory(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val iconUrl: String = "",
    val color: String = "#4CAF50",
    val questionCount: Int = 0,
    val totalPoints: Int = 0,
    val averageDifficulty: Double = 2.5,
    val isPremium: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)