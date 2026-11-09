package com.jigmnnaga.core.datastore

import android.util.Base64
import org.json.JSONObject

object JwtUtils {

    private const val EXPIRATION_BUFFER_SECONDS = 30L

    fun isExpired(token: String?): Boolean {
        if (token.isNullOrBlank()) return true

        return try {
            val parts = token.split(".")
            if (parts.size != 3) return true

            val payload = String(
                Base64.decode(
                    parts[1],
                    Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
                ),
                Charsets.UTF_8
            )

            val json = JSONObject(payload)

            if (!json.has("exp")) return true

            val expirationTime = json.getLong("exp")
            val currentTime = System.currentTimeMillis() / 1000

            currentTime >= expirationTime - EXPIRATION_BUFFER_SECONDS
        } catch (e: Exception) {
            true
        }
    }
}