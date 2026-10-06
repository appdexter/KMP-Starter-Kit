package com.kotlinfoundation.koko.core.navigation

import com.kotlinfoundation.koko.data.repository.AttributionRepository
import com.kotlinfoundation.koko.data.source.remote.apiservices.attribution.AttributionApiService
import com.kotlinfoundation.koko.presentation.navigation.AccountScreenRoute
import com.kotlinfoundation.koko.presentation.navigation.CreditBalanceScreenRoute
import com.kotlinfoundation.koko.presentation.navigation.DebugMenuScreenRoute
import com.kotlinfoundation.koko.presentation.navigation.GalleryScreenRoute
import com.kotlinfoundation.koko.presentation.navigation.GenerationResultScreenRoute
import com.kotlinfoundation.koko.presentation.navigation.HelpAndSupportScreenRoute
import com.kotlinfoundation.koko.presentation.navigation.HomeScreenRoute
import com.kotlinfoundation.koko.presentation.navigation.OnBoardingScreenRoute
import com.kotlinfoundation.koko.presentation.navigation.PaywallScreenRoute
import com.kotlinfoundation.koko.presentation.navigation.ProfileScreenRoute
import com.kotlinfoundation.koko.presentation.navigation.SignInScreenRoute
import com.kotlinfoundation.koko.presentation.navigation.SubscriptionsScreenRoute
import io.ktor.client.HttpClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeepLinkParserTest {

    private val parser = DeepLinkParser()

    @Test
    fun parseValidSchemes() {
        val premiumResult = parser.parse("myapp://premium")
        assertNotNull(premiumResult)
        assertEquals(PaywallScreenRoute(), premiumResult.route)

        val creditsResult = parser.parse("koko://credits")
        assertNotNull(creditsResult)
        assertEquals(CreditBalanceScreenRoute, creditsResult.route)

        val httpsResult = parser.parse("https://koko.app/generation/12345")
        assertNotNull(httpsResult)
        assertEquals(GenerationResultScreenRoute(id = "12345"), httpsResult.route)
    }

    @Test
    fun parseAllSingleSegmentRoutes() {
        assertEquals(PaywallScreenRoute(), parser.parse("myapp://paywall")?.route)
        assertEquals(PaywallScreenRoute(), parser.parse("myapp://subscribe")?.route)
        assertEquals(SubscriptionsScreenRoute, parser.parse("myapp://subscriptions")?.route)
        assertEquals(CreditBalanceScreenRoute, parser.parse("myapp://credits")?.route)
        assertEquals(CreditBalanceScreenRoute, parser.parse("myapp://credit_balance")?.route)
        assertEquals(ProfileScreenRoute, parser.parse("myapp://profile")?.route)
        assertEquals(AccountScreenRoute, parser.parse("myapp://account")?.route)
        assertEquals(GalleryScreenRoute, parser.parse("myapp://gallery")?.route)

        assertEquals(SignInScreenRoute(isSignIn = true), parser.parse("myapp://signin")?.route)
        assertEquals(SignInScreenRoute(isSignIn = true), parser.parse("myapp://login")?.route)
        assertEquals(SignInScreenRoute(isSignIn = true), parser.parse("myapp://auth")?.route)

        assertEquals(SignInScreenRoute(isSignIn = false), parser.parse("myapp://signup")?.route)
        assertEquals(SignInScreenRoute(isSignIn = false), parser.parse("myapp://register")?.route)

        assertEquals(OnBoardingScreenRoute, parser.parse("myapp://onboarding")?.route)
        assertEquals(HelpAndSupportScreenRoute, parser.parse("myapp://help")?.route)
        assertEquals(HelpAndSupportScreenRoute, parser.parse("myapp://support")?.route)
        assertEquals(DebugMenuScreenRoute(from = "settings"), parser.parse("myapp://debug")?.route)
        assertEquals(HomeScreenRoute, parser.parse("myapp://home")?.route)
    }

    @Test
    fun parseDynamicSegmentRoutes() {
        val genResult = parser.parse("myapp://generation/test-gen-id-999")
        assertNotNull(genResult)
        assertEquals(GenerationResultScreenRoute("test-gen-id-999"), genResult.route)

        val resResult = parser.parse("koko://result/item-output-456")
        assertNotNull(resResult)
        assertEquals(GenerationResultScreenRoute("item-output-456"), resResult.route)

        val webGenResult = parser.parse("https://koko.app/generation/web-12345")
        assertNotNull(webGenResult)
        assertEquals(GenerationResultScreenRoute("web-12345"), webGenResult.route)
    }

    @Test
    fun parseAttributionQueryParams() {
        val result = parser.parse("myapp://paywall?fbclid=xyz123&utm_source=meta")
        assertNotNull(result)
        assertEquals(PaywallScreenRoute(), result.route)

        val attribution = result.attribution
        assertNotNull(attribution)
        assertEquals("xyz123", attribution.fbclid)
        assertEquals("meta", attribution.utmSource)
        assertNull(attribution.gclid)
        assertNull(attribution.utmCampaign)

        // Comprehensive attribution test
        val fullUri = "https://koko.app/generation/12345?" +
            "fbclid=fb_val_1" +
            "&gclid=g_val_2" +
            "&fbp=fbp_val_3" +
            "&fbc=fbc_val_4" +
            "&utm_source=google" +
            "&utm_campaign=summer_promo" +
            "&utm_medium=cpc"

        val fullResult = parser.parse(fullUri)
        assertNotNull(fullResult)
        assertEquals(GenerationResultScreenRoute("12345"), fullResult.route)

        val fullAttr = fullResult.attribution
        assertNotNull(fullAttr)
        assertEquals("fb_val_1", fullAttr.fbclid)
        assertEquals("g_val_2", fullAttr.gclid)
        assertEquals("fbp_val_3", fullAttr.fbp)
        assertEquals("fbc_val_4", fullAttr.fbc)
        assertEquals("google", fullAttr.utmSource)
        assertEquals("summer_promo", fullAttr.utmCampaign)
        assertEquals("cpc", fullAttr.utmMedium)
    }

    @Test
    fun parseUnknownOrMalformedUrls() {
        assertNull(parser.parse(null))
        assertNull(parser.parse(""))
        assertNull(parser.parse("   "))
        assertNull(parser.parse("not-a-valid-url"))
        assertNull(parser.parse("ftp://myapp/home"))
        assertNull(parser.parse("myapp://"))
        assertNull(parser.parse("myapp://unknown_feature"))
        assertNull(parser.parse("myapp://generation")) // missing ID
        assertNull(parser.parse("myapp://generation/id/extra/deep")) // too many segments
    }

    @Test
    fun parseCaseInsensitivityAndTrailingSlashes() {
        val profileResult = parser.parse("MYAPP://Profile/")
        assertNotNull(profileResult)
        assertEquals(ProfileScreenRoute, profileResult.route)

        val paywallWithParams = parser.parse("KOKO://PAYWALL/?FBCLID=case_test&UTM_SOURCE=meta_case")
        assertNotNull(paywallWithParams)
        assertEquals(PaywallScreenRoute(), paywallWithParams.route)
        assertEquals("case_test", paywallWithParams.attribution?.fbclid)
        assertEquals("meta_case", paywallWithParams.attribution?.utmSource)

        val webCaseResult = parser.parse("HTTPS://KOKO.APP/GENERATION/MixedCaseId/")
        assertNotNull(webCaseResult)
        assertEquals(GenerationResultScreenRoute("MixedCaseId"), webCaseResult.route)
    }

    @Test
    fun deepLinkManagerReceivesAndDispatches() {
        val manager = DeepLinkManager(parser = parser)

        assertFalse(manager.onDeepLinkReceived("invalid-url"))
        assertNull(manager.pendingEvent)

        assertTrue(manager.onDeepLinkReceived("myapp://account"))
        assertNotNull(manager.pendingEvent)
        assertEquals(AccountScreenRoute, manager.pendingEvent?.route)

        val consumed = manager.consumePendingEvent()
        assertEquals(AccountScreenRoute, consumed?.route)
        assertNull(manager.pendingEvent)
    }

    @Test
    fun deepLinkManagerAttributionForwarding() {
        val fakeRepo = AttributionRepository(
            attributionApiService = AttributionApiService(HttpClient()),
        )
        val manager = DeepLinkManager(parser = parser, attributionRepository = fakeRepo)

        val handled = manager.onDeepLinkReceived(
            "myapp://paywall?fbclid=fb_auto&gclid=g_auto&fbp=fbp_auto&fbc=fbc_auto",
        )
        assertTrue(handled)

        val cachedParams = fakeRepo.currentParams()
        assertEquals("fb_auto", cachedParams.fbclid)
        assertEquals("g_auto", cachedParams.gclid)
        assertEquals("fbp_auto", cachedParams.fbp)
        assertEquals("fbc_auto", cachedParams.fbc)
    }
}
