
package com.jigmnnaga.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal val Context.tokenDataStore by preferencesDataStore(
    name = "auth_tokens"
)

class TokenManagerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val tokenEncryption: TokenEncryption
) : TokenManager {

    companion object {
        private val ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    }

    private val tokens: Flow<Pair<String?, String?>> =
        context.tokenDataStore.data.map { preferences ->
            val encryptedAccess = preferences[ACCESS_TOKEN]
            val encryptedRefresh = preferences[REFRESH_TOKEN]

            if (encryptedAccess == null && encryptedRefresh == null) {
                null to null
            } else {
                try {
                    val access = encryptedAccess?.let(tokenEncryption::decrypt)
                    val refresh = encryptedRefresh?.let(tokenEncryption::decrypt)

                    if (access.isNullOrBlank() || refresh.isNullOrBlank()) {
                        null to null
                    } else {
                        access to refresh
                    }
                } catch (e: Exception) {
                    null to null
                }
            }
        }

    override val accessToken: Flow<String?> =
        tokens.map { it.first }

    override val refreshToken: Flow<String?> =
        tokens.map { it.second }

    override suspend fun saveTokens(
        accessToken: String,
        refreshToken: String
    ) {
        require(accessToken.isNotBlank() && refreshToken.isNotBlank()) {
            "Tokens must not be blank"
        }

        val encryptedAccess = tokenEncryption.encrypt(accessToken)
        val encryptedRefresh = tokenEncryption.encrypt(refreshToken)

        context.tokenDataStore.edit { preferences ->
            preferences[ACCESS_TOKEN] = encryptedAccess
            preferences[REFRESH_TOKEN] = encryptedRefresh
        }
    }

    override suspend fun clearTokens() {
        context.tokenDataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN)
            preferences.remove(REFRESH_TOKEN)
        }
    }

    override suspend fun isAccessTokenExpired(): Boolean {
        return JwtUtils.isExpired(accessToken.first())
    }

    override suspend fun isRefreshTokenExpired(): Boolean {
        return JwtUtils.isExpired(refreshToken.first())
    }

    override suspend fun validateAndClearInvalidTokens(): Boolean {
        val preferences = context.tokenDataStore.data.first()

        val encryptedAccess = preferences[ACCESS_TOKEN]
        val encryptedRefresh = preferences[REFRESH_TOKEN]

        if (encryptedAccess == null && encryptedRefresh == null) {
            return false
        }

        val isValid = try {
            val access = encryptedAccess?.let(tokenEncryption::decrypt)
            val refresh = encryptedRefresh?.let(tokenEncryption::decrypt)

            !access.isNullOrBlank() && !refresh.isNullOrBlank()
        } catch (e: java.security.GeneralSecurityException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        }

        if (!isValid) {
            context.tokenDataStore.edit { current ->
                if (
                    current[ACCESS_TOKEN] == encryptedAccess &&
                    current[REFRESH_TOKEN] == encryptedRefresh
                ) {
                    current.remove(ACCESS_TOKEN)
                    current.remove(REFRESH_TOKEN)
                }
            }
        }

        return isValid
    }
}