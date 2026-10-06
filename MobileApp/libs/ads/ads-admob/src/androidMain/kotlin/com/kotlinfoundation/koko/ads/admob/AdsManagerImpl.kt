package com.kotlinfoundation.koko.ads.admob

import android.content.Context
import androidx.activity.ComponentActivity
import com.google.android.gms.ads.MobileAds
import com.kotlinfoundation.koko.presentation.components.ads.AdPaidEventListener
import com.kotlinfoundation.koko.presentation.components.ads.AdsManager
import com.kotlinfoundation.koko.presentation.components.ads.AdsRewardItem
import com.kotlinfoundation.koko.presentation.components.ads.BannerAdProvider
import com.kotlinfoundation.koko.presentation.components.ads.FullScreenAdDisplayer
import com.kotlinfoundation.koko.presentation.components.ads.FullScreenAdLoader

class AdsManagerImpl(
    private val context: Context,
    private val adPaidEventListener: AdPaidEventListener? = null,
    private val bannerAdUnitId: () -> String = { "ca-app-pub-3940256099942544/9214589741" },
    private val interstitialAdUnitId: () -> String = { "ca-app-pub-3940256099942544/1033173712" },
    private val rewardedAdUnitId: () -> String = { "ca-app-pub-3940256099942544/5224354917" },
) : AdsManager {
    override fun initialize() {
        MobileAds.initialize(context)
    }

    override val interstitialAdLoader: FullScreenAdLoader by lazy {
        InterstitialAdLoader(context, adPaidEventListener, interstitialAdUnitId)
    }

    override val rewardedAdLoader: FullScreenAdLoader by lazy {
        RewardedAdLoader(context, adPaidEventListener, rewardedAdUnitId)
    }

    override val bannerProvider: BannerAdProvider by lazy {
        AndroidAdmobBannerProvider(adPaidEventListener, bannerAdUnitId)
    }

    override fun createInterstitialDisplayer(activity: Any?): FullScreenAdDisplayer {
        return InterstitialAdDisplayer(activity as? ComponentActivity, interstitialAdLoader)
    }

    override fun createRewardedDisplayer(
        activity: Any?,
        onRewarded: (AdsRewardItem) -> Unit,
    ): FullScreenAdDisplayer {
        return RewardedAdDisplayer(activity as? ComponentActivity, rewardedAdLoader, onRewarded)
    }
}
