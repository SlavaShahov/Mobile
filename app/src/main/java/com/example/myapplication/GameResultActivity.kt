package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class GameResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_result)

        val score = intent.getIntExtra("score", 0)
        val tvScore = findViewById<TextView>(R.id.tvFinalScore)
        val btnRestart = findViewById<Button>(R.id.btnRestart)
        val btnMenu = findViewById<Button>(R.id.btnMenu)

        tvScore.text = "Ваш счет: $score"

        btnRestart.setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java))
            finish()
        }

        btnMenu.setOnClickListener {
            finish()
        }
    }
}