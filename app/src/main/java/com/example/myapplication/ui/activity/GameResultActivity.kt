package com.example.myapplication.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.data.local.PreferencesManager

class GameResultActivity : AppCompatActivity() {

    private lateinit var tvFinalScore: TextView // ИЗМЕНЕНО: tvFinalScore вместо tvScore
    private lateinit var btnRestart: Button
    private lateinit var btnMenu: Button

    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_result)

        preferencesManager = PreferencesManager(this)
        initViews()
        displayResults()
        setupButtons()
    }

    private fun initViews() {
        tvFinalScore = findViewById(R.id.tvFinalScore) // ИЗМЕНЕНО: tvFinalScore
        btnRestart = findViewById(R.id.btnRestart)
        btnMenu = findViewById(R.id.btnMenu)
    }

    private fun displayResults() {
        val score = intent.getIntExtra("score", 0)

        // Сохраняем рекорд
        preferencesManager.saveHighScore(score)
        val highScore = preferencesManager.getHighScore()

        // Показываем счет и рекорд в одном TextView
        tvFinalScore.text = "Ваш счет: $score\nРекорд: $highScore"
    }

    private fun setupButtons() {
        btnRestart.setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java))
            finish()
        }

        btnMenu.setOnClickListener {
            finish()
        }
    }
}