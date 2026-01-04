package com.example.tgcloud.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HelpScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "📖 Инструкция по настройке",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Шаг 1: Создание бота
        item {
            HelpCard(
                stepNumber = 1,
                title = "Создайте бота в Telegram",
                icon = Icons.Default.SmartToy,
                steps = listOf(
                    "Откройте Telegram и найдите @BotFather",
                    "Отправьте команду /newbot",
                    "Придумайте имя бота (например: My Photo Cloud)",
                    "Придумайте username бота (например: my_photo_cloud_bot)",
                    "Скопируйте полученный токен — он выглядит так:\n7123456789:AAHxxxxxxxxxxxxxxxxxxxxxxxx"
                )
            )
        }

        // Шаг 2: Создание канала
        item {
            HelpCard(
                stepNumber = 2,
                title = "Создайте приватный канал",
                icon = Icons.Default.Forum,
                steps = listOf(
                    "В Telegram нажмите ≡ → Создать канал",
                    "Введите название (например: Мои фото)",
                    "Выберите тип: Приватный канал",
                    "Нажмите 'Создать'"
                )
            )
        }

        // Шаг 3: Добавление бота в канал
        item {
            HelpCard(
                stepNumber = 3,
                title = "Добавьте бота в канал",
                icon = Icons.Default.PersonAdd,
                steps = listOf(
                    "Откройте созданный канал",
                    "Нажмите на название канала вверху",
                    "Выберите 'Администраторы' → 'Добавить администратора'",
                    "Найдите вашего бота по username",
                    "Включите права: 'Отправка сообщений'",
                    "Нажмите 'Готово'"
                )
            )
        }

        // Шаг 4: Получение ID канала
        item {
            HelpCard(
                stepNumber = 4,
                title = "Получите ID канала",
                icon = Icons.Default.Tag,
                steps = listOf(
                    "Способ 1 (простой):",
                    "• Если у канала есть @username — используйте его",
                    "• Например: @my_photos_backup",
                    "",
                    "Способ 2 (для приватных каналов):",
                    "• Перешлите любое сообщение из канала боту @userinfobot",
                    "• Бот покажет ID канала (начинается с -100...)",
                    "• Например: -1001234567890"
                )
            )
        }

        // Шаг 5: Настройка приложения
        item {
            HelpCard(
                stepNumber = 5,
                title = "Настройте приложение",
                icon = Icons.Default.Settings,
                steps = listOf(
                    "Откройте ⚙️ Настройки в приложении",
                    "Нажмите 'Bot Token' и вставьте токен от BotFather",
                    "Нажмите 'Channel ID' и введите ID канала",
                    "Нажмите 'Проверить подключение'",
                    "Если всё верно — появится ✅ Подключение успешно!"
                )
            )
        }

        // Шаг 6: Добавление папок
        item {
            HelpCard(
                stepNumber = 6,
                title = "Добавьте папки для синхронизации",
                icon = Icons.Default.Folder,
                steps = listOf(
                    "На главном экране нажмите 'Папки'",
                    "Нажмите на нужные папки: Camera, Pictures, Downloads и др.",
                    "Включённые папки будут сканироваться на наличие фото"
                )
            )
        }

        // Шаг 7: Загрузка
        item {
            HelpCard(
                stepNumber = 7,
                title = "Загрузите фотографии",
                icon = Icons.Default.CloudUpload,
                steps = listOf(
                    "Нажмите 'Скан' — приложение найдёт все фото в папках",
                    "Нажмите 'Загрузить всё' — начнётся фоновая загрузка",
                    "Можно свернуть приложение — загрузка продолжится",
                    "Прогресс виден в уведомлении"
                )
            )
        }

        // Советы
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lightbulb, null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "💡 Полезные советы",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Text("• Включите 'Как файл' чтобы сохранить оригинальное качество")
                    Spacer(Modifier.height(4.dp))
                    Text("• Включите 'Только по Wi-Fi' для экономии мобильного трафика")
                    Spacer(Modifier.height(4.dp))
                    Text("• Фото с одинаковым содержимым не будут загружены повторно")
                    Spacer(Modifier.height(4.dp))
                    Text("• При ошибках нажмите 'Повтор' для повторной загрузки")
                }
            }
        }

        // Проблемы
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "⚠️ Возможные проблемы",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "Ошибка 'Bad Request: chat not found'",
                        fontWeight = FontWeight.Medium
                    )
                    Text("→ Убедитесь что бот добавлен как администратор канала")

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "Ошибка 'Unauthorized'",
                        fontWeight = FontWeight.Medium
                    )
                    Text("→ Проверьте правильность токена бота")

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "Фото не находятся",
                        fontWeight = FontWeight.Medium
                    )
                    Text("→ Дайте приложению разрешение на доступ к фото в настройках Android")
                }
            }
        }

        item {
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun HelpCard(
    stepNumber: Int,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    steps: List<String>
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "$stepNumber",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)

                Spacer(Modifier.width(8.dp))

                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            steps.forEachIndexed { index, step ->
                if (step.isNotEmpty()) {
                    Text(
                        text = if (step.startsWith("•") || step.startsWith("Способ")) step else "• $step",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = if (step.startsWith("•")) 8.dp else 0.dp)
                    )
                    if (index < steps.size - 1) {
                        Spacer(Modifier.height(4.dp))
                    }
                } else {
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}