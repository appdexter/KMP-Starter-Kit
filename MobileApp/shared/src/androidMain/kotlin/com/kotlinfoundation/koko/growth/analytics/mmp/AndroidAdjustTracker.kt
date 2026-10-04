package com.kotlinfoundation.koko.growth.analytics.mmp

import android.content.Context
import com.kotlinfoundation.koko.util.logging.AppLogger
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Android implementation of [MmpTracker] for Adjust using defensive reflection.
 * Never crashes if the Adjust SDK is not linked in build.gradle.
 */
class AndroidAdjustTracker(
    private val context: Context,
) : MmpTracker {

    private var isTrackingEnabled: Boolean = true
    private var isInitialized: Boolean = false

    override val providerName: String = "Adjust"

    override fun getRevenueCatAttributeKey(): String = "\$adjustId"

    override fun initialize(config: MmpConfig) {
        if (!config.isEnabled || isInitialized) return
        val appToken = config.adjustAppTokenAndroid
        if (appToken.isBlank()) {
            AppLogger.d("AndroidAdjustTracker: Adjust Android app token is blank, skipping initialization.")
            return
        }

        try {
            val adjustClass = Class.forName("com.adjust.sdk.Adjust")
            val adjustConfigClass = Class.forName("com.adjust.sdk.AdjustConfig")

            val environmentField = if (config.environment == MmpEnvironment.PRODUCTION) {
                adjustConfigClass.getField("ENVIRONMENT_PRODUCTION")
            } else {
                adjustConfigClass.getField("ENVIRONMENT_SANDBOX")
            }
            val environment = environmentField.get(null)

            val constructor = adjustConfigClass.getConstructor(Context::class.java, String::class.java, String::class.java)
            val adjustConfigInstance = constructor.newInstance(context.applicationContext, appToken, environment)

            // Adjust.initSdk(adjustConfig)
            val initSdkMethod = adjustClass.getMethod("initSdk", adjustConfigClass)
            initSdkMethod.invoke(null, adjustConfigInstance)

            isInitialized = true
            AppLogger.d("AndroidAdjustTracker: Native Adjust SDK initialized successfully.")
        } catch (_: ClassNotFoundException) {
            AppLogger.d("AndroidAdjustTracker: Adjust SDK not found on classpath. Running in fallback mode.")
        } catch (t: Throwable) {
            AppLogger.e("AndroidAdjustTracker: Failed to initialize Adjust SDK: ${t.message}", t)
        }
    }

    override fun trackEvent(eventTokenOrName: String, params: Map<String, Any>?) {
        if (!isTrackingEnabled) return
        try {
            val adjustClass = Class.forName("com.adjust.sdk.Adjust")
            val adjustEventClass = Class.forName("com.adjust.sdk.AdjustEvent")

            val constructor = adjustEventClass.getConstructor(String::class.java)
            val eventInstance = constructor.newInstance(eventTokenOrName)

            if (!params.isNullOrEmpty()) {
                val addCallbackParamMethod = adjustEventClass.getMethod("addCallbackParameter", String::class.java, String::class.java)
                params.forEach { (key, value) ->
                    addCallbackParamMethod.invoke(eventInstance, key, value.toString())
                }
            }

            val trackEventMethod = adjustClass.getMethod("trackEvent", adjustEventClass)
            trackEventMethod.invoke(null, eventInstance)
        } catch (_: ClassNotFoundException) {
            // Adjust SDK not present
        } catch (t: Throwable) {
            AppLogger.e("AndroidAdjustTracker: Failed to track event '$eventTokenOrName': ${t.message}", t)
        }
    }

    override fun trackAdRevenue(
        source: String,
        value: Double,
        currency: String,
        params: Map<String, Any>?,
    ) {
        if (!isTrackingEnabled) return
        try {
            val adjustClass = Class.forName("com.adjust.sdk.Adjust")
            val adjustAdRevenueClass = Class.forName("com.adjust.sdk.AdjustAdRevenue")

            val constructor = adjustAdRevenueClass.getConstructor(String::class.java)
            val adRevenueInstance = constructor.newInstance(source)

            val setRevenueMethod = adjustAdRevenueClass.getMethod("setRevenue", Double::class.javaObjectType, String::class.java)
            setRevenueMethod.invoke(adRevenueInstance, value, currency)

            val trackAdRevenueMethod = adjustClass.getMethod("trackAdRevenue", adjustAdRevenueClass)
            trackAdRevenueMethod.invoke(null, adRevenueInstance)
            AppLogger.d("AndroidAdjustTracker: Tracked ad revenue $value $currency from $source")
        } catch (_: ClassNotFoundException) {
            // Adjust SDK not present
        } catch (t: Throwable) {
            AppLogger.e("AndroidAdjustTracker: Failed to track ad revenue: ${t.message}", t)
        }
    }

    override suspend fun getAttributionId(timeoutMs: Long): String? = suspendCancellableCoroutine { continuation ->
        try {
            val adjustClass = Class.forName("com.adjust.sdk.Adjust")
            val getAdidMethod = adjustClass.getMethod("getAdid")
            val adid = getAdidMethod.invoke(null) as? String
            continuation.resume(adid)
        } catch (_: Throwable) {
            continuation.resume(null)
        }
    }

    override fun setEnabled(enabled: Boolean) {
        this.isTrackingEnabled = enabled
    }

    override fun isEnabled(): Boolean = isTrackingEnabled
}
