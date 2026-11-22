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
class LocationSharingLocalStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun TestScope.openStore(file: File, job: Job): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + job)
        ) { file }

    @Test
    fun `저장된 값이 없으면 기본값을 돌려 준다`() = runTest {
        val job = Job()
        val store = LocationSharingLocalStore(openStore(File(temporaryFolder.root, "a.preferences_pb"), job))

        assertEquals(LocationSharingSettings(), store.settings.first())
        job.cancelAndJoin()
    }

    @Test
    fun `저장한 설정을 그대로 돌려 준다`() = runTest {
        val job = Job()
        val store = LocationSharingLocalStore(openStore(File(temporaryFolder.root, "b.preferences_pb"), job))
        val settings = LocationSharingSettings(
            sharingEnabled = false,
            scope = LocationSharingScope.NONE,
            startTime = LocationSharingStartTime.HOUR_1
        )

        store.save(settings)

        assertEquals(settings, store.settings.first())
        job.cancelAndJoin()
    }

    @Test
    fun `저장소를 새로 열어도 저장한 값이 남아 있다`() = runTest {
        val file = File(temporaryFolder.root, "c.preferences_pb")
        val settings = LocationSharingSettings(
            sharingEnabled = false,
            scope = LocationSharingScope.NONE,
            startTime = LocationSharingStartTime.MINUTES_10
        )
        val firstJob = Job()
        LocationSharingLocalStore(openStore(file, firstJob)).save(settings)
        firstJob.cancelAndJoin()

        val secondJob = Job()
        val reopened = LocationSharingLocalStore(openStore(file, secondJob))

        assertEquals(settings, reopened.settings.first())
        secondJob.cancelAndJoin()
    }

    @Test
    fun `알 수 없는 저장 값은 기본값으로 읽는다`() = runTest {
        val job = Job()
        val dataStore = openStore(File(temporaryFolder.root, "d.preferences_pb"), job)
        dataStore.edit {
            it[stringPreferencesKey("scope")] = "EVERYONE"
            it[stringPreferencesKey("start_time")] = "MINUTES_5"
        }

        val settings = LocationSharingLocalStore(dataStore).settings.first()

        assertEquals(LocationSharingScope.FRIENDS_ONLY, settings.scope)
        assertEquals(LocationSharingStartTime.MINUTES_30, settings.startTime)
        job.cancelAndJoin()
    }
}
