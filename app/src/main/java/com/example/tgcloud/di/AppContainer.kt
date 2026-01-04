package com.example.tgcloud.di

import android.content.Context
import androidx.room.Room
import com.example.tgcloud.data.database.AppDatabase
import com.example.tgcloud.data.database.PhotoDao
import com.example.tgcloud.data.preferences.AppPreferences
import com.example.tgcloud.data.repository.PhotoRepository
import com.example.tgcloud.telegram.TelegramUploader

object AppContainer {

    private var database: AppDatabase? = null
    private var preferences: AppPreferences? = null
    private var repository: PhotoRepository? = null
    private var uploader: TelegramUploader? = null

    fun init(context: Context) {
        if (database == null) {
            database = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "photo_cloud.db"
            ).build()
        }

        if (preferences == null) {
            preferences = AppPreferences(context.applicationContext)
        }

        if (uploader == null) {
            uploader = TelegramUploader()
        }

        if (repository == null) {
            repository = PhotoRepository(
                context = context.applicationContext,
                photoDao = database!!.photoDao(),
                preferences = preferences!!,
                telegramUploader = uploader!!
            )
        }
    }

    fun getDatabase(): AppDatabase = database!!
    fun getPhotoDao(): PhotoDao = database!!.photoDao()
    fun getPreferences(): AppPreferences = preferences!!
    fun getRepository(): PhotoRepository = repository!!
    fun getUploader(): TelegramUploader = uploader!!
}