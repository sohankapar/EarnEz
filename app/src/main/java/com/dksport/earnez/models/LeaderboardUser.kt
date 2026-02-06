package com.dksport.earnez.models

data class LeaderboardUser(
    val username: String,
    val coins: Int,
    val loyaltyPoints: Int,
    val userId: String
)