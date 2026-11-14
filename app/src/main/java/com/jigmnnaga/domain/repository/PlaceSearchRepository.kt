package com.jigmnnaga.domain.repository

import com.jigmnnaga.domain.model.PlaceSearchPage
import com.jigmnnaga.domain.model.PlaceSearchResult
import com.jigmnnaga.domain.model.PlaceSearchType

interface PlaceSearchRepository {
    /**
     * 키워드로 장소를 검색한다. 검색어가 비어 있으면 서버를 부르지 않고 빈 결과를 돌려준다.
     *
     * @param page 1부터 시작하는 페이지 번호(최대 45)
     * @param size 한 페이지 크기(1~15)
     */
    suspend fun search(
        query: String,
        type: PlaceSearchType,
        page: Int = 1,
        size: Int = 15
    ): PlaceSearchResult<PlaceSearchPage>
}
