package com.kotlinfoundation.koko.monetization.ads

data class AdRule(
    val isEnabled: Boolean = true,
    val minActionThreshold: Int = 3,
    val cooldownSeconds: Long = 60L,
    val excludePremium: Boolean = true,
)
