package com.example.myapplication.data.local.dao

import androidx.room.*
import com.example.myapplication.data.local.entity.ScoreRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ScoreDao {
    @Query("SELECT * FROM score_records ORDER BY score DESC LIMIT :limit")
    fun getTopScores(limit: Int): Flow<List<ScoreRecord>>

    @Query("SELECT * FROM score_records WHERE userId = :userId ORDER BY score DESC LIMIT 10")
    fun getUserScores(userId: Long): Flow<List<ScoreRecord>>

    @Query("SELECT MAX(score) FROM score_records WHERE userId = :userId")
    fun getUserHighScore(userId: Long): Flow<Int?>

    @Insert
    fun insertScore(scoreRecord: ScoreRecord)

    @Query("DELETE FROM score_records WHERE userId = :userId")
    fun deleteUserScores(userId: Long)

    @Query("DELETE FROM score_records WHERE id NOT IN (SELECT id FROM score_records ORDER BY score DESC LIMIT 50)")
    fun cleanupOldScores()

    @Query("DELETE FROM score_records")
    fun deleteAllScores()
}