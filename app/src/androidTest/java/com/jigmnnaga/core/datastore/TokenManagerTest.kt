
package com.jigmnnaga.core.datastore

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TokenManagerTest {

    private val context =
        InstrumentationRegistry.getInstrumentation().targetContext

    private val tokenManager = TokenManagerImpl(
        context = context,
        tokenEncryption = TokenEncryption()
    )

    private val accessTokenKey =
        stringPreferencesKey("access_token")

    private val refreshTokenKey =
        stringPreferencesKey("refresh_token")

    @Test
    fun saveAndReadTokens() = runBlocking {
        tokenManager.clearTokens()

        try {
            tokenManager.saveTokens(
                accessToken = "test_access_token",
                refreshToken = "test_refresh_token"
            )

            assertEquals(
                "test_access_token",
                tokenManager.accessToken.first()
            )

            assertEquals(
                "test_refresh_token",
                tokenManager.refreshToken.first()
            )
        } finally {
            tokenManager.clearTokens()
        }
    }

    @Test
    fun clearTokensRemovesStoredTokens() = runBlocking {
        tokenManager.clearTokens()

        tokenManager.saveTokens(
            accessToken = "test_access_token",
            refreshToken = "test_refresh_token"
        )

        tokenManager.clearTokens()

        assertNull(tokenManager.accessToken.first())
        assertNull(tokenManager.refreshToken.first())
    }

    @Test
    fun missingTokensAreExpired() = runBlocking {
        tokenManager.clearTokens()

        assertTrue(tokenManager.isAccessTokenExpired())
        assertTrue(tokenManager.isRefreshTokenExpired())
    }

    @Test
    fun validTokensArePreserved() = runBlocking {
        tokenManager.clearTokens()

        try {
            tokenManager.saveTokens(
                accessToken = "test_access_token",
                refreshToken = "test_refresh_token"
            )

            assertTrue(
                tokenManager.validateAndClearInvalidTokens()
            )

            assertEquals(
                "test_access_token",
                tokenManager.accessToken.first()
            )

            assertEquals(
                "test_refresh_token",
                tokenManager.refreshToken.first()
            )
        } finally {
            tokenManager.clearTokens()
        }
    }

    @Test
    fun missingTokensReturnFalse() = runBlocking {
        tokenManager.clearTokens()

        assertFalse(
            tokenManager.validateAndClearInvalidTokens()
        )

        assertNull(tokenManager.accessToken.first())
        assertNull(tokenManager.refreshToken.first())
    }

    @Test
    fun corruptedTokensAreCleared() = runBlocking {
        tokenManager.clearTokens()

        try {
            context.tokenDataStore.edit { preferences ->
                preferences[accessTokenKey] = "invalid_encrypted_access"
                preferences[refreshTokenKey] = "invalid_encrypted_refresh"
            }

            assertFalse(
                tokenManager.validateAndClearInvalidTokens()
            )

            val preferences = context.tokenDataStore.data.first()

            assertNull(preferences[accessTokenKey])
            assertNull(preferences[refreshTokenKey])

            assertNull(tokenManager.accessToken.first())
            assertNull(tokenManager.refreshToken.first())
        } finally {
            tokenManager.clearTokens()
        }
    }

    @Test
    fun saveTokensForRestartTest() = runBlocking {
        tokenManager.saveTokens(
            accessToken = "restart_test_access",
            refreshToken = "restart_test_refresh"
        )

        assertEquals(
            "restart_test_access",
            tokenManager.accessToken.first()
        )

        assertEquals(
            "restart_test_refresh",
            tokenManager.refreshToken.first()
        )
    }


    @Test
    fun verifyTokensAfterRestartTest() = runBlocking {
        assertEquals(
            "restart_test_access",
            tokenManager.accessToken.first()
        )

        assertEquals(
            "restart_test_refresh",
            tokenManager.refreshToken.first()
        )

        assertTrue(
            tokenManager.validateAndClearInvalidTokens()
        )

        tokenManager.clearTokens()
    }

}
