package com.example.myapplication.domain.repository

import com.example.myapplication.domain.model.GameSettings
import com.example.myapplication.domain.model.Player

interface GameRepository {
    fun savePlayer(player: Player)
    fun getPlayer(): Player?
    fun saveGameSettings(settings: GameSettings)
    fun getGameSettings(): GameSettings
    fun saveHighScore(score: Int)
    fun getHighScore(): Int
}