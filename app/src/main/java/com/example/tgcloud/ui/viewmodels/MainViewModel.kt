package com.example.tgcloud.ui.viewmodels

import kotlinx.coroutines.delay
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tgcloud.data.database.entities.PhotoEntity
import com.example.tgcloud.data.database.entities.UploadStatus
import com.example.tgcloud.data.database.entities.WatchFolderEntity
import com.example.tgcloud.data.preferences.AppPreferences
import com.example.tgcloud.data.repository.PhotoRepository
import com.example.tgcloud.di.AppContainer
import com.example.tgcloud.service.UploadService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*


enum class Screen {
    Home, Settings, Folders, Errors, Help  // ← Добавили Help
}

data class MainUiState(
    val isConfigured: Boolean = false,
    val isSyncing: Boolean = false,
    val isServiceRunning: Boolean = false,
    val pendingCount: Int = 0,
    val completedCount: Int = 0,
    val failedCount: Int = 0,
    val totalSize: String = "0 B",
    val lastSyncTime: String = "Никогда",
    val autoSyncEnabled: Boolean = false,
    val currentScreen: Screen = Screen.Home,
    val message: String? = null,
    val channelId: String = ""
)

class MainViewModel : ViewModel() {

    private val repository: PhotoRepository = AppContainer.getRepository()
    private val preferences: AppPreferences = AppContainer.getPreferences()

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val folders: Flow<List<WatchFolderEntity>> = repository.getAllFolders()
    val settings: Flow<AppPreferences.Settings> = preferences.settings

    init {
        loadData()
        watchServiceStatus()
    }

    private fun watchServiceStatus() {
        viewModelScope.launch {
            while (true) {
                _uiState.update {
                    it.copy(isServiceRunning = UploadService.isRunning)
                }
                refreshCounts()
                delay(2000)
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            preferences.settings.collect { settings ->
                _uiState.update { state ->
                    state.copy(
                        isConfigured = settings.isConfigured,
                        autoSyncEnabled = settings.autoSyncEnabled,
                        lastSyncTime = formatLastSync(settings.lastSyncTime),
                        channelId = settings.channelId
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.getTotalCount().collect { count ->
                _uiState.update { it.copy(completedCount = count) }
            }
        }

        viewModelScope.launch {
            repository.getTotalUploadedSize().collect { size ->
                _uiState.update { it.copy(totalSize = formatSize(size ?: 0)) }
            }
        }

        refreshCounts()
    }

    fun stopUpload(context: Context) {
        UploadService.stop(context)
        _uiState.update {
            it.copy(isServiceRunning = false, message = "Загрузка остановлена")
        }
    }

    fun refreshCounts() {
        viewModelScope.launch {
            val counts = repository.getStatusCounts()
            _uiState.update { state ->
                state.copy(
                    pendingCount = counts[UploadStatus.PENDING] ?: 0,
                    failedCount = counts[UploadStatus.FAILED] ?: 0,
                    completedCount = counts[UploadStatus.COMPLETED] ?: 0
                )
            }
        }
    }

    fun navigateTo(screen: Screen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun scanFolders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, message = "Сканирование...") }
            val added = repository.scanFolders()
            _uiState.update {
                it.copy(
                    isSyncing = false,
                    message = "Добавлено файлов: $added"
                )
            }
            refreshCounts()
        }
    }

    fun startUpload() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, message = "Загрузка...") }

            val stats = repository.uploadPendingPhotos { current, total, fileName ->
                _uiState.update {
                    it.copy(message = "Загрузка $current/$total: $fileName")
                }
            }

            _uiState.update {
                it.copy(
                    isSyncing = false,
                    message = "Готово: ✅${stats.successful} ❌${stats.failed} ⏭${stats.skipped}"
                )
            }
            refreshCounts()
        }
    }

    // Загрузка через Foreground Service
    fun uploadAll(context: Context) {
        _uiState.update { it.copy(message = "Запуск фоновой загрузки...") }
        UploadService.start(context)
    }



    fun syncNow(context: Context) {
        uploadAll(context)
    }

    fun retryFailed() {
        viewModelScope.launch {
            repository.resetFailed()
            _uiState.update { it.copy(message = "Ошибки сброшены") }
            refreshCounts()
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun getErrorPhotos(): Flow<List<PhotoEntity>> {
        return repository.getAllPhotos()
    }

    fun saveBotToken(token: String) {
        viewModelScope.launch {
            preferences.setBotToken(token)
        }
    }

    fun saveChannelId(channelId: String) {
        viewModelScope.launch {
            preferences.setChannelId(channelId)
        }
    }

    fun setSendAsFile(asFile: Boolean) {
        viewModelScope.launch {
            preferences.setSendAsFile(asFile)
        }
    }

    fun setAutoSync(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setAutoSyncEnabled(enabled)
        }
    }

    fun setWifiOnly(wifiOnly: Boolean) {
        viewModelScope.launch {
            preferences.setWifiOnly(wifiOnly)
        }
    }

    fun setSyncInterval(hours: Int) {
        viewModelScope.launch {
            preferences.setSyncInterval(hours)
        }
    }

    fun setDeleteAfterUpload(delete: Boolean) {
        viewModelScope.launch {
            preferences.setDeleteAfterUpload(delete)
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(message = "Проверка...") }
            val currentSettings = preferences.settings.first()
            val uploader = AppContainer.getUploader()
            val success = uploader.testConnection(currentSettings.botToken, currentSettings.channelId)
            _uiState.update {
                it.copy(
                    message = if (success) "✅ Подключение успешно!" else "❌ Ошибка подключения"
                )
            }
        }
    }

    fun addFolder(path: String, name: String) {
        viewModelScope.launch {
            repository.addFolder(path, name)
            _uiState.update { it.copy(message = "Папка добавлена: $name") }
        }
    }

    fun toggleFolder(folder: WatchFolderEntity) {
        viewModelScope.launch {
            repository.updateFolder(folder.copy(isEnabled = !folder.isEnabled))
        }
    }

    fun toggleSubfolders(folder: WatchFolderEntity) {
        viewModelScope.launch {
            repository.updateFolder(folder.copy(includeSubfolders = !folder.includeSubfolders))
        }
    }

    fun deleteFolder(folder: WatchFolderEntity) {
        viewModelScope.launch {
            repository.deleteFolder(folder)
            _uiState.update { it.copy(message = "Папка удалена") }
        }
    }

    private fun formatLastSync(timestamp: Long): String {
        if (timestamp == 0L) return "Никогда"
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun formatSize(size: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB")
        var fileSize = size.toDouble()
        var unitIndex = 0
        while (fileSize >= 1024 && unitIndex < units.size - 1) {
            fileSize /= 1024
            unitIndex++
        }
        return "%.1f %s".format(fileSize, units[unitIndex])
    }
}