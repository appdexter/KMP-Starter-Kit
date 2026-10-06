package com.kotlinfoundation.koko.presentation.components.ads

fun interface AdPaidEventListener {
    fun onAdPaid(revenue: Double, currency: String, adPlatform: String, adFormat: String)
}
