package com.jigmnnaga.domain.model

data class AppointmentInvite(
    val appointmentId: Long,
    val title: String,
    val canceled: Boolean,
    val hostNickname: String?,
    val responseDeadline: String?,
    val expired: Boolean,
    val respondedCount: Int,
    val joinedCount: Int,
    val myStatus: InviteParticipationStatus,
    val myRole: InviteParticipantRole?,
    val participants: List<InviteParticipant>
)

data class InviteParticipant(
    val memberId: Long,
    val nickname: String,
    val profileImageUrl: String?,
    val role: InviteParticipantRole,
    val status: InviteParticipationStatus
)

enum class InviteParticipantRole {
    HOST,
    MEMBER,
    OTHER
}

enum class InviteParticipationStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    KICKED,
    OTHER
}

enum class InviteUnavailableReason {
    NOT_FOUND,
    CANCELED,
    KICKED,
    HOST_CANNOT_RESPOND,
    OTHER
}

sealed interface InviteOperationResult<out T> {
    data class Success<T>(val value: T) : InviteOperationResult<T>
    data object LoginRequired : InviteOperationResult<Nothing>
    data object Expired : InviteOperationResult<Nothing>
    data object AlreadyResponded : InviteOperationResult<Nothing>
    data class Unavailable(val reason: InviteUnavailableReason) : InviteOperationResult<Nothing>
    data class Failure(val retryable: Boolean) : InviteOperationResult<Nothing>
}
