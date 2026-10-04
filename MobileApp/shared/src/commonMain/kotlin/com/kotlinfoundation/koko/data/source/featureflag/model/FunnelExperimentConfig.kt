package com.kotlinfoundation.koko.data.source.featureflag.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class CloseButtonMode {
    IMMEDIATE,
    DELAYED,
    HIDDEN,
}

@Serializable
enum class PaywallVariantType {
    STANDARD,
    PERSONALIZED_PITCH,
    TRIAL_TIMELINE,
}

@Serializable
data class PrimaryPaywallConfig(
    @SerialName("variant") val variant: PaywallVariantType = PaywallVariantType.STANDARD,
    @SerialName("offering_id") val offeringId: String? = null,
    @SerialName("close_button_mode") val closeButtonMode: CloseButtonMode = CloseButtonMode.DELAYED,
    @SerialName("close_delay_ms") val closeDelayMs: Long = 4000L,
    @SerialName("show_trial_timeline") val showTrialTimeline: Boolean = true,
)

@Serializable
data class ExitIntentDownsellConfig(
    @SerialName("enabled") val enabled: Boolean = true,
    @SerialName("discount_percent") val discountPercent: Int = 40,
    @SerialName("offering_id") val offeringId: String? = null,
    @SerialName("countdown_seconds") val countdownSeconds: Int = 600,
)

@Serializable
data class MicroCreditDownsellConfig(
    @SerialName("enabled") val enabled: Boolean = true,
    @SerialName("credit_pack_id") val creditPackId: String? = "starter_pack_50",
    @SerialName("price_text") val priceText: String = "$4.99",
    @SerialName("credits_amount") val creditsAmount: Int = 50,
)

@Serializable
data class DownsellLadderConfig(
    @SerialName("tier2_exit_intent") val tier2ExitIntent: ExitIntentDownsellConfig = ExitIntentDownsellConfig(),
    @SerialName("tier3_micro_credit") val tier3MicroCredit: MicroCreditDownsellConfig = MicroCreditDownsellConfig(),
)

@Serializable
data class FunnelExperimentConfig(
    @SerialName("funnel_id") val funnelId: String = "deep_assessment_v3",
    @SerialName("show_onboarding_paywall") val showOnboardingPaywall: Boolean = true,
    @SerialName("onboarding_style") val onboardingStyle: String = "DEEP_ASSESSMENT",
    @SerialName("primary_paywall") val primaryPaywall: PrimaryPaywallConfig = PrimaryPaywallConfig(),
    @SerialName("downsell_ladder") val downsellLadder: DownsellLadderConfig = DownsellLadderConfig(),
)
