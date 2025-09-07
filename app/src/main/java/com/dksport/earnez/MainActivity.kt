package com.dksport.earnez

import android.annotation.SuppressLint
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.dksport.earnez.ui.theme.EarnezTheme
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

// Main Activity
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        FirebaseFirestore.getInstance()
        MobileAds.initialize(this)
        AdHelper.loadRewardedAd(this)
        AdHelper.loadInterstitialAd(this)
        createFirestoreRedemptionOptions() // Initialize redemption options

        setContent {
            EarnezTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNavigator()
                }
            }
        }

        // Check daily streak on app start
        checkDailyStreak()
    }

    private fun createFirestoreRedemptionOptions() {
        val db = FirebaseFirestore.getInstance()
        val options = listOf(
            hashMapOf(
                "title" to "eSewa Redemption",
                "description" to "20000 coins = 1$",
                "coins_required" to 20000,
                "type" to "esewa"
            ),
            hashMapOf(
                "title" to "PayPal Cashout",
                "description" to "25000 coins = 1$",
                "coins_required" to 25000,
                "type" to "paypal"
            ),
            hashMapOf(
                "title" to "Amazon Gift Card",
                "description" to "30000 coins = $10 Gift Card",
                "coins_required" to 30000,
                "type" to "giftcard"
            )
        )

        options.forEach { option ->
            db.collection("redemption_options")
                .whereEqualTo("title", option["title"])
                .get()
                .addOnSuccessListener { docs ->
                    if (docs.isEmpty) {
                        db.collection("redemption_options").add(option)
                    }
                }
        }

        // Create app info document
        val appInfo = hashMapOf(
            "title" to "Redemption Information",
            "content" to "• Minimum redemption: 20,000 coins\n" +
                    "• Redemptions process within 24-48 hours\n" +
                    "• Daily ad watch limit: 5 times per day\n" +
                    "• Contact support@earnez.com for issues"
        )

        db.collection("app_info").document("redeem_info")
            .set(appInfo, SetOptions.merge())
    }

    private fun checkDailyStreak() {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user != null) {
            val db = FirebaseFirestore.getInstance()
            val userRef = db.collection("users").document(user.uid)

            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            db.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val streakData = snapshot.get("streak") as? Map<*, *> ?: mapOf(
                    "lastLogin" to 0L,
                    "streakCount" to 0,
                    "lastClaimed" to 0L
                )

                val lastLogin = streakData["lastLogin"] as? Long ?: 0L
                val streakCount = streakData["streakCount"] as? Int ?: 0
                val lastClaimed = streakData["lastClaimed"] as? Long ?: 0L

                val calendar = Calendar.getInstance().apply { timeInMillis = lastLogin }
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                val nextDay = calendar.timeInMillis

                val newStreakCount = when {
                    lastLogin == 0L -> 1
                    today >= nextDay && today < nextDay + 86400000 -> streakCount + 1
                    today >= nextDay + 86400000 -> 1
                    else -> streakCount
                }

                // Only update streak if not already updated today
                if (lastLogin < today) {
                    val newStreak = mapOf(
                        "lastLogin" to today,
                        "streakCount" to newStreakCount,
                        "lastClaimed" to lastClaimed // Preserve last claimed date
                    )
                    transaction.update(userRef, "streak", newStreak)
                }

                mapOf(
                    "streakCount" to newStreakCount
                )
            }.addOnSuccessListener { result ->
                val streak = result["streakCount"] as Int
                Log.d("Streak", "Daily streak updated: $streak days")
            }.addOnFailureListener {
                Log.e("Streak", "Error updating streak", it)
            }
        }
    }
}

data class QuizCategory(
    val id: String = "",
    val name: String = "",
    val imageUrl: String = ""
)

data class QuizQuestion(
    val id: String = "",
    val categoryId: String = "",
    val question: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Int = 0
)
data class Feature(
    val title: String,
    val icon: ImageVector,
    val route: String,
    val color: Color
)
data class MotivationalQuote(
    val text: String,
    val author: String = "Unknown"
)
data class LeaderboardUser(
    val username: String,
    val coins: Int,
    val loyaltyPoints: Int,
    val userId: String
)

data class RedemptionOption(
    val id: String,
    val title: String,
    val description: String,
    val coinsRequired: Int,
    val type: String
)

@Composable
fun BannerAdView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val adView = remember {
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = "ca-app-pub-7794881051916625/5046740670"
            loadAd(AdRequest.Builder().build())
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            adView.destroy()
        }
    }

    AndroidView(
        factory = { adView },
        modifier = modifier.fillMaxWidth()
    )
}

object AdHelper {
    internal var rewardedAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null
    private const val REWARDED_AD_ID = "ca-app-pub-7794881051916625/2323781516"
    private const val INTERSTITIAL_AD_ID = "ca-app-pub-7794881051916625/4891662157"
    private val adLoadListeners = mutableListOf<() -> Unit>()

    fun addAdLoadListener(listener: () -> Unit) {
        adLoadListeners.add(listener)
    }

    fun removeAdLoadListener(listener: () -> Unit) {
        adLoadListeners.remove(listener)
    }

