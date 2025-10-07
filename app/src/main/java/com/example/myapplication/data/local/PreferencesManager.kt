package com.example.myapplication.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.myapplication.domain.model.GameSettings
import com.example.myapplication.domain.model.Player
import com.example.myapplication.domain.repository.GameRepository

class PreferencesManager(context: Context) : GameRepository {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("game_preferences", Context.MODE_PRIVATE)

    override fun savePlayer(player: Player) {
        with(sharedPreferences.edit()) {
            putString("player_name", player.fullName)
            putString("player_gender", player.gender)
            putString("player_course", player.course)
            putInt("player_difficulty", player.difficulty)
            putLong("player_birth_date", player.birthDate)
            putString("player_zodiac", player.zodiacSign)
            apply()
        }
    }

    override fun getPlayer(): Player? {
        val name = sharedPreferences.getString("player_name", null) ?: return null
        val gender = sharedPreferences.getString("player_gender", "") ?: ""
        val course = sharedPreferences.getString("player_course", "") ?: ""
        val difficulty = sharedPreferences.getInt("player_difficulty", 5)
        val birthDate = sharedPreferences.getLong("player_birth_date", 0)
        val zodiac = sharedPreferences.getString("player_zodiac", "") ?: ""

        return Player(name, gender, course, difficulty, birthDate, zodiac)
    }

    override fun saveGameSettings(settings: GameSettings) {
        with(sharedPreferences.edit()) {
            putInt("game_speed", settings.gameSpeed)
            putInt("max_cockroaches", settings.maxCockroaches)
            putInt("bonus_interval", settings.bonusInterval)
            putInt("round_duration", settings.roundDuration)
            apply()
        }
    }

    override fun getGameSettings(): GameSettings {
        return GameSettings(
            gameSpeed = sharedPreferences.getInt("game_speed", 5),
            maxCockroaches = sharedPreferences.getInt("max_cockroaches", 10),
            bonusInterval = sharedPreferences.getInt("bonus_interval", 30),
            roundDuration = sharedPreferences.getInt("round_duration", 120)
        )
    }

    override fun saveHighScore(score: Int) {
        val currentHighScore = getHighScore()
        if (score > currentHighScore) {
            sharedPreferences.edit().putInt("high_score", score).apply()
        }
    }

    override fun getHighScore(): Int {
        return sharedPreferences.getInt("high_score", 0)
    }
}