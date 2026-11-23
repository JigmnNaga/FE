package com.jigmnnaga.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.jigmnnaga.R

/**
 * 푸시 알림(시스템 알림) 채널 초기화 및 빌더 헬퍼. Figma COM-004 참조.
 *
 * FCM 메시지 수신 처리는 별도 이슈에서 구현하며, 이 클래스는 그 때 사용될
 * NotificationChannel 초기화와 Notification 빌드 로직만 담당한다.
 *
 * 채널은 앱 시작 시 한 번만 등록하면 되므로 [JigmnnagaApplication.onCreate]에서 호출한다.
 */
object PushNotificationHelper {

    /** 일반 알림 채널 ID. FCM 데이터 메시지에서 `channel_id` 로 참조한다. */
    const val CHANNEL_ID_GENERAL = "jigmnnaga_general"

    /**
     * 앱에서 사용할 알림 채널을 등록한다.
     * Android 8.0(API 26) 미만에서는 채널 개념이 없으므로 호출해도 무해하다.
     */
    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val generalChannel = NotificationChannel(
            CHANNEL_ID_GENERAL,
            context.getString(R.string.push_notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.push_notification_channel_description)
        }

        manager.createNotificationChannel(generalChannel)
    }

    /**
     * 주어진 제목과 본문으로 알림을 빌드한다.
     *
     * FCM 메시지를 수신한 Service에서 호출하며, [title]과 [body]는 서버가 보낸 값을 그대로 쓴다.
     * 알림 아이콘은 상태 바 표시를 위한 단색 소형 아이콘([R.drawable.ic_push_notification])을 사용한다.
     *
     * @param notificationId 알림을 갱신·취소할 때 사용하는 고유 ID
     */
    fun buildNotification(
        context: Context,
        title: String,
        body: String,
        notificationId: Int = 0
    ): Notification = NotificationCompat.Builder(context, CHANNEL_ID_GENERAL)
        .setSmallIcon(R.drawable.ic_push_notification)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()
}
