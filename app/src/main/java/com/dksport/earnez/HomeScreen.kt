package com.dksport.earnez

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

// Theme Colors for 2026
val DarkBackground = Color(0xFF0A0A0A)
val CardSurface = Color(0xFF161618)
val AccentPurple = Color(0xFF8E2DE2)
val AccentBlue = Color(0xFF4A00E0)
val FinanceGreen = Color(0xFF00C853)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    coins: Int,
    loyaltyPoints: Int,
    onCoinChange: (Int) -> Unit,
    onLoyaltyChange: (Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val db = Firebase.firestore
    val auth = Firebase.auth
    var username by remember { mutableStateOf("User") }

    // Real-time Sync
    LaunchedEffect(Unit) {
        auth.currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    username = it.getString("username") ?: "User"
                    onCoinChange(it.getLong("coins")?.toInt() ?: 0)
                    onLoyaltyChange(it.getLong("loyaltyPoints")?.toInt() ?: 0)
                }
            }
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = { ModernTopBar(username, coins) }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // HERO SECTION: Watch & Earn (Full Width)
            item(span = { GridItemSpan(maxLineSpan) }) {
                BentoHeroCard(
                    title = "High-Yield Earnings",
                    subtitle = "Watch 30s for 100+ Coins",
                    actionText = "Boost Wallet",
                    onClick = { navController.navigate("watch_ads") }
                )
            }

            // High Revenue Context: Personal Finance Quiz
            item {
                BentoSmallCard(
                    title = "Wealth Quiz",
                    subtitle = "Learn Banking",
                    icon = Icons.Default.AccountBalance,
                    gradient = Brush.linearGradient(listOf(Color(0xFF11998e), Color(0xFF38ef7d))),
                    onClick = { navController.navigate("quiz_categories") }
                )
            }

            // Engagement: Daily Streak
            item {
                BentoSmallCard(
                    title = "Daily Streak",
                    subtitle = "X2 Multiplier",
                    icon = Icons.Default.Bolt,
                    gradient = Brush.linearGradient(listOf(Color(0xFFFF512F), Color(0xFFDD2476))),
                    onClick = { /* Daily Bonus Logic */ }
                )
            }

            // Quick Action: Games
            item {
                BentoSmallCard(
                    title = "Finance Arena",
                    subtitle = "Play & Earn",
                    icon = Icons.Default.SportsEsports,
                    backgroundColor = CardSurface,
                    onClick = { navController.navigate("finance_game") }
                )
            }

            // Quick Action: Redeem
            item {
                BentoSmallCard(
                    title = "Redeem",
                    subtitle = "Cash Out Now",
                    icon = Icons.Default.Payments,
                    backgroundColor = CardSurface,
                    onClick = { navController.navigate("redeem") }
                )
            }

            // Invite Section (Full Width)
            item(span = { GridItemSpan(maxLineSpan) }) {
                InviteBanner {
                    Toast.makeText(context, "Link Copied!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

@Composable
fun ModernTopBar(name: String, coins: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Welcome back,",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Text(
                    text = name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            }
            // Glassy Coin Display
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.MonetizationOn,
                        contentDescription = "Coins",
                        tint = Color.Yellow,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = coins.toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun BentoHeroCard(
    title: String,
    subtitle: String,
    actionText: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(listOf(AccentPurple, AccentBlue))
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.align(Alignment.CenterStart)) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp
            )
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.2f)
            ) {
                Text(
                    text = actionText,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 12.sp
                )
            }
        }
        Icon(
            Icons.Default.TrendingUp,
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .align(Alignment.CenterEnd)
                .alpha(0.2f),
            tint = Color.White
        )
    }
}

@Composable
fun BentoSmallCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    backgroundColor: Color = CardSurface,
    gradient: Brush? = null,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                gradient ?: Brush.linearGradient(
                    listOf(backgroundColor, backgroundColor)
                )
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(20.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(
            text = subtitle,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp
        )
    }
}

@Composable
fun InviteBanner(onInvite: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF1E1E1E))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onInvite() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.GroupAdd,
                contentDescription = null,
                tint = AccentPurple
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Invite Friends & Earn 500+",
                color = Color.White,
                fontSize = 14.sp
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.Gray
        )
    }
}