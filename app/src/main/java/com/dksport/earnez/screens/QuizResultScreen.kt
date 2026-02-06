package com.dksport.earnez.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.dksport.earnez.AdHelper

@Composable
fun QuizResultScreen(navController: NavHostController, score: Int, total: Int) {
    val coinsEarned = score * 3
    val loyaltyEarned = score
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        AdHelper.showInterstitialAd(
            context = context,
            onAdDismissed = { /* Do nothing */ },
            onAdFailed = { /* Do nothing */ }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Quiz Complete!", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("You scored $score out of $total!", fontSize = 24.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("You earned $coinsEarned coins!", fontSize = 20.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("+$loyaltyEarned loyalty points!", fontSize = 18.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                navController.navigate("quiz_categories") {
                    popUpTo("quiz_result/{score}/{total}") { inclusive = true }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Play Again")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                navController.navigate("home") {
                    popUpTo("quiz_result/{score}/{total}") { inclusive = true }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Go to Home")
        }
    }
}