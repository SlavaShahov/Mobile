package com.example.myapplication.game.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.example.myapplication.R
import kotlin.math.ceil
import kotlin.math.min

class GoldRateWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 28f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private var goldRate: Double = 0.0
    private var pointsPerGoldBug: Int = 0

    fun updateGoldRate(rate: Double) {
        this.goldRate = rate
        this.pointsPerGoldBug = ceil(rate / 100).toInt()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = min(width, height) / 2f - 10

        // Рисуем шар
        paint.color = Color.argb(200, 255, 215, 0) // золотой цвет с прозрачностью
        canvas.drawCircle(centerX, centerY, radius, paint)

        // Обводка
        paint.color = Color.YELLOW
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawCircle(centerX, centerY, radius, paint)

        // Текст
        paint.style = Paint.Style.FILL
        val rateText = "₽ ${goldRate.toInt()}"
        val pointsText = "За жука: $pointsPerGoldBug"

        canvas.drawText(rateText, centerX, centerY - 15, textPaint)

        textPaint.textSize = 18f
        canvas.drawText(pointsText, centerX, centerY + 25, textPaint)
    }
}