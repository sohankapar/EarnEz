package com.dksport.earnez.models

data class RedemptionOption(
    val id: String,
    val title: String,
    val description: String,
    val coinsRequired: Int,
    val type: String
)