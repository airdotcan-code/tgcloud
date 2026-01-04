package com.example.tgcloud.telegram

import android.webkit.MimeTypeMap
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

sealed class UploadResult {
    data class Success(val fileId: String, val messageId: Long) : UploadResult()
    data class Error(val message: String) : UploadResult()
}

class TelegramUploader {

    private val boundary = "----${System.currentTimeMillis()}"
    private val lineEnd = "\r\n"

    private fun getApiUrl(botToken: String, method: String): String {
        return "https://api.telegram.org/bot$botToken/$method"
    }

    suspend fun sendDocument(
        botToken: String,
        channelId: String,
        file: File,
        caption: String? = null
    ): UploadResult = withContext(Dispatchers.IO) {
        try {
            val url = URL(getApiUrl(botToken, "sendDocument"))
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                useCaches = false
                connectTimeout = 60000
                readTimeout = 120000
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            }

            DataOutputStream(connection.outputStream).use { outputStream ->
                // chat_id
                writeFormField(outputStream, "chat_id", channelId)

                // parse_mode
                writeFormField(outputStream, "parse_mode", "Markdown")

                // caption
                caption?.let { writeFormField(outputStream, "caption", it) }

                // document file
                writeFileField(outputStream, "document", file)

                // End boundary
                outputStream.writeBytes("--$boundary--$lineEnd")
                outputStream.flush()
            }

            val responseCode = connection.responseCode
            val response = if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().readText()
            } else {
                connection.errorStream?.bufferedReader()?.readText() ?: "Error: $responseCode"
            }

            connection.disconnect()
            parseResponse(response)

        } catch (e: Exception) {
            UploadResult.Error("Error: ${e.message}")
        }
    }

    suspend fun sendPhoto(
        botToken: String,
        channelId: String,
        file: File,
        caption: String? = null
    ): UploadResult = withContext(Dispatchers.IO) {
        try {
            val url = URL(getApiUrl(botToken, "sendPhoto"))
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                useCaches = false
                connectTimeout = 60000
                readTimeout = 120000
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            }

            DataOutputStream(connection.outputStream).use { outputStream ->
                writeFormField(outputStream, "chat_id", channelId)
                writeFormField(outputStream, "parse_mode", "Markdown")
                caption?.let { writeFormField(outputStream, "caption", it) }
                writeFileField(outputStream, "photo", file)
                outputStream.writeBytes("--$boundary--$lineEnd")
                outputStream.flush()
            }

            val responseCode = connection.responseCode
            val response = if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().readText()
            } else {
                connection.errorStream?.bufferedReader()?.readText() ?: "Error: $responseCode"
            }

            connection.disconnect()
            parseResponse(response)

        } catch (e: Exception) {
            UploadResult.Error("Error: ${e.message}")
        }
    }

    private fun writeFormField(outputStream: DataOutputStream, name: String, value: String) {
        outputStream.writeBytes("--$boundary$lineEnd")
        outputStream.writeBytes("Content-Disposition: form-data; name=\"$name\"$lineEnd")
        outputStream.writeBytes(lineEnd)
        outputStream.write(value.toByteArray(Charsets.UTF_8))
        outputStream.writeBytes(lineEnd)
    }

    private fun writeFileField(outputStream: DataOutputStream, name: String, file: File) {
        val mimeType = getMimeType(file.name) ?: "application/octet-stream"

        outputStream.writeBytes("--$boundary$lineEnd")
        outputStream.writeBytes("Content-Disposition: form-data; name=\"$name\"; filename=\"${file.name}\"$lineEnd")
        outputStream.writeBytes("Content-Type: $mimeType$lineEnd")
        outputStream.writeBytes(lineEnd)

        FileInputStream(file).use { fileInputStream ->
            val buffer = ByteArray(4096)
            var bytesRead: Int
            while (fileInputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
        }

        outputStream.writeBytes(lineEnd)
    }

    private fun parseResponse(response: String): UploadResult {
        return try {
            val json = JSONObject(response)

            if (json.getBoolean("ok")) {
                val result = json.getJSONObject("result")
                val messageId = result.getLong("message_id")

                val fileId = when {
                    result.has("document") -> result.getJSONObject("document").getString("file_id")
                    result.has("photo") -> {
                        val photos = result.getJSONArray("photo")
                        photos.getJSONObject(photos.length() - 1).getString("file_id")
                    }
                    else -> ""
                }

                UploadResult.Success(fileId, messageId)
            } else {
                val description = json.optString("description", "Unknown error")
                UploadResult.Error(description)
            }
        } catch (e: Exception) {
            UploadResult.Error("Parse error: ${e.message}")
        }
    }

    suspend fun testConnection(botToken: String, channelId: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val url = URL(getApiUrl(botToken, "getChat?chat_id=$channelId"))
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)
                    connection.disconnect()
                    json.getBoolean("ok")
                } else {
                    connection.disconnect()
                    false
                }
            } catch (e: Exception) {
                false
            }
        }

    fun generateCaption(file: File): String {
        val sizeStr = formatFileSize(file.length())
        val exifInfo = getExifInfo(file)

        val parts = mutableListOf<String>()
        parts.add("📁 `${file.name}`")
        parts.add("📊 $sizeStr")

        exifInfo.date?.let { parts.add("📅 $it") }
        exifInfo.camera?.let { parts.add("📷 $it") }

        val ext = file.extension.uppercase()
        if (ext.isNotEmpty()) {
            parts.add("🏷 #$ext")
        }

        return parts.joinToString("\n")
    }

    private fun getExifInfo(file: File): ExifInfo {
        return try {
            val exif = ExifInterface(file)
            ExifInfo(
                date = exif.getAttribute(ExifInterface.TAG_DATETIME)?.let {
                    formatExifDate(it)
                },
                camera = exif.getAttribute(ExifInterface.TAG_MODEL)
                    ?: exif.getAttribute(ExifInterface.TAG_MAKE)
            )
        } catch (e: Exception) {
            ExifInfo()
        }
    }

    private fun formatExifDate(exifDate: String): String? {
        return try {
            val inputFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            inputFormat.parse(exifDate)?.let { outputFormat.format(it) }
        } catch (e: Exception) {
            null
        }
    }

    private fun getMimeType(fileName: String): String? {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }

    private fun formatFileSize(size: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB")
        var fileSize = size.toDouble()
        var unitIndex = 0

        while (fileSize >= 1024 && unitIndex < units.size - 1) {
            fileSize /= 1024
            unitIndex++
        }

        return "%.1f %s".format(fileSize, units[unitIndex])
    }

    private data class ExifInfo(
        val date: String? = null,
        val camera: String? = null
    )
}