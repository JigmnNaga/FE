package com.jigmnnaga.data.repository

import com.jigmnnaga.core.websocket.StompClient
import com.jigmnnaga.core.websocket.StompConnectionState
import com.jigmnnaga.data.mapper.locationShareErrorOf
import com.jigmnnaga.data.mapper.toDomain
import com.jigmnnaga.data.remote.dto.ApiErrorBody
import com.jigmnnaga.data.remote.dto.ArrivalBroadcastDto
import com.jigmnnaga.data.remote.dto.EtaBroadcastDto
import com.jigmnnaga.data.remote.dto.LocationBroadcastDto
import com.jigmnnaga.data.remote.dto.LocationSendRequestDto
import com.jigmnnaga.data.remote.dto.LocationSharingEndedDto
import com.jigmnnaga.domain.model.LocationShareEnd
import com.jigmnnaga.domain.model.LocationShareError
import com.jigmnnaga.domain.model.MemberArrival
import com.jigmnnaga.domain.model.MemberEtaUpdate
import com.jigmnnaga.domain.model.MemberLocationUpdate
import com.jigmnnaga.domain.repository.LocationRealtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.serialization.json.Json

/**
 * 공통 [StompClient] 위에서 약속의 위치 공유 목적지를 구독하고 내 위치를 보낸다.
 * 연결은 앱 전체가 하나라서, [connect]를 부른 만큼 [disconnect]가 불려야 실제로 끊는다.
 * 약속방 채팅([StompChatRealtime])의 연결 수와는 따로 센다.
 */
class StompLocationRealtime(
    private val stomp: StompClient,
    private val json: Json
) : LocationRealtime {

    private val lock = Any()
    private var users = 0

    override val isConnected: Flow<Boolean> =
        stomp.connectionState.map { it == StompConnectionState.CONNECTED }.distinctUntilChanged()

    override fun connect() {
        synchronized(lock) { users++ }
        stomp.connect()
    }

    override fun disconnect() {
        val last = synchronized(lock) {
            if (users == 0) return
            users--
            users == 0
        }
        if (last) stomp.disconnect()
    }

    override fun locations(appointmentId: Long): Flow<MemberLocationUpdate> =
        stomp.subscribe("/sub/appointments/$appointmentId/location").mapNotNull { message ->
            decode<LocationBroadcastDto>(message.body)?.toDomain()
        }

    override fun etas(appointmentId: Long): Flow<MemberEtaUpdate> =
        stomp.subscribe("/sub/appointments/$appointmentId/eta").mapNotNull { message ->
            decode<EtaBroadcastDto>(message.body)?.toDomain()
        }

    override fun arrivals(appointmentId: Long): Flow<MemberArrival> =
        stomp.subscribe("/sub/appointments/$appointmentId/arrival").mapNotNull { message ->
            decode<ArrivalBroadcastDto>(message.body)?.toDomain()
        }

    override fun sharingEnds(appointmentId: Long): Flow<LocationShareEnd> =
        stomp.subscribe("/sub/appointments/$appointmentId/location-end").mapNotNull { message ->
            decode<LocationSharingEndedDto>(message.body)?.toDomain()
        }

    override fun errors(): Flow<LocationShareError> =
        stomp.subscribe("/user/queue/errors").mapNotNull { message ->
            locationShareErrorOf(decode<ApiErrorBody>(message.body)?.code)
        }

    override fun sendLocation(appointmentId: Long, latitude: Double, longitude: Double): Boolean = stomp.send(
        destination = "/pub/appointments/$appointmentId/location",
        body = json.encodeToString(LocationSendRequestDto.serializer(), LocationSendRequestDto(latitude, longitude))
    )

    private inline fun <reified T> decode(body: String): T? = try {
        json.decodeFromString<T>(body)
    } catch (e: Exception) {
        null
    }
}
