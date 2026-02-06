package com.dksport.earnez

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.dksport.earnez.ui.theme.EarnezTheme
import com.google.android.gms.ads.MobileAds
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import java.util.Calendar

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