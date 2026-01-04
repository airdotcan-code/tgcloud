package com.example.tgcloud.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_folders")
data class WatchFolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val path: String,
    val displayName: String,
    val isEnabled: Boolean = true,
    val includeSubfolders: Boolean = true,
    val addedAt: Long = System.currentTimeMillis(),
    val lastScanAt: Long? = null
)