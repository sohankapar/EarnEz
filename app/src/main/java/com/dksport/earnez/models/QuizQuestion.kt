package com.dksport.earnez.models

data class QuizQuestion(
    val id: String = "",
    val categoryId: String = "",
    val question: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Int = -1
) {
    // Helper function to check if answer is correct
    fun isAnswerCorrect(selectedIndex: Int): Boolean {
        return selectedIndex == correctIndex
    }
}