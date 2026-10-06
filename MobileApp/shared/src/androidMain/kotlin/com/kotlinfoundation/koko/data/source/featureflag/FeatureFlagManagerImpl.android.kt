package com.kotlinfoundation.koko.data.source.featureflag

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.kotlinfoundation.koko.util.logging.AppLogger

class FeatureFlagManagerImpl(private val remoteConfig: FirebaseRemoteConfig) : FeatureFlagManager {

    override fun syncsFlagsAsync() {
        remoteConfig.fetchAndActivate()
            .addOnSuccessListener {
                AppLogger.d("Feature Flag Sync is completed, result: $it")
            }
            .addOnFailureListener {
                AppLogger.e("Feature Flag Sync Failed, Error: ${it.message}")
            }
    }

    override fun getBoolean(key: String): Boolean {
        val configValue = remoteConfig.getValue(key)
        return if (configValue.source == FirebaseRemoteConfig.VALUE_SOURCE_STATIC) {
            FeatureFlagManager.DEFAULT_VALUES[key] as? Boolean ?: false
        } else {
            configValue.asBoolean()
        }
    }

    override fun getString(key: String): String {
        val configValue = remoteConfig.getValue(key)
        return if (configValue.source == FirebaseRemoteConfig.VALUE_SOURCE_STATIC) {
            FeatureFlagManager.DEFAULT_VALUES[key] as? String ?: ""
        } else {
            configValue.asString()
        }
    }

    override fun getLong(key: String): Long {
        val configValue = remoteConfig.getValue(key)
        return if (configValue.source == FirebaseRemoteConfig.VALUE_SOURCE_STATIC) {
            FeatureFlagManager.DEFAULT_VALUES[key] as? Long ?: 0L
        } else {
            configValue.asLong()
        }
    }

    override fun getDouble(key: String): Double {
        val configValue = remoteConfig.getValue(key)
        return if (configValue.source == FirebaseRemoteConfig.VALUE_SOURCE_STATIC) {
            FeatureFlagManager.DEFAULT_VALUES[key] as? Double ?: 0.0
        } else {
            configValue.asDouble()
        }
    }
}
