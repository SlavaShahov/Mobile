package com.example.myapplication.di

import android.content.Context
import com.example.myapplication.data.local.PreferencesManager
import com.example.myapplication.domain.repository.GameRepository
import com.example.myapplication.domain.usecase.CalculateZodiacUseCase
import com.example.myapplication.domain.usecase.GameLogicUseCase
import com.example.myapplication.domain.usecase.SettingsUseCase

object ServiceLocator {

    private var preferencesManager: PreferencesManager? = null
    private var calculateZodiacUseCase: CalculateZodiacUseCase? = null
    private var gameLogicUseCase: GameLogicUseCase? = null
    private var settingsUseCase: SettingsUseCase? = null

    fun providePreferencesManager(context: Context): PreferencesManager {
        return preferencesManager ?: PreferencesManager(context).also {
            preferencesManager = it
        }
    }

    fun provideCalculateZodiacUseCase(): CalculateZodiacUseCase {
        return calculateZodiacUseCase ?: CalculateZodiacUseCase().also {
            calculateZodiacUseCase = it
        }
    }

    fun provideGameLogicUseCase(): GameLogicUseCase {
        return gameLogicUseCase ?: GameLogicUseCase().also {
            gameLogicUseCase = it
        }
    }

    fun provideSettingsUseCase(): SettingsUseCase {
        return settingsUseCase ?: SettingsUseCase().also {
            settingsUseCase = it
        }
    }

    fun provideGameRepository(context: Context): GameRepository {
        return providePreferencesManager(context)
    }
}