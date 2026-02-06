package com.dksport.earnez.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.dksport.earnez.AdHelper
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceGameScreen(
    navController: NavHostController,
    onCoinChange: (Int) -> Unit,
    onLoyaltyChange: (Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = Firebase.firestore
    val auth = Firebase.auth

    // High-Value Finance Content for 2026
    val quizData = remember {
        listOf(
            QuizQuestion("What is Asset Allocation?", listOf("Spreading investments", "Saving money", "Paying taxes"), 0),
            QuizQuestion("Which is a 'High-Yield' asset?", listOf("Fixed Deposit", "Index Funds", "Cash"), 1),
            QuizQuestion("What is a Credit Score used for?", listOf("Shopping", "Loan Eligibility", "Social Media"), 1),
            QuizQuestion("What is Inflation?", listOf("Price Decrease", "Currency Growth", "Price Increase"), 2)
        )
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var isFinished by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableIntStateOf(-1) }

    Scaffold(
        containerColor = Color(0xFF0A0A0A),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("FINANCE ARENA", fontSize = 16.sp, fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Rounded.Close, "Exit", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isFinished) {
                // Progress Bar
                LinearProgressIndicator(
                    progress = (currentIndex.toFloat() / quizData.size),
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(10.dp)),
                    color = Color(0xFF00C853),
                    trackColor = Color(0xFF1E1E1E)
                )

                Spacer(Modifier.height(40.dp))

                // Question Card
                Text(
                    text = quizData[currentIndex].question,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(32.dp))

                // Options
                quizData[currentIndex].options.forEachIndexed { index, option ->
                    OptionCard(
                        text = option,
                        isSelected = selectedOption == index,
                        onClick = {
                            selectedOption = index
                            scope.launch {
                                delay(300) // Small delay for visual feedback
                                if (index == quizData[currentIndex].correctIndex) score++

                                if (currentIndex < quizData.size - 1) {
                                    currentIndex++
                                    selectedOption = -1
                                } else {
                                    isFinished = true
                                }
                            }
                        }
                    )
                }

                Spacer(Modifier.weight(1f))

                // Reward Trigger for Help
                TextButton(
                    onClick = {
                        AdHelper.showRewardedAd(context, onRewardEarned = {
                            Toast.makeText(context, "Correct answer is: ${quizData[currentIndex].options[quizData[currentIndex].correctIndex]}", Toast.LENGTH_LONG).show()
                        })
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF00C853))
                ) {
                    Icon(Icons.Rounded.Lightbulb, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Unlock Answer Hint (Watch Ad)")
                }

            } else {
                // Game Over State
                GameOverSection(score, quizData.size, auth, db, scope, onCoinChange, onLoyaltyChange, navController)
            }
        }
    }
}

@Composable
fun OptionCard(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) Color(0xFF00C853).copy(alpha = 0.2f) else Color(0xFF161618))
            .border(
                1.dp,
                if (isSelected) Color(0xFF00C853) else Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Text(text, color = if (isSelected) Color(0xFF00C853) else Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun GameOverSection(
    score: Int,
    total: Int,
    auth: com.google.firebase.auth.FirebaseAuth,
    db: com.google.firebase.firestore.FirebaseFirestore,
    scope: kotlinx.coroutines.CoroutineScope,
    onCoinChange: (Int) -> Unit,
    onLoyaltyChange: (Int) -> Unit,
    navController: NavHostController
) {
    val context = LocalContext.current
    val earnedCoins = score * 25 // Increased rewards for 2026
    val earnedLoyalty = score * 2

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Rounded.EmojiEvents, null, modifier = Modifier.size(100.dp), tint = Color(0xFFFFD700))
        Text("Investment Mastery Complete!", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        Spacer(Modifier.height(8.dp))
        Text("You scored $score out of $total", color = Color.Gray)

        Spacer(Modifier.height(32.dp))

        // Result Card
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color(0xFF161618)).padding(24.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                ResultItem("Coins", "+$earnedCoins", Color.Yellow)
                ResultItem("Loyalty", "+$earnedLoyalty", Color(0xFF8E2DE2))
            }
        }

        Spacer(Modifier.height(40.dp))

        Button(
            onClick = {
                scope.launch {
                    val uid = auth.currentUser?.uid ?: return@launch
                    try {
                        db.collection("users").document(uid).update(
                            "coins", FieldValue.increment(earnedCoins.toLong()),
                            "loyaltyPoints", FieldValue.increment(earnedLoyalty.toLong())
                        ).await()
                        onCoinChange(earnedCoins)
                        onLoyaltyChange(earnedLoyalty)
                        AdHelper.showInterstitialAd(context)
                        navController.popBackStack()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error saving progress", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Claim Rewards & Exit", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ResultItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 12.sp, color = Color.Gray)
    }
}

data class QuizQuestion(val question: String, val options: List<String>, val correctIndex: Int)