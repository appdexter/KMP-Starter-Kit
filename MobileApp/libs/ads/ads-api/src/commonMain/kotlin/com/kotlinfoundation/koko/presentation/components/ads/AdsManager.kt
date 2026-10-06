package com.kotlinfoundation.koko.presentation.components.ads

interface AdsManager {
    fun initialize()
    val interstitialAdLoader: FullScreenAdLoader
    val rewardedAdLoader: FullScreenAdLoader
    val bannerProvider: BannerAdProvider get() = NoImplBannerAdProvider

    fun createInterstitialDisplayer(activity: Any?): FullScreenAdDisplayer = NoImplFullScreenAdDisplayer
    fun createRewardedDisplayer(activity: Any?, onRewarded: (AdsRewardItem) -> Unit): FullScreenAdDisplayer = NoImplFullScreenAdDisplayer
}
