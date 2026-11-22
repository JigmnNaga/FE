package com.jigmnnaga.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jigmnnaga.domain.model.LocationSharingScope
import com.jigmnnaga.domain.model.LocationSharingSettings
import com.jigmnnaga.domain.model.LocationSharingStartTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

internal val Context.locationSharingDataStore by preferencesDataStore(
    name = "location_sharing_settings"
)

/** 위치 공유 설정을 기기 DataStore에 저장한다. 서버에서 마지막으로 받은 값이나 사용자가 바꾼 값이 들어 있다. */
class LocationSharingLocalStore(
    private val dataStore: DataStore<Preferences>
) {
    val settings: Flow<LocationSharingSettings> = dataStore.data
        .catch { error ->
            // 파일을 읽지 못하면 기본값으로 보여 주고, 그 밖의 오류는 그대로 올린다.
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { it.toSettings() }

    suspend fun save(settings: LocationSharingSettings) {
        dataStore.edit {
            it[SHARING_ENABLED] = settings.sharingEnabled
            it[SCOPE] = settings.scope.name
            it[START_TIME] = settings.startTime.name
        }
    }

    private fun Preferences.toSettings(): LocationSharingSettings {
        val default = LocationSharingSettings()
        return LocationSharingSettings(
            sharingEnabled = this[SHARING_ENABLED] ?: default.sharingEnabled,
            scope = enumValues<LocationSharingScope>().firstOrNull { it.name == this[SCOPE] } ?: default.scope,
            startTime = enumValues<LocationSharingStartTime>().firstOrNull { it.name == this[START_TIME] }
                ?: default.startTime
        )
    }

    private companion object {
        val SHARING_ENABLED = booleanPreferencesKey("sharing_enabled")
        val SCOPE = stringPreferencesKey("scope")
        val START_TIME = stringPreferencesKey("start_time")
    }
}
