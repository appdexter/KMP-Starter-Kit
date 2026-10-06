package com.kotlinfoundation.koko.util

import com.kotlinfoundation.koko.data.source.featureflag.FeatureFlagManager
import com.kotlinfoundation.koko.growth.analytics.mmp.MmpTracker
import com.kotlinfoundation.koko.growth.analytics.mmp.NoImplMmpTracker
import com.kotlinfoundation.koko.presentation.components.ads.AdsManager
import com.kotlinfoundation.koko.presentation.components.ads.IosAdsDisplayer
import com.kotlinfoundation.koko.presentation.components.ads.NoImplAdsManager
import com.kotlinfoundation.koko.presentation.components.ads.NoImplIosAdsDisplayer
import com.kotlinfoundation.koko.util.analytics.Analytics

/**
This factory is used to help to use swift libraries in KMP. Actual implementations are provided in swift.
 */
interface SwiftLibDependencyFactory {
    fun provideFeatureFlagManagerImpl(): FeatureFlagManager
    fun provideFirebaseAnalyticsImpl(): Analytics
    fun provideAdsManagerImpl(): AdsManager = NoImplAdsManager
    fun provideIosAdsDisplayer(): IosAdsDisplayer = NoImplIosAdsDisplayer
    fun provideMmpTracker(): MmpTracker = NoImplMmpTracker
}