    fun loadRewardedAd(context: Context) {
        rewardedAd = null
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(context, REWARDED_AD_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAd = ad
                adLoadListeners.forEach { it() }
                Log.d("AdHelper", "Rewarded ad loaded")
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                rewardedAd = null
                Log.e("AdHelper", "Rewarded ad failed to load: ${loadAdError.message}")
            }
        })
    }

    fun loadInterstitialAd(context: Context) {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context, INTERSTITIAL_AD_ID, adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.d("AdHelper", "Interstitial ad loaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                    Log.e("AdHelper", "Interstitial ad failed to load: ${loadAdError.message}")
                }
            })
    }

    fun showInterstitialAd(
        context: Context,
        onAdDismissed: () -> Unit,
        onAdFailed: () -> Unit
    ) {
        interstitialAd?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    onAdDismissed()
                    loadInterstitialAd(context)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    onAdFailed()
                }
            }
            ad.show(context as Activity)
        } ?: run {
            onAdFailed()
            loadInterstitialAd(context)
        }
    }

    fun showRewardedAd(
        context: Context,
        onRewardEarned: () -> Unit,
        onAdDismissed: () -> Unit,
        onAdFailed: () -> Unit
    ) {
        rewardedAd?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    onAdDismissed()
                    loadRewardedAd(context) // Preload next ad
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    onAdFailed()
                }

                override fun onAdShowedFullScreenContent() {
                    rewardedAd = null
                }
            }

            ad.show(context as Activity) {
                onRewardEarned()
            }
        } ?: run {
            onAdFailed()
            loadRewardedAd(context) // Try to load if not available
        }
    }
}

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

    NavHost(navController, startDestination = startDestination) {
        composable("splash") { SplashScreen(navController) }
        composable("onboarding") { OnboardingScreen(navController) }
        composable("register") {
            RegisterNewUserScreen(navController) {
                navController.navigate("home") {
                    popUpTo("onboarding") { inclusive = true }
                    popUpTo("login") { inclusive = true }
                    popUpTo("register") { inclusive = true }
                }
            }
        }
        composable("login") {
            LoginScreen(navController) {
                navController.navigate("home") {
                    popUpTo("onboarding") { inclusive = true }
                    popUpTo("login") { inclusive = true }
                    popUpTo("register") { inclusive = true }
                }
            }
        }
        composable("home") {
            HomeScreen(
                navController,
                coins,
                loyaltyPoints,
                { newCoins -> coins = newCoins },
                { newPoints -> loyaltyPoints = newPoints }
            )
        }
        composable("watch_ads") {
            WatchAdsScreen(
                navController,
                coins,
                loyaltyPoints,
                { newCoins -> coins = newCoins },
                { newPoints -> loyaltyPoints = newPoints }
            )
        }
        composable("quiz_categories") { QuizCategoriesScreen(navController) }
        composable("quiz_play/{categoryId}") { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            QuizPlayScreen(
                navController,
                categoryId,
                { newCoins -> coins = newCoins },
                { newPoints -> loyaltyPoints = newPoints },
                loyaltyPoints
            )
        }
        composable("quiz_result/{score}/{total}") { backStackEntry ->
            val score = backStackEntry.arguments?.getString("score")?.toIntOrNull() ?: 0
            val total = backStackEntry.arguments?.getString("total")?.toIntOrNull() ?: 0
            QuizResultScreen(navController, score, total)
        }
        composable("Invite Friends") { ReferEarnScreen(navController) }
        composable("redeem") {
            RedeemScreen(
                navController,
                coins,
                loyaltyPoints,
                { newCoins -> coins = newCoins },
                { newPoints -> loyaltyPoints = newPoints }
            )
        }
        composable("profile") {
            ProfileScreen(
                navController,
                coins,
                loyaltyPoints,
                { newCoins -> coins = newCoins },
                { newPoints -> loyaltyPoints = newPoints }
            )
        }
        composable("privacy_policy") { PrivacyPolicyScreen(navController) }
        composable("redemption_history") { RedemptionHistoryScreen(navController) }
        composable("leaderboard") { LeaderboardScreen(navController) }
        composable("finance_game") {
            FinanceGameScreen(
                navController,
                { newCoins -> coins = newCoins },
                { newPoints -> loyaltyPoints = newPoints }
            )
        }
        composable("puzzle_game") {
            PuzzleGameScreen(
                navController,
                { newCoins -> coins = newCoins },
                { newPoints -> loyaltyPoints = newPoints }
            )
        }
        composable("info") { InfoScreen(navController) } // New info screen

    }
}

