package com.kotlinfoundation.koko.core.navigation

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
import com.kotlinfoundation.koko.presentation.navigation.ScreenRoute
import com.kotlinfoundation.koko.presentation.navigation.SignInScreenRoute
import com.kotlinfoundation.koko.presentation.navigation.SubscriptionsScreenRoute

/**
 * Typealias for subscription paywall route to support legacy and convenience naming.
 */
typealias SubscriptionPaywallScreenRoute = PaywallScreenRoute

/**
 * Clean data class holding marketing attribution query parameters.
 */
data class AttributionQueryData(
    val fbclid: String? = null,
    val gclid: String? = null,
    val fbp: String? = null,
    val fbc: String? = null,
    val utmSource: String? = null,
    val utmCampaign: String? = null,
    val utmMedium: String? = null,
) {
    val hasData: Boolean
        get() = fbclid != null || gclid != null || fbp != null || fbc != null ||
            utmSource != null || utmCampaign != null || utmMedium != null
}

/**
 * Result of successfully parsing a deep link URI.
 */
data class ParsedDeepLink(
    val route: ScreenRoute,
    val attribution: AttributionQueryData? = null,
    val rawUri: String = "",
)

/**
 * Parser for handling incoming deep links, custom schemes, universal links, and attribution params.
 */
