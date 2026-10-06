package com.kotlinfoundation.koko.ads.admob

import androidx.activity.ComponentActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.kotlinfoundation.koko.presentation.components.ads.FullScreenAdDisplayer
import com.kotlinfoundation.koko.presentation.components.ads.FullScreenAdLoader

class InterstitialAdDisplayer(
    private val activity: ComponentActivity?,
    private val adLoader: FullScreenAdLoader,
) : FullScreenAdDisplayer {

    override fun show() {
        if (adLoader !is InterstitialAdLoader) return
        val interstitialAd = adLoader.interstitialAd

        if (interstitialAd == null || activity == null) {
            adLoader.load()
            return
        }

        interstitialAd.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent()
                adLoader.interstitialAd = null
                adLoader.load()
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                super.onAdFailedToShowFullScreenContent(p0)
                adLoader.interstitialAd = null
            }

            override fun onAdShowedFullScreenContent() {
                super.onAdShowedFullScreenContent()
            }
        }

        interstitialAd.show(activity)
    }
}
