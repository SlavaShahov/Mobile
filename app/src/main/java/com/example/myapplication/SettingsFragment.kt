package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment() {

    private lateinit var sbGameSpeed: SeekBar
    private lateinit var tvGameSpeed: TextView
    private lateinit var sbMaxCockroaches: SeekBar
    private lateinit var tvMaxCockroaches: TextView
    private lateinit var sbBonusInterval: SeekBar
    private lateinit var tvBonusInterval: TextView
    private lateinit var sbRoundDuration: SeekBar
    private lateinit var tvRoundDuration: TextView
    private lateinit var btnSaveSettings: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        initViews(view)
        setupSeekBars()
        setupSaveButton()

        return view
    }

    private fun initViews(view: View) {
        sbGameSpeed = view.findViewById(R.id.sbGameSpeed)
        tvGameSpeed = view.findViewById(R.id.tvGameSpeed)
        sbMaxCockroaches = view.findViewById(R.id.sbMaxCockroaches)
        tvMaxCockroaches = view.findViewById(R.id.tvMaxCockroaches)
        sbBonusInterval = view.findViewById(R.id.sbBonusInterval)
        tvBonusInterval = view.findViewById(R.id.tvBonusInterval)
        sbRoundDuration = view.findViewById(R.id.sbRoundDuration)
        tvRoundDuration = view.findViewById(R.id.tvRoundDuration)
        btnSaveSettings = view.findViewById(R.id.btnSaveSettings)
    }

    private fun setupSeekBars() {
        setupSeekBar(sbGameSpeed, tvGameSpeed, "Скорость: ", "x")
        setupSeekBar(sbMaxCockroaches, tvMaxCockroaches, "Макс. тараканов: ", "")
        setupSeekBar(sbBonusInterval, tvBonusInterval, "Интервал бонусов: ", "сек")
        setupSeekBar(sbRoundDuration, tvRoundDuration, "Длительность раунда: ", "сек")
    }

    private fun setupSeekBar(seekBar: SeekBar, textView: TextView, prefix: String, suffix: String) {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                textView.text = "$prefix$progress$suffix"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        // Инициализируем начальные значения
        seekBar.progress = seekBar.progress
    }

    private fun setupSaveButton() {
        btnSaveSettings.setOnClickListener {
            saveSettings()
        }
    }

    private fun saveSettings() {
        val settings = GameSettings(
            gameSpeed = sbGameSpeed.progress,
            maxCockroaches = sbMaxCockroaches.progress,
            bonusInterval = sbBonusInterval.progress,
            roundDuration = sbRoundDuration.progress
        )
        // Сохраняем настройки (можно использовать SharedPreferences)
    }
}

data class GameSettings(
    val gameSpeed: Int,
    val maxCockroaches: Int,
    val bonusInterval: Int,
    val roundDuration: Int
)