package com.example.myapplication.domain.usecase

import com.example.myapplication.domain.model.InsectType
import kotlin.math.ceil

class GameLogicUseCase {

    fun calculatePoints(insectType: InsectType, goldRate: Double = 5000.0): Int {
        return when (insectType) {
            InsectType.REGULAR -> 10
            InsectType.FAST -> 15
            InsectType.RARE -> 100
            InsectType.BONUS -> 50
            InsectType.PENALTY -> -15
            InsectType.GOLDEN -> ceil(goldRate / 100).toInt().coerceAtLeast(1)
        }
    }

    fun shouldSpawnInsect(currentCount: Int, maxCount: Int, gameSpeed: Int): Boolean {
        return currentCount < maxCount && kotlin.random.Random.nextInt(100) < (10 + gameSpeed)
    }

    fun shouldSpawnBonus(lastBonusTime: Long, bonusInterval: Int, gameSpeed: Int): Boolean {
        val adjustedInterval = (bonusInterval * 1000L / (gameSpeed * 0.5f + 0.5f)).toLong()
        return System.currentTimeMillis() - lastBonusTime > adjustedInterval
    }

    fun shouldSpawnGoldBug(lastGoldBugTime: Long): Boolean {
        return System.currentTimeMillis() - lastGoldBugTime > 20000L // 20 секунд
    }
}