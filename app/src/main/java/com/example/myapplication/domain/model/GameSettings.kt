package com.example.myapplication.domain.model

data class GameSettings(
    val gameSpeed: Int = 5,
    val maxCockroaches: Int = 10,
    val bonusInterval: Int = 30,
    val roundDuration: Int = 120
)