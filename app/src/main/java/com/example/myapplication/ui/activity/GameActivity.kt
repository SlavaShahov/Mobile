package com.example.myapplication.ui.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.data.network.CbrApiService
import com.example.myapplication.data.repository.GoldRateRepository
import com.example.myapplication.domain.model.InsectType
import com.example.myapplication.game.view.GameView
import com.example.myapplication.game.view.GoldRateWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import java.util.concurrent.TimeUnit

class GameActivity : AppCompatActivity() {

    private lateinit var gameView: GameView
    private lateinit var goldRateWidget: GoldRateWidget
    private lateinit var tvScore: TextView
    private lateinit var tvTime: TextView
    private lateinit var btnPause: Button
    private lateinit var btnMenu: Button

    private var score = 0
    private var timeLeft = 120
    private var isPlaying = false
    private var gameHandler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null

    private var gameSpeed = 5
    private var maxCockroaches = 10
    private var bonusInterval = 30
    private var roundDuration = 120
    private var currentGoldRate: Double = 5000.0

    // Курс золота
    private lateinit var goldRateRepository: GoldRateRepository
    private var goldRateUpdateHandler = Handler(Looper.getMainLooper())
    private val goldRateUpdateInterval = 60000L // 1 минута

    companion object {
        const val TAG = "GameActivity"
        const val EXTRA_GAME_SPEED = "game_speed"
        const val EXTRA_MAX_COCKROACHES = "max_cockroaches"
        const val EXTRA_BONUS_INTERVAL = "bonus_interval"
        const val EXTRA_ROUND_DURATION = "round_duration"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        Log.d(TAG, "GameActivity onCreate")

        getSettingsFromIntent()
        setupGoldRateService()
        initViews()
        setupGame()
        startGame()
    }

    private fun setupGoldRateService() {
        try {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl("https://www.cbr.ru/")
                .client(client)
                .addConverterFactory(SimpleXmlConverterFactory.create())
                .build()

            val apiService = retrofit.create(CbrApiService::class.java)
            goldRateRepository = GoldRateRepository(apiService)

            startGoldRateUpdates()
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up gold rate service", e)
            // Используем значение по умолчанию
            goldRateRepository = GoldRateRepository(object : CbrApiService {
                override suspend fun getGoldRates(dateFrom: String, dateTo: String)
                        = com.example.myapplication.data.model.GoldRatesResponse()
            })
            startGoldRateUpdates()
        }
    }

    private fun startGoldRateUpdates() {
        val updateRunnable = object : Runnable {
            override fun run() {
                updateGoldRate()
                goldRateUpdateHandler.postDelayed(this, goldRateUpdateInterval)
            }
        }
        goldRateUpdateHandler.post(updateRunnable)

        // Первоначальное обновление
        updateGoldRate()
    }

    private fun updateGoldRate() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val rate = goldRateRepository.getCurrentGoldRate()
                runOnUiThread {
                    currentGoldRate = rate // Сохраняем текущий курс
                    goldRateWidget.updateGoldRate(rate)
                    gameView.updateGoldRate(rate)
                    Log.d(TAG, "Gold rate updated: $rate, points per bug: ${(rate / 100).toInt()}")

                    // УБРАНО: показ тоста с курсом золота
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating gold rate", e)
                runOnUiThread {
                    val defaultRate = 5000.0
                    currentGoldRate = defaultRate
                    goldRateWidget.updateGoldRate(defaultRate)
                    gameView.updateGoldRate(defaultRate)
                    Log.d(TAG, "Using default gold rate: $defaultRate")
                }
            }
        }
    }

    private fun getSettingsFromIntent() {
        gameSpeed = intent.getIntExtra(EXTRA_GAME_SPEED, 5)
        maxCockroaches = intent.getIntExtra(EXTRA_MAX_COCKROACHES, 10)
        bonusInterval = intent.getIntExtra(EXTRA_BONUS_INTERVAL, 30)
        roundDuration = intent.getIntExtra(EXTRA_ROUND_DURATION, 120)
        timeLeft = roundDuration

        Log.d(TAG, "Settings: speed=$gameSpeed, maxCockroaches=$maxCockroaches, duration=$roundDuration")
    }

    private fun initViews() {
        try {
            gameView = findViewById(R.id.gameView)
            goldRateWidget = findViewById(R.id.goldRateWidget)
            tvScore = findViewById(R.id.tvScore)
            tvTime = findViewById(R.id.tvTime)
            btnPause = findViewById(R.id.btnPause)
            btnMenu = findViewById(R.id.btnMenu)

            btnPause.setOnClickListener { togglePause() }
            btnMenu.setOnClickListener { finish() }

            tvTime.text = "Время: ${timeLeft}с"
            Log.d(TAG, "Views initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views", e)
            Toast.makeText(this, "Ошибка инициализации игры", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupGame() {
        try {
            gameView.setGameSettings(gameSpeed, maxCockroaches, bonusInterval)

            gameView.setOnInsectClickListener { insect ->
                // Получаем актуальные очки из GameView
                val points = when (insect.type) {
                    InsectType.REGULAR -> 10
                    InsectType.FAST -> 15
                    InsectType.RARE -> 100
                    InsectType.BONUS -> 50
                    InsectType.PENALTY -> -15
                    InsectType.GOLDEN -> gameView.getGoldBugPoints() // Используем актуальное значение
                }
                score += points
                updateScore()

                // УБРАНО: все всплывающие сообщения о типах жуков
            }

            gameView.setOnTiltBonusListener { isActive ->
                if (isActive) {
                    showToast("Наклоняйте телефон - жуки летят в сторону наклона!")
                } else {
                    // УБРАНО: сообщение о завершении гироскоп-режима
                }
            }

            gameView.setOnMissListener {
                score -= 5
                updateScore()
            }
            Log.d(TAG, "Game setup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up game", e)
            Toast.makeText(this, "Ошибка настройки игры", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startGame() {
        try {
            isPlaying = true
            gameView.startGame()
            startTimer()
            btnPause.text = "Пауза"
            Log.d(TAG, "Game started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting game", e)
            Toast.makeText(this, "Ошибка запуска игры", Toast.LENGTH_SHORT).show()
        }
    }

    private fun togglePause() {
        isPlaying = !isPlaying
        if (isPlaying) {
            gameView.resumeGame()
            btnPause.text = "Пауза"
            startTimer()
        } else {
            gameView.pauseGame()
            btnPause.text = "Продолжить"
            stopTimer()
        }
    }

    private fun startTimer() {
        stopTimer()

        timerRunnable = object : Runnable {
            override fun run() {
                if (isPlaying) {
                    timeLeft--
                    tvTime.text = "Время: ${timeLeft}с"

                    if (timeLeft <= 0) {
                        endGame()
                    } else {
                        gameHandler.postDelayed(this, 1000)
                    }
                }
            }
        }
        gameHandler.post(timerRunnable!!)
    }

    private fun stopTimer() {
        timerRunnable?.let {
            gameHandler.removeCallbacks(it)
        }
    }

    private fun updateScore() {
        tvScore.text = "Очки: $score"
    }

    private fun endGame() {
        isPlaying = false
        stopTimer()
        gameView.endGame()
        goldRateUpdateHandler.removeCallbacksAndMessages(null)

        val intent = Intent(this, GameResultActivity::class.java)
        intent.putExtra("score", score)
        startActivity(intent)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
        if (isPlaying) {
            togglePause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
        goldRateUpdateHandler.removeCallbacksAndMessages(null)
        gameHandler.removeCallbacksAndMessages(null)
        gameView.endGame()
    }
}