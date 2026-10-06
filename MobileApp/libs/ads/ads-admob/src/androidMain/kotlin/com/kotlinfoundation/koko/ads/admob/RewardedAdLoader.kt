package com.kotlinfoundation.koko.ads.admob

import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.kotlinfoundation.koko.presentation.components.ads.AdPaidEventListener
import com.kotlinfoundation.koko.presentation.components.ads.FullScreenAdLoader

class RewardedAdLoader(
    private val context: Context,
    private val adPaidEventListener: AdPaidEventListener? = null,
    private val adUnitIdProvider: () -> String = { "ca-app-pub-3940256099942544/5224354917" },
) : FullScreenAdLoader {
    var rewardedAd: RewardedAd? = null

    override fun load() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            adUnitIdProvider(),
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(p0: RewardedAd) {
                    super.onAdLoaded(p0)
                    p0.setOnPaidEventListener { adValue ->
                        val revenue = adValue.valueMicros / 1_000_000.0
                        adPaidEventListener?.onAdPaid(
                            revenue = revenue,
                            currency = adValue.currencyCode,
                            adPlatform = "admob",
                            adFormat = "rewarded",
                        )
                    }
                    rewardedAd = p0
                }

                override fun onAdFailedToLoad(p0: LoadAdError) {
                    super.onAdFailedToLoad(p0)
                    rewardedAd = null
                }
            },
        )
    }
}
