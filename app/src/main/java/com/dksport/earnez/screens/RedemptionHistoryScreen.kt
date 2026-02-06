package com.dksport.earnez.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// Data class for redemption history
data class RedemptionHistory(
    val id: String = "",
    val itemId: String = "",
    val itemName: String = "",
    val costInCoins: Long = 0L,
    val costInLoyalty: Long = 0L,
    val redeemedAt: Long = 0L,
    val validUntil: Long = 0L,
    val status: RedemptionStatus = RedemptionStatus.REDEEMED,
    val code: String = "", // Optional redemption code
    val category: String = ""
) {
    // Check if redemption is expired
    val isExpired: Boolean
        get() = System.currentTimeMillis() > validUntil && status != RedemptionStatus.USED

    // Check if redemption is valid (not expired and not used)
    val isValid: Boolean
        get() = !isExpired && status == RedemptionStatus.REDEEMED

    // Days remaining until expiration
    val daysRemaining: Long
        get() {
            val remainingMillis = validUntil - System.currentTimeMillis()
            return TimeUnit.MILLISECONDS.toDays(remainingMillis)
        }
}

// Redemption status enum
enum class RedemptionStatus {
    REDEEMED,
    USED,
    EXPIRED,
    CANCELLED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedemptionHistoryScreen(
    navController: NavHostController
) {
    val context = LocalContext.current
    val firestore = remember { Firebase.firestore }
    val auth = remember { Firebase.auth }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // State variables
    var redemptionHistory by remember { mutableStateOf<List<RedemptionHistory>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    // Load redemption history
    LaunchedEffect(Unit) {
        loadRedemptionHistory(
            firestore = firestore,
            userId = auth.currentUser?.uid,
            onSuccess = { history ->
                redemptionHistory = history.sortedByDescending { it.redeemedAt }
                isLoading = false
            },
            onError = { error ->
                errorMessage = error
                isLoading = false
            }
        )
    }

    // Filter history based on selected filter and search
    val filteredHistory = redemptionHistory.filter { history ->
        val matchesFilter = when (selectedFilter) {
            "All" -> true
            "Valid" -> history.isValid
            "Used" -> history.status == RedemptionStatus.USED
            "Expired" -> history.status == RedemptionStatus.EXPIRED || history.isExpired
            else -> true
        }

        val matchesSearch = searchQuery.isEmpty() ||
                history.itemName.contains(searchQuery, ignoreCase = true) ||
                history.category.contains(searchQuery, ignoreCase = true) ||
                history.code.contains(searchQuery, ignoreCase = true)

        matchesFilter && matchesSearch
    }

    // Get total coins and loyalty spent
    val totalCoinsSpent = redemptionHistory.sumOf { it.costInCoins }
    val totalLoyaltySpent = redemptionHistory.sumOf { it.costInLoyalty }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Redemption History") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Summary card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total Redemptions",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${redemptionHistory.size} items",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Icon(
                            Icons.Default.History,
                            contentDescription = "History",
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Divider()

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Coins Spent",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = totalCoinsSpent.toString(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD700) // Gold color for coins
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Loyalty Spent",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = totalLoyaltySpent.toString(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50) // Green color for loyalty
                            )
                        }
                    }
                }
            }

            // Search and filter section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search redemption history...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Filter chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf("All", "Valid", "Used", "Expired")
                    filters.forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Loading state
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Loading redemption history...")
                    }
                }
            }

            // Error state
            errorMessage?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Error",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Error Loading History",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                isLoading = true
                                errorMessage = null
                                coroutineScope.launch {
                                    loadRedemptionHistory(
                                        firestore = firestore,
                                        userId = auth.currentUser?.uid,
                                        onSuccess = { history ->
                                            redemptionHistory = history.sortedByDescending { it.redeemedAt }
                                            isLoading = false
                                        },
                                        onError = { newError ->
                                            errorMessage = newError
                                            isLoading = false
                                        }
                                    )
                                }
                            }
                        ) {
                            Text("Try Again")
                        }
                    }
                }
            }

            // History list
            if (filteredHistory.isNotEmpty()) {
                Text(
                    text = "${filteredHistory.size} redemption(s) found",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    items(filteredHistory) { history ->
                        RedemptionHistoryCard(history = history)
                    }
                }
            } else if (!isLoading && errorMessage == null) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.CardGiftcard,
                            contentDescription = "No redemptions",
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty()) {
                                "No redemptions found for '$searchQuery'"
                            } else {
                                "No redemption history yet"
                            },
                            textAlign = TextAlign.Center,
                            fontSize = 16.sp
                        )
                        if (searchQuery.isNotEmpty()) {
                            Button(onClick = { searchQuery = "" }) {
                                Text("Clear Search")
                            }
                        } else {
                            Text(
                                text = "Redeem rewards to see them here",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun RedemptionHistoryCard(
    history: RedemptionHistory
) {
    val isExpired = history.isExpired
    val isValid = history.isValid
    val isUsed = history.status == RedemptionStatus.USED

    // Format dates
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val redeemedDate = dateFormat.format(Date(history.redeemedAt))
    val validUntilDate = dateFormat.format(Date(history.validUntil))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isUsed -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                isExpired -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                isValid -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row with status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = history.itemName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                // Status chip
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = when {
                        isUsed -> MaterialTheme.colorScheme.secondary
                        isExpired -> MaterialTheme.colorScheme.error
                        isValid -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when {
                                isUsed -> Icons.Default.CheckCircle
                                isExpired -> Icons.Default.Warning
                                isValid -> Icons.Default.CheckCircle
                                else -> Icons.Default.Pending
                            },
                            contentDescription = "Status",
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when {
                                isUsed -> "Used"
                                isExpired -> "Expired"
                                isValid -> "Valid"
                                else -> history.status.name.lowercase().replaceFirstChar { it.uppercase() }
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Category
            if (history.category.isNotEmpty()) {
                Text(
                    text = history.category,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Cost information
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    if (history.costInCoins > 0) {
                        Text(
                            text = "${history.costInCoins} coins",
                            fontSize = 14.sp,
                            color = Color(0xFFFFD700)
                        )
                    }
                    if (history.costInLoyalty > 0) {
                        Text(
                            text = "${history.costInLoyalty} loyalty points",
                            fontSize = 14.sp,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }

                // Days remaining or expired info
                if (isValid) {
                    Text(
                        text = "${history.daysRemaining} days left",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                } else if (isExpired) {
                    Text(
                        text = "Expired",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Dates information
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Redeemed: $redeemedDate",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Valid until: $validUntilDate",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Redemption code (if available)
                if (history.code.isNotEmpty()) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Code:",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = history.code,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

// Function to load redemption history
private suspend fun loadRedemptionHistory(
    firestore: FirebaseFirestore,
    userId: String?,
    onSuccess: (List<RedemptionHistory>) -> Unit,
    onError: (String) -> Unit
) {
    if (userId == null) {
        onError("User not logged in")
        return
    }

    try {
        // Query redemptions for this user, ordered by redemption date
        val querySnapshot = firestore.collection("redemptions")
            .whereEqualTo("userId", userId)
            .orderBy("redeemedAt", Query.Direction.DESCENDING)
            .get()
            .await()

        val historyList = mutableListOf<RedemptionHistory>()

        for (document in querySnapshot.documents) {
            val id = document.id
            val itemId = document.getString("itemId") ?: ""
            val itemName = document.getString("itemName") ?: "Unknown Item"
            val costInCoins = document.getLong("costInCoins") ?: 0L
            val costInLoyalty = document.getLong("costInLoyalty") ?: 0L
            val redeemedAt = document.getLong("redeemedAt") ?: 0L
            val validUntil = document.getLong("validUntil") ?: (redeemedAt + 30L * 24 * 60 * 60 * 1000) // Default 30 days
            val statusString = document.getString("status") ?: "REDEEMED"
            val code = document.getString("code") ?: ""
            val category = document.getString("category") ?: ""

            val status = try {
                RedemptionStatus.valueOf(statusString.uppercase())
            } catch (e: IllegalArgumentException) {
                RedemptionStatus.REDEEMED
            }

            historyList.add(
                RedemptionHistory(
                    id = id,
                    itemId = itemId,
                    itemName = itemName,
                    costInCoins = costInCoins,
                    costInLoyalty = costInLoyalty,
                    redeemedAt = redeemedAt,
                    validUntil = validUntil,
                    status = status,
                    code = code,
                    category = category
                )
            )
        }

        onSuccess(historyList)

    } catch (exception: Exception) {
        onError("Failed to load redemption history: ${exception.message}")
    }
}

// Extension function to get display status
private fun RedemptionHistory.getDisplayStatus(): String {
    return when {
        isExpired -> "Expired"
        status == RedemptionStatus.USED -> "Used"
        status == RedemptionStatus.CANCELLED -> "Cancelled"
        isValid -> "Valid"
        else -> status.name.lowercase().replaceFirstChar { it.uppercase() }
    }
}