package com.example.myapplication.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.myapplication.data.local.database.AppDatabase
import com.example.myapplication.domain.model.GameSettings
import com.example.myapplication.domain.model.Player
import com.example.myapplication.domain.repository.GameRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class PreferencesManager(context: Context) : GameRepository {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("game_preferences", Context.MODE_PRIVATE)

    private val database = AppDatabase.getInstance(context)
    private val userDao = database.userDao()
    private val scoreDao = database.scoreDao()

    private var currentUserId: Long
        get() = sharedPreferences.getLong("current_user_id", -1L)
        set(value) = sharedPreferences.edit().putLong("current_user_id", value).apply()

    suspend fun savePlayer(player: Player, password: String) {
        Log.d("PreferencesManager", "Saving player: ${player.fullName}")

        val user = com.example.myapplication.data.local.entity.User(
            fullName = player.fullName,
            gender = player.gender,
            course = player.course,
            difficulty = player.difficulty,
            birthDate = player.birthDate,
            zodiacSign = player.zodiacSign,
            password = password
        )

        userDao.insertUser(user)
        Log.d("PreferencesManager", "User inserted into database")

        // Получаем ID после вставки через Flow
        try {
            val insertedUser = userDao.getUserByName(player.fullName).first()
            currentUserId = insertedUser.id
            Log.d("PreferencesManager", "User saved with ID: $currentUserId")
        } catch (e: Exception) {
            Log.e("PreferencesManager", "Error getting user ID after insert", e)
        }
    }

    override suspend fun savePlayer(player: Player) {
        savePlayer(player, "default")
    }

    override suspend fun getPlayer(): Player? {
        val userId = currentUserId
        if (userId == -1L) return null

        return try {
            val user = userDao.getUserById(userId).first()
            Player(
                fullName = user.fullName,
                gender = user.gender,
                course = user.course,
                difficulty = user.difficulty,
                birthDate = user.birthDate,
                zodiacSign = user.zodiacSign
            )
        } catch (e: Exception) {
            null
        }
    }

    fun getAllUsers(): Flow<List<Player>> {
        return userDao.getAllUsers().map { users ->
            users.map { user ->
                Player(
                    fullName = user.fullName,
                    gender = user.gender,
                    course = user.course,
                    difficulty = user.difficulty,
                    birthDate = user.birthDate,
                    zodiacSign = user.zodiacSign
                )
            }
        }
    }

    suspend fun setCurrentUser(userId: Long) {
        currentUserId = userId
    }

    suspend fun loginUser(fullName: String, password: String): Boolean {
        return try {
            val user = userDao.getUserByName(fullName).first()
            if (user.password == password) {
                currentUserId = user.id
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun saveScore(score: Int, gameSettings: GameSettings) {
        val userId = currentUserId
        if (userId == -1L) return

        val currentUser = getPlayer()
        if (currentUser == null) {
            Log.e("PreferencesManager", "No current user found")
            return
        }

        val existingScores = scoreDao.getUserScores(userId).first()
        val similarScoreExists = existingScores.any {
            it.score == score &&
                    it.gameSpeed == gameSettings.gameSpeed &&
                    it.maxCockroaches == gameSettings.maxCockroaches &&
                    it.bonusInterval == gameSettings.bonusInterval &&
                    it.roundDuration == gameSettings.roundDuration
        }

        if (!similarScoreExists) {
            val scoreRecord = com.example.myapplication.data.local.entity.ScoreRecord(
                userId = userId,
                userName = currentUser.fullName,
                score = score,
                gameSpeed = gameSettings.gameSpeed,
                maxCockroaches = gameSettings.maxCockroaches,
                bonusInterval = gameSettings.bonusInterval,
                roundDuration = gameSettings.roundDuration
            )
            scoreDao.insertScore(scoreRecord)

            scoreDao.cleanupOldScores()

            Log.d("PreferencesManager", "Score saved: $score for user: ${currentUser.fullName}")
        } else {
            Log.d("PreferencesManager", "Similar score already exists: $score")
        }
    }

    fun getTopScores(): Flow<List<com.example.myapplication.data.local.entity.ScoreRecord>> {
        return scoreDao.getTopScores(5)
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

    override suspend fun saveHighScore(score: Int) {
        val currentHighScore = getHighScore()
        if (score > currentHighScore) {
            sharedPreferences.edit().putInt("high_score", score).apply()
        }
    }

    override fun getHighScore(): Int {
        return sharedPreferences.getInt("high_score", 0)
    }
}