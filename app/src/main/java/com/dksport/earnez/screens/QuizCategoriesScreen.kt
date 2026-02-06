package com.dksport.earnez.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.dksport.earnez.models.QuizCategory
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizCategoriesScreen(
    navController: NavHostController
) {
    val context = LocalContext.current
    val firestore = remember { Firebase.firestore }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // State variables
    var categories by remember { mutableStateOf<List<QuizCategory>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Function to load quiz categories
    fun loadCategories() {
        isLoading = true
        errorMessage = null

        coroutineScope.launch {
            try {
                // Get documents from the 'quiz_categories' collection
                val querySnapshot = firestore.collection("quiz_categories")
                    .whereEqualTo("isActive", true)
                    .get()
                    .await()

                val categoryList = mutableListOf<QuizCategory>()

                for (document in querySnapshot.documents) {
                    // Get the document ID
                    val id = document.id

                    // Get string values with null safety
                    val name = document.getString("name") ?: document.getString("title") ?: "Untitled"
                    val description = document.getString("description") ?: "No description available"
                    val iconUrl = document.getString("iconUrl") ?: document.getString("imageUrl") ?: ""
                    val color = document.getString("color") ?: "#4CAF50"

                    // Get numeric values
                    val questionCount = (document.getLong("questionCount") ?: 0L).toInt()
                    val totalPoints = (document.getLong("totalPoints") ?: 0L).toInt()
                    val averageDifficulty = document.getDouble("averageDifficulty") ?: 2.5

                    // Get boolean values
                    val isPremium = document.getBoolean("isPremium") ?: false

                    // Get timestamp values
                    val createdAt = document.getLong("createdAt") ?: System.currentTimeMillis()
                    val updatedAt = document.getLong("updatedAt") ?: System.currentTimeMillis()

                    categoryList.add(
                        QuizCategory(
                            id = id,
                            name = name,
                            description = description,
                            iconUrl = iconUrl,
                            color = color,
                            questionCount = questionCount,
                            totalPoints = totalPoints,
                            averageDifficulty = averageDifficulty,
                            isPremium = isPremium,
                            createdAt = createdAt,
                            updatedAt = updatedAt
                        )
                    )
                }

                categories = categoryList

            } catch (e: Exception) {
                errorMessage = "Failed to load categories: ${e.message}"
                snackbarHostState.showSnackbar("Error loading categories: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    // Load categories when screen is first shown
    LaunchedEffect(Unit) {
        loadCategories()
    }

    // Filter categories based on search query
    val filteredCategories = categories.filter { category ->
        searchQuery.isEmpty() ||
                category.name.contains(searchQuery, ignoreCase = true) ||
                category.description.contains(searchQuery, ignoreCase = true)
    }

    // Get difficulty level from averageDifficulty
    fun getDifficultyString(averageDifficulty: Double): String {
        return when {
            averageDifficulty < 2.0 -> "Easy"
            averageDifficulty < 3.5 -> "Medium"
            else -> "Hard"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quiz Categories") },
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
            // Search bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search categories...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }

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
                        Text("Loading categories...")
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
                            text = "Error Loading Categories",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { loadCategories() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }

            // Categories list
            if (filteredCategories.isNotEmpty()) {
                Text(
                    text = "${filteredCategories.size} categories available",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    items(filteredCategories) { category ->
                        CategoryCard(
                            category = category,
                            difficultyString = getDifficultyString(category.averageDifficulty),
                            onClick = {
                                navController.navigate("quiz_play/${category.id}")
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
                        Text(
                            text = "No quiz categories found",
                            style = MaterialTheme.typography.titleMedium
                        )

                        if (searchQuery.isNotEmpty()) {
                            Text("Try a different search term")
                            Button(onClick = { searchQuery = "" }) {
                                Text("Clear Search")
                            }
                        } else {
                            Text("Check back later for new categories")
                            Button(onClick = { loadCategories() }) {
                                Text("Refresh")
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
fun CategoryCard(
    category: QuizCategory,
    difficultyString: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                // Premium badge
                if (category.isPremium) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "PREMIUM",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            if (category.description.isNotEmpty()) {
                Text(
                    text = category.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Difficulty badge
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = when (difficultyString.lowercase()) {
                        "easy" -> MaterialTheme.colorScheme.primaryContainer
                        "hard" -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.secondaryContainer
                    }
                ) {
                    Text(
                        text = difficultyString,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Questions count
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${category.questionCount}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = " questions",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                // Points
                if (category.totalPoints > 0) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${category.totalPoints}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD700)
                            )
                            Text(
                                text = " pts",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Start quiz button
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Start Quiz",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Quiz")
            }
        }
    }
}