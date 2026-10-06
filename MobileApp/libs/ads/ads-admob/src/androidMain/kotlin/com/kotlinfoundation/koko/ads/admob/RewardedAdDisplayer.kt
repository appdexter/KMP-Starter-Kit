package com.kotlinfoundation.koko.ads.admob

import androidx.activity.ComponentActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.kotlinfoundation.koko.presentation.components.ads.AdsRewardItem
import com.kotlinfoundation.koko.presentation.components.ads.FullScreenAdDisplayer
import com.kotlinfoundation.koko.presentation.components.ads.FullScreenAdLoader

class RewardedAdDisplayer(
    private val activity: ComponentActivity?,
    private val adLoader: FullScreenAdLoader,
    private val onRewarded: (AdsRewardItem) -> Unit,
) : FullScreenAdDisplayer {

    override fun show() {
        if (adLoader !is RewardedAdLoader) return
        val rewardedAd = adLoader.rewardedAd

        if (rewardedAd == null || activity == null) {
            adLoader.load()
            return
        }

        rewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent()
                adLoader.rewardedAd = null
                adLoader.load()
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                super.onAdFailedToShowFullScreenContent(p0)
                adLoader.rewardedAd = null
            }

            override fun onAdShowedFullScreenContent() {
                super.onAdShowedFullScreenContent()
            }
        }
        rewardedAd.show(activity) { rewardItem ->
            val adsRewardItem = AdsRewardItem(
                amount = rewardItem.amount,
                type = rewardItem.type,
            )
            onRewarded(adsRewardItem)
        }
    }
}
