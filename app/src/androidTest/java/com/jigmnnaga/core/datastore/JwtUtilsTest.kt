package com.jigmnnaga.core.datastore

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JwtUtilsTest {

    private fun createTestJwt(expirationTime: Long): String {
        val header = JSONObject()
            .put("alg", "none")
            .put("typ", "JWT")

        val payload = JSONObject()
            .put("exp", expirationTime)

        fun encode(value: String): String =
            Base64.encodeToString(
                value.toByteArray(Charsets.UTF_8),
                Base64.URL_SAFE or
                        Base64.NO_WRAP or
                        Base64.NO_PADDING
            )

        return "${encode(header.toString())}.${encode(payload.toString())}.test"
    }

    @Test
    fun validTokenIsNotExpired() {
        val expiration = System.currentTimeMillis() / 1000 + 3600
        val token = createTestJwt(expiration)

        assertFalse(JwtUtils.isExpired(token))
    }

    @Test
    fun expiredTokenIsExpired() {
        val expiration = System.currentTimeMillis() / 1000 - 3600
        val token = createTestJwt(expiration)

        assertTrue(JwtUtils.isExpired(token))
    }

    @Test
    fun tokenExpiringSoonIsExpired() {
        val expiration = System.currentTimeMillis() / 1000 + 10
        val token = createTestJwt(expiration)

        assertTrue(JwtUtils.isExpired(token))
    }

    @Test
    fun nullTokenIsExpired() {
        assertTrue(JwtUtils.isExpired(null))
    }

    @Test
    fun malformedTokenIsExpired() {
        assertTrue(JwtUtils.isExpired("invalid_token"))
    }
}