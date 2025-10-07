package com.example.myapplication.game.engine

import com.example.myapplication.game.engine.InsectFactory
import com.example.myapplication.domain.model.GameSettings
import com.example.myapplication.domain.model.Insect
import com.example.myapplication.domain.model.InsectType
import com.example.myapplication.game.sound.SoundManager
import kotlin.math.sqrt
import kotlin.random.Random

class GameEngine(
    private var settings: GameSettings,
    private val soundManager: SoundManager
) {
    private val insects = mutableListOf<Insect>()
    private var lastUpdateTime = 0L
    private var lastBonusTime = 0L
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

    var onTiltBonusChanged: ((Boolean) -> Unit)? = null

    fun setScreenSize(width: Int, height: Int) {
        this.screenWidth = width.coerceAtLeast(1)
        this.screenHeight = height.coerceAtLeast(1)
    }

    fun updateSettings(newSettings: GameSettings) {
        this.settings = newSettings
    }

    fun startGame() {
        isGameRunning = true
        lastUpdateTime = System.currentTimeMillis()
        lastBonusTime = System.currentTimeMillis()
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

        // Проверяем окончание гироскоп-бонуса
        if (isTiltBonusActive && currentTime > tiltBonusEndTime) {
            deactivateTiltBonus()
        }

        // Добавляем обычных жуков
        val totalBugCount = insects.count { it.type in listOf(InsectType.REGULAR, InsectType.FAST, InsectType.RARE) }
        if (totalBugCount < settings.maxCockroaches &&
            Random.nextInt(100) < (10 + settings.gameSpeed)) {
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

        // Обновляем позиции с учетом наклона
        val speedMultiplier = settings.gameSpeed * 0.5f + 0.5f
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
            insect.update(deltaTime * speedMultiplier)
        }

        // Удаляем вышедших за границы
        removeOutOfBoundsInsects()
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
}