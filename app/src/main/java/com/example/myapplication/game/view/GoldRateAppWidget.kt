package com.example.myapplication.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.Gravity
import android.widget.RemoteViews
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.R
import com.example.myapplication.data.network.CbrApiService
import com.example.myapplication.data.repository.GoldRateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import java.util.concurrent.TimeUnit

class GoldRateAppWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        updateAllWidgets(context)
    }

    override fun onDisabled(context: Context) {
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_gold_rate)

        //Пока не прогрузилось у нас ---
        views.setTextViewText(R.id.tv_gold_rate, "₽---")


        //нажал на виджет и он обновился
        val updateIntent = Intent(context, GoldRateAppWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
        }
        val pendingUpdateIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            updateIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, pendingUpdateIntent)

        views.setInt(R.id.tv_gold_rate, "setGravity", Gravity.CENTER)//текст по центру

        appWidgetManager.updateAppWidget(appWidgetId, views)

        //Загружаем актуальный курс
        loadAndUpdateGoldRate(context, appWidgetManager, appWidgetId)
    }

    private fun loadAndUpdateGoldRate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = GoldRateRepository(createCbrApiService())
                val rate = repository.getCurrentGoldRate()

                val views = RemoteViews(context.packageName, R.layout.widget_gold_rate)

                val formattedRate = "₽${String.format("%,d", rate.toInt())}"
                views.setTextViewText(R.id.tv_gold_rate, formattedRate)

                views.setInt(R.id.tv_gold_rate, "setGravity", Gravity.CENTER)

                appWidgetManager.updateAppWidget(appWidgetId, views)

            } catch (e: Exception) {
                val views = RemoteViews(context.packageName, R.layout.widget_gold_rate)
                views.setTextViewText(R.id.tv_gold_rate, "Ошибка")
                views.setInt(R.id.tv_gold_rate, "setGravity", Gravity.CENTER)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    private fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, GoldRateAppWidget::class.java)
        )
        onUpdate(context, appWidgetManager, appWidgetIds)
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
}