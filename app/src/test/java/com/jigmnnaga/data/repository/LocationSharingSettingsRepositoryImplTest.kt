package com.jigmnnaga.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jigmnnaga.domain.model.LocationSharingScope
import com.jigmnnaga.domain.model.LocationSharingSettings
import com.jigmnnaga.domain.model.LocationSharingStartTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class LocationSharingSettingsRepositoryImplTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun TestScope.openStore(file: File, job: Job): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + job)
        ) { file }

    @Test
    fun `저장된 값이 없으면 기본값을 돌려 준다`() = runTest {
        val job = Job()
        val repository = LocationSharingSettingsRepositoryImpl(openStore(File(temporaryFolder.root, "a.preferences_pb"), job))

        assertEquals(LocationSharingSettings(), repository.settings.first())
        job.cancelAndJoin()
    }

    @Test
    fun `공유 사용 범위 시작 시간을 바꾸면 저장한 값을 돌려 준다`() = runTest {
        val job = Job()
        val repository = LocationSharingSettingsRepositoryImpl(openStore(File(temporaryFolder.root, "b.preferences_pb"), job))

        repository.setSharingEnabled(false)
        repository.setScope(LocationSharingScope.NONE)
        repository.setStartTime(LocationSharingStartTime.HOUR_1)

        assertEquals(
            LocationSharingSettings(
                sharingEnabled = false,
                scope = LocationSharingScope.NONE,
                startTime = LocationSharingStartTime.HOUR_1
            ),
            repository.settings.first()
        )
        job.cancelAndJoin()
    }

    @Test
    fun `한 항목을 바꿔도 다른 항목은 그대로다`() = runTest {
        val job = Job()
        val repository = LocationSharingSettingsRepositoryImpl(openStore(File(temporaryFolder.root, "c.preferences_pb"), job))

        repository.setStartTime(LocationSharingStartTime.MINUTES_10)
        repository.setSharingEnabled(false)

        val settings = repository.settings.first()
        assertEquals(false, settings.sharingEnabled)
        assertEquals(LocationSharingStartTime.MINUTES_10, settings.startTime)
        assertEquals(LocationSharingScope.FRIENDS_ONLY, settings.scope)
        job.cancelAndJoin()
    }

    @Test
    fun `저장소를 새로 열어도 저장한 값이 남아 있다`() = runTest {
        val file = File(temporaryFolder.root, "d.preferences_pb")
        val firstJob = Job()
        LocationSharingSettingsRepositoryImpl(openStore(file, firstJob)).apply {
            setSharingEnabled(false)
            setScope(LocationSharingScope.NONE)
            setStartTime(LocationSharingStartTime.MINUTES_10)
        }
        firstJob.cancelAndJoin()

        val secondJob = Job()
        val reopened = LocationSharingSettingsRepositoryImpl(openStore(file, secondJob))

        assertEquals(
            LocationSharingSettings(
                sharingEnabled = false,
                scope = LocationSharingScope.NONE,
                startTime = LocationSharingStartTime.MINUTES_10
            ),
            reopened.settings.first()
        )
        secondJob.cancelAndJoin()
    }

    @Test
    fun `알 수 없는 저장 값은 기본값으로 읽는다`() = runTest {
        val job = Job()
        val store = openStore(File(temporaryFolder.root, "e.preferences_pb"), job)
        store.edit {
            it[stringPreferencesKey("scope")] = "EVERYONE"
            it[stringPreferencesKey("start_time")] = "MINUTES_5"
        }

        val settings = LocationSharingSettingsRepositoryImpl(store).settings.first()

        assertEquals(LocationSharingScope.FRIENDS_ONLY, settings.scope)
        assertEquals(LocationSharingStartTime.MINUTES_30, settings.startTime)
        job.cancelAndJoin()
    }
}
