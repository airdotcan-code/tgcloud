package com.example.tgcloud.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AppPreferences(context: Context) {

    private val dataStore = context.dataStore

    private object Keys {
        val BOT_TOKEN = stringPreferencesKey("bot_token")
        val CHANNEL_ID = stringPreferencesKey("channel_id")
        val SEND_AS_FILE = booleanPreferencesKey("send_as_file")
        val AUTO_SYNC_ENABLED = booleanPreferencesKey("auto_sync_enabled")
        val SYNC_INTERVAL_HOURS = intPreferencesKey("sync_interval_hours")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val CHARGING_ONLY = booleanPreferencesKey("charging_only")
        val BATCH_SIZE = intPreferencesKey("batch_size")
        val DELETE_AFTER_UPLOAD = booleanPreferencesKey("delete_after_upload")
        val NOTIFY_ON_COMPLETE = booleanPreferencesKey("notify_on_complete")
        val LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
        val IS_CONFIGURED = booleanPreferencesKey("is_configured")
        val MOVE_TO_TRASH = booleanPreferencesKey("move_to_trash")
    }

    data class Settings(
        val botToken: String = "",
        val channelId: String = "",
        val sendAsFile: Boolean = true,
        val autoSyncEnabled: Boolean = true,
        val syncIntervalHours: Int = 6,
        val wifiOnly: Boolean = true,
        val chargingOnly: Boolean = false,
        val batchSize: Int = 10,
        val deleteAfterUpload: Boolean = false,
        val notifyOnComplete: Boolean = true,
        val lastSyncTime: Long = 0,
        val isConfigured: Boolean = false,
        val moveToTrash: Boolean = true
    )

    val settings: Flow<Settings> = dataStore.data.map { prefs ->
        Settings(
            botToken = prefs[Keys.BOT_TOKEN] ?: "",
            channelId = prefs[Keys.CHANNEL_ID] ?: "",
            sendAsFile = prefs[Keys.SEND_AS_FILE] ?: true,
            autoSyncEnabled = prefs[Keys.AUTO_SYNC_ENABLED] ?: true,
            syncIntervalHours = prefs[Keys.SYNC_INTERVAL_HOURS] ?: 6,
            wifiOnly = prefs[Keys.WIFI_ONLY] ?: true,
            chargingOnly = prefs[Keys.CHARGING_ONLY] ?: false,
            batchSize = prefs[Keys.BATCH_SIZE] ?: 10,
            deleteAfterUpload = prefs[Keys.DELETE_AFTER_UPLOAD] ?: false,
            notifyOnComplete = prefs[Keys.NOTIFY_ON_COMPLETE] ?: true,
            lastSyncTime = prefs[Keys.LAST_SYNC_TIME] ?: 0,
            isConfigured = prefs[Keys.IS_CONFIGURED] ?: false,
            moveToTrash = prefs[Keys.MOVE_TO_TRASH] ?: true
        )
    }

    suspend fun setBotToken(token: String) {
        dataStore.edit { it[Keys.BOT_TOKEN] = token }
        updateConfiguredStatus()
    }

    suspend fun setChannelId(channelId: String) {
        dataStore.edit { it[Keys.CHANNEL_ID] = channelId }
        updateConfiguredStatus()
    }

    suspend fun setSendAsFile(asFile: Boolean) {
        dataStore.edit { it[Keys.SEND_AS_FILE] = asFile }
    }

    suspend fun setAutoSyncEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.AUTO_SYNC_ENABLED] = enabled }
    }

    suspend fun setSyncInterval(hours: Int) {
        dataStore.edit { it[Keys.SYNC_INTERVAL_HOURS] = hours }
    }

    suspend fun setWifiOnly(wifiOnly: Boolean) {
        dataStore.edit { it[Keys.WIFI_ONLY] = wifiOnly }
    }

    suspend fun setChargingOnly(chargingOnly: Boolean) {
        dataStore.edit { it[Keys.CHARGING_ONLY] = chargingOnly }
    }

    suspend fun setBatchSize(size: Int) {
        dataStore.edit { it[Keys.BATCH_SIZE] = size }
    }

    suspend fun setDeleteAfterUpload(delete: Boolean) {
        dataStore.edit { it[Keys.DELETE_AFTER_UPLOAD] = delete }
    }

    suspend fun setNotifyOnComplete(notify: Boolean) {
        dataStore.edit { it[Keys.NOTIFY_ON_COMPLETE] = notify }
    }

    suspend fun updateLastSyncTime() {
        dataStore.edit { it[Keys.LAST_SYNC_TIME] = System.currentTimeMillis() }
    }

    suspend fun setMoveToTrash(move: Boolean) {
        dataStore.edit { it[Keys.MOVE_TO_TRASH] = move }
    }

    private suspend fun updateConfiguredStatus() {
        dataStore.edit { prefs ->
            val token = prefs[Keys.BOT_TOKEN] ?: ""
            val channel = prefs[Keys.CHANNEL_ID] ?: ""
            prefs[Keys.IS_CONFIGURED] = token.isNotBlank() && channel.isNotBlank()
        }
    }
}