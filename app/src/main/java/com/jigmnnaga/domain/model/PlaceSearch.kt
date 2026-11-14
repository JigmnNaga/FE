package com.jigmnnaga.domain.model

/** 장소 검색 종류. */
enum class PlaceSearchType {
    /** 지하철역만 검색한다. 약속 후보 장소를 고를 때 쓴다. */
    STATION,

    /** 상호·장소 전체를 검색한다. 세부 위치를 고를 때 쓴다. */
    PLACE
}

/** 검색으로 찾은 장소 한 곳. */
data class SearchedPlace(
    val kakaoPlaceId: String,
    val name: String,
    /** 지하철역 검색일 때 같은 역의 노선을 묶은 문구. */
    val lineNames: String? = null,
    val roadAddress: String? = null,
    val jibunAddress: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

/** 장소 검색 한 페이지. [isEnd]가 true면 더 불러올 페이지가 없다. */
data class PlaceSearchPage(
    val places: List<SearchedPlace>,
    val isEnd: Boolean
)

/** 장소 검색 API 호출 결과. */
sealed interface PlaceSearchResult<out T> {
    data class Success<T>(val value: T) : PlaceSearchResult<T>
    data object LoginRequired : PlaceSearchResult<Nothing>
    data class Failure(val retryable: Boolean) : PlaceSearchResult<Nothing>
}
