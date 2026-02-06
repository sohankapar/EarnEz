package com.dksport.earnez.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.dksport.earnez.models.LeaderboardUser
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore

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
                    android.widget.Toast.makeText(context, "Failed to load leaderboard", android.widget.Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Loyalty Leaderboard") },
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