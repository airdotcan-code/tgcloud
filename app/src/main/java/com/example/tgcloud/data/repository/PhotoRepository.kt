package com.example.tgcloud.data.repository

import android.content.Context
import com.example.tgcloud.data.database.PhotoDao
import com.example.tgcloud.data.database.entities.PhotoEntity
import com.example.tgcloud.data.database.entities.UploadStatus
import com.example.tgcloud.data.database.entities.WatchFolderEntity
import com.example.tgcloud.data.preferences.AppPreferences
import com.example.tgcloud.telegram.TelegramUploader
import com.example.tgcloud.telegram.UploadResult
import com.example.tgcloud.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

class PhotoRepository(
    private val context: Context,
    private val photoDao: PhotoDao,
    private val preferences: AppPreferences,
    private val telegramUploader: TelegramUploader
) {

    private val trashFolder: File by lazy {
        File(context.getExternalFilesDir(null), "trash").apply {
            if (!exists()) mkdirs()
        }
    }

    fun getAllPhotos(): Flow<List<PhotoEntity>> = photoDao.getAllPhotos()

    fun getTotalCount(): Flow<Int> = photoDao.getTotalCount()

    fun getTotalUploadedSize(): Flow<Long?> = photoDao.getTotalUploadedSize()

    suspend fun getPendingPhotos(limit: Int): List<PhotoEntity> =
        photoDao.getPendingPhotos(limit)

    suspend fun getStatusCounts(): Map<UploadStatus, Int> {
        return UploadStatus.values().associateWith { status ->
            photoDao.getCountByStatus(status)
        }
    }

    suspend fun scanFolders(): Int = withContext(Dispatchers.IO) {
        val folders = photoDao.getEnabledFolders()
        var addedCount = 0

        for (folder in folders) {
            val folderFile = File(folder.path)
            if (!folderFile.exists()) continue

            val files = if (folder.includeSubfolders) {
                folderFile.walkTopDown()
                    .filter { it.isFile && FileUtils.isImageFile(it.name) }
                    .toList()
            } else {
                folderFile.listFiles()
                    ?.filter { it.isFile && FileUtils.isImageFile(it.name) }
                    ?: emptyList()
            }

            for (file in files) {
                try {
                    val hash = FileUtils.calculateMD5(file)

                    if (!photoDao.existsByHash(hash)) {
                        val photo = PhotoEntity(
                            filePath = file.absolutePath,
                            fileName = file.name,
                            fileHash = hash,
                            fileSize = file.length(),
                            mimeType = FileUtils.getMimeType(file.name) ?: "image/*"
                        )

                        val id = photoDao.insertPhoto(photo)
                        if (id > 0) addedCount++
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            photoDao.updateFolderScanTime(folder.id)
        }

        addedCount
    }

    suspend fun uploadPendingPhotos(
        onProgress: (current: Int, total: Int, fileName: String) -> Unit = { _, _, _ -> }
    ): UploadStats = withContext(Dispatchers.IO) {
        val settings = preferences.settings.first()
        val pending = getPendingPhotos(settings.batchSize)

        var successful = 0
        var failed = 0
        var skipped = 0

        pending.forEachIndexed { index, photo ->
            onProgress(index + 1, pending.size, photo.fileName)

            val file = File(photo.filePath)

            if (!file.exists()) {
                photoDao.markAsFailed(photo.id, error = "Файл не найден")
                skipped++
                return@forEachIndexed
            }

            photoDao.updatePhoto(photo.copy(status = UploadStatus.UPLOADING))

            val caption = telegramUploader.generateCaption(file)

            val result = if (settings.sendAsFile) {
                telegramUploader.sendDocument(
                    botToken = settings.botToken,
                    channelId = settings.channelId,
                    file = file,
                    caption = caption
                )
            } else {
                telegramUploader.sendPhoto(
                    botToken = settings.botToken,
                    channelId = settings.channelId,
                    file = file,
                    caption = caption
                )
            }

            when (result) {
                is UploadResult.Success -> {
                    photoDao.markAsCompleted(
                        id = photo.id,
                        fileId = result.fileId,
                        messageId = result.messageId
                    )
                    successful++

                    if (settings.deleteAfterUpload) {
                        moveToTrash(file)
                    }
                }
                is UploadResult.Error -> {
                    photoDao.markAsFailed(photo.id, error = result.message)
                    failed++
                }
            }

            delay(1500)
        }

        preferences.updateLastSyncTime()
        UploadStats(successful, failed, skipped)
    }

    private fun moveToTrash(file: File): Boolean {
        return try {
            val destFile = File(trashFolder, "${System.currentTimeMillis()}_${file.name}")
            val moved = file.renameTo(destFile)
            if (moved) {
                android.util.Log.d("PhotoRepository", "Перемещено в корзину: ${file.name}")
            } else {
                android.util.Log.w("PhotoRepository", "Не удалось переместить: ${file.name}")
            }
            moved
        } catch (e: Exception) {
            android.util.Log.e("PhotoRepository", "Ошибка перемещения: ${e.message}")
            false
        }
    }

    suspend fun clearTrash(): Int = withContext(Dispatchers.IO) {
        var deleted = 0
        trashFolder.listFiles()?.forEach { file ->
            if (file.delete()) deleted++
        }
        deleted
    }

    fun getTrashSize(): Long {
        return trashFolder.listFiles()?.sumOf { it.length() } ?: 0
    }

    fun getTrashCount(): Int {
        return trashFolder.listFiles()?.size ?: 0
    }

    suspend fun resetFailed() {
        photoDao.resetFailed()
    }

    fun getAllFolders(): Flow<List<WatchFolderEntity>> = photoDao.getAllFolders()

    suspend fun addFolder(path: String, displayName: String) {
        val folder = WatchFolderEntity(
            path = path,
            displayName = displayName
        )
        photoDao.insertFolder(folder)
    }

    suspend fun updateFolder(folder: WatchFolderEntity) {
        photoDao.updateFolder(folder)
    }

    suspend fun deleteFolder(folder: WatchFolderEntity) {
        photoDao.deleteFolder(folder)
    }

    data class UploadStats(
        val successful: Int,
        val failed: Int,
        val skipped: Int
    )
}