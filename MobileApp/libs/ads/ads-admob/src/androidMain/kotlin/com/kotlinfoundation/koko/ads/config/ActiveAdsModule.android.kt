package com.kotlinfoundation.koko.ads.config

import com.kotlinfoundation.koko.ads.admob.AdsManagerImpl
import com.kotlinfoundation.koko.presentation.components.ads.AdsManager
import org.koin.core.module.Module
import org.koin.dsl.module

actual val activeAdsModule: Module = module {
    single<AdsManager> {
        AdsManagerImpl(
            context = get(),
            adPaidEventListener = getOrNull(),
        )
    }
}
