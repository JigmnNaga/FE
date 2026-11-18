package com.jigmnnaga.presentation.mypage.location

/** 기본 위치로 등록할 수 있는 최대 개수. Figma(MYP-001-01) 기준이며 서버 한도보다 작다. */
const val MAX_DEFAULT_LOCATIONS = 3

/** 위치 이름으로 입력할 수 있는 최대 글자 수. */
const val MAX_DEFAULT_LOCATION_NAME_LENGTH = 20

/** 목록에 보이는 기본 위치 한 곳. */
data class DefaultLocationItem(
    val id: Long,
    val name: String,
    val address: String?,
    val latitude: Double,
    val longitude: Double
)

/** 위치 추가·수정 화면에서 고를 수 있는 장소. 검색 결과이거나, 수정할 때 처음 선택돼 있는 기존 위치다. */
data class DefaultLocationPlace(
    val id: String,
    val title: String,
    val detail: String?,
    val address: String?,
    val latitude: Double,
    val longitude: Double
)

enum class DefaultLocationLoadStatus { LOADING, LOADED, FAILED }

enum class DefaultLocationActionStatus { IDLE, LOADING, FAILED }

enum class DefaultLocationSearchStatus { IDLE, LOADING, EMPTY, FAILED }

/**
 * 위치 추가·수정 화면 상태. [editingId]가 null이면 새로 추가하는 화면이다.
 * 이름을 쓰고 위치를 하나 골라야 저장할 수 있다.
 */
data class DefaultLocationEditorUiState(
    val editingId: Long? = null,
    val name: String = "",
    val query: String = "",
    val searchStatus: DefaultLocationSearchStatus = DefaultLocationSearchStatus.IDLE,
    val searchResults: List<DefaultLocationPlace> = emptyList(),
    val selected: DefaultLocationPlace? = null,
    val saveStatus: DefaultLocationActionStatus = DefaultLocationActionStatus.IDLE
) {
    val isEditing: Boolean get() = editingId != null

    val canSave: Boolean
        get() = name.isNotBlank() && selected != null && saveStatus != DefaultLocationActionStatus.LOADING

    fun isSelected(place: DefaultLocationPlace): Boolean = selected?.id == place.id
}

/** 기본 위치 설정 화면(MYP-001-01) 상태. */
data class DefaultLocationUiState(
    val loadStatus: DefaultLocationLoadStatus = DefaultLocationLoadStatus.LOADING,
    val locations: List<DefaultLocationItem> = emptyList(),
    val editor: DefaultLocationEditorUiState? = null,
    val deleteTarget: DefaultLocationItem? = null,
    val deleteStatus: DefaultLocationActionStatus = DefaultLocationActionStatus.IDLE,
    val loginRequired: Boolean = false
) {
    val count: Int get() = locations.size

    /** 3개를 다 채우면 더 추가할 수 없다. 목록을 불러오기 전에는 개수를 몰라서 막는다. */
    val canAdd: Boolean
        get() = loadStatus == DefaultLocationLoadStatus.LOADED && count < MAX_DEFAULT_LOCATIONS
}
