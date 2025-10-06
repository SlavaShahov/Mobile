package com.example.myapplication

import android.content.Context
import android.graphics.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import kotlin.random.Random
import kotlin.math.sqrt
import kotlin.math.abs

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), SensorEventListener {

    private val insects = mutableListOf<Insect>()
    private val paint = Paint()
    private var lastUpdateTime = 0L
    private var gameSpeed = 5
    private var maxCockroaches = 10
    private var bonusInterval = 30
    private var isGameRunning = false
    private var lastBonusTime = 0L

    // Гироскоп-бонус
    private var isTiltBonusActive = false
    private var tiltBonusEndTime = 0L
    private val TILT_BONUS_DURATION = 10000L // 10 секунд длительность бонуса

    // Сенсоры
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    // Звуки
    private var tiltBonusSound: MediaPlayer? = null
    private var insectScreamSound: MediaPlayer? = null

    // Ускорение от наклона
    private var tiltX = 0f
    private var tiltY = 0f
    private val TILT_FORCE_MULTIPLIER = 800f // Увеличиваем силу для лучшего эффекта

    private val gameHandler = Handler(Looper.getMainLooper())

    // Bitmap'ы
    private var regularBugBitmap: Bitmap? = null
    private var fastBugBitmap: Bitmap? = null
    private var rareBugBitmap: Bitmap? = null
    private var bonusBitmap: Bitmap? = null // Этот бонус теперь активирует гироскоп
    private var penaltyBitmap: Bitmap? = null
    private var backgroundBitmap: Bitmap? = null

    private var onInsectClickListener: ((Insect) -> Unit)? = null
    private var onMissListener: (() -> Unit)? = null
    private var onTiltBonusListener: ((Boolean) -> Unit)? = null

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
            setupSensors()
            setupSounds()
            paint.isAntiAlias = true
            Log.d(TAG, "GameView initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing GameView", e)
        }
    }

    private fun setupSensors() {
        try {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

            if (accelerometer == null) {
                Log.w(TAG, "Accelerometer not available on this device")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up sensors", e)
        }
    }

    private fun setupSounds() {
        try {
            tiltBonusSound = MediaPlayer.create(context, R.raw.tilt_bonus_activate)
            insectScreamSound = MediaPlayer.create(context, R.raw.insect_scream)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up sounds", e)
        }
    }

    private fun setupBitmaps() {
        try {
            // Загружаем фон
            backgroundBitmap = loadBackgroundFromResource(R.drawable.game_background)

            // Загружаем bitmap'ы для жуков
            regularBugBitmap = loadBitmapFromResource(R.drawable.bug_regular, 120)
            fastBugBitmap = loadBitmapFromResource(R.drawable.bug_fast, 110)
            rareBugBitmap = loadBitmapFromResource(R.drawable.bug_rare, 140)
            bonusBitmap = loadBitmapFromResource(R.drawable.bonus, 80) // Этот бонус активирует гироскоп
            penaltyBitmap = loadBitmapFromResource(R.drawable.penalty, 80)

            Log.d(TAG, "All bitmaps loaded successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Error loading PNG bitmaps, creating fallback", e)
            createFallbackBitmaps()
        }

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
        regularBugBitmap = createColoredBugBitmap(Color.GREEN, 120, "Обычный")
        fastBugBitmap = createColoredBugBitmap(Color.BLUE, 110, "Быстрый")
        rareBugBitmap = createColoredBugBitmap(Color.YELLOW, 140, "Редкий")
        bonusBitmap = createColoredBugBitmap(Color.CYAN, 80, "Гиро") // Синий для гироскоп-бонуса
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

        // Рисуем тело жука (эллипс)
        canvas.drawOval(15f, size * 0.2f, size - 15f, size * 0.8f, paint)

        // Рисуем голову (круг)
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

    fun setOnTiltBonusListener(listener: (Boolean) -> Unit) {
        this.onTiltBonusListener = listener
    }

    fun startGame() {
        try {
            isGameRunning = true
            lastUpdateTime = System.currentTimeMillis()
            lastBonusTime = System.currentTimeMillis()
            insects.clear()

            // Регистрируем сенсоры
            accelerometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }

            gameHandler.post(updateRunnable)
            Log.d(TAG, "Game started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting game", e)
        }
    }

    fun pauseGame() {
        isGameRunning = false
        gameHandler.removeCallbacks(updateRunnable)
        sensorManager.unregisterListener(this)
    }

    fun resumeGame() {
        if (isGameRunning) return
        isGameRunning = true
        lastUpdateTime = System.currentTimeMillis()

        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }

        gameHandler.post(updateRunnable)
    }

    fun endGame() {
        isGameRunning = false
        isTiltBonusActive = false
        gameHandler.removeCallbacks(updateRunnable)
        sensorManager.unregisterListener(this)
        insects.clear()

        tiltBonusSound?.release()
        insectScreamSound?.release()

        invalidate()
    }

    private fun updateGame() {
        if (!isGameRunning || width == 0 || height == 0) return

        val currentTime = System.currentTimeMillis()
        val deltaTime = (currentTime - lastUpdateTime) / 1000f
        lastUpdateTime = currentTime

        // Добавляем обычных жуков
        val totalBugCount = insects.count { it.type in listOf(InsectType.REGULAR, InsectType.FAST, InsectType.RARE) }
        if (totalBugCount < maxCockroaches && Random.nextInt(100) < (10 + gameSpeed)) {
            addRandomBug()
        }

        // Добавляем бонусы/штрафы
        val adjustedBonusInterval = (bonusInterval * 1000L / (gameSpeed * 0.5f + 0.5f)).toLong()
        if (currentTime - lastBonusTime > adjustedBonusInterval) {
            if (Random.nextBoolean()) {
                addRandomInsect(InsectType.BONUS) // Этот бонус теперь активирует гироскоп
            } else {
                addRandomInsect(InsectType.PENALTY)
            }
            lastBonusTime = currentTime
        }

        // Проверяем окончание гироскоп-бонуса
        if (isTiltBonusActive && currentTime > tiltBonusEndTime) {
            deactivateTiltBonus()
        }

        // Обновляем позиции с учетом наклона
        val speedMultiplier = gameSpeed * 0.5f + 0.5f
        insects.forEach { insect ->
            if (isTiltBonusActive) {
                // При активном бонусе добавляем силу от наклона
                insect.speedX += tiltX * deltaTime * TILT_FORCE_MULTIPLIER
                insect.speedY += tiltY * deltaTime * TILT_FORCE_MULTIPLIER

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
            }
            insect.update(deltaTime * speedMultiplier)
        }

        // Удаляем вышедших за границы
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
            InsectType.BONUS -> bonusBitmap // Этот бонус активирует гироскоп
            InsectType.PENALTY -> penaltyBitmap
        } ?: return

        val side = Random.nextInt(4)
        val x: Float
        val y: Float

        when (side) {
            0 -> {
                x = -bitmap.width.toFloat()
                y = Random.nextInt(0, (height - bitmap.height).coerceAtLeast(1)).toFloat()
            }
            1 -> {
                x = width.toFloat()
                y = Random.nextInt(0, (height - bitmap.height).coerceAtLeast(1)).toFloat()
            }
            2 -> {
                x = Random.nextInt(0, (width - bitmap.width).coerceAtLeast(1)).toFloat()
                y = -bitmap.height.toFloat()
            }
            else -> {
                x = Random.nextInt(0, (width - bitmap.width).coerceAtLeast(1)).toFloat()
                y = height.toFloat()
            }
        }

        val targetX = width / 2f + Random.nextInt(-300, 300)
        val targetY = height / 2f + Random.nextInt(-300, 300)

        val dx = targetX - x
        val dy = targetY - y
        val length = sqrt(dx * dx + dy * dy)

        val baseSpeed = when (type) {
            InsectType.REGULAR -> Random.nextInt(120, 200).toFloat()
            InsectType.FAST -> Random.nextInt(220, 320).toFloat()
            InsectType.RARE -> Random.nextInt(100, 170).toFloat()
            InsectType.BONUS -> Random.nextInt(80, 120).toFloat() // Медленнее для бонуса
            InsectType.PENALTY -> Random.nextInt(140, 220).toFloat()
        }

        val health = when (type) {
            InsectType.RARE -> 3
            else -> 1
        }

        val insect = Insect(
            type = type,
            x = x, y = y,
            speedX = dx / length * baseSpeed,
            speedY = dy / length * baseSpeed,
            bitmap = bitmap,
            health = health,
            maxHealth = health
        )

        insects.add(insect)
    }

    private fun activateTiltBonus() {
        isTiltBonusActive = true
        tiltBonusEndTime = System.currentTimeMillis() + TILT_BONUS_DURATION

        // Проигрываем звук активации бонуса
        try {
            tiltBonusSound?.seekTo(0)
            tiltBonusSound?.start()
        } catch (e: Exception) {
            Log.e(TAG, "Error playing tilt bonus sound", e)
        }

        // Уведомляем активность
        onTiltBonusListener?.invoke(true)

        Log.d(TAG, "Tilt bonus activated for ${TILT_BONUS_DURATION}ms")
    }

    private fun deactivateTiltBonus() {
        isTiltBonusActive = false
        tiltX = 0f
        tiltY = 0f

        // Уведомляем активность
        onTiltBonusListener?.invoke(false)

        Log.d(TAG, "Tilt bonus deactivated")
    }

    private fun playInsectScream() {
        // Звук крика проигрывается с вероятностью 30%
        if (Random.nextInt(100) < 30) {
            try {
                insectScreamSound?.seekTo(0)
                insectScreamSound?.start()
            } catch (e: Exception) {
                Log.e(TAG, "Error playing insect scream sound", e)
            }
        }
    }

    // SensorEventListener методы
    override fun onSensorChanged(event: SensorEvent?) {
        if (!isTiltBonusActive) return

        event?.let { sensorEvent ->
            when (sensorEvent.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    // Получаем данные акселерометра
                    // X: наклон влево/вправо, Y: наклон вперед/назад
                    val rawTiltX = sensorEvent.values[0]
                    val rawTiltY = sensorEvent.values[1]

                    // Инвертируем и настраиваем чувствительность
                    tiltX = -rawTiltX * 2f  // Увеличиваем чувствительность
                    tiltY = rawTiltY * 2f

                    // Фильтруем небольшие колебания
                    val filterThreshold = 0.3f
                    if (abs(tiltX) < filterThreshold) tiltX = 0f
                    if (abs(tiltY) < filterThreshold) tiltY = 0f

                    // Проигрываем звук крика при значительном наклоне
                    if (abs(tiltX) > 3f || abs(tiltY) > 3f) {
                        playInsectScream()
                    }

                    Log.d(TAG, "Tilt - X: $tiltX, Y: $tiltY")
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Не используется
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Рисуем фон
        if (backgroundBitmap != null) {
            val scaledBackground = Bitmap.createScaledBitmap(backgroundBitmap!!, width, height, true)
            canvas.drawBitmap(scaledBackground, 0f, 0f, paint)
        } else {
            canvas.drawColor(Color.WHITE)
        }

        // Рисуем всех насекомых
        insects.forEach { insect ->
            canvas.drawBitmap(insect.bitmap, insect.x, insect.y, paint)
        }

        // Рисуем индикатор гироскоп-бонуса если активен
        if (isTiltBonusActive) {
            val timeLeft = (tiltBonusEndTime - System.currentTimeMillis()) / 1000f
            drawTiltBonusIndicator(canvas, timeLeft)
        }
    }

    private fun drawTiltBonusIndicator(canvas: Canvas, timeLeft: Float) {
        val indicatorPaint = Paint().apply {
            color = Color.argb(180, 0, 200, 255) // Полупрозрачный голубой
            style = Paint.Style.FILL
        }

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 42f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        val borderPaint = Paint().apply {
            color = Color.BLUE
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }

        // Рисуем полупрозрачный фон
        val indicatorHeight = 100f
        canvas.drawRect(0f, 0f, width.toFloat(), indicatorHeight, indicatorPaint)
        canvas.drawRect(0f, 0f, width.toFloat(), indicatorHeight, borderPaint)

        // Рисуем текст
        val text = "🎯 ГИРОСКОП-РЕЖИМ: ${"%.1f".format(timeLeft)}с 🎯"
        canvas.drawText(text, width / 2f, 60f, textPaint)

        // Рисуем прогресс-бар
        val progressWidth = (width * (timeLeft / (TILT_BONUS_DURATION / 1000f))).toFloat()
        val progressPaint = Paint().apply {
            color = Color.YELLOW
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, indicatorHeight - 10f, progressWidth, indicatorHeight, progressPaint)
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
                        InsectType.BONUS -> {
                            // АКТИВИРУЕМ ГИРОСКОП-БОНУС при клике на обычный бонус!
                            onInsectClickListener?.invoke(insect)
                            activateTiltBonus()
                            iterator.remove()
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

// Enum остается без изменений
enum class InsectType { REGULAR, FAST, RARE, BONUS, PENALTY }

// Insect data class остается без изменений
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