package com.kotlinfoundation.koko.ads.config

import com.kotlinfoundation.koko.presentation.components.ads.AdsManager
import com.kotlinfoundation.koko.presentation.components.ads.NoImplAdsManager
import org.koin.core.module.Module
import org.koin.dsl.module

val activeAdsModule: Module = module {
    single<AdsManager> { NoImplAdsManager }
}
