package com.example.myapplication.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "score_records",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"])]
)
data class ScoreRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val userName: String, // Добавлено имя пользователя
    val score: Int,

    // Все настройки игры
    val gameSpeed: Int,
    val maxCockroaches: Int,
    val bonusInterval: Int,
    val roundDuration: Int,

    // Дополнительная информация
    val timestamp: Long = System.currentTimeMillis()
)