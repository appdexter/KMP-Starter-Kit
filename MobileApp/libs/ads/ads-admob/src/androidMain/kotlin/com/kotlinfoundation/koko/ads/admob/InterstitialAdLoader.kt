package com.kotlinfoundation.koko.ads.admob

import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.kotlinfoundation.koko.presentation.components.ads.AdPaidEventListener
import com.kotlinfoundation.koko.presentation.components.ads.FullScreenAdLoader

class InterstitialAdLoader(
    private val context: Context,
    private val adPaidEventListener: AdPaidEventListener? = null,
    private val adUnitIdProvider: () -> String = { "ca-app-pub-3940256099942544/1033173712" },
) : FullScreenAdLoader {
    var interstitialAd: InterstitialAd? = null

    override fun load() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            adUnitIdProvider(),
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(p0: InterstitialAd) {
                    super.onAdLoaded(p0)
                    p0.setOnPaidEventListener { adValue ->
                        val revenue = adValue.valueMicros / 1_000_000.0
                        adPaidEventListener?.onAdPaid(
                            revenue = revenue,
                            currency = adValue.currencyCode,
                            adPlatform = "admob",
                            adFormat = "interstitial",
                        )
                    }
                    interstitialAd = p0
                }

                override fun onAdFailedToLoad(p0: LoadAdError) {
                    super.onAdFailedToLoad(p0)
                    interstitialAd = null
                }
            },
        )
    }
}
