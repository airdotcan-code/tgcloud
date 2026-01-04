package com.example.tgcloud.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.tgcloud.data.database.entities.PhotoEntity
import com.example.tgcloud.data.database.entities.WatchFolderEntity

@Database(
    entities = [PhotoEntity::class, WatchFolderEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun photoDao(): PhotoDao
}