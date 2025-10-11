package com.example.myapplication.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.R
import com.example.myapplication.data.local.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

class GameResultActivity : AppCompatActivity() {

    private lateinit var tvFinalScore: TextView
    private lateinit var btnRestart: Button
    private lateinit var btnMenu: Button

    private val preferencesManager: PreferencesManager by inject()

    // Флаг для предотвращения повторной обработки
    private var isResultProcessed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_result)

        initViews()

        // Обрабатываем результат только один раз
        if (!isResultProcessed) {
            displayResults()
            isResultProcessed = true
        }

        setupButtons()
    }

    private fun initViews() {
        tvFinalScore = findViewById(R.id.tvFinalScore)
        btnRestart = findViewById(R.id.btnRestart)
        btnMenu = findViewById(R.id.btnMenu)
    }

    private fun displayResults() {
        lifecycleScope.launch {
            try {
                val score = intent.getIntExtra("score", 0)
                withContext(Dispatchers.IO) {
                    preferencesManager.saveHighScore(score)
                }
                val highScore = preferencesManager.getHighScore()

                runOnUiThread {
                    tvFinalScore.text = "Ваш счет: $score\nРекорд: $highScore"
                }
            } catch (e: Exception) {
                runOnUiThread {
                    tvFinalScore.text = "Ваш счет: ${intent.getIntExtra("score", 0)}"
                }
            }
        }
    }

    private fun setupButtons() {
        btnRestart.setOnClickListener {
            // Запускаем новую игру и завершаем эту активность
            startActivity(Intent(this, GameActivity::class.java))
            finish()
        }

        btnMenu.setOnClickListener {
            // Просто завершаем активность, возвращаясь в MainActivity
            finish()
        }
    }

    override fun onBackPressed() {
        // При нажатии кнопки назад просто завершаем активность
        finish()
    }
}