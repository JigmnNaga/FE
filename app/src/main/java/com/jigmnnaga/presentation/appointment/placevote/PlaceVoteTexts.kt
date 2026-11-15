package com.jigmnnaga.presentation.appointment.placevote

/**
 * 서버 값으로 만드는 투표 현황 문구. 문구 자체는 화면 쪽에서 리소스로 만들어 넘긴다.
 * ViewModel에 넘기지 않으면 투표 수와 선택 현황 카드는 보여 주지 않는다.
 */
class PlaceVoteTexts(
    /** 장소 하나의 투표 수. 예: "3/3명" */
    val voteLabel: (voteCount: Int, totalCount: Int) -> String,
    /** 선택 현황 카드 제목. 예: "1명 선택 완료" */
    val progressTitle: (selectedCount: Int) -> String,
    /** 선택 현황 카드 보조 문구. 예: "2명 더 선택하면 돼요!" */
    val progressSubtitle: (remainingCount: Int) -> String,
    /** 아직 선택하지 않은 멤버가 있을 때의 상태 문구. */
    val progressSelecting: String,
    /** 모두 선택했을 때의 상태 문구. */
    val progressDone: String
)
