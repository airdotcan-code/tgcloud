package com.example.tgcloud.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.tgcloud.App
import com.example.tgcloud.MainActivity
import com.example.tgcloud.R
import com.example.tgcloud.di.AppContainer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class UploadService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "START"
        const val ACTION_STOP = "STOP"

        @Volatile
        var isRunning = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, UploadService::class.java).apply {
                action = ACTION_START
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, UploadService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                if (!isRunning) {
                    startForeground(NOTIFICATION_ID, createNotification("Подготовка..."))
                    startUploading()
                }
            }
            ACTION_STOP -> {
                stopUploading()
            }
        }
        return START_NOT_STICKY
    }

    private fun startUploading() {
        isRunning = true

        serviceScope.launch {
            try {
                val repository = AppContainer.getRepository()
                val preferences = AppContainer.getPreferences()

                // Сначала сканируем
                updateNotification("Сканирование папок...")
                val added = repository.scanFolders()

                if (added > 0) {
                    updateNotification("Найдено $added новых фото")
                    delay(1000)
                }

                // Загружаем всё
                var totalSuccess = 0
                var totalFailed = 0
                var hasMore = true
                var iteration = 0

                while (hasMore && isRunning && iteration < 500) {
                    iteration++

                    val settings = preferences.settings.first()
                    if (!settings.isConfigured) {
                        updateNotification("❌ Не настроено")
                        break
                    }

                    val stats = repository.uploadPendingPhotos { current, total, fileName ->
                        val shortName = if (fileName.length > 20) {
                            fileName.take(17) + "..."
                        } else {
                            fileName
                        }
                        updateNotification("📤 ${totalSuccess + current}: $shortName")
                    }

                    totalSuccess += stats.successful
                    totalFailed += stats.failed

                    if (stats.successful == 0 && stats.failed == 0) {
                        hasMore = false
                    }
                }

                // Завершено
                showCompletionNotification(totalSuccess, totalFailed)

            } catch (e: Exception) {
                updateNotification("❌ Ошибка: ${e.message}")
                delay(3000)
            } finally {
                stopSelf()
            }
        }
    }

    private fun stopUploading() {
        isRunning = false
        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, UploadService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, App.CHANNEL_UPLOAD)
            .setContentTitle("Photo Cloud")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Стоп", stopPendingIntent)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = createNotification(text)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun showCompletionNotification(success: Int, failed: Int) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, App.CHANNEL_UPLOAD)
            .setContentTitle("Загрузка завершена")
            .setContentText("✅ $success  ❌ $failed")
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID + 1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceScope.cancel()
    }
}