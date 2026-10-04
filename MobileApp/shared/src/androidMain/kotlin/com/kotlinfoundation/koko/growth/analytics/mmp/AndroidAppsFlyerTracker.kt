package com.kotlinfoundation.koko.growth.analytics.mmp

import android.content.Context
import com.kotlinfoundation.koko.util.logging.AppLogger

/**
 * Android implementation of [MmpTracker] for AppsFlyer using defensive reflection.
 * Never crashes if the AppsFlyer SDK is not linked in build.gradle.
 */
class AndroidAppsFlyerTracker(
    private val context: Context,
) : MmpTracker {

    private var isTrackingEnabled: Boolean = true
    private var isInitialized: Boolean = false

    override val providerName: String = "AppsFlyer"

    override fun getRevenueCatAttributeKey(): String = "\$appsflyerId"

    override fun initialize(config: MmpConfig) {
        if (!config.isEnabled || isInitialized) return
        val devKey = config.appsFlyerDevKey
        if (devKey.isBlank()) {
            AppLogger.d("AndroidAppsFlyerTracker: devKey is blank, skipping initialization.")
            return
        }

        try {
            val appsFlyerClass = Class.forName("com.appsflyer.AppsFlyerLib")
            val getInstanceMethod = appsFlyerClass.getMethod("getInstance")
            val appsFlyerInstance = getInstanceMethod.invoke(null)

            // init(devKey, listener, context)
            val initMethod = appsFlyerClass.getMethod(
                "init",
                String::class.java,
                Class.forName("com.appsflyer.AppsFlyerConversionListener"),
                Context::class.java,
            )
            initMethod.invoke(appsFlyerInstance, devKey, null, context.applicationContext)

            // start(context)
            val startMethod = appsFlyerClass.getMethod("start", Context::class.java)
            startMethod.invoke(appsFlyerInstance, context.applicationContext)

            isInitialized = true
            AppLogger.d("AndroidAppsFlyerTracker: Native AppsFlyer SDK initialized successfully.")
        } catch (_: ClassNotFoundException) {
            AppLogger.d("AndroidAppsFlyerTracker: AppsFlyer SDK not found on classpath. Running in fallback mode.")
        } catch (t: Throwable) {
            AppLogger.e("AndroidAppsFlyerTracker: Failed to initialize AppsFlyer SDK: ${t.message}", t)
        }
    }

    override fun trackEvent(eventTokenOrName: String, params: Map<String, Any>?) {
        if (!isTrackingEnabled) return
        try {
            val appsFlyerClass = Class.forName("com.appsflyer.AppsFlyerLib")
            val getInstanceMethod = appsFlyerClass.getMethod("getInstance")
            val appsFlyerInstance = getInstanceMethod.invoke(null)

            val logEventMethod = appsFlyerClass.getMethod(
                "logEvent",
                Context::class.java,
                String::class.java,
                Map::class.java,
            )
            logEventMethod.invoke(appsFlyerInstance, context.applicationContext, eventTokenOrName, params)
        } catch (_: ClassNotFoundException) {
            // SDK not present
        } catch (t: Throwable) {
            AppLogger.e("AndroidAppsFlyerTracker: Failed to log event '$eventTokenOrName': ${t.message}", t)
        }
    }

    override fun trackAdRevenue(
        source: String,
        value: Double,
        currency: String,
        params: Map<String, Any>?,
    ) {
        if (!isTrackingEnabled) return
        val adRevenueMap = buildMap<String, Any> {
            put("ad_platform", source)
            put("revenue", value)
            put("currency", currency)
            if (params != null) putAll(params)
        }
        trackEvent("af_ad_impression", adRevenueMap)
    }

    override suspend fun getAttributionId(timeoutMs: Long): String? = try {
        val appsFlyerClass = Class.forName("com.appsflyer.AppsFlyerLib")
        val getInstanceMethod = appsFlyerClass.getMethod("getInstance")
        val appsFlyerInstance = getInstanceMethod.invoke(null)

        val getUidMethod = appsFlyerClass.getMethod("getAppsFlyerUID", Context::class.java)
        getUidMethod.invoke(appsFlyerInstance, context.applicationContext) as? String
    } catch (_: Throwable) {
        null
    }

    override fun setEnabled(enabled: Boolean) {
        this.isTrackingEnabled = enabled
    }

    override fun isEnabled(): Boolean = isTrackingEnabled
}
