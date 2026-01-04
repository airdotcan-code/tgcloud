package com.example.tgcloud

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.example.tgcloud.di.AppContainer

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // Инициализация зависимостей
        AppContainer.init(this)

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val syncChannel = NotificationChannel(
            CHANNEL_SYNC,
            "Синхронизация",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Уведомления о синхронизации фото"
        }

        val uploadChannel = NotificationChannel(
            CHANNEL_UPLOAD,
            "Загрузка",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Прогресс загрузки фото"
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(syncChannel)
        manager.createNotificationChannel(uploadChannel)
    }

    companion object {
        const val CHANNEL_SYNC = "sync_channel"
        const val CHANNEL_UPLOAD = "upload_channel"
    }
}