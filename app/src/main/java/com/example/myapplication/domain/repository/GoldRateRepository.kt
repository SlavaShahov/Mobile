package com.example.myapplication.data.repository

import android.util.Log
import com.example.myapplication.data.network.CbrApiService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GoldRateRepository(private val apiService: CbrApiService) {

    suspend fun getCurrentGoldRate(): Double {
        try {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val currentDate = dateFormat.format(Date())

            Log.d("GoldRateRepository", "Fetching gold rate for date: $currentDate")

            val response = apiService.getGoldRates(currentDate, currentDate)

            Log.d("GoldRateRepository", "Response records count: ${response.records.size}")

            response.records.forEach { record ->
                Log.d("GoldRateRepository", "Record: code=${record.code}, buy=${record.buy}, date=${record.date}")
            }

            val goldRecord = response.records
                .firstOrNull { it.code == "1" } // 1 - код золота

            return if (goldRecord != null && goldRecord.buy.isNotEmpty()) {
                val rate = goldRecord.buy.replace(",", ".").toDoubleOrNull() ?: 5000.0
                Log.d("GoldRateRepository", "Gold rate found: $rate")
                rate
            } else {
                Log.d("GoldRateRepository", "Gold record not found, using default: 5000.0")
                5000.0
            }

        } catch (e: Exception) {
            Log.e("GoldRateRepository", "Error fetching gold rate", e)
            return 5000.0
        }
    }
}