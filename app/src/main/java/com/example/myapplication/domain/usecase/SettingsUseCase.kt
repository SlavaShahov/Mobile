package com.example.myapplication.domain.usecase

import com.example.myapplication.domain.model.GameSettings

class SettingsUseCase {

    fun validateSettings(settings: GameSettings): Boolean {
        return settings.gameSpeed in 1..10 &&
                settings.maxCockroaches in 1..20 &&
                settings.bonusInterval in 5..60 &&
                settings.roundDuration in 30..300
    }

    fun getDefaultSettings(): GameSettings {
        return GameSettings()
    }
}