@Composable
fun SplashScreen(navController: NavHostController) {
    LaunchedEffect(Unit) {
        delay(2000)
        val auth = Firebase.auth
        if (auth.currentUser != null) {
            navController.navigate("home") {
                popUpTo("splash") { inclusive = true }
            }
        } else {
            navController.navigate("onboarding") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = rememberAsyncImagePainter("https://via.placeholder.com/150x150.png?text=App+Logo"),
                contentDescription = "App Logo",
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("EarnEz", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
fun OnboardingScreen(navController: NavHostController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Welcome to EarnEz!", fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { navController.navigate("register") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign Up")
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = { navController.navigate("login") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }
        Spacer(modifier = Modifier.height(32.dp))
        TextButton(onClick = { navController.navigate("privacy_policy") }) {
            Text("Privacy Policy", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterNewUserScreen(navController: NavHostController, onSuccess: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var enteredReferralCode by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val auth = Firebase.auth
    val db = Firebase.firestore
    val scope = rememberCoroutineScope()
    val signUpBonus = 0 // Sign-up bonus
    val referralBonus = 500L

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Create New Account", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = enteredReferralCode,
            onValueChange = { enteredReferralCode = it },
            label = { Text("Referral Code (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                errorMessage = null
                if (email.isBlank() || password.isBlank() || username.isBlank()) {
                    errorMessage = "Please fill all required fields."
                    return@Button
                }
                isLoading = true
                scope.launch {
                    try {
                        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                        val uid = authResult.user?.uid ?: throw Exception("User UID not found")
                        val userReferralCode = generateReferralCode()

                        var referredByCode: String? = null
                        if (enteredReferralCode.isNotBlank()) {
                            if (enteredReferralCode == userReferralCode) {
                                throw Exception("You can't refer yourself!")
                            }
                            val referrerDocs = db.collection("users")
                                .whereEqualTo("referralCode", enteredReferralCode)
                                .get().await()
                            if (referrerDocs.isEmpty) {
                                throw Exception("Invalid referral code.")
                            }
                            referredByCode = enteredReferralCode
                        }

                        // Create user document
                        val userData = hashMapOf(
                            "email" to email,
                            "username" to username,
                            "coins" to if (referredByCode != null) signUpBonus + referralBonus else signUpBonus,
                            "referralCode" to userReferralCode,
                            "referredBy" to referredByCode,
                            "referrals" to emptyList<String>(),
                            "loyaltyPoints" to 100, // Initial loyalty points
                            "streak" to mapOf(
                                "lastLogin" to Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }.timeInMillis,
                                "streakCount" to 1,
                                "lastClaimed" to 0L
                            )
                        )
                        db.collection("users").document(uid).set(userData, SetOptions.merge()).await()

                        // Update referrer if needed
                        referredByCode?.let { refCode ->
                            db.collection("users")
                                .whereEqualTo("referralCode", refCode)
                                .get().await()
                                .documents.firstOrNull()?.let { referrerDoc ->
                                    val referrerId = referrerDoc.id
                                    // Add referral to referrer's list
                                    db.collection("users").document(referrerId)
                                        .update(
                                            "referrals",
                                            FieldValue.arrayUnion(uid)
                                        ).await()
                                    // Award bonus coins to referrer
                                    db.collection("users").document(referrerId)
                                        .update(
                                            "coins",
                                            FieldValue.increment(referralBonus)
                                        ).await()
                                }
                        }
                        Toast.makeText(context, "Registration successful!", Toast.LENGTH_SHORT).show()
                        onSuccess()
                    } catch (e: Exception) {
                        errorMessage = "Registration failed: ${e.message}"
                        Log.e("RegisterUser", "Firestore Error", e)
                        if (e is FirebaseFirestoreException) {
                            Log.e("FirestoreError", "Code: ${e.code}, Message: ${e.message}")
                        }
                    }
                    finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            else Text("Register")
        }
        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp), textAlign = TextAlign.Center)
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = { navController.navigate("login") }) {
            Text("Already have an account? Login")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavHostController, onSuccess: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val auth = Firebase.auth
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Login to Your Account", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                errorMessage = null
                if (email.isBlank() || password.isBlank()) {
                    errorMessage = "Email and password cannot be empty."
                    return@Button
                }
                isLoading = true
                scope.launch {
                    try {
                        auth.signInWithEmailAndPassword(email, password).await()
                        Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()
                        onSuccess()
                    } catch (e: Exception) {
                        errorMessage = e.message ?: "Login failed."
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                        Log.e("LoginScreen", "Login error: ${e.message}", e)
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            else Text("Login")
        }
        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp), textAlign = TextAlign.Center)
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = { navController.navigate("register") }) {
            Text("Don't have an account? Sign Up")
        }
    }
}

@SuppressLint("SuspiciousIndentation")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    coins: Int,
    loyaltyPoints: Int,
    onCoinChange: (Int) -> Unit,
    onLoyaltyChange: (Int) -> Unit
) {
    val auth = Firebase.auth
    val db = Firebase.firestore
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Fixed coin synchronization with real-time listener
    LaunchedEffect(auth.currentUser?.uid) {
        auth.currentUser?.uid?.let { uid ->
            try {
                val docRef = db.collection("users").document(uid)
                docRef.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w("HomeScreen", "Listen failed.", error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        val firebaseCoins = snapshot.getLong("coins")?.toInt() ?: 0
                        val firebaseLoyalty = snapshot.getLong("loyaltyPoints")?.toInt() ?: 0

                        if (firebaseCoins != coins) {
                            onCoinChange(firebaseCoins)
                        }
                        if (firebaseLoyalty != loyaltyPoints) {
                            onLoyaltyChange(firebaseLoyalty)
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Failed to load data: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("HomeScreen", "Error fetching data: ${e.message}")
            }
        }
    }

    // Daily bonus state
    var showDailyBonus by remember { mutableStateOf(false) }
    var dailyBonusAmount by remember { mutableStateOf(0) }
    var adAvailable by remember { mutableStateOf(false) }

    // Ad availability listener
    DisposableEffect(Unit) {
        val listener = { adAvailable = AdHelper.rewardedAd != null }
        AdHelper.addAdLoadListener(listener)
        onDispose { AdHelper.removeAdLoadListener(listener) }
    }

    // Check for daily bonus
    LaunchedEffect(Unit) {
        val user = auth.currentUser
        if (user != null) {
            try {
                val doc = db.collection("users").document(user.uid).get().await()
                val streakData = doc.get("streak") as? Map<*, *> ?: mapOf(
                    "lastClaimed" to 0L,
                    "streakCount" to 1
                )

                val lastClaimed = streakData["lastClaimed"] as? Long ?: 0L
                val streakCount = streakData["streakCount"] as? Int ?: 1

                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                // Calculate bonus amount (100 coins per streak day)
                val bonus = streakCount * 100
                dailyBonusAmount = bonus

                // Show dialog if not claimed today
                if (lastClaimed < today) {
                    showDailyBonus = true
                }
            } catch (e: Exception) {
                Log.e("HomeScreen", "Error checking daily bonus", e)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.navigate("profile") }) {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = "Profile",
                    modifier = Modifier.size(36.dp)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Coins: $coins",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Loyalty: $loyaltyPoints",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(onClick = { navController.navigate("watch_ads") }) {
                    Icon(
                        Icons.Default.AddCircle,
                        contentDescription = "Get More Coins",
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        val quotes = remember {
            listOf(
                MotivationalQuote("Every coin earned is a step toward your dreams", "EarnEz Team"),
                MotivationalQuote("Small efforts repeated daily lead to big results"),
                MotivationalQuote("Your earning potential is limitless"),
                MotivationalQuote("Turn your spare time into spare money"),
                MotivationalQuote("Consistency is the key to financial freedom")
            )
        }
        val randomQuote by remember { mutableStateOf(quotes.random()) }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "\"${randomQuote.text}\"",
                    style = MaterialTheme.typography.bodyLarge,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "- ${randomQuote.author}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            }
        }

        val features = listOf(
            Feature(
                "Watch & Collect Points",
                Icons.Default.PlayCircle,
                "watch_ads",
                Color(0xFFFF9800)
            ),
            Feature("Play Quizzes", Icons.Default.Quiz, "quiz_categories", Color(0xFF4CAF50)),
            Feature("Finance Game", Icons.Default.MonetizationOn, "finance_game", Color(0xFF2196F3)),
            Feature("Puzzle Game", Icons.Default.Quiz, "puzzle_game", Color(0xFF9C27B0)),
            Feature("Redeem", Icons.Default.Store, "redeem", Color(0xFFFFC107)),
            Feature("Leaderboard", Icons.Default.Leaderboard, "leaderboard", Color(0xFFE91E63)),
            Feature("Information", Icons.Default.Info, "info", Color(0xFF795548)),
            Feature("Invite Friends", Icons.Default.Share, "Invite Friends", Color(0xFF2196F3))

        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(features) { feature ->
                FeatureCard(
                    feature = feature,
                    onClick = { navController.navigate(feature.route) })
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Daily Bonus Dialog
        if (showDailyBonus) {
            AlertDialog(
                onDismissRequest = { showDailyBonus = false },
                title = { Text("Daily Bonus Available!") },
                text = { Text("Watch a short video to claim your $dailyBonusAmount coins daily bonus!") },
                confirmButton = {
                    Button(
                        onClick = {
                            if (adAvailable) {
                                AdHelper.showRewardedAd(
                                    context = context,
                                    onRewardEarned = {
                                        // Award bonus coins
                                        val userId = auth.currentUser?.uid
                                        if (userId != null) {
                                            scope.launch {
                                                try {
                                                    // Update last claimed date
                                                    val claimTime = Calendar.getInstance().timeInMillis

                                                    db.collection("users").document(userId).update(
                                                        "coins", FieldValue.increment(dailyBonusAmount.toLong()),
                                                        "streak.lastClaimed", claimTime,
                                                        "loyaltyPoints", FieldValue.increment(10) // Add loyalty points
                                                    ).await()

                                                    onCoinChange(coins + dailyBonusAmount)
                                                    onLoyaltyChange(loyaltyPoints + 10)
                                                    Toast.makeText(context, "$dailyBonusAmount coins + 10 loyalty claimed!", Toast.LENGTH_SHORT).show()

                                                    // Close dialog
                                                    showDailyBonus = false
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Failed to claim bonus", Toast.LENGTH_SHORT).show()
                                                    Log.e("DailyBonus", "Claim error: ${e.message}")
                                                }
                                            }
                                        }
                                    },
                                    onAdDismissed = {
                                        // Don't close dialog on dismiss without claim
                                    },
                                    onAdFailed = {
                                        Toast.makeText(context, "Ad failed to load", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            } else {
                                Toast.makeText(context, "Ad not ready. Trying to load...", Toast.LENGTH_SHORT).show()
                                AdHelper.loadRewardedAd(context)
                            }
                        }
                    ) {
                        Text("Claim Bonus")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDailyBonus = false }) {
                        Text("Maybe Later")
                    }
                }
            )
        }

        BannerAdView()
    }
}

@Composable
fun WatchAdsScreen(
    navController: NavHostController,
    coins: Int,
    loyaltyPoints: Int,
    onCoinChange: (Int) -> Unit,
    onLoyaltyChange: (Int) -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    var adAvailable by remember { mutableStateOf(false) }
    val adReward = 50L
    val loyaltyReward = 5
    var adWatchesToday by remember { mutableStateOf(0) }
    var maxWatchesReached by remember { mutableStateOf(false) }
    val auth = Firebase.auth
    val db = Firebase.firestore
    val scope = rememberCoroutineScope()

    // Check daily ad watches
    LaunchedEffect(auth.currentUser?.uid) {
        auth.currentUser?.uid?.let { userId ->
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val docRef = db.collection("users").document(userId)
                .collection("daily_limits").document(today.toString())

            docRef.get().addOnSuccessListener { doc ->
                adWatchesToday = doc.getLong("ad_watches")?.toInt() ?: 0
                maxWatchesReached = adWatchesToday >= 5
            }
        }
    }

    // Ad availability state
    DisposableEffect(Unit) {
        val listener = { adAvailable = AdHelper.rewardedAd != null }
        AdHelper.addAdLoadListener(listener)
        onDispose { AdHelper.removeAdLoadListener(listener) }
    }

    // Load ad on start
    LaunchedEffect(Unit) {
        AdHelper.loadRewardedAd(context)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {


        Text("Watch & Collect Points!", fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Watch a short video ad to earn $adReward points and $loyaltyReward loyalty points!", fontSize = 16.sp, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Today: $adWatchesToday/5 ads watched", fontSize = 14.sp)
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (maxWatchesReached) {
                    Toast.makeText(context, "Daily limit reached (5 ads per day)", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (!adAvailable) {
                    Toast.makeText(context, "Ad not ready yet. Loading...", Toast.LENGTH_SHORT).show()
                    AdHelper.loadRewardedAd(context)
                    return@Button
                }

                isLoading = true
                AdHelper.showRewardedAd(
                    context = context,
                    onRewardEarned = {
                        // Update coins and loyalty
                        val userId = auth.currentUser?.uid
                        if (userId != null) {
                            scope.launch {
                                try {
                                    // Update daily ad watches
                                    val today = Calendar.getInstance().apply {
                                        set(Calendar.HOUR_OF_DAY, 0)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }.timeInMillis

                                    val docRef = db.collection("users").document(userId)
                                        .collection("daily_limits").document(today.toString())

                                    val data = hashMapOf(
                                        "ad_watches" to FieldValue.increment(1),
                                        "date" to today
                                    )
                                    docRef.set(data, SetOptions.merge()).await()

                                    // Update coins and loyalty
                                    db.collection("users").document(userId).update(
                                        "coins", FieldValue.increment(adReward),
                                        "loyaltyPoints", FieldValue.increment(loyaltyReward.toLong())
                                    ).await()

                                    onCoinChange(coins + adReward.toInt())
                                    onLoyaltyChange(loyaltyPoints + loyaltyReward)
                                    adWatchesToday++
                                    maxWatchesReached = adWatchesToday >= 5
                                    Toast.makeText(context, "+$adReward Coins +$loyaltyReward Loyalty!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to update: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    },
                    onAdDismissed = {
                        isLoading = false
                        AdHelper.loadRewardedAd(context) // Preload next
                    },
                    onAdFailed = {
                        isLoading = false
                        Toast.makeText(context, "Ad failed. Trying again...", Toast.LENGTH_SHORT).show()
                        AdHelper.loadRewardedAd(context)
                    }
                )
            },
            enabled = !isLoading && adAvailable && !maxWatchesReached,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            else Text(if (maxWatchesReached) "Daily Limit Reached" else "Watch Ad Now")
        }

        Spacer(modifier = Modifier.height(32.dp))
        OutlinedButton(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth()) {
            Text("Back to Home")
        }
        Spacer(modifier = Modifier.height(16.dp))

        BannerAdView()
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizCategoriesScreen(navController: NavHostController) {
    val context = LocalContext.current
    var categories by remember { mutableStateOf<List<QuizCategory>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    // Fetch categories from Firestore
    LaunchedEffect(Unit) {
        try {
            Firebase.firestore.collection("quiz_categories")
                .get()
                .addOnSuccessListener { result ->
                    categories = result.documents.map { doc ->
                        QuizCategory(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            imageUrl = doc.getString("imageUrl") ?: ""
                        )
                    }
                    loading = false
                }
                .addOnFailureListener {
                    loading = false
                    Toast.makeText(context, "Failed to load categories", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Quiz Categories") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                })
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (categories.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No categories available", fontSize = 18.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.padding(16.dp)) {
                    items(categories) { category ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            onClick = { navController.navigate("quiz_play/${category.id}") }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (category.imageUrl.isNotEmpty()) {
                                    Image(
                                        painter = rememberAsyncImagePainter(category.imageUrl),
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                }
                                Text(
                                    text = category.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
        BannerAdView()
    }
}

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
                    Toast.makeText(context, "Failed to load questions", Toast.LENGTH_SHORT).show()
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
            TopAppBar(title = { Text("Quiz: $categoryName") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                })
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
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
                                        Toast.makeText(context, "You earned $earnedCoins coins and $earnedLoyalty loyalty!", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Failed to award quiz coins: ${e.message}", Toast.LENGTH_LONG).show()
                                        Log.e("QuizPlay", "Error awarding coins: ${e.message}")
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Please log in to earn coins from quizzes.", Toast.LENGTH_SHORT).show()
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
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Quiz Complete!", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("You scored $score out of $total!", fontSize = 24.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("You earned $coinsEarned coins!", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("+$loyaltyEarned loyalty points!", fontSize = 18.sp, color = MaterialTheme.colorScheme.secondary)
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

fun generateReferralCode(): String {
    return UUID.randomUUID().toString().substring(0, 6).uppercase()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferEarnScreen(navController: NavHostController) {
    val context = LocalContext.current
    val auth = Firebase.auth
    val db = Firebase.firestore
    val userId = auth.currentUser?.uid
    var referralCode by remember { mutableStateOf("Loading...") }
    var referralCount by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }


    LaunchedEffect(userId) {
        if (userId != null) {
            try {
                val document = db.collection("users").document(userId).get().await()
                if (document.exists()) {
                    referralCode = document.getString("referralCode") ?: "N/A"
                    val referrals = document.get("referrals") as? List<*> ?: emptyList<Any>()
                    referralCount = referrals.size
                } else {
                    referralCode = "Error: User data not found"
                    Toast.makeText(context, "User data not found in Firestore.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                referralCode = "Error loading code"
                Toast.makeText(context, "Failed to load referral data: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("ReferEarnScreen", "Error loading referral data: ${e.message}", e)
            } finally {
                loading = false
            }
        } else {
            Toast.makeText(context, "Please log in to see your referral details.", Toast.LENGTH_LONG).show()
            navController.navigate("login") { popUpTo("home") { inclusive = true } }
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Refer & Earn") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {



            Text("Refer & Gets points!", fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))

            if (loading) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Loading referral data...")
            } else {
                Text("Your Referral Code:", fontSize = 18.sp)
                Text(referralCode, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Text("You've successfully referred $referralCount people!", fontSize = 18.sp, textAlign = TextAlign.Center)
                Text("Earn 500 coins for each successful referral!", fontSize = 14.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (referralCode != "N/A" && referralCode.isNotBlank() && referralCode != "Loading..." && referralCode != "Error loading code") {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "Join EarnEz and earn coins! Use my referral code: $referralCode to get a bonus on signup!")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share your referral code via"))
                        } else {
                            Toast.makeText(context, "Referral code not available yet. Please wait.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loading && referralCode.isNotBlank() && referralCode != "N/A" && referralCode != "Error loading code"
                ) {
                    Text("Share Code")
                }
            }
            Spacer(Modifier.height(32.dp))
            OutlinedButton(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth()) {
                Text("Back to Home")
            }
            Spacer(modifier = Modifier.height(16.dp))

            BannerAdView()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedeemScreen(
    navController: NavHostController,
    coins: Int,
    loyaltyPoints: Int,
    onCoinChange: (Int) -> Unit,
    onLoyaltyChange: (Int) -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = Firebase.firestore
    var redemptionOptions by remember { mutableStateOf<List<RedemptionOption>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var selectedOption by remember { mutableStateOf<RedemptionOption?>(null) }
    var accountInfo by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // Fetch redemption options
    LaunchedEffect(Unit) {
        db.collection("redemption_options")
            .get()
            .addOnSuccessListener { result ->
                redemptionOptions = result.documents.map { doc ->
                    RedemptionOption(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        coinsRequired = doc.getLong("coins_required")?.toInt() ?: 0,
                        type = doc.getString("type") ?: ""
                    )
                }
                loading = false
            }
            .addOnFailureListener {
                loading = false
                Toast.makeText(context, "Failed to load options", Toast.LENGTH_SHORT).show()
            }
    }

    if (selectedOption != null) {
        AlertDialog(
            onDismissRequest = { selectedOption = null },
            title = { Text("Redeem ${selectedOption!!.title}") },
            text = {
                Column {
                    Text("Required: ${selectedOption!!.coinsRequired} coins")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = accountInfo,
                        onValueChange = { accountInfo = it },
                        label = {
                            Text(
                                when (selectedOption!!.type) {
                                    "esewa" -> "eSewa Number"
                                    "paypal" -> "PayPal Email"
                                    else -> "Your Email"
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (accountInfo.isBlank()) {
                            Toast.makeText(context, "Please enter account info", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (coins < selectedOption!!.coinsRequired) {
                            Toast.makeText(context, "Insufficient coins", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val userId = auth.currentUser?.uid
                        if (userId != null) {
                            scope.launch {
                                try {
                                    val redemptionData = hashMapOf(
                                        "userId" to userId,
                                        "optionId" to selectedOption!!.id,
                                        "coinsRedeemed" to selectedOption!!.coinsRequired,
                                        "accountInfo" to accountInfo,
                                        "status" to "pending",
                                        "timestamp" to FieldValue.serverTimestamp()
                                    )
                                    db.collection("redemptions").add(redemptionData).await()

                                    // Deduct coins
                                    db.collection("users").document(userId)
                                        .update("coins", FieldValue.increment(-selectedOption!!.coinsRequired.toLong()))
                                        .await()

                                    onCoinChange(coins - selectedOption!!.coinsRequired)
                                    message = "Redemption submitted!"
                                    selectedOption = null
                                    accountInfo = ""
                                } catch (e: Exception) {
                                    message = "Failed: ${e.message}"
                                }
                            }
                        }
                    }
                ) {
                    Text("Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedOption = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Redeem Coins", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { navController.navigate("info") }) {
                Icon(Icons.Default.Info, contentDescription = "Info")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (redemptionOptions.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No redemption options available")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f)
            ) {
                items(redemptionOptions) { option ->
                    Card(
                        onClick = { selectedOption = option },
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(option.title, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(option.description)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("${option.coinsRequired} coins", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        Text(message, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(16.dp))

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { navController.navigate("redemption_history") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View Redemption History")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Home")

        }


    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoScreen(navController: NavHostController) {
    val context = LocalContext.current
    val db = Firebase.firestore
    var infoContent by remember { mutableStateOf("Loading information...") }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        db.collection("app_info").document("redeem_info")
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    infoContent = doc.getString("content") ?: "No information available."
                } else {
                    infoContent = "Information not found."
                }
                loading = false
            }
            .addOnFailureListener {
                infoContent = "Failed to load information."
                loading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Redemption Information") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Text(
                    text = infoContent,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedemptionHistoryScreen(navController: NavHostController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = Firebase.firestore
    var history by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            try {
                val result = db.collection("redemptions")
                    .whereEqualTo("userId", uid)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get().await()

                history = result.documents.mapNotNull {
                    it.data?.toMutableMap()?.apply {
                        put("id", it.id)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading history: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("RedemptionHistory", "Error: ${e.message}")
            } finally {
                loading = false
            }
        } else {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Redemption History") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                history.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No redemption history found", fontSize = 18.sp)
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        items(history) { item ->
                            val amount = item["coinsRedeemed"]?.toString() ?: "0"
                            val status = item["status"]?.toString() ?: "pending"
                            val optionId = item["optionId"]?.toString() ?: ""
                            val timestamp = item["timestamp"] as? com.google.firebase.Timestamp
                            val date = timestamp?.toDate()?.toString()?.substring(0, 16) ?: "Unknown date"

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "$amount Coins",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                        Text(
                                            text = status.replaceFirstChar { it.uppercaseChar() },
                                            color = when (status) {
                                                "pending" -> MaterialTheme.colorScheme.secondary
                                                "approved" -> MaterialTheme.colorScheme.primary
                                                "rejected" -> MaterialTheme.colorScheme.error
                                                else -> MaterialTheme.colorScheme.onSurface
                                            },
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Option ID: $optionId", fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Date: $date", fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavHostController,
    coins: Int,
    loyaltyPoints: Int,
    onCoinChange: (Int) -> Unit,
    onLoyaltyChange: (Int) -> Unit
) {
    val auth = Firebase.auth
    val db = Firebase.firestore
    val currentUser = auth.currentUser
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf("Loading...") }
    var userEmail by remember { mutableStateOf("Loading...") }
    var userReferralCode by remember { mutableStateOf("Loading...") }
    var streakCount by remember { mutableStateOf(0) }
    var lastClaimedDate by remember { mutableStateOf("Never") }
    var loading by remember { mutableStateOf(true) }

    // Fixed coin synchronization with real-time listener
    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            try {
                val docRef = db.collection("users").document(uid)
                docRef.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w("ProfileScreen", "Listen failed.", error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        val firebaseCoins = snapshot.getLong("coins")?.toInt() ?: 0
                        val firebaseLoyalty = snapshot.getLong("loyaltyPoints")?.toInt() ?: 0

                        if (firebaseCoins != coins) {
                            onCoinChange(firebaseCoins)
                        }
                        if (firebaseLoyalty != loyaltyPoints) {
                            onLoyaltyChange(firebaseLoyalty)
                        }

                        // Update streak data
                        val streakData = snapshot.get("streak") as? Map<*, *>
                        streakCount = streakData?.get("streakCount") as? Int ?: 0

                        // Update last claimed date
                        val lastClaimed = streakData?.get("lastClaimed") as? Long ?: 0L
                        lastClaimedDate = if (lastClaimed > 0) {
                            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(lastClaimed))
                        } else {
                            "Never"
                        }
                    }
                }

                // Initial load
                val document = docRef.get().await()
                if (document.exists()) {
                    username = document.getString("username") ?: "N/A"
                    userEmail = currentUser.email ?: "N/A"
                    userReferralCode = document.getString("referralCode") ?: "N/A"
                    loading = false
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load user data", Toast.LENGTH_SHORT).show()
                loading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Your Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            if (loading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Loading profile data...")
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = "Username", modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Username: $username", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Email, contentDescription = "Email", modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Email: $userEmail", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MonetizationOn, contentDescription = "Coins", modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Coins: $coins", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, contentDescription = "Loyalty", modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Loyalty Points: $loyaltyPoints", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, contentDescription = "Streak", modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Daily Streak: $streakCount days", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, contentDescription = "Last Claimed", modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Last Claimed: $lastClaimedDate", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VpnKey, contentDescription = "Referral Code", modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Referral Code: $userReferralCode", fontSize = 18.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        scope.launch {
                            try {
                                auth.signOut()
                                Toast.makeText(context, "Logged out successfully.", Toast.LENGTH_SHORT).show()
                                navController.navigate("login") {
                                    popUpTo(navController.graph.id) { inclusive = true }
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Logout failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                Log.e("ProfileScreen", "Logout error: ${e.message}")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Logout")
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Back to Home")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Privacy Policy") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                })
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp)) {
            Text(
                text = "Your privacy is important to us. This policy explains how we collect, use, and protect your personal information within the EarnEz app.\n\n" +
                        "**Information Collection:** We collect minimal personal data, primarily your email for account management and in-app activity data (like coins earned, quizzes played) to enhance your gamified experience. We do not collect sensitive personal information.\n\n" +
                        "**Information Use:** Your data is used solely to provide and improve the app's features, personalize your experience, and facilitate core functionalities like coin tracking and referral rewards. We do not sell your data to third parties.\n\n" +
                        "**Data Security:** We implement industry-standard security measures to protect your information from unauthorized access, alteration, disclosure, or destruction.\n\n" +
                        "**Third-Party Services:** Our app may integrate with third-party services (e.g., Firebase for authentication and database, ad networks for earning opportunities). These services have their own privacy policies, which we encourage you to review.\n\n" +
                        "**Changes to This Policy:** We may update our Privacy Policy from time to time. We will notify you of any changes by posting the new Privacy Policy on this page.\n\n" +
                        "**Contact Us:** If you have any questions about this Privacy Policy, please contact us at support@earnez.com.",
                fontSize = 16.sp,
                lineHeight = 24.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth()) {
                Text("Back")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(navController: NavHostController) {
    val context = LocalContext.current
    var leaderboardData by remember { mutableStateOf<List<LeaderboardUser>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val currentUserId = Firebase.auth.currentUser?.uid

    // Fetch leaderboard data
    LaunchedEffect(Unit) {
        try {
            Firebase.firestore.collection("users")
                .orderBy("loyaltyPoints", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener { result ->
                    leaderboardData = result.documents.mapNotNull { doc ->
                        LeaderboardUser(
                            username = doc.getString("username") ?: "Anonymous",
                            coins = (doc.getLong("coins") ?: 0).toInt(),
                            loyaltyPoints = (doc.getLong("loyaltyPoints") ?: 0).toInt(),
                            userId = doc.id
                        )
                    }
                    loading = false
                }
                .addOnFailureListener {
                    loading = false
                    Toast.makeText(context, "Failed to load leaderboard", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Loyalty Leaderboard") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (leaderboardData.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No data available", fontSize = 18.sp)
                }
            } else {
                LazyColumn {
                    itemsIndexed(leaderboardData) { index, user ->
                        val backgroundColor = if (user.userId == currentUserId) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        val textColor = if (user.userId == currentUserId) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = backgroundColor)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}.",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = textColor,
                                    modifier = Modifier.width(36.dp)
                                )
                                Text(
                                    text = user.username,
                                    fontSize = 18.sp,
                                    color = textColor,
                                    modifier = Modifier.weight(1f)
                                )
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${user.loyaltyPoints} LP",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${user.coins} coins",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FeatureCard(feature: Feature, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = feature.color.copy(alpha = 0.2f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            feature.color.copy(alpha = 0.1f),
                            feature.color.copy(alpha = 0.3f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    feature.icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = feature.color
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    feature.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceGameScreen(
    navController: NavHostController,
    onCoinChange: (Int) -> Unit,
    onLoyaltyChange: (Int) -> Unit
) {
    val context = LocalContext.current
    val questions = remember {
        listOf(
            "What is the primary purpose of a budget?" to "Track income and expenses",
            "Which investment typically has the highest risk?" to "Stocks",
            "What does APR stand for?" to "Annual Percentage Rate",
            "What is compound interest?" to "Interest on initial principal + accumulated interest",
            "What is diversification in investing?" to "Spreading investments to reduce risk"
        )
    }
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var userAnswer by remember { mutableStateOf("") }
    var showHint by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf(0) }
    val auth = Firebase.auth
    val db = Firebase.firestore
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Finance Quiz") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (currentQuestionIndex < questions.size) {
                Text(
                    "Question ${currentQuestionIndex + 1}/${questions.size}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    questions[currentQuestionIndex].first,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = userAnswer,
                    onValueChange = { userAnswer = it },
                    label = { Text("Your Answer") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (showHint) {
                    Text(
                        "Hint: ${questions[currentQuestionIndex].second}",
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = {
                            if (userAnswer.equals(questions[currentQuestionIndex].second, ignoreCase = true)) {
                                score += 10
                                Toast.makeText(context, "Correct! +10 points", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Incorrect!", Toast.LENGTH_SHORT).show()
                            }
                            currentQuestionIndex++
                            userAnswer = ""
                            showHint = false
                        }
                    ) {
                        Text("Submit")
                    }

                    Button(
                        onClick = {
                            AdHelper.showRewardedAd(
                                context = context,
                                onRewardEarned = { showHint = true },
                                onAdDismissed = { /* Do nothing */ },
                                onAdFailed = { Toast.makeText(context, "Ad failed to load", Toast.LENGTH_SHORT).show() }
                            )
                        }
                    ) {
                        Text("Get Hint (Ad)")
                    }
                }
            } else {
                val earnedCoins = score * 5
                val earnedLoyalty = score
                LaunchedEffect(Unit) {
                    // Show interstitial ad when entering screen
                    AdHelper.showInterstitialAd(
                        context = context,
                        onAdDismissed = { /* Do nothing */ },
                        onAdFailed = { /* Do nothing */ }
                    )
                }
                Text("Game Over!", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Your Score: $score", fontSize = 24.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("You earned $earnedCoins coins!", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                Text("+$earnedLoyalty loyalty points!", fontSize = 18.sp, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        val userId = auth.currentUser?.uid
                        if (userId != null) {
                            scope.launch {
                                try {
                                    db.collection("users").document(userId).update(
                                        "coins", FieldValue.increment(earnedCoins.toLong()),
                                        "loyaltyPoints", FieldValue.increment(earnedLoyalty.toLong())
                                    ).await()
                                    onCoinChange(earnedCoins)
                                    onLoyaltyChange(earnedLoyalty)
                                    Toast.makeText(context, "Rewards added!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to add rewards", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Finish Game")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuzzleGameScreen(
    navController: NavHostController,
    onCoinChange: (Int) -> Unit,
    onLoyaltyChange: (Int) -> Unit
) {
    val puzzleSize = 4
    val solvedPuzzle = List(puzzleSize * puzzleSize) { it }
    var puzzle by remember { mutableStateOf(solvedPuzzle.shuffled()) }
    var moves by remember { mutableStateOf(0) }
    var showHint by remember { mutableStateOf(false) }
    var gameSolved by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val auth = Firebase.auth
    val db = Firebase.firestore
    val scope = rememberCoroutineScope()

    fun isSolvable(): Boolean {
        // Simple solvability check
        return puzzle.filter { it != 0 }.withIndex().count { (i, value) ->
            puzzle.drop(i + 1).count { it < value && it != 0 } % 2 == 0
        } % 2 == 0
    }

    fun moveTile(index: Int) {
        val emptyIndex = puzzle.indexOf(0)
        val row = index / puzzleSize
        val col = index % puzzleSize
        val emptyRow = emptyIndex / puzzleSize
        val emptyCol = emptyIndex % puzzleSize

        if ((row == emptyRow && Math.abs(col - emptyCol) == 1) ||
            (col == emptyCol && Math.abs(row - emptyRow) == 1)
        ) {
            val newPuzzle = puzzle.toMutableList()
            newPuzzle[emptyIndex] = puzzle[index]
            newPuzzle[index] = 0
            puzzle = newPuzzle
            moves++

            // Check if solved
            if (puzzle == solvedPuzzle) {
                gameSolved = true
            }
        }
    }

    LaunchedEffect(Unit) {
        // Ensure puzzle is solvable
        while (!isSolvable()) {
            puzzle = solvedPuzzle.shuffled()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Puzzle Game") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Sliding Puzzle", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Moves: $moves", fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))

            if (showHint) {
                Text(
                    "Hint: Try to solve row by row from top to bottom",
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (gameSolved) {
                val earnedCoins = 100
                val earnedLoyalty = 20
                Text("Puzzle Solved!", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Moves: $moves", fontSize = 24.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("You earned $earnedCoins coins!", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                Text("+$earnedLoyalty loyalty points!", fontSize = 18.sp, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        val userId = auth.currentUser?.uid
                        if (userId != null) {
                            scope.launch {
                                try {
                                    db.collection("users").document(userId).update(
                                        "coins", FieldValue.increment(earnedCoins.toLong()),
                                        "loyaltyPoints", FieldValue.increment(earnedLoyalty.toLong())
                                    ).await()
                                    onCoinChange(earnedCoins)
                                    onLoyaltyChange(earnedLoyalty)
                                    Toast.makeText(context, "Rewards added!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to add rewards", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Finish Game")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(puzzleSize),
                    modifier = Modifier.size(300.dp)
                ) {
                    items(puzzle.size) { index ->
                        val number = puzzle[index]
                        if (number != 0) {
                            Card(
                                onClick = { moveTile(index) },
                                modifier = Modifier
                                    .padding(4.dp)
                                    .aspectRatio(1f),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$number",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier
                                .padding(4.dp)
                                .aspectRatio(1f)
                                .background(Color.LightGray))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        AdHelper.showRewardedAd(
                            context = context,
                            onRewardEarned = { showHint = true },
                            onAdDismissed = { /* Do nothing */ },
                            onAdFailed = { Toast.makeText(context, "Ad failed to load", Toast.LENGTH_SHORT).show() }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Get Hint (Ad)")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            BannerAdView()
        }

    }

}

