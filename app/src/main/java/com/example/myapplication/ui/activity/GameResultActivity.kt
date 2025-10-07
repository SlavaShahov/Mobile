package com.example.myapplication.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.data.local.PreferencesManager

class GameResultActivity : AppCompatActivity() {

    private lateinit var tvScore: TextView
    private lateinit var tvHighScore: TextView
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
        tvScore = findViewById(R.id.tvScore)
        btnRestart = findViewById(R.id.btnRestart)
        btnMenu = findViewById(R.id.btnMenu)
    }

    private fun displayResults() {
        val score = intent.getIntExtra("score", 0)
        tvScore.text = "Ваш счет: $score"

        // Сохраняем и показываем рекорд
        preferencesManager.saveHighScore(score)
        val highScore = preferencesManager.getHighScore()
        tvHighScore.text = "Рекорд: $highScore"
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