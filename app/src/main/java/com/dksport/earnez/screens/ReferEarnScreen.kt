package com.dksport.earnez.screens

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.dksport.earnez.ui.components.BannerAdView
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

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
                    android.widget.Toast.makeText(context, "User data not found in Firestore.", android.widget.Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                referralCode = "Error loading code"
                android.widget.Toast.makeText(context, "Failed to load referral data: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                Log.e("ReferEarnScreen", "Error loading referral data: ${e.message}", e)
            } finally {
                loading = false
            }
        } else {
            android.widget.Toast.makeText(context, "Please log in to see your referral details.", android.widget.Toast.LENGTH_LONG).show()
            navController.navigate("login") { popUpTo("home") { inclusive = true } }
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Refer & Earn") },
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
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Refer & Get points!", fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
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
                            android.widget.Toast.makeText(context, "Referral code not available yet. Please wait.", android.widget.Toast.LENGTH_SHORT).show()
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