package com.jigmnnaga.domain.repository

import com.jigmnnaga.domain.model.LocationShareEnd
import com.jigmnnaga.domain.model.LocationShareError
import com.jigmnnaga.domain.model.MemberArrival
import com.jigmnnaga.domain.model.MemberEtaUpdate
import com.jigmnnaga.domain.model.MemberLocationUpdate
import kotlinx.coroutines.flow.Flow

/**
 * 약속 참여자들의 실시간 위치 공유(STOMP). [connect]와 [disconnect]는 짝으로 부르고,
 * 마지막 사용자가 끊을 때 실제로 연결을 닫는다. 모두 모으는 동안만 구독하고, 연결 전에 모아도 연결되면 받기 시작한다.
 */
interface LocationRealtime {
    val isConnected: Flow<Boolean>

    fun connect()
    fun disconnect()

    /** 참여자(나 포함)의 위치 갱신. */
    fun locations(appointmentId: Long): Flow<MemberLocationUpdate>

    /** 참여자의 도착 예정 갱신. */
    fun etas(appointmentId: Long): Flow<MemberEtaUpdate>

    fun arrivals(appointmentId: Long): Flow<MemberArrival>

    fun sharingEnds(appointmentId: Long): Flow<LocationShareEnd>

    /** 내가 보낸 위치를 서버가 거절했을 때. 채팅 오류와 같은 채널이라 위치 관련 코드만 걸러서 준다. */
    fun errors(): Flow<LocationShareError>

    /** 연결되어 있지 않거나 보내지 못하면 false. */
    fun sendLocation(appointmentId: Long, latitude: Double, longitude: Double): Boolean
}
