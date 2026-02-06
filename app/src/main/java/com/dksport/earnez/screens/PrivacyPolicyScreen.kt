package com.dksport.earnez.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(24.dp)) {
            Text(
                text = "Your privacy is important to us. This policy explains how we collect, use, and protect your personal information within the EarnEz app.\n\n" +
                        "**Information Collection:** We collect minimal personal data, primarily your email for account management and in-app activity data (like coins earned, quizzes played) to enhance your gamified experience. We do not collect sensitive personal information.\n\n" +
                        "**Information Use:** Your data is used solely to provide and improve the app's features, personalize your experience, and facilitate core functionalities like coin tracking and referral rewards. We do not sell your data to third parties.\n\n" +
                        "**Data Security:** We implement industry-standard security measures to protect your information from unauthorized access, alteration, disclosure, or destruction.\n\n" +
                        "**Third-Party Services:** Our app may integrate with third-party services (e.g., Firebase for authentication and database, ad networks for earning opportunities). These services have their own privacy policies, which we encourage you to review.\n\n" +
                        "**Changes to This Policy:** We may update our Privacy Policy from time to time. We will notify you of any changes by posting the new Privacy Policy on this page.\n\n" +
                        "**Contact Us:** If you have any questions about this Privacy Policy, please contact us at support@earnez.com.",
                fontSize = 16.sp,
                lineHeight = 24.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth()) {
                Text("Back")
            }
        }
    }
}