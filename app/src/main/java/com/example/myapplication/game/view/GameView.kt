package com.example.myapplication.game.view

import android.content.Context
import android.graphics.*
import android.hardware.SensorManager
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.example.myapplication.R
import com.example.myapplication.data.sensor.GyroscopeManager
import com.example.myapplication.game.engine.InsectFactory
import com.example.myapplication.domain.model.GameSettings
import com.example.myapplication.domain.model.Insect
import com.example.myapplication.domain.model.InsectType
import com.example.myapplication.game.engine.GameEngine
import com.example.myapplication.game.sound.SoundManager

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        isAntiAlias = true
    }

    private lateinit var gameEngine: GameEngine
    private lateinit var gyroscopeManager: GyroscopeManager
    private lateinit var soundManager: SoundManager

    private var lastUpdateTime = 0L
    private var isGameRunning = false

    // Bitmap'ы
    private var regularBugBitmap: Bitmap? = null
    private var fastBugBitmap: Bitmap? = null
    private var rareBugBitmap: Bitmap? = null
    private var bonusBitmap: Bitmap? = null
    private var penaltyBitmap: Bitmap? = null
    private var goldBugBitmap: Bitmap? = null
    private var backgroundBitmap: Bitmap? = null

    private var onInsectClickListener: ((Insect) -> Unit)? = null
    private var onMissListener: (() -> Unit)? = null
    private var onTiltBonusListener: ((Boolean) -> Unit)? = null

    companion object {
        private const val TAG = "GameView"
    }

    private val gameHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            if (isGameRunning) {
                updateGame()
                invalidate()
                gameHandler.postDelayed(this, 16)
            }
        }
    }

    init {
        try {
            setupBitmaps()
            setupGameEngine()
            setupSensors()
            Log.d(TAG, "GameView initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing GameView", e)
        }
    }

    private fun setupGameEngine() {
        soundManager = SoundManager(context)
        gameEngine = GameEngine(
            settings = GameSettings(),
            soundManager = soundManager
        )

        gameEngine.onTiltBonusChanged = { isActive ->
            onTiltBonusListener?.invoke(isActive)
        }

        gameEngine.onGoldRateUpdated = { rate, points ->
            Log.d(TAG, "Gold rate updated in GameView: $rate, points: $points")
        }
    }

    private fun setupSensors() {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gyroscopeManager = GyroscopeManager(sensorManager) { tiltX, tiltY ->
            if (isGameRunning) {
                gameEngine.updateTilt(tiltX, tiltY)
            }
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
            bonusBitmap = loadBitmapFromResource(R.drawable.bonus, 80)
            penaltyBitmap = loadBitmapFromResource(R.drawable.penalty, 80)
            goldBugBitmap = loadBitmapFromResource(R.drawable.golden_bug, 100)

            // Передаем bitmap'ы в InsectFactory
            regularBugBitmap?.let { InsectFactory.regularBugBitmap = it }
            fastBugBitmap?.let { InsectFactory.fastBugBitmap = it }
            rareBugBitmap?.let { InsectFactory.rareBugBitmap = it }
            bonusBitmap?.let { InsectFactory.bonusBitmap = it }
            penaltyBitmap?.let { InsectFactory.penaltyBitmap = it }
            goldBugBitmap?.let { InsectFactory.goldBugBitmap = it }

            Log.d(TAG, "All bitmaps loaded successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Error loading PNG bitmaps", e)
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

    private fun scaleBitmap(bitmap: Bitmap, targetSize: Int): Bitmap {
        val scale = targetSize.toFloat() / bitmap.width.toFloat()
        val newWidth = (bitmap.width * scale).toInt()
        val newHeight = (bitmap.height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun createFallbackBitmaps() {
        Log.d(TAG, "Creating fallback bitmaps")
        // InsectFactory сам создаст fallback bitmap'ы при необходимости
    }

    fun setGameSettings(gameSpeed: Int, maxCockroaches: Int, bonusInterval: Int) {
        val settings = GameSettings(
            gameSpeed = gameSpeed,
            maxCockroaches = maxCockroaches,
            bonusInterval = bonusInterval
        )
        gameEngine.updateSettings(settings)
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

    fun updateGoldRate(rate: Double) {
        gameEngine.updateGoldRate(rate)
    }

    fun getGoldBugPoints(): Int {
        return gameEngine.getGoldBugPoints()
    }

    fun startGame() {
        try {
            isGameRunning = true
            gameEngine.setScreenSize(width, height)
            gameEngine.startGame()
            gyroscopeManager.startListening()
            lastUpdateTime = System.currentTimeMillis()
            gameHandler.post(updateRunnable)
            Log.d(TAG, "Game started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting game", e)
        }
    }

    fun pauseGame() {
        isGameRunning = false
        gameEngine.pauseGame()
        gyroscopeManager.stopListening()
        gameHandler.removeCallbacks(updateRunnable)
    }

    fun resumeGame() {
        if (isGameRunning) return
        isGameRunning = true
        gameEngine.resumeGame()
        gyroscopeManager.startListening()
        lastUpdateTime = System.currentTimeMillis()
        gameHandler.post(updateRunnable)
    }

    fun endGame() {
        isGameRunning = false
        gameEngine.endGame()
        gyroscopeManager.stopListening()
        soundManager.release()
        gameHandler.removeCallbacks(updateRunnable)
    }

    private fun updateGame() {
        val currentTime = System.currentTimeMillis()
        val deltaTime = (currentTime - lastUpdateTime) / 1000f
        lastUpdateTime = currentTime

        gameEngine.updateGame(deltaTime)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            gameEngine.setScreenSize(w, h)
        }
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
        gameEngine.getInsects().forEach { insect ->
            canvas.drawBitmap(insect.bitmap, insect.x, insect.y, paint)
        }

        // Индикатор гироскоп-бонуса
        if (gameEngine.isTiltBonusActive()) {
            val timeLeft = gameEngine.getTiltBonusTimeLeft()
            drawTiltBonusIndicator(canvas, timeLeft)
        }
    }

    private fun drawTiltBonusIndicator(canvas: Canvas, timeLeft: Float) {
        val indicatorPaint = Paint().apply {
            color = Color.argb(180, 0, 200, 255)
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

        val indicatorHeight = 100f
        canvas.drawRect(0f, 0f, width.toFloat(), indicatorHeight, indicatorPaint)
        canvas.drawRect(0f, 0f, width.toFloat(), indicatorHeight, borderPaint)

        val text = "🎯 ГИРОСКОП-РЕЖИМ: ${"%.1f".format(timeLeft)}с 🎯"
        canvas.drawText(text, width / 2f, 60f, textPaint)

        val progressWidth = (width * (timeLeft / 10f)).toFloat()
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

            val collidedInsect = gameEngine.checkCollision(x, y)
            if (collidedInsect != null) {
                handleInsectClick(collidedInsect)
            } else {
                onMissListener?.invoke()
            }

            invalidate()
        }
        return true
    }

    private fun handleInsectClick(insect: Insect) {
        when (insect.type) {
            InsectType.RARE -> {
                insect.health--
                if (insect.health <= 0) {
                    onInsectClickListener?.invoke(insect)
                    gameEngine.removeInsect(insect)
                } else {
                    onInsectClickListener?.invoke(insect.copy(health = insect.health))
                }
            }
            InsectType.BONUS -> {
                onInsectClickListener?.invoke(insect)
                gameEngine.activateTiltBonus()
                gameEngine.removeInsect(insect)
            }
            InsectType.GOLDEN -> {
                onInsectClickListener?.invoke(insect)
                gameEngine.removeInsect(insect)
            }
            else -> {
                onInsectClickListener?.invoke(insect)
                gameEngine.removeInsect(insect)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        endGame()
        gameHandler.removeCallbacksAndMessages(null)
    }
}