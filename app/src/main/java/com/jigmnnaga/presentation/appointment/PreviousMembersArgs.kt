package com.jigmnnaga.presentation.appointment

import com.jigmnnaga.domain.model.PreviousMembers

/** 멤버 ID 목록을 이동 인자로 넘기기 위해 쉼표로 잇는다. 예: `[2, 3]` → `"2,3"` */
internal fun List<Long>.toMemberIdsArgument(): String = joinToString(",")

/** [toMemberIdsArgument]의 반대. 숫자가 아닌 값과 중복은 버린다. */
internal fun String.toMemberIds(): List<Long> =
    split(",").mapNotNull { it.trim().toLongOrNull() }.distinct()

/** 이전 약속 멤버가 하나도 없으면 null이다. 이때는 일반 약속 생성으로 본다. */
internal fun AppointmentCreateFromPreviousDestination.toPreviousMembers(): PreviousMembers? {
    val ids = memberIds.toMemberIds()
    return if (ids.isEmpty()) null else PreviousMembers(previousAppointmentId, ids)
}
