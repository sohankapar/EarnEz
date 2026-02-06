package com.dksport.earnez.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

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
                android.widget.Toast.makeText(context, "Failed to load user data", android.widget.Toast.LENGTH_SHORT).show()
                loading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Profile") },
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
                                android.widget.Toast.makeText(context, "Logged out successfully.", android.widget.Toast.LENGTH_SHORT).show()
                                navController.navigate("login") {
                                    popUpTo(navController.graph.id) { inclusive = true }
                                }
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Logout failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
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