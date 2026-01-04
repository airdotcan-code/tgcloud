package com.example.tgcloud.ui.screens

import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch
import com.example.tgcloud.ui.screens.HelpScreen
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tgcloud.data.database.entities.PhotoEntity
import com.example.tgcloud.data.database.entities.UploadStatus
import com.example.tgcloud.data.database.entities.WatchFolderEntity
import com.example.tgcloud.data.preferences.AppPreferences
import com.example.tgcloud.ui.viewmodels.MainViewModel
import com.example.tgcloud.ui.viewmodels.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (uiState.currentScreen) {
                            Screen.Home -> "📷 Photo Cloud"
                            Screen.Settings -> "⚙️ Настройки"
                            Screen.Folders -> "📁 Папки"
                            Screen.Errors -> "❌ Ошибки"
                            Screen.Help -> "📖 Помощь"
                        }
                    )
                },
                navigationIcon = {
                    if (uiState.currentScreen != Screen.Home) {
                        IconButton(onClick = { viewModel.navigateTo(Screen.Home) }) {
                            Icon(Icons.Default.ArrowBack, "Назад")
                        }
                    }
                },
                actions = {
                    if (uiState.currentScreen == Screen.Home) {
                        IconButton(onClick = { viewModel.navigateTo(Screen.Help) }) {
                            Icon(Icons.Default.Help, "Помощь")
                        }
                        IconButton(onClick = { viewModel.navigateTo(Screen.Settings) }) {
                            Icon(Icons.Default.Settings, "Настройки")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (uiState.currentScreen) {
                Screen.Home -> HomeContent(viewModel, uiState, context)
                Screen.Settings -> SettingsContent(viewModel)
                Screen.Folders -> FoldersContent(viewModel)
                Screen.Errors -> ErrorsContent(viewModel)
                Screen.Help -> HelpScreen()
            }
        }
    }
}

// Функция открытия канала в Telegram
fun openTelegramChannel(context: Context, channelId: String) {
    if (channelId.isBlank()) return

    val channelLink = when {
        channelId.startsWith("@") -> "https://t.me/${channelId.removePrefix("@")}"
        channelId.startsWith("-100") -> "https://t.me/c/${channelId.removePrefix("-100")}"
        else -> "https://t.me/$channelId"
    }

    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(channelLink))
        context.startActivity(intent)
    } catch (e: Exception) {
        // Если не удалось открыть
    }
}

