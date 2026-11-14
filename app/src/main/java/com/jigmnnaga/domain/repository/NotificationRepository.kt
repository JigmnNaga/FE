package com.jigmnnaga.domain.repository

import com.jigmnnaga.domain.model.NotificationPage
import com.jigmnnaga.domain.model.NotificationResult

interface NotificationRepository {
    /** [cursor]보다 이전 알림을 최신순으로 [size]개 가져온다. 첫 조회는 [cursor]를 null로 보낸다. */
    suspend fun getNotifications(cursor: Long?, size: Int): NotificationResult<NotificationPage>

    suspend fun readNotification(notificationId: Long): NotificationResult<Unit>

    /** 안 읽은 알림을 모두 읽음 처리하고, 읽음 처리한 개수를 돌려준다. */
    suspend fun readAllNotifications(): NotificationResult<Long>
}
