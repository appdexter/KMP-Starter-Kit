package com.kotlinfoundation.koko.presentation.components.ads

interface IosAdsDisplayer {

    fun provideInterstitialAdDisplayer(adLoader: FullScreenAdLoader): FullScreenAdDisplayer
    fun provideRewardedAdDisplayer(
        adLoader: FullScreenAdLoader,
        onRewarded: (AdsRewardItem) -> Unit,
    ): FullScreenAdDisplayer
}

object NoImplIosAdsDisplayer : IosAdsDisplayer {
    override fun provideInterstitialAdDisplayer(adLoader: FullScreenAdLoader): FullScreenAdDisplayer = NoImplFullScreenAdDisplayer

    override fun provideRewardedAdDisplayer(
        adLoader: FullScreenAdLoader,
        onRewarded: (AdsRewardItem) -> Unit,
    ): FullScreenAdDisplayer = NoImplFullScreenAdDisplayer
}