@Composable
fun HomeContent(
    viewModel: MainViewModel,
    uiState: com.example.tgcloud.ui.viewmodels.MainUiState,
    context: Context
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ... карточки статуса остаются без изменений ...

        // Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.isConfigured)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (uiState.isConfigured) Icons.Default.Cloud else Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = if (uiState.isConfigured) "Готов к работе" else "Требуется настройка",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Последняя синхр.: ${uiState.lastSyncTime}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Stats
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "В очереди",
                    value = uiState.pendingCount.toString(),
                    icon = Icons.Default.Schedule,
                    onClick = null
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Загружено",
                    value = uiState.completedCount.toString(),
                    icon = Icons.Default.CloudDone,
                    onClick = { openTelegramChannel(context, uiState.channelId) }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Ошибки",
                    value = uiState.failedCount.toString(),
                    icon = Icons.Default.Error,
                    onClick = {
                        if (uiState.failedCount > 0) {
                            viewModel.navigateTo(Screen.Errors)
                        }
                    },
                    isError = uiState.failedCount > 0
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Размер",
                    value = uiState.totalSize,
                    icon = Icons.Default.Storage,
                    onClick = { openTelegramChannel(context, uiState.channelId) }
                )
            }
        }

        // Actions
        item {
            Text("Действия", style = MaterialTheme.typography.titleMedium)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.navigateTo(Screen.Folders) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Folder, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Папки")
                }

                Button(
                    onClick = { viewModel.scanFolders() },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isSyncing
                ) {
                    Icon(Icons.Default.Search, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Скан")
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.startUpload() },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isSyncing && uiState.isConfigured && uiState.pendingCount > 0
                ) {
                    Icon(Icons.Default.CloudUpload, null)
                    Spacer(Modifier.width(4.dp))
                    Text("10 шт")
                }

                OutlinedButton(
                    onClick = { viewModel.retryFailed() },
                    modifier = Modifier.weight(1f),
                    enabled = uiState.failedCount > 0
                ) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Повтор")
                }
            }
        }

        // ГЛАВНАЯ КНОПКА — меняется в зависимости от статуса
        item {
            if (uiState.isServiceRunning) {
                // Показываем кнопку СТОП
                Button(
                    onClick = { viewModel.stopUpload(context) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Stop, null)
                    Spacer(Modifier.width(8.dp))
                    Text("⏹ Остановить загрузку")
                }
            } else {
                // Показываем кнопку ЗАГРУЗИТЬ
                Button(
                    onClick = { viewModel.uploadAll(context) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.isConfigured && uiState.pendingCount > 0
                ) {
                    Icon(Icons.Default.CloudUpload, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Загрузить всё (${uiState.pendingCount})")
                }
            }
        }

        // Кнопка открыть канал
        if (uiState.isConfigured) {
            item {
                OutlinedButton(
                    onClick = { openTelegramChannel(context, uiState.channelId) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.OpenInNew, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Открыть канал в Telegram")
                }
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: (() -> Unit)? = null,
    isError: Boolean = false
) {
    Card(
        modifier = modifier,
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        colors = if (isError && value != "0") {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                null,
                tint = if (isError && value != "0")
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(title, style = MaterialTheme.typography.bodySmall)
            if (onClick != null) {
                Text(
                    "нажмите",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

// ===== ЭКРАН ОШИБОК =====
@Composable
fun ErrorsContent(viewModel: MainViewModel) {
    val allPhotos by viewModel.getErrorPhotos().collectAsState(initial = emptyList())
    val errorPhotos = allPhotos.filter { it.status == UploadStatus.FAILED }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Кнопка повторить все
        if (errorPhotos.isNotEmpty()) {
            Button(
                onClick = { viewModel.retryFailed() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("Повторить все (${errorPhotos.size})")
            }

            Spacer(Modifier.height(16.dp))
        }

        if (errorPhotos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CheckCircle,
                        null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Ошибок нет!", style = MaterialTheme.typography.titleLarge)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(errorPhotos, key = { it.id }) { photo ->
                    ErrorPhotoItem(photo)
                }
            }
        }
    }
}

@Composable
fun ErrorPhotoItem(photo: PhotoEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.BrokenImage,
                    null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    photo.fileName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                "Путь: ${photo.filePath}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            photo.errorMessage?.let { error ->
                Spacer(Modifier.height(4.dp))
                Text(
                    "Ошибка: $error",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Text(
                "Попыток: ${photo.retryCount}/3",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

// ===== ЭКРАН НАСТРОЕК =====
@Composable
fun SettingsContent(viewModel: MainViewModel) {
    val settings by viewModel.settings.collectAsState(initial = AppPreferences.Settings())
    var showTokenDialog by remember { mutableStateOf(false) }
    var showChannelDialog by remember { mutableStateOf(false) }

    // Для корзины
    var trashCount by remember { mutableStateOf(0) }
    var trashSize by remember { mutableStateOf("0 B") }

    LaunchedEffect(Unit) {
        val repo = com.example.tgcloud.di.AppContainer.getRepository()
        trashCount = repo.getTrashCount()
        trashSize = com.example.tgcloud.utils.FileUtils.formatFileSize(repo.getTrashSize())
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ===== TELEGRAM =====
        item {
            Text(
                "Telegram",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showTokenDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Key, null)
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Bot Token", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            if (settings.botToken.isNotBlank())
                                "••••${settings.botToken.takeLast(6)}"
                            else
                                "Нажмите чтобы ввести",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Icon(Icons.Default.ChevronRight, null)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showChannelDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Send, null)
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Channel ID", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            settings.channelId.ifBlank { "Нажмите чтобы ввести" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Icon(Icons.Default.ChevronRight, null)
                }
            }
        }

        item {
            Button(
                onClick = { viewModel.testConnection() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.NetworkCheck, null)
                Spacer(Modifier.width(8.dp))
                Text("Проверить подключение")
            }
        }

        // ===== ЗАГРУЗКА =====
        item {
            Text(
                "Загрузка",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.InsertDriveFile, null)
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Как файл (без сжатия)")
                        Text(
                            "Сохраняет оригинальное качество",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Switch(
                        checked = settings.sendAsFile,
                        onCheckedChange = { viewModel.setSendAsFile(it) }
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DeleteForever, null)
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Удалять после загрузки")
                        Text(
                            "Перемещает в корзину приложения",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Switch(
                        checked = settings.deleteAfterUpload,
                        onCheckedChange = { viewModel.setDeleteAfterUpload(it) }
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Wifi, null)
                    Spacer(Modifier.width(16.dp))
                    Text("Только по Wi-Fi", modifier = Modifier.weight(1f))
                    Switch(
                        checked = settings.wifiOnly,
                        onCheckedChange = { viewModel.setWifiOnly(it) }
                    )
                }
            }
        }

        // ===== КОРЗИНА =====
        item {
            Text(
                "Корзина",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            val scope = rememberCoroutineScope()

            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Delete, null)
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Файлов: $trashCount")
                        Text(
                            "Размер: $trashSize",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    if (trashCount > 0) {
                        Button(
                            onClick = {
                                scope.launch {
                                    val repo = com.example.tgcloud.di.AppContainer.getRepository()
                                    repo.clearTrash()
                                    trashCount = 0
                                    trashSize = "0 B"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Очистить")
                        }
                    }
                }
            }
        }
    }

    // ===== ДИАЛОГИ =====

    if (showTokenDialog) {
        var tokenInput by remember { mutableStateOf(settings.botToken) }

        AlertDialog(
            onDismissRequest = { showTokenDialog = false },
            title = { Text("Bot Token") },
            text = {
                Column {
                    Text(
                        "Получите токен у @BotFather в Telegram",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        label = { Text("Вставьте токен") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveBotToken(tokenInput)
                    showTokenDialog = false
                }) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { showTokenDialog = false }) { Text("Отмена") }
            }
        )
    }

    if (showChannelDialog) {
        var channelInput by remember { mutableStateOf(settings.channelId) }

        AlertDialog(
            onDismissRequest = { showChannelDialog = false },
            title = { Text("Channel ID") },
            text = {
                Column {
                    Text(
                        "Введите @username или ID канала (-100...)",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    OutlinedTextField(
                        value = channelInput,
                        onValueChange = { channelInput = it },
                        label = { Text("Channel ID") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveChannelId(channelInput)
                    showChannelDialog = false
                }) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { showChannelDialog = false }) { Text("Отмена") }
            }
        )
    }
}
// ===== ЭКРАН ПАПОК =====
@Composable
fun FoldersContent(viewModel: MainViewModel) {
    val folders by viewModel.folders.collectAsState(initial = emptyList())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Быстрое добавление", style = MaterialTheme.typography.titleSmall)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SuggestionChip(
                    onClick = {
                        val path = Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DCIM
                        ).absolutePath + "/Camera"
                        viewModel.addFolder(path, "📷 Camera")
                    },
                    label = { Text("📷 Camera") }
                )
                SuggestionChip(
                    onClick = {
                        val path = Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PICTURES
                        ).absolutePath
                        viewModel.addFolder(path, "🖼 Pictures")
                    },
                    label = { Text("🖼 Pictures") }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SuggestionChip(
                    onClick = {
                        val path = Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS
                        ).absolutePath
                        viewModel.addFolder(path, "📥 Downloads")
                    },
                    label = { Text("📥 Downloads") }
                )
                SuggestionChip(
                    onClick = {
                        val path = Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DCIM
                        ).absolutePath + "/Screenshots"
                        viewModel.addFolder(path, "📱 Screenshots")
                    },
                    label = { Text("📱 Screenshots") }
                )
            }
        }

        item {
            Text(
                "Добавленные папки (${folders.size})",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        if (folders.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.FolderOpen,
                            null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Нет папок")
                        Text(
                            "Добавьте папку кнопками выше",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        items(folders, key = { it.id }) { folder ->
            FolderItem(
                folder = folder,
                onToggle = { viewModel.toggleFolder(folder) },
                onDelete = { viewModel.deleteFolder(folder) }
            )
        }
    }
}

@Composable
fun FolderItem(
    folder: WatchFolderEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Folder,
                null,
                tint = if (folder.isEnabled)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(folder.displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    folder.path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Switch(checked = folder.isEnabled, onCheckedChange = { onToggle() })
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}