class DeepLinkParser(
    private val allowedSchemes: Set<String> = DEFAULT_SCHEMES,
) {
    constructor() : this(DEFAULT_SCHEMES)

    companion object {
        val DEFAULT_SCHEMES = setOf("myapp", "koko", "https", "http")

        fun parse(uri: String?): ParsedDeepLink? = DeepLinkParser().parse(uri)
        fun parseRoute(uri: String?): ScreenRoute? = parse(uri)?.route
        fun parseAttribution(uri: String?): AttributionQueryData? = parse(uri)?.attribution
    }

    /**
     * Parses a raw URI string into a [ParsedDeepLink], or null if invalid or unknown.
     */
    fun parse(uri: String?): ParsedDeepLink? {
        if (uri.isNullOrBlank()) return null
        val trimmed = uri.trim()

        val delimiterIndex = trimmed.indexOf("://")
        if (delimiterIndex <= 0) return null

        val scheme = trimmed.substring(0, delimiterIndex).lowercase()
        if (scheme !in allowedSchemes) return null

        val remainder = trimmed.substring(delimiterIndex + 3).trim()
        if (remainder.isEmpty()) return null

        // Strip fragment
        val withoutFragment = remainder.substringBefore('#')
        val pathAndQuery = withoutFragment

        // Separate path and query string
        val rawPath = pathAndQuery.substringBefore('?')
        val queryString = if (pathAndQuery.contains('?')) pathAndQuery.substringAfter('?') else null

        // Extract query parameters
        val queryParams = parseQueryParams(queryString)
        val attribution = extractAttribution(queryParams)

        // Parse path segments
        val isHttpScheme = scheme == "https" || scheme == "http"
        val pathString = if (isHttpScheme) {
            val slashIndex = rawPath.indexOf('/')
            if (slashIndex != -1) rawPath.substring(slashIndex + 1) else ""
        } else {
            val firstSlash = rawPath.indexOf('/')
            val firstPart = if (firstSlash != -1) rawPath.substring(0, firstSlash) else rawPath
            if (firstPart.contains('.') || firstPart.equals("koko.app", ignoreCase = true)) {
                if (firstSlash != -1) rawPath.substring(firstSlash + 1) else ""
            } else {
                rawPath
            }
        }

        val segments = pathString
            .split('/')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val route = resolveRoute(segments, queryParams) ?: return null

        return ParsedDeepLink(
            route = route,
            attribution = if (attribution.hasData) attribution else null,
            rawUri = trimmed,
        )
    }

    private fun resolveRoute(
        segments: List<String>,
        queryParams: Map<String, String>,
    ): ScreenRoute? = when (segments.size) {
        1 -> {
            when (segments[0].lowercase()) {
                "paywall", "premium", "subscribe" -> {
                    val placementId = queryParams["placement_id"]
                        ?: queryParams["placementId"]
                        ?: queryParams["placement"]
                    PaywallScreenRoute(placementId = placementId)
                }

                "subscriptions" -> SubscriptionsScreenRoute

                "credits", "credit_balance" -> CreditBalanceScreenRoute

                "profile" -> ProfileScreenRoute

                "account" -> AccountScreenRoute

                "gallery" -> GalleryScreenRoute

                "signin", "login", "auth" -> {
                    val isSignIn = queryParams["isSignIn"]?.toBooleanStrictOrNull()
                        ?: queryParams["is_sign_in"]?.toBooleanStrictOrNull()
                        ?: true
                    SignInScreenRoute(isSignIn = isSignIn)
                }

                "signup", "register" -> {
                    val isSignIn = queryParams["isSignIn"]?.toBooleanStrictOrNull()
                        ?: queryParams["is_sign_in"]?.toBooleanStrictOrNull()
                        ?: false
                    SignInScreenRoute(isSignIn = isSignIn)
                }

                "onboarding" -> OnBoardingScreenRoute

                "help", "support" -> HelpAndSupportScreenRoute

                "debug" -> {
                    val from = queryParams["from"] ?: "settings"
                    DebugMenuScreenRoute(from = from)
                }

                "home" -> HomeScreenRoute

                else -> null
            }
        }

        2 -> {
            val first = segments[0].lowercase()
            val second = segments[1]
            when (first) {
                "generation", "result" -> {
                    if (second.isNotBlank()) GenerationResultScreenRoute(id = second) else null
                }

                else -> null
            }
        }

        else -> null
    }

    private fun extractAttribution(queryParams: Map<String, String>): AttributionQueryData {
        if (queryParams.isEmpty()) return AttributionQueryData()

        val lowerParams = queryParams.mapKeys { it.key.lowercase() }
        val fbclid = lowerParams["fbclid"]
        val gclid = lowerParams["gclid"]
        val fbp = lowerParams["fbp"] ?: lowerParams["_fbp"]
        val fbc = lowerParams["fbc"] ?: lowerParams["_fbc"]
        val utmSource = lowerParams["utm_source"] ?: lowerParams["utmsource"]
        val utmCampaign = lowerParams["utm_campaign"] ?: lowerParams["utmcampaign"]
        val utmMedium = lowerParams["utm_medium"] ?: lowerParams["utmmedium"]

        return AttributionQueryData(
            fbclid = fbclid,
            gclid = gclid,
            fbp = fbp,
            fbc = fbc,
            utmSource = utmSource,
            utmCampaign = utmCampaign,
            utmMedium = utmMedium,
        )
    }

    private fun parseQueryParams(queryString: String?): Map<String, String> {
        if (queryString.isNullOrBlank()) return emptyMap()
        val params = mutableMapOf<String, String>()
        val pairs = queryString.split('&', ';')
        for (pair in pairs) {
            if (pair.isBlank()) continue
            val eqIndex = pair.indexOf('=')
            if (eqIndex != -1) {
                val key = decodeUrlComponent(pair.substring(0, eqIndex).trim())
                val value = decodeUrlComponent(pair.substring(eqIndex + 1).trim())
                if (key.isNotEmpty()) {
                    params[key] = value
                }
            } else {
                val key = decodeUrlComponent(pair.trim())
                if (key.isNotEmpty()) {
                    params[key] = ""
                }
            }
        }
        return params
    }

    private fun decodeUrlComponent(value: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < value.length) {
            val c = value[i]
            when (c) {
                '+' -> {
                    sb.append(' ')
                    i++
                }

                '%' -> {
                    if (i + 2 < value.length) {
                        val hex = value.substring(i + 1, i + 3)
                        val code = hex.toIntOrNull(16)
                        if (code != null) {
                            sb.append(code.toChar())
                            i += 3
                        } else {
                            sb.append(c)
                            i++
                        }
                    } else {
                        sb.append(c)
                        i++
                    }
                }

                else -> {
                    sb.append(c)
                    i++
                }
            }
        }
        return sb.toString()
    }
}
