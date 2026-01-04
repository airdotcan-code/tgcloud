package com.example.tgcloud.utils

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object FileUtils {

    fun calculateMD5(file: File): String {
        val md = MessageDigest.getInstance("MD5")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var read: Int
            while (fis.read(buffer).also { read = it } != -1) {
                md.update(buffer, 0, read)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    fun getMimeType(fileName: String): String? {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }

    fun isImageFile(fileName: String): Boolean {
        val extensions = listOf(
            "jpg", "jpeg", "png", "gif", "webp",
            "bmp", "heic", "heif", "raw", "cr2", "nef"
        )
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in extensions
    }

    fun formatFileSize(size: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB")
        var fileSize = size.toDouble()
        var unitIndex = 0

        while (fileSize >= 1024 && unitIndex < units.size - 1) {
            fileSize /= 1024
            unitIndex++
        }

        return "%.1f %s".format(fileSize, units[unitIndex])
    }

    /**
     * Удаляет файл. На Android 11+ может потребоваться подтверждение пользователя.
     */
    fun deleteFile(context: Context, file: File): Boolean {
        if (!file.exists()) return true

        val filePath = file.absolutePath

        // Находим URI файла в MediaStore
        val uri = getMediaUri(context, filePath)

        if (uri != null) {
            return try {
                // Пробуем удалить через ContentResolver
                val deleted = context.contentResolver.delete(uri, null, null) > 0

                if (deleted) {
                    // Также удаляем физический файл если ещё существует
                    if (file.exists()) {
                        file.delete()
                    }
                    true
                } else {
                    // Если не получилось — пробуем напрямую
                    file.delete()
                }
            } catch (e: SecurityException) {
                // На Android 11+ может потребоваться разрешение
                android.util.Log.w("FileUtils", "SecurityException при удалении: ${e.message}")
                // Пробуем удалить напрямую
                try {
                    file.delete()
                } catch (e2: Exception) {
                    false
                }
            } catch (e: Exception) {
                android.util.Log.e("FileUtils", "Ошибка удаления: ${e.message}")
                file.delete()
            }
        } else {
            // URI не найден — удаляем напрямую
            return try {
                file.delete()
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Получает URI файла из MediaStore
     */
    private fun getMediaUri(context: Context, filePath: String): Uri? {
        val contentResolver = context.contentResolver

        // Ищем в Images
        var cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Images.Media._ID),
            MediaStore.Images.Media.DATA + "=?",
            arrayOf(filePath),
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            }
        }

        // Ищем в Downloads
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            cursor = contentResolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Downloads._ID),
                MediaStore.Downloads.DATA + "=?",
                arrayOf(filePath),
                null
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.Downloads._ID))
                    return ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)
                }
            }
        }

        return null
    }

    /**
     * Пакетное удаление файлов (для Android 11+)
     */
    fun deleteFiles(context: Context, files: List<File>): Int {
        var deletedCount = 0

        for (file in files) {
            if (deleteFile(context, file)) {
                deletedCount++
            }
        }

        return deletedCount
    }
}