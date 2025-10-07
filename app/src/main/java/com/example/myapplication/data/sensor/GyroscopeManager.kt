package com.example.myapplication.data.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs

class GyroscopeManager(
    private val sensorManager: SensorManager,
    private val onTiltChanged: (x: Float, y: Float) -> Unit
) : SensorEventListener {

    private var accelerometer: Sensor? = null
    private var isListening = false

    init {
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    fun startListening() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            isListening = true
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
        isListening = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isListening) return

        event?.let { sensorEvent ->
            when (sensorEvent.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    val rawTiltX = sensorEvent.values[0]
                    val rawTiltY = sensorEvent.values[1]

                    // Инвертируем и настраиваем чувствительность
                    val tiltX = -rawTiltX * 2f
                    val tiltY = rawTiltY * 2f

                    // Фильтруем небольшие колебания
                    val filterThreshold = 0.3f
                    val filteredTiltX = if (abs(tiltX) < filterThreshold) 0f else tiltX
                    val filteredTiltY = if (abs(tiltY) < filterThreshold) 0f else tiltY

                    onTiltChanged(filteredTiltX, filteredTiltY)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Не используется
    }

    fun isGyroscopeAvailable(): Boolean {
        return accelerometer != null
    }
}