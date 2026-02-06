package com.dksport.earnez.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.dksport.earnez.AdHelper
import com.dksport.earnez.ui.components.BannerAdView
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuzzleGameScreen(
    navController: NavHostController,
    onCoinChange: (Int) -> Unit,
    onLoyaltyChange: (Int) -> Unit
) {
    val puzzleSize = 4
    val solvedPuzzle = List(puzzleSize * puzzleSize) { it }
    var puzzle by remember { mutableStateOf(solvedPuzzle.shuffled()) }
    var moves by remember { mutableStateOf(0) }
    var showHint by remember { mutableStateOf(false) }
    var gameSolved by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val auth = Firebase.auth
    val db = Firebase.firestore
    val scope = rememberCoroutineScope()

    fun isSolvable(): Boolean {
        // Simple solvability check
        return puzzle.filter { it != 0 }.withIndex().count { (i, value) ->
            puzzle.drop(i + 1).count { it < value && it != 0 } % 2 == 0
        } % 2 == 0
    }

    fun moveTile(index: Int) {
        val emptyIndex = puzzle.indexOf(0)
        val row = index / puzzleSize
        val col = index % puzzleSize
        val emptyRow = emptyIndex / puzzleSize
        val emptyCol = emptyIndex % puzzleSize

        if ((row == emptyRow && Math.abs(col - emptyCol) == 1) ||
            (col == emptyCol && Math.abs(row - emptyRow) == 1)
        ) {
            val newPuzzle = puzzle.toMutableList()
            newPuzzle[emptyIndex] = puzzle[index]
            newPuzzle[index] = 0
            puzzle = newPuzzle
            moves++

            // Check if solved
            if (puzzle == solvedPuzzle) {
                gameSolved = true
            }
        }
    }

    LaunchedEffect(Unit) {
        // Ensure puzzle is solvable
        while (!isSolvable()) {
            puzzle = solvedPuzzle.shuffled()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Puzzle Game") },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Sliding Puzzle", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Moves: $moves", fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))

            if (showHint) {
                Text(
                    "Hint: Try to solve row by row from top to bottom",
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (gameSolved) {
                val earnedCoins = 100
                val earnedLoyalty = 20
                Text("Puzzle Solved!", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Moves: $moves", fontSize = 24.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("You earned $earnedCoins coins!", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                Text("+$earnedLoyalty loyalty points!", fontSize = 18.sp, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        val userId = auth.currentUser?.uid
                        if (userId != null) {
                            scope.launch {
                                try {
                                    db.collection("users").document(userId).update(
                                        "coins", FieldValue.increment(earnedCoins.toLong()),
                                        "loyaltyPoints", FieldValue.increment(earnedLoyalty.toLong())
                                    ).await()
                                    onCoinChange(earnedCoins)
                                    onLoyaltyChange(earnedLoyalty)
                                    android.widget.Toast.makeText(context, "Rewards added!", android.widget.Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Failed to add rewards", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Finish Game")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(puzzleSize),
                    modifier = Modifier.size(300.dp)
                ) {
                    items(puzzle.size) { index ->
                        val number = puzzle[index]
                        if (number != 0) {
                            Card(
                                onClick = { moveTile(index) },
                                modifier = Modifier
                                    .padding(4.dp)
                                    .aspectRatio(1f),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$number",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier
                                .padding(4.dp)
                                .aspectRatio(1f)
                                .background(Color.LightGray))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        AdHelper.showRewardedAd(
                            context = context,
                            onRewardEarned = { showHint = true },
                            onAdDismissed = { /* Do nothing */ },
                            onAdFailed = { android.widget.Toast.makeText(context, "Ad failed to load", android.widget.Toast.LENGTH_SHORT).show() }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Get Hint (Ad)")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            BannerAdView()
        }
    }
}