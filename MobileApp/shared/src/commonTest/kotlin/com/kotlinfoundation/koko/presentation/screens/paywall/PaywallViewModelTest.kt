@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.kotlinfoundation.koko.presentation.screens.paywall

import com.kotlinfoundation.koko.data.BackgroundExecutor
import com.kotlinfoundation.koko.data.repository.CreditRepository
import com.kotlinfoundation.koko.data.repository.SubscriptionRepository
import com.kotlinfoundation.koko.data.repository.UserRepository
import com.kotlinfoundation.koko.data.source.featureflag.FeatureFlagManager
import com.kotlinfoundation.koko.data.source.featureflag.model.DownsellLadderConfig
import com.kotlinfoundation.koko.data.source.featureflag.model.ExitIntentDownsellConfig
import com.kotlinfoundation.koko.data.source.featureflag.model.FunnelExperimentConfig
import com.kotlinfoundation.koko.data.source.featureflag.model.MicroCreditDownsellConfig
import com.kotlinfoundation.koko.data.source.local.dao.CreditTransactionDao
import com.kotlinfoundation.koko.data.source.local.entity.CreditTransactionEntity
import com.kotlinfoundation.koko.data.source.preferences.FakeUserPreferences
import com.kotlinfoundation.koko.data.source.preferences.UserPreferences
import com.kotlinfoundation.koko.domain.model.credit.creditSystemConfig
import com.kotlinfoundation.koko.subscription.api.MockSubscriptionProvider
import com.kotlinfoundation.koko.util.ApplicationScope
import com.kotlinfoundation.koko.util.analytics.Analytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PaywallViewModelTest {

    private val testScheduler = TestCoroutineScheduler()
    private val mainDispatcher = StandardTestDispatcher(testScheduler)

    private lateinit var applicationScope: ApplicationScope
    private lateinit var userPreferences: FakeUserPreferences
    private lateinit var mockSubscriptionProvider: MockSubscriptionProvider
    private lateinit var subscriptionRepository: SubscriptionRepository
    private lateinit var creditTransactionDao: FakeCreditTransactionDao
    private lateinit var creditRepository: CreditRepository
    private lateinit var userRepository: UserRepository
    private lateinit var featureFlagManager: FakeFeatureFlagManager
    private lateinit var fakeAnalytics: FakeAnalytics

    private class FakeCreditTransactionDao : CreditTransactionDao {
        private val entities = mutableListOf<CreditTransactionEntity>()

        override suspend fun getById(id: String): CreditTransactionEntity? = entities.firstOrNull { it.id == id }

        override fun getByIdFlow(id: String): Flow<CreditTransactionEntity?> = flowOf(entities.firstOrNull { it.id == id })

        override fun getAllFlow(): Flow<List<CreditTransactionEntity>> = flowOf(entities.toList())

        override suspend fun getAll(): List<CreditTransactionEntity> = entities.toList()

        override fun getRecentsFlow(limit: Int): Flow<List<CreditTransactionEntity>> = flowOf(entities.take(limit))

        override suspend fun getRecents(limit: Int): List<CreditTransactionEntity> = entities.take(limit)

        override suspend fun upsert(entity: CreditTransactionEntity) {
            entities.removeAll { it.id == entity.id }
            entities.add(0, entity)
        }

        override suspend fun deleteById(id: String) {
            entities.removeAll { it.id == id }
        }

        override suspend fun delete(entity: CreditTransactionEntity) {
            entities.remove(entity)
        }

        override suspend fun deleteAll() {
            entities.clear()
        }
    }

    private class FakeAnalytics : Analytics {
        data class LoggedEvent(val name: String, val params: Map<String, Any>?)

        val events = mutableListOf<LoggedEvent>()

        override fun logEvent(event: String, params: Map<String, Any>?) {
            events.add(LoggedEvent(event, params))
        }

        override fun setEnabled(enabled: Boolean) {}

        fun hasEvent(name: String): Boolean = events.any { it.name == name }

        fun lastEventNamed(name: String): LoggedEvent? = events.lastOrNull { it.name == name }
    }

    private class FakeFeatureFlagManager(
        var config: FunnelExperimentConfig = FunnelExperimentConfig(),
        val booleans: MutableMap<String, Boolean> = mutableMapOf(),
    ) : FeatureFlagManager {
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        override fun syncsFlagsAsync() {}

        override fun getBoolean(key: String): Boolean = booleans[key] ?: false

        override fun getString(key: String): String = if (key == FeatureFlagManager.Keys.ONBOARDING_FUNNEL_CONFIG) {
            json.encodeToString(config)
        } else {
            ""
        }

        override fun getLong(key: String): Long = 0L

        override fun getDouble(key: String): Double = 0.0
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
        applicationScope = ApplicationScope()
        userPreferences = FakeUserPreferences()
        // Prevent UserRepository.init from running KMPAuth.signInAnonymously()
        userPreferences.putDirect(UserPreferences.Keys.KEY_FIRST_TIME_USER, false)

        fakeAnalytics = FakeAnalytics()
        featureFlagManager = FakeFeatureFlagManager()

        mockSubscriptionProvider = MockSubscriptionProvider(
            readPremiumPurchased = { userPreferences.getBoolean("mock_premium", false) },
            writePremiumPurchased = { userPreferences.putBoolean("mock_premium", it) },
        )

        subscriptionRepository = SubscriptionRepository(
            applicationScope = applicationScope,
            subscriptionProvider = mockSubscriptionProvider,
            userPreferences = userPreferences,
            analytics = fakeAnalytics,
            backgroundExecutor = BackgroundExecutor(mainDispatcher),
        )

        creditTransactionDao = FakeCreditTransactionDao()
        creditRepository = CreditRepository(
            config = creditSystemConfig { },
            creditTransactionDao = creditTransactionDao,
            subscriptionRepository = subscriptionRepository,
            userPreferences = userPreferences,
            applicationScope = applicationScope,
        )

        userRepository = UserRepository(
            subscriptionRepository = subscriptionRepository,
            userPreferences = userPreferences,
            backgroundExecutor = BackgroundExecutor(mainDispatcher),
            applicationScope = applicationScope,
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        placementId: String? = null,
        userGoal: String? = null,
        userBarrier: String? = null,
        userDailyMinutes: Int? = null,
    ): PaywallViewModel = PaywallViewModel(
        placementId = placementId,
        subscriptionRepository = subscriptionRepository,
        creditRepository = creditRepository,
        userRepository = userRepository,
        featureFlagManager = featureFlagManager,
        userPreferences = userPreferences,
        analytics = fakeAnalytics,
        userGoal = userGoal,
        userBarrier = userBarrier,
        userDailyMinutes = userDailyMinutes,
    )

    @Test
    fun testInitialization_resolvesGoalAndBarrierFromParametersWhenProvided() = runTest(mainDispatcher) {
        userPreferences.putString(UserPreferences.KEY_USER_GOAL, "pref_goal")
        userPreferences.putString(UserPreferences.KEY_USER_BARRIER, "pref_barrier")
        userPreferences.putInt(UserPreferences.KEY_DAILY_COMMITMENT, 10)

        val viewModel = createViewModel(
            userGoal = "param_goal",
            userBarrier = "param_barrier",
            userDailyMinutes = 20,
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("param_goal", state.userGoal)
        assertEquals("param_barrier", state.userBarrier)
        assertEquals(20, state.userDailyMinutes)
    }

    @Test
    fun testInitialization_fallsBackToUserPreferencesWhenParametersNull() = runTest(mainDispatcher) {
        userPreferences.putString(UserPreferences.KEY_USER_GOAL, "stored_goal")
        userPreferences.putString(UserPreferences.KEY_USER_BARRIER, "stored_barrier")
        userPreferences.putInt(UserPreferences.KEY_DAILY_COMMITMENT, 30)

        val viewModel = createViewModel(
            userGoal = null,
            userBarrier = null,
            userDailyMinutes = null,
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("stored_goal", state.userGoal)
        assertEquals("stored_barrier", state.userBarrier)
        assertEquals(30, state.userDailyMinutes)
    }

    @Test
    fun testOnDismissAttempt_triggersExitIntentDownsellWhenEnabledAndNotPreviouslySeen() = runTest(mainDispatcher) {
        featureFlagManager.config = FunnelExperimentConfig(
            downsellLadder = DownsellLadderConfig(
                tier2ExitIntent = ExitIntentDownsellConfig(
                    enabled = true,
                    discountPercent = 40,
                    countdownSeconds = 600,
                ),
            ),
        )
        userPreferences.setExitDownsellSeen(false)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onUiEvent(PaywallUiEvent.OnDismissAttempt)
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.showExitIntentDownsell)
        assertFalse(state.isDismissRequired)
        assertEquals(40, state.downsellDiscountPercent)
        assertNotNull(userPreferences.getDownsellStartTimeMillis())
        assertTrue(fakeAnalytics.hasEvent("downsell_view"))
    }

    @Test
    fun testOnDismissAttempt_dismissesDirectlyWhenUserHasAlreadySeenExitDownsell() = runTest(mainDispatcher) {
        userPreferences.setExitDownsellSeen(true)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onUiEvent(PaywallUiEvent.OnDismissAttempt)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.showExitIntentDownsell)
        assertTrue(state.isDismissRequired)
        val lastDismiss = fakeAnalytics.lastEventNamed(Analytics.EVENT_PAYWALL_DISMISSED)
        assertNotNull(lastDismiss)
        assertEquals("already_seen_downsell", lastDismiss.params?.get("reason"))
    }

    @Test
    fun testOnClaimDownsell_setsExitDownsellSeenAndAttemptsPurchase() = runTest(mainDispatcher) {
        featureFlagManager.config = FunnelExperimentConfig(
            downsellLadder = DownsellLadderConfig(
                tier2ExitIntent = ExitIntentDownsellConfig(
                    enabled = true,
                    discountPercent = 40,
                    countdownSeconds = 600,
                ),
            ),
        )
        userPreferences.setExitDownsellSeen(false)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Trigger exit intent first
        viewModel.onUiEvent(PaywallUiEvent.OnDismissAttempt)
        runCurrent()
        assertTrue(viewModel.uiState.value.showExitIntentDownsell)

        // User claims downsell
        viewModel.onUiEvent(PaywallUiEvent.OnClaimDownsell)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.showExitIntentDownsell)
        assertTrue(userPreferences.hasSeenExitDownsell())
        assertTrue(fakeAnalytics.hasEvent("downsell_claimed"))
        assertTrue(fakeAnalytics.hasEvent("initiate_checkout"))
        assertNotNull(state.successfulSubscription)
    }

    @Test
    fun testOnDeclineDownsell_setsExitDownsellSeenAndTriggersMicroCreditDownsellIfEnabled() = runTest(mainDispatcher) {
        featureFlagManager.config = FunnelExperimentConfig(
            downsellLadder = DownsellLadderConfig(
                tier2ExitIntent = ExitIntentDownsellConfig(enabled = true),
                tier3MicroCredit = MicroCreditDownsellConfig(
                    enabled = true,
                    creditsAmount = 50,
                    priceText = "$4.99",
                ),
            ),
        )
        userPreferences.setExitDownsellSeen(false)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onUiEvent(PaywallUiEvent.OnDismissAttempt)
        runCurrent()

        // Decline tier 2 exit intent downsell
        viewModel.onUiEvent(PaywallUiEvent.OnDeclineDownsell)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(userPreferences.hasSeenExitDownsell())
        assertFalse(state.showExitIntentDownsell)
        assertTrue(state.showMicroCreditDownsell)
        assertFalse(state.isDismissRequired)
        assertEquals(50, state.microCreditAmount)
        assertEquals("$4.99", state.microCreditPriceText)
        assertTrue(fakeAnalytics.hasEvent("downsell_declined"))
    }

    @Test
    fun testOnDeclineDownsell_dismissesWhenMicroCreditDisabled() = runTest(mainDispatcher) {
        featureFlagManager.config = FunnelExperimentConfig(
            downsellLadder = DownsellLadderConfig(
                tier2ExitIntent = ExitIntentDownsellConfig(enabled = true),
                tier3MicroCredit = MicroCreditDownsellConfig(enabled = false),
            ),
        )
        userPreferences.setExitDownsellSeen(false)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onUiEvent(PaywallUiEvent.OnDismissAttempt)
        advanceUntilIdle()

        viewModel.onUiEvent(PaywallUiEvent.OnDeclineDownsell)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.showExitIntentDownsell)
        assertFalse(state.showMicroCreditDownsell)
        assertTrue(state.isDismissRequired)
    }

    @Test
    fun testOnBuyMicroCredit_addsCreditsToRepositoryAndRequestsDismiss() = runTest(mainDispatcher) {
        featureFlagManager.config = FunnelExperimentConfig(
            downsellLadder = DownsellLadderConfig(
                tier2ExitIntent = ExitIntentDownsellConfig(enabled = false),
                tier3MicroCredit = MicroCreditDownsellConfig(
                    enabled = true,
                    creditsAmount = 50,
                ),
            ),
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Micro credit downsell is triggered directly since tier 2 is disabled
        viewModel.onUiEvent(PaywallUiEvent.OnDismissAttempt)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showMicroCreditDownsell)

        val initialCredits = creditRepository.balance.first()

        viewModel.onUiEvent(PaywallUiEvent.OnBuyMicroCredit)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.showMicroCreditDownsell)
        assertTrue(state.isDismissRequired)
        assertEquals(initialCredits + 50, creditRepository.balance.first())
        assertTrue(fakeAnalytics.hasEvent("downsell_claimed"))
        assertTrue(fakeAnalytics.hasEvent(Analytics.EVENT_PURCHASE))
    }

    @Test
    fun testOnDeclineMicroCredit_logsAnalyticsAndRequestsDismiss() = runTest(mainDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onUiEvent(PaywallUiEvent.OnDeclineMicroCredit)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.showMicroCreditDownsell)
        assertTrue(state.isDismissRequired)

        val declinedEvent = fakeAnalytics.lastEventNamed("downsell_declined")
        assertNotNull(declinedEvent)
        assertEquals("micro_credit", declinedEvent.params?.get("type"))

        val dismissedEvent = fakeAnalytics.lastEventNamed(Analytics.EVENT_PAYWALL_DISMISSED)
        assertNotNull(dismissedEvent)
        assertEquals("declined_micro_credit", dismissedEvent.params?.get("reason"))
    }

    @Test
    fun testDownsellCountdownTimer_updatesDownsellTimeRemainingSeconds() = runTest(mainDispatcher) {
        featureFlagManager.config = FunnelExperimentConfig(
            downsellLadder = DownsellLadderConfig(
                tier2ExitIntent = ExitIntentDownsellConfig(
                    enabled = true,
                    countdownSeconds = 600,
                ),
            ),
        )
        userPreferences.setExitDownsellSeen(false)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onUiEvent(PaywallUiEvent.OnDismissAttempt)
        runCurrent()

        assertEquals(600, viewModel.uiState.value.downsellTimeRemainingSeconds)

        // Advance 1 second
        advanceTimeBy(1000L)
        runCurrent()
        assertEquals(599, viewModel.uiState.value.downsellTimeRemainingSeconds)

        // Advance 2 more seconds
        advanceTimeBy(2000L)
        runCurrent()
        assertEquals(597, viewModel.uiState.value.downsellTimeRemainingSeconds)
    }
}
