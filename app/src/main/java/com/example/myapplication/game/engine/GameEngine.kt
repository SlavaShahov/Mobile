// game/engine/GameEngine.kt
package com.example.myapplication.game.engine

import android.util.Log
import com.example.myapplication.game.engine.InsectFactory
import com.example.myapplication.domain.model.GameSettings
import com.example.myapplication.domain.model.Insect
import com.example.myapplication.domain.model.InsectType
import com.example.myapplication.game.sound.SoundManager
import kotlin.math.ceil
import kotlin.math.sqrt
import kotlin.random.Random

class GameEngine(
    private var settings: GameSettings,
    private val soundManager: SoundManager
) {
    private val insects = mutableListOf<Insect>()
    private var lastUpdateTime = 0L
    private var lastBonusTime = 0L
    private var lastGoldBugTime = 0L
    private var isGameRunning = false

    // Гироскоп-бонус
    private var isTiltBonusActive = false
    private var tiltBonusEndTime = 0L
    private val TILT_BONUS_DURATION = 10000L

    // Для управления криками при скатывании
    private var lastScreamTime = 0L
    private val SCREAM_INTERVAL = 800L // Интервал между криками

    // Размеры экрана
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0

    // Текущие значения наклона
    private var currentTiltX = 0f
    private var currentTiltY = 0f

    // Курс золота
    private var currentGoldRate: Double = 5000.0
    private var goldBugPoints: Int = 50

    var onTiltBonusChanged: ((Boolean) -> Unit)? = null
    var onGoldRateUpdated: ((Double, Int) -> Unit)? = null

    fun setScreenSize(width: Int, height: Int) {
        this.screenWidth = width.coerceAtLeast(1)
        this.screenHeight = height.coerceAtLeast(1)
    }

    fun updateSettings(newSettings: GameSettings) {
        this.settings = newSettings
        Log.d("GameEngine", "Settings updated: speed=${newSettings.gameSpeed}")
    }

    fun updateGoldRate(rate: Double) {
        this.currentGoldRate = rate
        this.goldBugPoints = ceil(rate / 100).toInt().coerceAtLeast(1)
        onGoldRateUpdated?.invoke(rate, goldBugPoints)
    }

    fun startGame() {
        isGameRunning = true
        lastUpdateTime = System.currentTimeMillis()
        lastBonusTime = System.currentTimeMillis()
        lastGoldBugTime = System.currentTimeMillis()
        insects.clear()
    }

    fun pauseGame() {
        isGameRunning = false
    }

    fun resumeGame() {
        isGameRunning = true
        lastUpdateTime = System.currentTimeMillis()
    }

    fun endGame() {
        isGameRunning = false
        deactivateTiltBonus()
        insects.clear()
    }

    fun updateGame(deltaTime: Float) {
        if (!isGameRunning || screenWidth == 0 || screenHeight == 0) return

        val currentTime = System.currentTimeMillis()

        // ФИКС: Нормальная прогрессия скорости (убрал * 0.3f)
        val speedMultiplier = when (settings.gameSpeed) {
            1 -> 0.3f   // ОЧЕНЬ МЕДЛЕННО
            2 -> 0.6f
            3 -> 0.9f
            4 -> 1.2f
            5 -> 1.5f   // НОРМАЛЬНО
            6 -> 2.0f
            7 -> 2.5f
            8 -> 3.0f
            9 -> 3.5f
            10 -> 4.0f  // ОЧЕНЬ БЫСТРО
            else -> 1.5f
        }

        // Проверяем окончание гироскоп-бонуса
        if (isTiltBonusActive && currentTime > tiltBonusEndTime) {
            deactivateTiltBonus()
        }

        // Добавляем обычных жуков
        val totalBugCount = insects.count { it.type in listOf(InsectType.REGULAR, InsectType.FAST, InsectType.RARE) }
        if (totalBugCount < settings.maxCockroaches &&
            Random.nextInt(100) < (15 + settings.gameSpeed * 2)) {
            spawnRandomBug()
        }

        // Добавляем бонусы/штрафы
        if (shouldSpawnBonus(currentTime)) {
            if (Random.nextBoolean()) {
                spawnInsect(InsectType.BONUS)
            } else {
                spawnInsect(InsectType.PENALTY)
            }
            lastBonusTime = currentTime
        }

        // Добавляем золотого жука
        if (shouldSpawnGoldBug(currentTime)) {
            spawnGoldBug()
            lastGoldBugTime = currentTime
        }

        // Обновляем позиции - УБРАЛ * 0.3f !!!
        insects.forEach { insect ->
            if (isTiltBonusActive) {
                // При активном бонусе добавляем силу от наклона
                val TILT_FORCE_MULTIPLIER = 800f
                insect.speedX += currentTiltX * deltaTime * TILT_FORCE_MULTIPLIER
                insect.speedY += currentTiltY * deltaTime * TILT_FORCE_MULTIPLIER

                // Ограничиваем максимальную скорость
                val currentSpeed = sqrt(insect.speedX * insect.speedX + insect.speedY * insect.speedY)
                val maxSpeed = when (insect.type) {
                    InsectType.FAST -> 600f
                    else -> 500f
                }
                if (currentSpeed > maxSpeed) {
                    insect.speedX = insect.speedX / currentSpeed * maxSpeed
                    insect.speedY = insect.speedY / currentSpeed * maxSpeed
                }

                // ПРОВЕРЯЕМ СКАТЫВАНИЕ К УГЛУ И ПРОИГРЫВАЕМ КРИКИ
                checkCornerRolling(insect, currentTime)
            }
            // ТЕПЕРЬ БЕЗ ДОПОЛНИТЕЛЬНОГО ЗАМЕДЛЕНИЯ!
            insect.update(deltaTime * speedMultiplier)
        }

        // Удаляем вышедших за границы
        removeOutOfBoundsInsects()
    }

    fun getCurrentSettings(): GameSettings {
        return settings
    }

    private fun checkCornerRolling(insect: Insect, currentTime: Long) {
        if (currentTime - lastScreamTime < SCREAM_INTERVAL) return

        // Простая проверка: если жук близко к краю и гироскоп активен
        val edgeThreshold = 100f
        val isNearEdge =
            insect.x <= edgeThreshold ||
                    insect.x >= screenWidth - edgeThreshold ||
                    insect.y <= edgeThreshold ||
                    insect.y >= screenHeight - edgeThreshold

        if (isNearEdge && isTiltBonusActive) {
            // Случайная вероятность крика 30%
            if (Random.nextInt(100) < 30) {
                soundManager.playRollingScream()
                lastScreamTime = currentTime
            }
        }
    }

    fun updateTilt(tiltX: Float, tiltY: Float) {
        currentTiltX = tiltX
        currentTiltY = tiltY
    }

    private fun spawnRandomBug() {
        val random = Random.nextInt(100)
        val type = when {
            random < 60 -> InsectType.REGULAR
            random < 85 -> InsectType.FAST
            else -> InsectType.RARE
        }
        spawnInsect(type)
    }

    private fun spawnInsect(type: InsectType) {
        val insect = InsectFactory.createInsect(type, screenWidth, screenHeight)
        insects.add(insect)
    }

    private fun spawnGoldBug() {
        val insect = InsectFactory.createGoldBug(screenWidth, screenHeight)
        insects.add(insect)
    }

    fun getInsects(): List<Insect> {
        return insects.toList()
    }

    fun removeInsect(insect: Insect) {
        insects.remove(insect)
    }

    fun checkCollision(x: Float, y: Float): Insect? {
        return insects.find { insect ->
            x >= insect.x && x <= insect.x + insect.bitmap.width &&
                    y >= insect.y && y <= insect.y + insect.bitmap.height
        }
    }

    fun activateTiltBonus() {
        isTiltBonusActive = true
        tiltBonusEndTime = System.currentTimeMillis() + TILT_BONUS_DURATION
        soundManager.playTiltBonusSound()
        onTiltBonusChanged?.invoke(true)
    }

    private fun deactivateTiltBonus() {
        isTiltBonusActive = false
        currentTiltX = 0f
        currentTiltY = 0f
        onTiltBonusChanged?.invoke(false)
    }

    private fun shouldSpawnBonus(currentTime: Long): Boolean {
        val adjustedInterval = (settings.bonusInterval * 1000L / (settings.gameSpeed * 0.5f + 0.5f)).toLong()
        return currentTime - lastBonusTime > adjustedInterval
    }

    private fun shouldSpawnGoldBug(currentTime: Long): Boolean {
        return currentTime - lastGoldBugTime > 20000L // 20 секунд
    }

    private fun removeOutOfBoundsInsects() {
        insects.removeAll { insect ->
            insect.x < -insect.bitmap.width ||
                    insect.x > screenWidth + insect.bitmap.width ||
                    insect.y < -insect.bitmap.height ||
                    insect.y > screenHeight + insect.bitmap.height
        }
    }

    fun isTiltBonusActive(): Boolean = isTiltBonusActive
    fun getTiltBonusTimeLeft(): Float = ((tiltBonusEndTime - System.currentTimeMillis()) / 1000f).coerceAtLeast(0f)
    fun getGoldBugPoints(): Int = goldBugPoints
}