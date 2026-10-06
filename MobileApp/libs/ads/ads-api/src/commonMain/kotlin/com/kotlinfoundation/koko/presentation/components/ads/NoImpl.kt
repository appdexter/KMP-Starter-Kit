package com.kotlinfoundation.koko.presentation.components.ads

object NoImplFullScreenAdLoader : FullScreenAdLoader {
    override fun load() {
        // No-op implementation for disabled/unsupported ads
    }
}

object NoImplFullScreenAdDisplayer : FullScreenAdDisplayer {
    override fun show() {
        // No-op implementation for disabled/unsupported ads
    }
}

object NoImplAdsManager : AdsManager {
    override fun initialize() {
        // No-op implementation for disabled/unsupported ads
    }

    override val interstitialAdLoader: FullScreenAdLoader
        get() = NoImplFullScreenAdLoader
    override val rewardedAdLoader: FullScreenAdLoader
        get() = NoImplFullScreenAdLoader
}
