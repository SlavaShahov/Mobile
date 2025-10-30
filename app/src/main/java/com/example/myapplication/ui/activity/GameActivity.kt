package com.example.myapplication.ui.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.R
import com.example.myapplication.data.local.PreferencesManager
import com.example.myapplication.data.network.CbrApiService
import com.example.myapplication.data.repository.GoldRateRepository
import com.example.myapplication.domain.model.GameSettings
import com.example.myapplication.domain.model.InsectType
import com.example.myapplication.game.view.GameView
import com.example.myapplication.game.view.GoldRateWidget
import com.example.myapplication.ui.viewmodel.GameViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.android.inject
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

    private val viewModel: GameViewModel by viewModels()
    private val preferencesManager: PreferencesManager by inject()

    private var gameHandler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null

    private var goldRateUpdateHandler = Handler(Looper.getMainLooper())
    private val goldRateUpdateInterval = 60000L

    private lateinit var goldRateRepository: GoldRateRepository

    private var isGameEnded = false

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

        setupGoldRateService()
        initViews()

        // Связали ViewModel с GameView
        gameView.setViewModel(viewModel, this)

        setupObservers()

        if (savedInstanceState == null) {
            getSettingsFromIntent()
            setupGame()
            if (!viewModel.isPlaying.value && !isGameEnded) {
                startGame()
            }
        } else {
            Log.d(TAG, "Settings restored from saved state")
            setupGame()
        }

        startGoldRateUpdates()
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

        } catch (e: Exception) {
            Log.e(TAG, "Error setting up gold rate service", e)
            goldRateRepository = GoldRateRepository(object : CbrApiService {
                override suspend fun getGoldRates(dateFrom: String, dateTo: String)
                        = com.example.myapplication.data.model.GoldRatesResponse()
            })
        }
    }

    private fun getSettingsFromIntent() {
        val savedSettings = preferencesManager.getGameSettings()

        val gameSpeed = savedSettings.gameSpeed
        val maxCockroaches = savedSettings.maxCockroaches
        val bonusInterval = savedSettings.bonusInterval
        val roundDuration = savedSettings.roundDuration

        viewModel.initializeSettings(gameSpeed, maxCockroaches, bonusInterval, roundDuration)

        Log.d(TAG, "Settings loaded: speed=$gameSpeed, maxCockroaches=$maxCockroaches, duration=$roundDuration")
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
            btnMenu.setOnClickListener {
                finish()
            }

            Log.d(TAG, "Views initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views", e)
            Toast.makeText(this, "Ошибка инициализации игры", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.score.collect { score ->
                tvScore.text = "Очки: $score"
            }
        }

        lifecycleScope.launch {
            viewModel.timeLeft.collect { time ->
                tvTime.text = "Время: ${time}с"

                if (time <= 0 && viewModel.isPlaying.value && !isGameEnded) {
                    endGame()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isPlaying.collect { isPlaying ->
                btnPause.text = if (isPlaying) "Пауза" else "Продолжить"
            }
        }

        lifecycleScope.launch {
            viewModel.currentGoldRate.collect { rate ->
                goldRateWidget.updateGoldRate(rate)
                gameView.updateGoldRate(rate)
                Log.d(TAG, "Gold rate updated: $rate")
            }
        }
    }

    private fun setupGame() {
        try {
            gameView.setGameSettings(
                viewModel.gameSpeed,
                viewModel.maxCockroaches,
                viewModel.bonusInterval
            )

            gameView.setOnInsectClickListener { insect ->
                val points = when (insect.type) {
                    InsectType.REGULAR -> 10
                    InsectType.FAST -> 15
                    InsectType.RARE -> 100
                    InsectType.BONUS -> 50
                    InsectType.PENALTY -> -15
                    InsectType.GOLDEN -> gameView.getGoldBugPoints()
                }

                if (points > 0) {
                    viewModel.addPoints(points)
                } else {
                    viewModel.subtractPoints(-points)
                }
            }


            gameView.setOnTiltBonusListener { isActive ->
                if (isActive) {
                    showToast("Наклоняйте телефон - жуки летят в сторону наклона!")
                }
            }


            gameView.setOnMissListener {
                viewModel.subtractPoints(5)
            }
            Log.d(TAG, "Game setup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up game", e)
            Toast.makeText(this, "Ошибка настройки игры", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startGame() {
        try {
            viewModel.startGame()
            gameView.startGame()
            startTimer()
            Log.d(TAG, "Game started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting game", e)
            Toast.makeText(this, "Ошибка запуска игры", Toast.LENGTH_SHORT).show()
        }
    }

    private fun togglePause() {
        if (viewModel.isPlaying.value) {
            viewModel.pauseGame()
            gameView.pauseGame()
            stopTimer()
        } else {
            viewModel.startGame()
            gameView.resumeGame()
            startTimer()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("score", viewModel.getFinalScore())
        outState.putInt("timeLeft", viewModel.timeLeft.value)
        outState.putBoolean("isPlaying", viewModel.isPlaying.value)
        outState.putBoolean("isGameEnded", isGameEnded)
        outState.putDouble("goldRate", viewModel.currentGoldRate.value)

        outState.putInt("gameSpeed", viewModel.gameSpeed)
        outState.putInt("maxCockroaches", viewModel.maxCockroaches)
        outState.putInt("bonusInterval", viewModel.bonusInterval)
        outState.putInt("roundDuration", viewModel.roundDuration)

        Log.d(TAG, "Game state saved: score=${viewModel.getFinalScore()}, time=${viewModel.timeLeft.value}")
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        val savedScore = savedInstanceState.getInt("score", 0)
        val savedTime = savedInstanceState.getInt("timeLeft", viewModel.roundDuration)
        val savedIsPlaying = savedInstanceState.getBoolean("isPlaying", false)
        val savedIsGameEnded = savedInstanceState.getBoolean("isGameEnded", false)
        val savedGoldRate = savedInstanceState.getDouble("goldRate", 5000.0)

        val savedGameSpeed = savedInstanceState.getInt("gameSpeed", viewModel.gameSpeed)
        val savedMaxCockroaches = savedInstanceState.getInt("maxCockroaches", viewModel.maxCockroaches)
        val savedBonusInterval = savedInstanceState.getInt("bonusInterval", viewModel.bonusInterval)
        val savedRoundDuration = savedInstanceState.getInt("roundDuration", viewModel.roundDuration)

        viewModel.initializeSettings(savedGameSpeed, savedMaxCockroaches, savedBonusInterval, savedRoundDuration)
        viewModel.addPoints(savedScore - viewModel.getFinalScore())
        viewModel.updateTimeLeft(savedTime)
        viewModel.setPlaying(savedIsPlaying)
        viewModel.updateGoldRate(savedGoldRate)
        isGameEnded = savedIsGameEnded

        gameView.setGameSettings(savedGameSpeed, savedMaxCockroaches, savedBonusInterval)

        Log.d(TAG, "Game state restored: score=$savedScore, time=$savedTime, playing=$savedIsPlaying")
    }

    private fun startTimer() {
        stopTimer()

        timerRunnable = object : Runnable {
            override fun run() {
                if (viewModel.isPlaying.value && !isGameEnded) {
                    val currentTime = viewModel.timeLeft.value - 1
                    viewModel.updateTimeLeft(currentTime)

                    if (currentTime <= 0) {
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

    private fun startGoldRateUpdates() {
        val updateRunnable = object : Runnable {
            override fun run() {
                updateGoldRate()
                goldRateUpdateHandler.postDelayed(this, goldRateUpdateInterval)
            }
        }
        goldRateUpdateHandler.post(updateRunnable)
        updateGoldRate()
    }

    private fun updateGoldRate() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val rate = goldRateRepository.getCurrentGoldRate()
                runOnUiThread {
                    viewModel.updateGoldRate(rate)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating gold rate", e)
                runOnUiThread {
                    viewModel.updateGoldRate(10500.0)
                }
            }
        }
    }

    private fun endGame() {
        if (isGameEnded) return

        isGameEnded = true
        stopTimer()
        gameView.endGame()
        goldRateUpdateHandler.removeCallbacksAndMessages(null)
        viewModel.endGame()

        lifecycleScope.launch {
            try {
                val finalScore = viewModel.getFinalScore()
                val settings = GameSettings(
                    gameSpeed = viewModel.gameSpeed,
                    maxCockroaches = viewModel.maxCockroaches,
                    bonusInterval = viewModel.bonusInterval,
                    roundDuration = viewModel.roundDuration
                )
                withContext(Dispatchers.IO) {
                    preferencesManager.saveScore(finalScore, settings)
                    preferencesManager.saveHighScore(finalScore)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving score", e)
            }
        }

        val intent = Intent(this, GameResultActivity::class.java)
        intent.putExtra("score", viewModel.getFinalScore())
        startActivity(intent)
        finish()
    }
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
        if (viewModel.isPlaying.value && !isGameEnded) {
            gameView.pauseGame()
            stopTimer()
        }
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.isPlaying.value && !isGameEnded) {
            gameView.resumeGame()
            startTimer()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
        goldRateUpdateHandler.removeCallbacksAndMessages(null)
        gameHandler.removeCallbacksAndMessages(null)

        if (!isGameEnded) {
            gameView.endGame()
        }
    }
}