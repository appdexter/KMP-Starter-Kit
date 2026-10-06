package com.kotlinfoundation.koko.ads.admob

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.kotlinfoundation.koko.presentation.components.ads.AdPaidEventListener
import com.kotlinfoundation.koko.presentation.components.ads.BannerAdProvider

class AndroidAdmobBannerProvider(
    private val adPaidEventListener: AdPaidEventListener? = null,
    private val adUnitIdProvider: () -> String = { "ca-app-pub-3940256099942544/9214589741" },
) : BannerAdProvider {

    @Composable
    override fun Banner(modifier: Modifier) {
        var isAdLoadFailed by remember { mutableStateOf(false) }
        if (isAdLoadFailed) return

        val height = 50.dp

        AndroidView(
            modifier = modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = height)
                .height(height),
            factory = { context ->
                AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = adUnitIdProvider()
                    loadAd(AdRequest.Builder().build())
                    setOnPaidEventListener { adValue ->
                        val revenue = adValue.valueMicros / 1_000_000.0
                        adPaidEventListener?.onAdPaid(
                            revenue = revenue,
                            currency = adValue.currencyCode,
                            adPlatform = "admob",
                            adFormat = "banner",
                        )
                    }
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            super.onAdLoaded()
                        }

                        override fun onAdFailedToLoad(p0: LoadAdError) {
                            super.onAdFailedToLoad(p0)
                            isAdLoadFailed = true
                        }
                    }
                }
            },
        )
    }
}
