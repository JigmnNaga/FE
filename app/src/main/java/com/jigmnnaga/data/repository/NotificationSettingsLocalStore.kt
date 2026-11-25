package com.jigmnnaga.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.jigmnnaga.domain.model.NotificationSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

internal val Context.notificationSettingsDataStore by preferencesDataStore(
    name = "notification_settings"
)

/** 알림 설정을 기기 DataStore에 저장한다. */
class NotificationSettingsLocalStore(
    private val dataStore: DataStore<Preferences>
) {
    val settings: Flow<NotificationSettings> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { it.toSettings() }

    suspend fun save(settings: NotificationSettings) {
        dataStore.edit {
            it[ALL_NOTIFICATIONS] = settings.allNotificationsEnabled
            it[APPOINTMENT_REMINDER] = settings.appointmentReminderEnabled
            it[DEPARTURE_TIME] = settings.departureTimeEnabled
            it[FRIEND_ARRIVAL] = settings.friendArrivalEnabled
            it[MARKETING] = settings.marketingEnabled
        }
    }

    private fun Preferences.toSettings(): NotificationSettings {
        val default = NotificationSettings()
        return NotificationSettings(
            allNotificationsEnabled = this[ALL_NOTIFICATIONS] ?: default.allNotificationsEnabled,
            appointmentReminderEnabled = this[APPOINTMENT_REMINDER] ?: default.appointmentReminderEnabled,
            departureTimeEnabled = this[DEPARTURE_TIME] ?: default.departureTimeEnabled,
            friendArrivalEnabled = this[FRIEND_ARRIVAL] ?: default.friendArrivalEnabled,
            marketingEnabled = this[MARKETING] ?: default.marketingEnabled,
        )
    }

    private companion object {
        val ALL_NOTIFICATIONS = booleanPreferencesKey("all_notifications_enabled")
        val APPOINTMENT_REMINDER = booleanPreferencesKey("appointment_reminder_enabled")
        val DEPARTURE_TIME = booleanPreferencesKey("departure_time_enabled")
        val FRIEND_ARRIVAL = booleanPreferencesKey("friend_arrival_enabled")
        val MARKETING = booleanPreferencesKey("marketing_enabled")
    }
}
