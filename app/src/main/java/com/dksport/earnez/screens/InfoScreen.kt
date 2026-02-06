package com.dksport.earnez.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.regex.Pattern

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoScreen() {
    val context = LocalContext.current
    val firestore = remember { Firebase.firestore }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // State variables
    var userData by remember { mutableStateOf(mapOf<String, Any>()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var parsedInfo by remember { mutableStateOf<List<String>>(emptyList()) }

    // Load user data on first composition
    LaunchedEffect(Unit) {
        loadUserData(firestore, onSuccess = { data ->
            userData = data
            // Parse data with regex (API 24+ compatible)
            parseUserInfo(data)
        }, onError = { error ->
            errorMessage = error
        })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "User Information",
            style = MaterialTheme.typography.headlineMedium
        )

        // Loading state
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        // Error state
        errorMessage?.let { error ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Error: $error",
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // User data display
        if (userData.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    userData.forEach { (key, value) ->
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "$key:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = value.toString(),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        } else if (!isLoading && errorMessage == null) {
            Text("No user data found")
        }

        // Parsed info section
        if (parsedInfo.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Parsed Information",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    parsedInfo.forEach { info ->
                        Text(
                            text = "• $info",
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Refresh button
        Button(
            onClick = {
                isLoading = true
                errorMessage = null
                coroutineScope.launch {
                    loadUserData(firestore, onSuccess = { data ->
                        userData = data
                        parseUserInfo(data)
                        isLoading = false
                    }, onError = { error ->
                        errorMessage = error
                        isLoading = false
                    })
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Refresh Data")
        }
    }
}

// Function to load user data from Firestore
private suspend fun loadUserData(
    firestore: com.google.firebase.firestore.FirebaseFirestore,
    onSuccess: (Map<String, Any>) -> Unit,
    onError: (String) -> Unit
) {
    try {
        // Replace "users" and "userId" with your actual collection and document ID
        val documentSnapshot = firestore.collection("users")
            .document("currentUserId") // Replace with actual user ID
            .get()
            .await()

        if (documentSnapshot.exists()) {
            val data = documentSnapshot.data ?: emptyMap()

            // Example of getting string values
            val name = documentSnapshot.getString("name") ?: "Unknown"
            val email = documentSnapshot.getString("email") ?: "No email"
            val phone = documentSnapshot.getString("phone") ?: "No phone"

            // Create a map with the data
            val userDataMap = mapOf(
                "Name" to name,
                "Email" to email,
                "Phone" to phone,
                // Add more fields as needed
            )

            onSuccess(userDataMap)
        } else {
            onError("User document does not exist")
        }
    } catch (e: Exception) {
        onError("Failed to load data: ${e.message}")
    }
}

// Function to parse user info with regex (API 24+ compatible)
private fun parseUserInfo(data: Map<String, Any>) {
    val parsedResults = mutableListOf<String>()

    // Example: Parse phone number (format: +1 (123) 456-7890)
    // Using Pattern and Matcher for API 24+ compatibility
    val phonePattern = Pattern.compile("\\(?(\\d{3})\\)?[-.\\s]?(\\d{3})[-.\\s]?(\\d{4})")

    data["Phone"]?.let { phone ->
        val matcher = phonePattern.matcher(phone.toString())
        if (matcher.find()) {
            // Use group indices (starting from 1) instead of named groups
            val areaCode = matcher.group(1) ?: ""
            val prefix = matcher.group(2) ?: ""
            val lineNumber = matcher.group(3) ?: ""

            if (areaCode.isNotEmpty()) {
                parsedResults.add("Area Code: $areaCode")
            }
            if (prefix.isNotEmpty() && lineNumber.isNotEmpty()) {
                parsedResults.add("Phone: ($areaCode) $prefix-$lineNumber")
            }
        }
    }

    // Example: Parse email to get domain
    data["Email"]?.let { email ->
        val emailStr = email.toString()
        val atIndex = emailStr.indexOf('@')
        if (atIndex > 0 && atIndex < emailStr.length - 1) {
            val domain = emailStr.substring(atIndex + 1)
            parsedResults.add("Email Domain: $domain")
        }
    }

    // Example: Simple regex without named groups for API 24
    val simpleRegex = Regex("""([A-Z][a-z]+)\s+([A-Z][a-z]+)""")
    data["Name"]?.let { name ->
        val matchResult = simpleRegex.find(name.toString())
        matchResult?.let {
            // Using groupValues (index 0 is entire match, 1 is first group, etc.)
            if (it.groupValues.size >= 3) {
                val firstName = it.groupValues[1]
                val lastName = it.groupValues[2]
                parsedResults.add("First Name: $firstName")
                parsedResults.add("Last Name: $lastName")
            }
        }
    }


}

