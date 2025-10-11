package com.example.myapplication.domain.repository

import com.example.myapplication.domain.model.GameSettings
import com.example.myapplication.domain.model.Player

interface GameRepository {
    suspend fun savePlayer(player: Player)
    suspend fun getPlayer(): Player?
    fun saveGameSettings(settings: GameSettings)
    fun getGameSettings(): GameSettings
    suspend fun saveHighScore(score: Int)
    fun getHighScore(): Int
}