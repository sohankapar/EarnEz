package com.dksport.earnez.screens

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.dksport.earnez.AdHelper
import com.dksport.earnez.DarkBackground
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchAdsScreen(
    navController: NavHostController,
    coins: Int,
    loyaltyPoints: Int,
    onCoinChange: (Int) -> Unit,
    onLoyaltyChange: (Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = Firebase.firestore
    val auth = Firebase.auth

    var isLoading by remember { mutableStateOf(false) }
    var adWatchesToday by remember { mutableIntStateOf(0) }
    var adAvailable by remember { mutableStateOf(false) }

    val maxAdsPerDay = 5
    val adReward = 100
    val loyaltyReward = 5

    // Progress Calculation
    val progress by animateFloatAsState(
        targetValue = adWatchesToday.toFloat() / maxAdsPerDay.toFloat(),
        label = "Progress"
    )

    // Sync Daily Limits
    LaunchedEffect(auth.currentUser?.uid) {
        auth.currentUser?.uid?.let { userId ->
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis.toString()

            db.collection("users").document(userId)
                .collection("daily_limits").document(today)
                .addSnapshotListener { snapshot, _ ->
                    adWatchesToday = snapshot?.getLong("ad_watches")?.toInt() ?: 0
                }
        }
    }

    // Load ad on screen appear
    LaunchedEffect(Unit) {
        AdHelper.loadRewardedAd(context)
    }

    // Ad availability listener
    DisposableEffect(Unit) {
        val listener = {
            adAvailable = true
        }
        AdHelper.addAdLoadListener(listener)

        onDispose {
            AdHelper.removeAdLoadListener(listener)
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "EARN COINS",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1A1A1A))
                    .padding(20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = "Star",
                            tint = Color(0xFFFFD700)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Daily Progress",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = Color(0xFF4CAF50),
                        trackColor = Color.DarkGray
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "$adWatchesToday / $maxAdsPerDay ads watched today",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // Reward Card
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2A2A2A))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50).copy(alpha = 0.2f))
                            .padding(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CardGiftcard,
                            contentDescription = "Reward",
                            modifier = Modifier.size(60.dp),
                            tint = Color(0xFF4CAF50)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "$adReward COINS",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )

                    Text(
                        "+ $loyaltyReward loyalty points",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Title & Description
            Text(
                "Watch & Earn Instantly",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Watch a 30-second video to earn coins instantly. " +
                        "Complete all $maxAdsPerDay videos for maximum daily earnings.",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    title = "Today",
                    value = "$adWatchesToday",
                    color = Color(0xFF4CAF50)
                )
                StatItem(
                    title = "Remaining",
                    value = "${maxAdsPerDay - adWatchesToday}",
                    color = Color(0xFFFF9800)
                )
                StatItem(
                    title = "Reward",
                    value = "$adReward",
                    color = Color(0xFFFFD700)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Main Action Button
            Button(
                onClick = {
                    if (adWatchesToday >= maxAdsPerDay) {
                        Toast.makeText(
                            context,
                            "Daily limit reached! Come back tomorrow.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else if (!adAvailable) {
                        Toast.makeText(
                            context,
                            "Ad loading... Please wait a moment.",
                            Toast.LENGTH_SHORT
                        ).show()
                        AdHelper.loadRewardedAd(context)
                    } else {
                        isLoading = true

                        // Show the ad using AdHelper
                        AdHelper.showRewardedAd(
                            context = context,
                            onRewardEarned = {
                                // This is called when user earns the reward
                                scope.launch {
                                    handleRewardUpdate(
                                        db = db,
                                        uid = auth.currentUser?.uid,
                                        reward = adReward.toLong(),
                                        loyalty = loyaltyReward,
                                        currentCoins = coins,
                                        currentLoyalty = loyaltyPoints,
                                        onCoinChange = onCoinChange,
                                        onLoyaltyChange = onLoyaltyChange
                                    )

                                    Toast.makeText(
                                        context,
                                        "Success! You earned $adReward coins",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            onAdDismissed = {
                                // Ad was dismissed
                                isLoading = false
                                // Load next ad
                                AdHelper.loadRewardedAd(context)
                            },
                            onAdFailed = {
                                // Ad failed to show
                                isLoading = false
                                Toast.makeText(
                                    context,
                                    "Ad failed to display. Please try again.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                // Try to load another ad
                                AdHelper.loadRewardedAd(context)
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (adWatchesToday >= maxAdsPerDay) {
                        Color.Gray
                    } else {
                        Color(0xFF4CAF50)
                    }
                ),
                enabled = !isLoading && adWatchesToday < maxAdsPerDay
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.OndemandVideo,
                            contentDescription = "Watch Ad",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = if (adWatchesToday >= maxAdsPerDay) {
                                "DAILY LIMIT REACHED"
                            } else if (!adAvailable) {
                                "LOADING AD..."
                            } else {
                                "WATCH AD & EARN"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            // Info Text
            Text(
                text = if (adAvailable) {
                    "Watch a short video ad to earn coins instantly"
                } else {
                    "Loading ad... Please wait"
                },
                color = Color.Gray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .padding(horizontal = 20.dp)
            )

            // Load Ad Button (if not available)
            if (!adAvailable) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        AdHelper.loadRewardedAd(context)
                        Toast.makeText(context, "Loading ad...", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF2196F3)
                    )
                ) {
                    Text("RELOAD AD")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun StatItem(
    title: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = color,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = title,
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}

private suspend fun handleRewardUpdate(
    db: com.google.firebase.firestore.FirebaseFirestore,
    uid: String?,
    reward: Long,
    loyalty: Int,
    currentCoins: Int,
    currentLoyalty: Int,
    onCoinChange: (Int) -> Unit,
    onLoyaltyChange: (Int) -> Unit
) {
    uid?.let { userId ->
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis.toString()

        try {
            val userRef = db.collection("users").document(userId)
            val limitRef = userRef.collection("daily_limits").document(today)

            // Update daily limit
            limitRef.set(
                mapOf(
                    "ad_watches" to FieldValue.increment(1),
                    "date" to today
                ),
                com.google.firebase.firestore.SetOptions.merge()
            ).await()

            // Update user coins and loyalty points
            userRef.update(
                mapOf(
                    "coins" to FieldValue.increment(reward),
                    "loyaltyPoints" to FieldValue.increment(loyalty.toLong())
                )
            ).await()

            // Update local state
            onCoinChange(currentCoins + reward.toInt())
            onLoyaltyChange(currentLoyalty + loyalty)
        } catch (e: Exception) {
            throw e
        }
    }
}