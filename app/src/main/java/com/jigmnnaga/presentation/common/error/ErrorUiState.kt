package com.jigmnnaga.presentation.common.error

sealed interface ErrorType {
    /** 일시적인 서비스 오류 (COM-003-01) */
    data object Service : ErrorType

    /** 네트워크 연결 끊김 (COM-003-02) */
    data object Network : ErrorType

    /** 위치 권한 미허용 (COM-003-03) */
    data object LocationPermission : ErrorType

    /** 존재하지 않는 페이지 (COM-003-04) */
    data object NotFound : ErrorType
}
