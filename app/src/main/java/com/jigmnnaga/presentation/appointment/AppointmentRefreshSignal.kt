package com.jigmnnaga.presentation.appointment

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 약속 관리 화면에서 약속 정보를 바꿨다는 신호. 이미 열려 있는 약속방 화면이
 * 약속 상세를 다시 불러오게 한다. 앱 프로세스 안에서만 쓰며 저장하지 않는다.
 */
object AppointmentRefreshSignal {
    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version.asStateFlow()

    fun notifyChanged() {
        _version.value += 1
    }
}
