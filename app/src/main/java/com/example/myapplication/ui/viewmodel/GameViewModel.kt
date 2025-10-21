package com.example.myapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameViewModel : ViewModel() {

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _timeLeft = MutableStateFlow(120)
    val timeLeft: StateFlow<Int> = _timeLeft.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentGoldRate = MutableStateFlow(5000.0)
    val currentGoldRate: StateFlow<Double> = _currentGoldRate.asStateFlow()

    private val _areResourcesLoaded = MutableStateFlow(false)
    val areResourcesLoaded: StateFlow<Boolean> = _areResourcesLoaded.asStateFlow()

    private var _gameSpeed = 5
    private var _maxCockroaches = 10
    private var _bonusInterval = 30
    private var _roundDuration = 120

    val gameSpeed: Int get() = _gameSpeed
    val maxCockroaches: Int get() = _maxCockroaches
    val bonusInterval: Int get() = _bonusInterval
    val roundDuration: Int get() = _roundDuration

    init {
        _timeLeft.value = _roundDuration
    }

    fun initializeSettings(
        gameSpeed: Int,
        maxCockroaches: Int,
        bonusInterval: Int,
        roundDuration: Int
    ) {
        _gameSpeed = gameSpeed
        _maxCockroaches = maxCockroaches
        _bonusInterval = bonusInterval
        _roundDuration = roundDuration
        _timeLeft.value = roundDuration
    }

    fun setResourcesLoaded(loaded: Boolean) {
        _areResourcesLoaded.value = loaded
    }

    fun addPoints(points: Int) {
        _score.value += points
    }

    fun subtractPoints(points: Int) {
        _score.value = (_score.value - points).coerceAtLeast(0)
    }

    fun updateTimeLeft(newTime: Int) {
        _timeLeft.value = newTime
    }

    fun setPlaying(playing: Boolean) {
        _isPlaying.value = playing
    }

    fun updateGoldRate(rate: Double) {
        _currentGoldRate.value = rate
    }
    
    fun startGame() {
        _isPlaying.value = true
    }

    fun pauseGame() {
        _isPlaying.value = false
    }

    fun endGame() {
        _isPlaying.value = false
    }

    fun getFinalScore(): Int = _score.value
}