package com.jigmnnaga.data.repository

import com.jigmnnaga.core.datastore.TokenManager
import com.jigmnnaga.domain.repository.MyMemberIdProvider
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** 서버가 발급한 액세스 토큰의 subject가 멤버 ID라서, 토큰만 읽어 내 멤버 ID를 알아낸다. */
class TokenMyMemberIdProvider @Inject constructor(
    private val tokenManager: TokenManager
) : MyMemberIdProvider {

    override suspend fun myMemberId(): Long? = memberIdFromAccessToken(tokenManager.accessToken.first())
}

/** JWT 가운데(payload) 조각을 풀어 `sub`를 숫자로 읽는다. 읽을 수 없으면 null. */
@OptIn(ExperimentalEncodingApi::class)
internal fun memberIdFromAccessToken(token: String?): Long? {
    val payload = token?.split('.')?.getOrNull(1)?.takeIf { it.isNotBlank() } ?: return null
    return try {
        val decoded = Base64.UrlSafe.withPadding(Base64.PaddingOption.PRESENT_OPTIONAL)
            .decode(payload)
            .decodeToString()
        (Json.parseToJsonElement(decoded) as? JsonObject)
            ?.get("sub")
            ?.jsonPrimitive
            ?.content
            ?.toLongOrNull()
    } catch (e: Exception) {
        null
    }
}
