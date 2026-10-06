package com.kotlinfoundation.koko.presentation.components.ads

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface BannerAdProvider {
    @Composable
    fun Banner(modifier: Modifier)
}

object NoImplBannerAdProvider : BannerAdProvider {
    @Composable
    override fun Banner(modifier: Modifier) {
        // No-op
    }
}
