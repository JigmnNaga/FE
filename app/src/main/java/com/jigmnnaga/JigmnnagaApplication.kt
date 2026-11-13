package com.jigmnnaga

import android.app.Application
import com.kakao.sdk.common.KakaoSdk
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class JigmnnagaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val appKey = BuildConfig.KAKAO_NATIVE_APP_KEY
        if (appKey.isNotBlank()) {
            KakaoSdk.init(this, appKey)
        }
    }
}