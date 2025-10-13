// game/engine/InsectFactory.kt
package com.example.myapplication.game.engine

import com.example.myapplication.domain.model.Insect
import com.example.myapplication.domain.model.InsectType
import kotlin.math.sqrt
import kotlin.random.Random

object InsectFactory {

    // Статические поля для bitmap'ов
    var regularBugBitmap: android.graphics.Bitmap? = null
    var fastBugBitmap: android.graphics.Bitmap? = null
    var rareBugBitmap: android.graphics.Bitmap? = null
    var bonusBitmap: android.graphics.Bitmap? = null
    var penaltyBitmap: android.graphics.Bitmap? = null
    var goldBugBitmap: android.graphics.Bitmap? = null

    fun createInsect(
        type: InsectType,
        screenWidth: Int,
        screenHeight: Int,
        bitmap: android.graphics.Bitmap? = null
    ): Insect {
        // Используем переданный bitmap или берем из статических полей
        val actualBitmap = bitmap ?: when (type) {
            InsectType.REGULAR -> regularBugBitmap
            InsectType.FAST -> fastBugBitmap
            InsectType.RARE -> rareBugBitmap
            InsectType.BONUS -> bonusBitmap
            InsectType.PENALTY -> penaltyBitmap
            InsectType.GOLDEN -> goldBugBitmap
        } ?: createFallbackBitmap(type)

        val side = Random.nextInt(4)
        val (x, y) = calculateStartPosition(side, screenWidth, screenHeight, actualBitmap)
        val (targetX, targetY) = calculateTargetPosition(screenWidth, screenHeight)

        val dx = targetX - x
        val dy = targetY - y
        val length = sqrt(dx * dx + dy * dy)

        val baseSpeed = when (type) {
            InsectType.REGULAR -> Random.nextInt(120, 200).toFloat()
            InsectType.FAST -> Random.nextInt(220, 320).toFloat()
            InsectType.RARE -> Random.nextInt(100, 170).toFloat()
            InsectType.BONUS -> Random.nextInt(80, 120).toFloat()
            InsectType.PENALTY -> Random.nextInt(140, 220).toFloat()
            InsectType.GOLDEN -> Random.nextInt(100, 180).toFloat()
        }

        val health = when (type) {
            InsectType.RARE -> 3
            else -> 1
        }

        return Insect(
            type = type,
            x = x,
            y = y,
            speedX = dx / length * baseSpeed,
            speedY = dy / length * baseSpeed,
            bitmap = actualBitmap,
            health = health,
            maxHealth = health
        )
    }

    fun createGoldBug(screenWidth: Int, screenHeight: Int, bitmap: android.graphics.Bitmap? = null): Insect {
        return createInsect(InsectType.GOLDEN, screenWidth, screenHeight, bitmap)
    }

    private fun calculateStartPosition(
        side: Int,
        screenWidth: Int,
        screenHeight: Int,
        bitmap: android.graphics.Bitmap
    ): Pair<Float, Float> {
        val safeWidth = screenWidth.coerceAtLeast(1)
        val safeHeight = screenHeight.coerceAtLeast(1)
        val bitmapWidth = bitmap.width.coerceAtLeast(1)
        val bitmapHeight = bitmap.height.coerceAtLeast(1)

        return when (side) {
            0 -> Pair(-bitmapWidth.toFloat(), Random.nextInt(0, (safeHeight - bitmapHeight).coerceAtLeast(1)).toFloat())
            1 -> Pair(safeWidth.toFloat(), Random.nextInt(0, (safeHeight - bitmapHeight).coerceAtLeast(1)).toFloat())
            2 -> Pair(Random.nextInt(0, (safeWidth - bitmapWidth).coerceAtLeast(1)).toFloat(), -bitmapHeight.toFloat())
            else -> Pair(Random.nextInt(0, (safeWidth - bitmapWidth).coerceAtLeast(1)).toFloat(), safeHeight.toFloat())
        }
    }

    private fun calculateTargetPosition(screenWidth: Int, screenHeight: Int): Pair<Float, Float> {
        val safeWidth = screenWidth.coerceAtLeast(1)
        val safeHeight = screenHeight.coerceAtLeast(1)

        return Pair(
            safeWidth / 2f + Random.nextInt(-300, 301),
            safeHeight / 2f + Random.nextInt(-300, 301)
        )
    }

    private fun createFallbackBitmap(type: InsectType): android.graphics.Bitmap {
        val size = when (type) {
            InsectType.REGULAR -> 120
            InsectType.FAST -> 110
            InsectType.RARE -> 140
            InsectType.BONUS -> 80
            InsectType.PENALTY -> 80
            InsectType.GOLDEN -> 100
        }

        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint().apply {
            color = getColorForType(type)
            style = android.graphics.Paint.Style.FILL
            isAntiAlias = true
        }

        // Простая отрисовка жука (упрощенная версия)
        canvas.drawOval(15f, size * 0.2f, size - 15f, size * 0.8f, paint)

        return bitmap
    }

    private fun getColorForType(type: InsectType): Int {
        return when (type) {
            InsectType.REGULAR -> android.graphics.Color.GREEN
            InsectType.FAST -> android.graphics.Color.BLUE
            InsectType.RARE -> android.graphics.Color.YELLOW
            InsectType.BONUS -> android.graphics.Color.CYAN
            InsectType.PENALTY -> android.graphics.Color.RED
            InsectType.GOLDEN -> android.graphics.Color.argb(255, 255, 215, 0) // золотой
        }
    }
}