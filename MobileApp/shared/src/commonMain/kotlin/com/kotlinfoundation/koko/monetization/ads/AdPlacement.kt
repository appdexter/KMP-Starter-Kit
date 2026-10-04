package com.kotlinfoundation.koko.monetization.ads

enum class AdPlacement(val placementKey: String, val isRewarded: Boolean = false) {
    APP_OPEN("app_open"),
    BANNER_MAIN("banner_main"),
    INTERSTITIAL_EXPORT("interstitial_export"),
    REWARDED_UNLOCK("rewarded_unlock", isRewarded = true),
}
