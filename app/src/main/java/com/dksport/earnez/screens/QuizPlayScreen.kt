package com.dksport.earnez.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.dksport.earnez.models.QuizQuestion
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizPlayScreen(
    navController: NavHostController,
    categoryId: String,
    onCoinChange: (Int) -> Unit,
    onLoyaltyChange: (Int) -> Unit,
    loyaltyPoints: Int
) {
    val context = LocalContext.current
    var questions by remember { mutableStateOf<List<QuizQuestion>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var categoryName by remember { mutableStateOf("") }

    // Fetch questions and category name
    LaunchedEffect(categoryId) {
        try {
            // Get category name
            Firebase.firestore.collection("quiz_categories")
                .document(categoryId)
                .get()
                .addOnSuccessListener { doc ->
                    categoryName = doc.getString("name") ?: "Quiz"
                }

            // Get questions
            Firebase.firestore.collection("quiz_questions")
                .whereEqualTo("categoryId", categoryId)
                .get()
                .addOnSuccessListener { result ->
                    questions = result.documents.mapNotNull { doc ->
                        val options = doc.get("options") as? List<*> ?: emptyList<Any>()
                        QuizQuestion(
                            id = doc.id,
                            categoryId = doc.getString("categoryId") ?: "",
                            question = doc.getString("question") ?: "",
                            options = options.filterIsInstance<String>(),
                            correctIndex = (doc.getLong("correctIndex") ?: 0).toInt()
                        )
                    }
                    loading = false
                }
                .addOnFailureListener {
                    loading = false
                    android.widget.Toast.makeText(context, "Failed to load questions", android.widget.Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            loading = false
        }
    }

    var currentIndex by rememberSaveable { mutableStateOf(0) }
    var score by rememberSaveable { mutableStateOf(0) }
    var selectedOptionIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var showNextButton by rememberSaveable { mutableStateOf(false) }
    val auth = Firebase.auth
    val db = Firebase.firestore
    val scope = rememberCoroutineScope()
    val coinsPerCorrectAnswer = 3
    val loyaltyPerCorrectAnswer = 1

    if (loading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (questions.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("No questions available for this quiz.", fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { navController.popBackStack() }) {
                Text("Go Back")
            }
        }
        return
    }

    val currentQuestion = questions[currentIndex]

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quiz: $categoryName") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            LinearProgressIndicator(
                progress = (currentIndex + 1) / questions.size.toFloat(),
                modifier = Modifier.fillMaxWidth().height(8.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Question ${currentIndex + 1}/${questions.size}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(currentQuestion.question, fontSize = 22.sp)
            Spacer(modifier = Modifier.height(24.dp))

            currentQuestion.options.forEachIndexed { index, option ->
                Button(
                    onClick = {
                        if (selectedOptionIndex == null) {
                            selectedOptionIndex = index
                            if (index == currentQuestion.correctIndex) {
                                score++
                            }
                            showNextButton = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when {
                            selectedOptionIndex == null -> MaterialTheme.colorScheme.primary
                            index == selectedOptionIndex -> if (index == currentQuestion.correctIndex) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                    enabled = selectedOptionIndex == null
                ) {
                    Text(option)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (showNextButton) {
                Button(
                    onClick = {
                        selectedOptionIndex = null
                        showNextButton = false
                        if (currentIndex + 1 < questions.size) {
                            currentIndex++
                        } else {
                            val earnedCoins = score * coinsPerCorrectAnswer
                            val earnedLoyalty = score * loyaltyPerCorrectAnswer
                            val userId = auth.currentUser?.uid
                            if (userId != null) {
                                scope.launch {
                                    try {
                                        db.collection("users").document(userId)
                                            .update(
                                                "coins", FieldValue.increment(earnedCoins.toLong()),
                                                "loyaltyPoints", FieldValue.increment(earnedLoyalty.toLong())
                                            )
                                            .await()
                                        onCoinChange(earnedCoins)
                                        onLoyaltyChange(loyaltyPoints + earnedLoyalty)
                                        android.widget.Toast.makeText(context, "You earned $earnedCoins coins and $earnedLoyalty loyalty!", android.widget.Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Failed to award quiz coins: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                }
                            } else {
                                android.widget.Toast.makeText(context, "Please log in to earn coins from quizzes.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                            navController.navigate("quiz_result/$score/${questions.size}") {
                                popUpTo("quiz_play/{categoryId}") { inclusive = true }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (currentIndex + 1 < questions.size) "Next Question" else "Finish Quiz")
                }
            }
        }
    }
}