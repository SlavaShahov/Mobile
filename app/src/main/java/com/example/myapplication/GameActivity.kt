package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class GameActivity : AppCompatActivity() {

    private lateinit var gameView: GameView
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
        initViews()
        setupGame()
        startGame()
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
                val points = when (insect.type) {
                    InsectType.REGULAR -> 10
                    InsectType.FAST -> 15
                    InsectType.RARE -> 100
                    InsectType.BONUS -> 50 // Очки за сбор бонуса
                    InsectType.PENALTY -> -15
                }
                score += points
                updateScore()

                when (insect.type) {
                    InsectType.RARE -> {
                        if (insect.health <= 0) {
                            showToast("Редкий жук уничтожен! +100 очков")
                        } else {
                            showToast("Попадание по редкому жуку! Осталось: ${insect.health}/3")
                        }
                    }
                    InsectType.FAST -> showToast("Быстрый жук! +15 очков")
                    InsectType.BONUS -> showToast("Гироскоп-бонус активирован! +50 очков")
                    else -> {}
                }
            }

            // Добавляем обработчик гироскоп-бонуса
            gameView.setOnTiltBonusListener { isActive ->
                if (isActive) {
                    showToast("Наклоняйте телефон - жуки летят в сторону наклона!")
                } else {
                    showToast("Гироскоп-режим завершен")
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
        gameHandler.removeCallbacksAndMessages(null)
        gameView.endGame()
    }
}