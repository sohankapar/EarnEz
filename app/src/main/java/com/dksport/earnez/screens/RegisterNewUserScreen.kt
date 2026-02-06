package com.dksport.earnez.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterNewUserScreen(navController: NavHostController, onSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var enteredReferralCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val auth = Firebase.auth
    val db = Firebase.firestore
    val scope = rememberCoroutineScope()
    val signUpBonus = 0
    val referralBonus = 500L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
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
            keyboardOptions =  KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
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
            keyboardOptions =   KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
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

                        val userData = hashMapOf(
                            "email" to email,
                            "username" to username,
                            "coins" to if (referredByCode != null) signUpBonus + referralBonus else signUpBonus,
                            "referralCode" to userReferralCode,
                            "referredBy" to referredByCode,
                            "referrals" to emptyList<String>(),
                            "loyaltyPoints" to 100,
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

                        referredByCode?.let { refCode ->
                            db.collection("users")
                                .whereEqualTo("referralCode", refCode)
                                .get().await()
                                .documents.firstOrNull()?.let { referrerDoc ->
                                    val referrerId = referrerDoc.id
                                    db.collection("users").document(referrerId)
                                        .update("referrals", FieldValue.arrayUnion(uid)).await()
                                    db.collection("users").document(referrerId)
                                        .update("coins", FieldValue.increment(referralBonus)).await()
                                }
                        }
                        onSuccess()
                    } catch (e: Exception) {
                        errorMessage = "Registration failed: ${e.message}"
                    } finally {
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
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = { navController.navigate("login") }) {
            Text("Already have an account? Login")
        }
    }
}

private fun generateReferralCode(): String {
    return java.util.UUID.randomUUID().toString().substring(0, 6).uppercase()
}