package com.kotlinfoundation.koko.presentation.screens.onboarding

import com.kotlinfoundation.koko.designsystem.generated.resources.UiRes
import com.kotlinfoundation.koko.designsystem.generated.resources.ic_logo
import com.kotlinfoundation.koko.generated.resources.Res
import com.kotlinfoundation.koko.generated.resources.desc_onboarding_page_1
import com.kotlinfoundation.koko.generated.resources.title_onboarding_page_1
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

data class OnBoardingScreenData(
    val title: StringResource,
    val description: StringResource,
    val imageRes: DrawableResource,
)

enum class AssessmentStep(val index: Int, val title: String) {
    HOOK_GOAL(1, "Primary Goal"),
    PAIN_POINT(2, "Biggest Barrier"),
    MICRO_COMMITMENT(3, "Daily Commitment"),
    SOCIAL_PROOF(4, "Social Proof"),
    LABOR_ILLUSION(5, "AI Analysis"),
    LOSS_AVERSION(6, "Habit Trajectory"),
    PAYWALL_HANDOFF(7, "Plan Ready"),
    ;

    companion object {
        fun fromIndex(index: Int): AssessmentStep = entries.firstOrNull { it.index == index } ?: HOOK_GOAL
    }
}

data class GoalOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconEmoji: String,
)

data class BarrierOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconEmoji: String,
)

data class CommitmentOption(
    val minutes: Int,
    val title: String,
    val subtitle: String,
    val tag: String? = null,
)

data class TestimonialItem(
    val name: String,
    val role: String,
    val quote: String,
    val rating: Float = 5.0f,
    val highlight: String,
)

data class OnBoardingUiState(
    // 7-step deep assessment state
    val currentStep: Int = 1,
    val totalSteps: Int = 7,
    val selectedGoal: String? = null,
    val selectedBarrier: String? = null,
    val dailyMinutes: Int? = null,
    val isAnalyzing: Boolean = false,
    val analysisProgress: Float = 0f,
    val analysisStatusText: String = "Analyzing your profile...",

    // Default assessment options
    val goalOptions: List<GoalOption> = listOf(
        GoalOption(
            id = "productivity",
            title = "Maximize Daily Productivity",
            subtitle = "Focus deeply, beat procrastination & execute with speed",
            iconEmoji = "🎯",
        ),
        GoalOption(
            id = "creativity",
            title = "Build Unbreakable Creative Habits",
            subtitle = "Create consistently without creative burnout or block",
            iconEmoji = "⚡",
        ),
        GoalOption(
            id = "growth",
            title = "Accelerate Skill Mastery",
            subtitle = "Acquire high-value skills 3x faster with structured routines",
            iconEmoji = "🚀",
        ),
        GoalOption(
            id = "clarity",
            title = "Gain Mental Clarity & Calm",
            subtitle = "Eliminate mental friction and overwhelm with focused systems",
            iconEmoji = "🧠",
        ),
    ),

    val barrierOptions: List<BarrierOption> = listOf(
        BarrierOption(
            id = "consistency",
            title = "Lack of Consistency",
            subtitle = "Starting strong but losing motivation after a few days",
            iconEmoji = "🔄",
        ),
        BarrierOption(
            id = "distractions",
            title = "Distractions & Procrastination",
            subtitle = "Social media, endless scrolling and fragmented attention",
            iconEmoji = "📱",
        ),
        BarrierOption(
            id = "no_structure",
            title = "No Structured System",
            subtitle = "Trying to do too much without a clear daily execution roadmap",
            iconEmoji = "🧭",
        ),
        BarrierOption(
            id = "limited_time",
            title = "Limited Daily Time & Energy",
            subtitle = "Busy work schedule leaving zero bandwidth for self-improvement",
            iconEmoji = "⏳",
        ),
    ),

    val commitmentOptions: List<CommitmentOption> = listOf(
        CommitmentOption(
            minutes = 5,
            title = "5 Minutes / Day",
            subtitle = "Easy start — low friction habit builder",
            tag = "Light",
        ),
        CommitmentOption(
            minutes = 10,
            title = "10 Minutes / Day",
            subtitle = "Sweet spot — highest long-term retention rate",
            tag = "Recommended",
        ),
        CommitmentOption(
            minutes = 15,
            title = "15 Minutes / Day",
            subtitle = "Accelerated track — maximum breakthrough",
            tag = "Intensive",
        ),
    ),

    val testimonials: List<TestimonialItem> = listOf(
        TestimonialItem(
            name = "Sarah Jenkins",
            role = "Product Designer",
            quote = "This transformed my routine in just 2 weeks. The structured micro-habits eliminated my afternoon brain fog.",
            rating = 5.0f,
            highlight = "+180% focus",
        ),
        TestimonialItem(
            name = "Alex Vance",
            role = "Software Engineer",
            quote = "I used to procrastinate for hours. The 10-minute daily framework gave me the momentum I needed.",
            rating = 5.0f,
            highlight = "30-day streak",
        ),
        TestimonialItem(
            name = "Elena Rostova",
            role = "Startup Founder",
            quote = "The personalized trajectory felt built just for me. Highly recommended for any serious builder.",
            rating = 5.0f,
            highlight = "Top 1% discipline",
        ),
    ),

    // Legacy pager pages for variation 1 & 2
    val pages: List<OnBoardingScreenData> = listOf(
        OnBoardingScreenData(
            Res.string.title_onboarding_page_1,
            Res.string.desc_onboarding_page_1,
            UiRes.drawable.ic_logo,
        ),
        OnBoardingScreenData(
            Res.string.title_onboarding_page_1,
            Res.string.desc_onboarding_page_1,
            UiRes.drawable.ic_logo,
        ),
        OnBoardingScreenData(
            Res.string.title_onboarding_page_1,
            Res.string.desc_onboarding_page_1,
            UiRes.drawable.ic_logo,
        ),
    ),
    val isOnBoardingFinished: Boolean = false,
    // isNewUser distinguishes a fresh completion from an already-onboarded user
    val isNewUser: Boolean = false,
    val isLoading: Boolean = true,
)

sealed interface OnBoardingUiEvent {
    data class SelectGoal(val goal: String) : OnBoardingUiEvent
    data class SelectBarrier(val barrier: String) : OnBoardingUiEvent
    data class SelectCommitment(val minutes: Int) : OnBoardingUiEvent
    data object NextStep : OnBoardingUiEvent
    data object PreviousStep : OnBoardingUiEvent
    data object FinishOnBoarding : OnBoardingUiEvent
    data object OnClickStart : OnBoardingUiEvent
}
