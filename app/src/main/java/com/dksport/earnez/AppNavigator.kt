package com.dksport.earnez

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dksport.earnez.screens.*
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@SuppressLint("SuspiciousIndentation")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigator() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("earnez_prefs", Context.MODE_PRIVATE) }
    val savedCoins = sharedPrefs.getInt("coins", 0)
    val savedLoyaltyPoints = sharedPrefs.getInt("loyalty", 0)
    var coins by rememberSaveable { mutableStateOf(savedCoins) }
    var loyaltyPoints by rememberSaveable { mutableStateOf(savedLoyaltyPoints) }

    LaunchedEffect(coins) {
        sharedPrefs.edit().putInt("coins", coins).apply()
    }
    LaunchedEffect(loyaltyPoints) {
        sharedPrefs.edit().putInt("loyalty", loyaltyPoints).apply()
    }

    // Preload ads
    LaunchedEffect(Unit) {
        AdHelper.loadRewardedAd(context)
        AdHelper.loadInterstitialAd(context)
    }

    val auth = Firebase.auth
    val startDestination = if (auth.currentUser != null) "home" else "splash"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("splash") { SplashScreen(navController = navController) }
        composable("onboarding") { OnboardingScreen(navController = navController) }
        composable("register") {
            RegisterNewUserScreen(navController = navController) {
                navController.navigate("home") {
                    popUpTo("onboarding") { inclusive = true }
                    popUpTo("login") { inclusive = true }
                    popUpTo("register") { inclusive = true }
                }
            }
        }
        composable("login") {
            LoginScreen(navController = navController) {
                navController.navigate("home") {
                    popUpTo("onboarding") { inclusive = true }
                    popUpTo("login") { inclusive = true }
                    popUpTo("register") { inclusive = true }
                }
            }
        }
        composable("home") {
            HomeScreen(
                navController = navController,
                coins = coins,
                loyaltyPoints = loyaltyPoints,
                onCoinChange = { newCoins -> coins = newCoins },
                onLoyaltyChange = { newPoints -> loyaltyPoints = newPoints }
            )
        }
        composable("watch_ads") {
            WatchAdsScreen(
                navController = navController,
                coins = coins,
                loyaltyPoints = loyaltyPoints,
                onCoinChange = { newCoins -> coins = newCoins },
                onLoyaltyChange = { newPoints -> loyaltyPoints = newPoints }
            )
        }
        composable("quiz_categories") {
            QuizCategoriesScreen(navController = navController)
        }
        composable("quiz_play/{categoryId}") { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            QuizPlayScreen(
                navController = navController,
                categoryId = categoryId,
                onCoinChange = { newCoins -> coins = newCoins },
                onLoyaltyChange = { newPoints -> loyaltyPoints = newPoints },
                loyaltyPoints = loyaltyPoints
            )
        }
        composable("quiz_result/{score}/{total}") { backStackEntry ->
            val score = backStackEntry.arguments?.getString("score")?.toIntOrNull() ?: 0
            val total = backStackEntry.arguments?.getString("total")?.toIntOrNull() ?: 0
            QuizResultScreen(
                navController = navController,
                score = score,
                total = total
            )
        }
        composable("invite_friends") {
            ReferEarnScreen(navController = navController)
        }
        composable("redeem") {
            RedeemScreen(
                navController = navController,
                onCoinChange = { newCoins -> coins = newCoins },
                onLoyaltyChange = { newPoints -> loyaltyPoints = newPoints }
            )
        }
        composable("profile") {
            ProfileScreen(
                navController = navController,
                coins = coins,
                loyaltyPoints = loyaltyPoints,
                onCoinChange = { newCoins -> coins = newCoins },
                onLoyaltyChange = { newPoints -> loyaltyPoints = newPoints }
            )
        }
        composable("privacy_policy") {
            PrivacyPolicyScreen(navController = navController)
        }
        composable("redemption_history") {
            RedemptionHistoryScreen(navController = navController)
        }
        composable("leaderboard") {
            LeaderboardScreen(navController = navController)
        }
        composable("finance_game") {
            FinanceGameScreen(
                navController = navController,
                onCoinChange = { newCoins -> coins = newCoins },
                onLoyaltyChange = { newPoints -> loyaltyPoints = newPoints }
            )
        }
        composable("puzzle_game") {
            PuzzleGameScreen(
                navController = navController,
                onCoinChange = { newCoins -> coins = newCoins },
                onLoyaltyChange = { newPoints -> loyaltyPoints = newPoints }
            )
        }
        composable("info") {
            InfoScreen()
        }
    }
}