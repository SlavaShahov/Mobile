package com.example.myapplication.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.data.local.PreferencesManager
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
        val score = intent.getIntExtra("score", 0)

        // Сохраняем рекорд
        preferencesManager.saveHighScore(score)
        val highScore = preferencesManager.getHighScore()

        // Показываем счет и рекорд
        tvFinalScore.text = "Ваш счет: $score\nРекорд: $highScore"
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