package com.kotlinfoundation.koko.presentation.components.ads

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import org.koin.compose.koinInject

@Composable
actual fun NativeAdmobBanner(modifier: Modifier) {
    val adsManager = koinInject<AdsManager>()
    adsManager.bannerProvider.Banner(modifier)
}

@Composable
actual fun rememberNativeInterstitialAdDisplayer(): FullScreenAdDisplayer {
    val adsManager = koinInject<AdsManager>()
    val interstitialAdLoader = adsManager.interstitialAdLoader
    LaunchedEffect(Unit) { interstitialAdLoader.load() }
    val activity = LocalContext.current.getActivity()
    return remember(activity, adsManager) {
        adsManager.createInterstitialDisplayer(activity)
    }
}

@Composable
actual fun rememberNativeRewardedAdDisplayer(onRewarded: (AdsRewardItem) -> Unit): FullScreenAdDisplayer {
    val adsManager = koinInject<AdsManager>()
    val rewardedAdLoader = adsManager.rewardedAdLoader
    LaunchedEffect(Unit) { rewardedAdLoader.load() }
    val activity = LocalContext.current.getActivity()
    val updatedOnRewarded by rememberUpdatedState(onRewarded)
    return remember(activity, adsManager, updatedOnRewarded) {
        adsManager.createRewardedDisplayer(activity, updatedOnRewarded)
    }
}

private fun Context.getActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}
