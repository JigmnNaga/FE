package com.jigmnnaga.core.navigation

/**
 * 로그인이 필요해 로그인 화면으로 보낸 화면이, 로그인 성공 후 이어서 할 일을 맡겨 두는 곳이다.
 * 로그인 화면은 성공하면 [consume]으로 꺼내 쓰고, 맡겨 둔 일이 없으면 평소처럼 홈으로 간다.
 * 앱 프로세스가 종료되면 사라지며, 그 경우 로그인 후 홈으로 가는 기본 흐름을 따른다.
 */
object LoginReturnRegistry {

    private var pending: (() -> Unit)? = null

    /** 로그인 성공 후 이어서 할 일을 맡긴다. 이전에 맡긴 일이 있으면 덮어쓴다. */
    fun request(onReturn: () -> Unit) {
        pending = onReturn
    }

    /** 맡겨 둔 일을 한 번만 꺼낸다. 없으면 null이다. */
    fun consume(): (() -> Unit)? {
        val current = pending
        pending = null
        return current
    }
}
