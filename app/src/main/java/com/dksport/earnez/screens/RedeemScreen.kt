package com.dksport.earnez.screens

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Data class for redeemable items
data class RedeemableItem(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val costInCoins: Long = 0L,
    val costInLoyalty: Long = 0L,
    val category: String = "",
    val stock: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val brand: String = "",
    val validityDays: Int = 30,
    val terms: String = ""
)

// Data class for user's balance
data class UserBalance(
    val coins: Long = 0L,
    val loyaltyPoints: Long = 0L
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedeemScreen(
    navController: NavHostController,
    onCoinChange: (Int) -> Unit = {},
    onLoyaltyChange: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val firestore = remember { Firebase.firestore }
    val auth = remember { Firebase.auth }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // State variables
    var redeemableItems by remember { mutableStateOf<List<RedeemableItem>>(emptyList()) }
    var userBalance by remember { mutableStateOf(UserBalance()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    // Fetch redeemable items and user balance
    LaunchedEffect(Unit) {
        loadRedeemableData(firestore, auth.currentUser?.uid) { items, balance, error ->
            redeemableItems = items
            userBalance = balance
            isLoading = false
            errorMessage = error
        }
    }

    // Get unique categories
    val categories = listOf("All") + redeemableItems.map { it.category }.distinct().sorted()

    // Filter items based on search and category
    val filteredItems = redeemableItems.filter { item ->
        (selectedCategory == "All" || item.category == selectedCategory) &&
                (searchQuery.isEmpty() ||
                        item.name.contains(searchQuery, ignoreCase = true) ||
                        item.description.contains(searchQuery, ignoreCase = true) ||
                        item.brand.contains(searchQuery, ignoreCase = true))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Redeem Rewards") },
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
            // User balance card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Your Balance",
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BalanceItem(
                            icon = Icons.Default.Diamond,
                            title = "Coins",
                            value = userBalance.coins.toString(),
                            color = Color(0xFFFFD700)
                        )
                        BalanceItem(
                            icon = Icons.Default.Wallet,
                            title = "Loyalty Points",
                            value = userBalance.loyaltyPoints.toString(),
                            color = Color(0xFF4CAF50)
                        )
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
                    label = { Text("Search rewards...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category filter chips
                Text(
                    text = "Categories:",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category) }
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
                        Text("Loading rewards...")
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
                        Text(
                            text = "Error Loading Rewards",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                isLoading = true
                                loadRedeemableData(firestore, auth.currentUser?.uid) { items, balance, error ->
                                    redeemableItems = items
                                    userBalance = balance
                                    isLoading = false
                                    errorMessage = error
                                }
                            }
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }

            // Redeemable items list
            if (filteredItems.isNotEmpty()) {
                Text(
                    text = "${filteredItems.size} rewards available",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    items(filteredItems) { item ->
                        RedeemableItemCard(
                            item = item,
                            userCoins = userBalance.coins,
                            userLoyalty = userBalance.loyaltyPoints,
                            onRedeem = {
                                coroutineScope.launch {
                                    redeemItem(
                                        item = item,
                                        userId = auth.currentUser?.uid,
                                        firestore = firestore,
                                        snackbarHostState = snackbarHostState,
                                        context = context,
                                        onSuccess = { newCoins, newLoyalty ->
                                            userBalance = UserBalance(newCoins, newLoyalty)
                                            onCoinChange(newCoins.toInt())
                                            onLoyaltyChange(newLoyalty.toInt())
                                        }
                                    )
                                }
                            }
                        )
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
                            Icons.Default.LocalOffer,
                            contentDescription = "No rewards",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty()) {
                                "No rewards found for '$searchQuery'"
                            } else {
                                "No rewards available"
                            },
                            textAlign = TextAlign.Center
                        )
                        if (searchQuery.isNotEmpty()) {
                            Button(onClick = { searchQuery = "" }) {
                                Text("Clear Search")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun BalanceItem(
    icon: ImageVector,
    title: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = title,
            tint = color,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun RedeemableItemCard(
    item: RedeemableItem,
    userCoins: Long,
    userLoyalty: Long,
    onRedeem: () -> Unit
) {
    val canAfford = userCoins >= item.costInCoins && userLoyalty >= item.costInLoyalty
    val isOutOfStock = item.stock <= 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isOutOfStock) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Image and title row
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Item image
                if (item.imageUrl.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(item.imageUrl)
                                .crossfade(true)
                                .build()
                        ),
                        contentDescription = item.name,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.LocalOffer,
                            contentDescription = "Reward",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Title and category
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    if (item.brand.isNotEmpty()) {
                        Text(
                            text = item.brand,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (item.category.isNotEmpty()) {
                        Text(
                            text = item.category,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            Text(
                text = item.description,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Cost and stock info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    if (item.costInCoins > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Diamond,
                                contentDescription = "Coins",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${item.costInCoins} coins",
                                fontSize = 14.sp
                            )
                        }
                    }
                    if (item.costInLoyalty > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Wallet,
                                contentDescription = "Loyalty",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${item.costInLoyalty} loyalty points",
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                if (item.stock > 0) {
                    Text(
                        text = "${item.stock} left",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Out of stock",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Redeem button
            Button(
                onClick = onRedeem,
                modifier = Modifier.fillMaxWidth(),
                enabled = canAfford && !isOutOfStock && item.isActive,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canAfford) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                if (!item.isActive) {
                    Text("Not Available")
                } else if (isOutOfStock) {
                    Text("Out of Stock")
                } else if (!canAfford) {
                    Text("Insufficient Balance")
                } else {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Redeem",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Redeem Now")
                }
            }

            // Terms and conditions
            if (item.terms.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Terms: ${item.terms}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}

// Function to load redeemable data
private fun loadRedeemableData(
    firestore: FirebaseFirestore,
    userId: String?,
    onComplete: (List<RedeemableItem>, UserBalance, String?) -> Unit
) {
    if (userId == null) {
        onComplete(emptyList(), UserBalance(), "User not logged in")
        return
    }

    // Load user balance
    firestore.collection("users").document(userId).get()
        .addOnSuccessListener { userDocument ->
            val userBalance = UserBalance(
                coins = userDocument.getLong("coins") ?: 0L,
                loyaltyPoints = userDocument.getLong("loyaltyPoints") ?: 0L
            )

            // Load redeemable items
            firestore.collection("redeemables")
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val items = mutableListOf<RedeemableItem>()

                    for (document in querySnapshot.documents) {
                        val id = document.id
                        val name = document.getString("name") ?: ""
                        val description = document.getString("description") ?: ""
                        val imageUrl = document.getString("imageUrl") ?: ""
                        val costInCoins = document.getLong("costInCoins") ?: 0L
                        val costInLoyalty = document.getLong("costInLoyalty") ?: 0L
                        val category = document.getString("category") ?: "General"
                        val stock = (document.getLong("stock") ?: 0L).toInt()
                        val isActive = document.getBoolean("isActive") ?: true
                        val brand = document.getString("brand") ?: ""
                        val validityDays = (document.getLong("validityDays") ?: 30L).toInt()
                        val terms = document.getString("terms") ?: ""

                        items.add(
                            RedeemableItem(
                                id = id,
                                name = name,
                                description = description,
                                imageUrl = imageUrl,
                                costInCoins = costInCoins,
                                costInLoyalty = costInLoyalty,
                                category = category,
                                stock = stock,
                                isActive = isActive,
                                brand = brand,
                                validityDays = validityDays,
                                terms = terms
                            )
                        )
                    }

                    onComplete(items, userBalance, null)
                }
                .addOnFailureListener { exception ->
                    onComplete(emptyList(), userBalance, "Failed to load rewards: ${exception.message}")
                }
        }
        .addOnFailureListener { exception ->
            onComplete(emptyList(), UserBalance(), "Failed to load user data: ${exception.message}")
        }
}

// Function to redeem an item
private suspend fun redeemItem(
    item: RedeemableItem,
    userId: String?,
    firestore: FirebaseFirestore,
    snackbarHostState: SnackbarHostState,
    context: Context,
    onSuccess: (Long, Long) -> Unit
) {
    if (userId == null) {
        snackbarHostState.showSnackbar("Please log in to redeem rewards")
        return
    }

    try {
        // Check if item is still in stock
        val itemDoc = firestore.collection("redeemables").document(item.id).get().await()
        val currentStock = (itemDoc.getLong("stock") ?: 0L).toInt()

        if (currentStock <= 0) {
            snackbarHostState.showSnackbar("Sorry, this item is out of stock")
            return
        }

        // Check user balance
        val userDoc = firestore.collection("users").document(userId).get().await()
        val userCoins = userDoc.getLong("coins") ?: 0L
        val userLoyalty = userDoc.getLong("loyaltyPoints") ?: 0L

        if (userCoins < item.costInCoins || userLoyalty < item.costInLoyalty) {
            snackbarHostState.showSnackbar("Insufficient balance to redeem this item")
            return
        }

        // Start a batch transaction
        val batch = firestore.batch()

        // Update item stock
        batch.update(firestore.collection("redeemables").document(item.id),
            "stock", FieldValue.increment(-1))

        // Update user balance
        batch.update(firestore.collection("users").document(userId),
            "coins", FieldValue.increment(-item.costInCoins),
            "loyaltyPoints", FieldValue.increment(-item.costInLoyalty))

        // Create redemption record
        val redemptionData = hashMapOf(
            "userId" to userId,
            "itemId" to item.id,
            "itemName" to item.name,
            "costInCoins" to item.costInCoins,
            "costInLoyalty" to item.costInLoyalty,
            "redeemedAt" to System.currentTimeMillis(),
            "validUntil" to System.currentTimeMillis() + (item.validityDays * 24 * 60 * 60 * 1000L),
            "status" to "redeemed"
        )

        batch.set(firestore.collection("redemptions").document(), redemptionData)

        // Commit the batch
        batch.commit().await()

        // Calculate new balance
        val newCoins = userCoins - item.costInCoins
        val newLoyalty = userLoyalty - item.costInLoyalty

        // Show success message
        snackbarHostState.showSnackbar("Successfully redeemed ${item.name}!")

        // Call success callback
        onSuccess(newCoins, newLoyalty)

    } catch (exception: Exception) {
        snackbarHostState.showSnackbar("Failed to redeem item: ${exception.message}")
    }
}