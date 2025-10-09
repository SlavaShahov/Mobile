package com.example.myapplication.di

import android.content.Context
import android.hardware.SensorManager
import com.example.myapplication.data.local.PreferencesManager
import com.example.myapplication.data.network.CbrApiService
import com.example.myapplication.data.repository.GoldRateRepository
import com.example.myapplication.data.sensor.GyroscopeManager
import com.example.myapplication.game.sound.SoundManager
import com.example.myapplication.ui.viewmodel.GameViewModel
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import java.util.concurrent.TimeUnit

val appModule = module {
    single { PreferencesManager(androidContext()) }
    single { SoundManager(androidContext()) }

    single<CbrApiService> {
        createCbrApiService()
    }

    single { GoldRateRepository(get()) }

    // ViewModel без зависимостей для простоты
    viewModel { GameViewModel() }

    factory { (onTiltChanged: (Float, Float) -> Unit) ->
        val sensorManager = androidContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        GyroscopeManager(sensorManager, onTiltChanged)
    }
}

private fun createCbrApiService(): CbrApiService {
    val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val retrofit = Retrofit.Builder()
        .baseUrl("https://www.cbr.ru/")
        .client(client)
        .addConverterFactory(SimpleXmlConverterFactory.create())
        .build()

    return retrofit.create(CbrApiService::class.java)
}