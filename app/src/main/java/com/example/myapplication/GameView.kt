package com.example.myapplication

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import kotlin.random.Random
import kotlin.math.sqrt

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val insects = mutableListOf<Insect>()
    private val paint = Paint()
    private var lastUpdateTime = 0L
    private var gameSpeed = 5
    private var maxCockroaches = 10
    private var bonusInterval = 30
    private var isGameRunning = false
    private var lastBonusTime = 0L

    private val gameHandler = Handler(Looper.getMainLooper())

    // Увеличиваем размеры жуков
    private var regularBugBitmap: Bitmap? = null
    private var fastBugBitmap: Bitmap? = null
    private var rareBugBitmap: Bitmap? = null
    private var bonusBitmap: Bitmap? = null
    private var penaltyBitmap: Bitmap? = null
    private var backgroundBitmap: Bitmap? = null

    private var onInsectClickListener: ((Insect) -> Unit)? = null
    private var onMissListener: (() -> Unit)? = null

    companion object {
        private const val TAG = "GameView"
    }

    private val updateRunnable = object : Runnable {
        override fun run() {
            try {
                if (isGameRunning) {
                    updateGame()
                    invalidate()
                    gameHandler.postDelayed(this, 16)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in game loop", e)
            }
        }
    }

    init {
        try {
            setupBitmaps()
            paint.isAntiAlias = true
            Log.d(TAG, "GameView initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing GameView", e)
        }
    }

    private fun setupBitmaps() {
        try {
            // Загружаем фон
            backgroundBitmap = loadBackgroundFromResource(R.drawable.game_background)

            // Увеличиваем размеры жуков (теперь они больше)
            regularBugBitmap = loadBitmapFromResource(R.drawable.bug_regular, 120) // было 80
            fastBugBitmap = loadBitmapFromResource(R.drawable.bug_fast, 110)       // было 70
            rareBugBitmap = loadBitmapFromResource(R.drawable.bug_rare, 140)       // было 90
            bonusBitmap = loadBitmapFromResource(R.drawable.bonus, 80)             // было 50
            penaltyBitmap = loadBitmapFromResource(R.drawable.penalty, 80)         // было 50

            Log.d(TAG, "All bitmaps loaded successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Error loading PNG bitmaps, creating fallback", e)
            // Если PNG не загрузились, создаем цветные круги
            createFallbackBitmaps()
        }

        // Проверяем что все bitmap созданы
        if (regularBugBitmap == null || fastBugBitmap == null || rareBugBitmap == null) {
            Log.w(TAG, "Some bitmaps are null, creating fallback")
            createFallbackBitmaps()
        }
    }

    private fun loadBackgroundFromResource(resId: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = false
                inScaled = false
            }

            BitmapFactory.decodeResource(resources, resId, options)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading background bitmap", e)
            null
        }
    }

    private fun loadBitmapFromResource(resId: Int, targetSize: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = false
                inScaled = false
            }

            val bitmap = BitmapFactory.decodeResource(resources, resId, options)
            if (bitmap != null) {
                // Масштабируем до нужного размера
                scaleBitmap(bitmap, targetSize)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading bitmap for resource $resId", e)
            null
        }
    }

    private fun createFallbackBitmaps() {
        Log.d(TAG, "Creating fallback bitmaps")
        // Увеличиваем размеры fallback жуков
        regularBugBitmap = createColoredBugBitmap(Color.GREEN, 120, "Обычный")
        fastBugBitmap = createColoredBugBitmap(Color.BLUE, 110, "Быстрый")
        rareBugBitmap = createColoredBugBitmap(Color.YELLOW, 140, "Редкий")
        bonusBitmap = createColoredBugBitmap(Color.MAGENTA, 80, "Бонус")
        penaltyBitmap = createColoredBugBitmap(Color.RED, 80, "Штраф")
    }

    private fun createColoredBugBitmap(color: Int, size: Int, label: String): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            this.color = color
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // Рисуем тело жука (эллипс) - больше
        canvas.drawOval(15f, size * 0.2f, size - 15f, size * 0.8f, paint)

        // Рисуем голову (круг) - больше
        canvas.drawCircle(size * 0.85f, size * 0.5f, size * 0.2f, paint)

        // Рисуем глаза
        paint.color = Color.WHITE
        canvas.drawCircle(size * 0.8f, size * 0.4f, size * 0.05f, paint)
        canvas.drawCircle(size * 0.8f, size * 0.6f, size * 0.05f, paint)

        // Рисуем усики
        paint.color = color
        paint.strokeWidth = 3f
        canvas.drawLine(size * 0.85f, size * 0.3f, size * 0.95f, size * 0.2f, paint)
        canvas.drawLine(size * 0.85f, size * 0.7f, size * 0.95f, size * 0.8f, paint)

        // Рисуем текст для отличия
        val textPaint = Paint().apply {
            this.color = Color.BLACK
            textSize = size * 0.15f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText(label, size / 2f, size * 0.95f, textPaint)

        return bitmap
    }

    private fun scaleBitmap(bitmap: Bitmap, targetSize: Int): Bitmap {
        val scale = targetSize.toFloat() / bitmap.width.toFloat()
        val newWidth = (bitmap.width * scale).toInt()
        val newHeight = (bitmap.height * scale).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    fun setGameSettings(speed: Int, maxCockroaches: Int, bonusInterval: Int) {
        this.gameSpeed = speed
        this.maxCockroaches = maxCockroaches
        this.bonusInterval = bonusInterval
        Log.d(TAG, "Game settings applied: speed=$speed, maxCockroaches=$maxCockroaches")
    }

    fun setOnInsectClickListener(listener: (Insect) -> Unit) {
        this.onInsectClickListener = listener
    }

    fun setOnMissListener(listener: () -> Unit) {
        this.onMissListener = listener
    }

    fun startGame() {
        try {
            isGameRunning = true
            lastUpdateTime = System.currentTimeMillis()
            lastBonusTime = System.currentTimeMillis()
            insects.clear()
            gameHandler.post(updateRunnable)
            Log.d(TAG, "Game started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting game", e)
        }
    }

    fun pauseGame() {
        isGameRunning = false
        gameHandler.removeCallbacks(updateRunnable)
    }

    fun resumeGame() {
        if (isGameRunning) return
        isGameRunning = true
        lastUpdateTime = System.currentTimeMillis()
        gameHandler.post(updateRunnable)
    }

    fun endGame() {
        isGameRunning = false
        gameHandler.removeCallbacks(updateRunnable)
        insects.clear()
        invalidate()
    }

    private fun updateGame() {
        if (!isGameRunning || width == 0 || height == 0) return

        val currentTime = System.currentTimeMillis()
        val deltaTime = (currentTime - lastUpdateTime) / 1000f
        lastUpdateTime = currentTime

        // Уменьшаем количество одновременно появляющихся жуков из-за их большего размера
        val totalBugCount = insects.count { it.type in listOf(InsectType.REGULAR, InsectType.FAST, InsectType.RARE) }
        if (totalBugCount < maxCockroaches && Random.nextInt(100) < (10 + gameSpeed)) {
            addRandomBug()
        }

        // Добавляем бонусы/штрафы
        val adjustedBonusInterval = (bonusInterval * 1000L / (gameSpeed * 0.5f + 0.5f)).toLong()
        if (currentTime - lastBonusTime > adjustedBonusInterval) {
            if (Random.nextBoolean()) {
                addRandomInsect(InsectType.BONUS)
            } else {
                addRandomInsect(InsectType.PENALTY)
            }
            lastBonusTime = currentTime
        }

        // Обновляем позиции
        val speedMultiplier = gameSpeed * 0.5f + 0.5f
        insects.forEach {
            it.update(deltaTime * speedMultiplier)
        }

        // Удаляем вышедших за границы (учитываем увеличенный размер)
        insects.removeAll { insect ->
            insect.x < -insect.bitmap.width ||
                    insect.x > width + insect.bitmap.width ||
                    insect.y < -insect.bitmap.height ||
                    insect.y > height + insect.bitmap.height
        }
    }

    private fun addRandomBug() {
        val random = Random.nextInt(100)
        val type = when {
            random < 60 -> InsectType.REGULAR
            random < 85 -> InsectType.FAST
            else -> InsectType.RARE
        }

        addRandomInsect(type)
    }

    private fun addRandomInsect(type: InsectType) {
        val bitmap = when (type) {
            InsectType.REGULAR -> regularBugBitmap
            InsectType.FAST -> fastBugBitmap
            InsectType.RARE -> rareBugBitmap
            InsectType.BONUS -> bonusBitmap
            InsectType.PENALTY -> penaltyBitmap
        } ?: return

        val side = Random.nextInt(4)
        val x: Float
        val y: Float

        when (side) {
            0 -> { // Слева
                x = -bitmap.width.toFloat()
                y = Random.nextInt(0, (height - bitmap.height).coerceAtLeast(1)).toFloat()
            }
            1 -> { // Справа
                x = width.toFloat()
                y = Random.nextInt(0, (height - bitmap.height).coerceAtLeast(1)).toFloat()
            }
            2 -> { // Сверху
                x = Random.nextInt(0, (width - bitmap.width).coerceAtLeast(1)).toFloat()
                y = -bitmap.height.toFloat()
            }
            else -> { // Снизу
                x = Random.nextInt(0, (width - bitmap.width).coerceAtLeast(1)).toFloat()
                y = height.toFloat()
            }
        }

        val targetX = width / 2f + Random.nextInt(-300, 300)
        val targetY = height / 2f + Random.nextInt(-300, 300)

        val dx = targetX - x
        val dy = targetY - y
        val length = sqrt(dx * dx + dy * dy)

        // Увеличиваем скорость для больших жуков
        val baseSpeed = when (type) {
            InsectType.REGULAR -> Random.nextInt(120, 200).toFloat()
            InsectType.FAST -> Random.nextInt(220, 320).toFloat()
            InsectType.RARE -> Random.nextInt(100, 170).toFloat()
            InsectType.BONUS -> Random.nextInt(100, 170).toFloat()
            InsectType.PENALTY -> Random.nextInt(140, 220).toFloat()
        }

        val health = when (type) {
            InsectType.RARE -> 3
            else -> 1
        }

        val insect = Insect(
            type = type,
            x = x,
            y = y,
            speedX = dx / length * baseSpeed,
            speedY = dy / length * baseSpeed,
            bitmap = bitmap,
            health = health,
            maxHealth = health
        )

        insects.add(insect)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Рисуем фон если он есть, иначе белый
        if (backgroundBitmap != null) {
            // Масштабируем фон под размер экрана
            val scaledBackground = Bitmap.createScaledBitmap(
                backgroundBitmap!!,
                width,
                height,
                true
            )
            canvas.drawBitmap(scaledBackground, 0f, 0f, paint)
        } else {
            canvas.drawColor(Color.WHITE)
        }

        // Рисуем всех насекомых
        insects.forEach { insect ->
            canvas.drawBitmap(insect.bitmap, insect.x, insect.y, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN && isGameRunning) {
            val x = event.x
            val y = event.y
            var hit = false

            val iterator = insects.iterator()
            while (iterator.hasNext()) {
                val insect = iterator.next()

                if (x >= insect.x && x <= insect.x + insect.bitmap.width &&
                    y >= insect.y && y <= insect.y + insect.bitmap.height) {

                    hit = true

                    when (insect.type) {
                        InsectType.RARE -> {
                            insect.health--
                            if (insect.health <= 0) {
                                onInsectClickListener?.invoke(insect)
                                iterator.remove()
                            } else {
                                onInsectClickListener?.invoke(insect.copy(health = insect.health))
                            }
                        }
                        else -> {
                            onInsectClickListener?.invoke(insect)
                            iterator.remove()
                        }
                    }
                    break
                }
            }

            if (!hit) {
                onMissListener?.invoke()
            }

            invalidate()
        }
        return true
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        endGame()
        gameHandler.removeCallbacksAndMessages(null)
    }
}

enum class InsectType { REGULAR, FAST, RARE, BONUS, PENALTY }

data class Insect(
    val type: InsectType,
    var x: Float,
    var y: Float,
    var speedX: Float,
    var speedY: Float,
    val bitmap: Bitmap,
    var health: Int = 1,
    val maxHealth: Int = 1
) {
    fun update(deltaTime: Float) {
        x += speedX * deltaTime
        y += speedY * deltaTime
    }
}