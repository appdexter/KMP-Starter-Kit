package com.kotlinfoundation.koko.util.inappreview

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.kotlinfoundation.koko.util.logging.AppLogger

@Composable
actual fun rememberInAppReviewManager(): InAppReviewManager {
    val context: Context = LocalContext.current
    val activity = context as? ComponentActivity
    return remember { InAppReviewManagerImpl(activity = activity) }
}

private class InAppReviewManagerImpl(
    private val activity: ComponentActivity?,
) : InAppReviewManager {

    companion object {
        @Volatile private var cachedReviewInfo: ReviewInfo? = null

        @Volatile private var isPreloading: Boolean = false

        @Volatile private var hasLaunchedReviewInSession: Boolean = false
    }

    private var reviewManager: ReviewManager? = null

    init {
        preloadReviewInfo()
    }

    override fun requestReview() {
        if (activity == null || hasLaunchedReviewInSession) return
        if (cachedReviewInfo != null) {
            launchReviewIfReady()
        } else {
            preloadReviewInfo(showWhenReady = true)
        }
    }

    private fun preloadReviewInfo(showWhenReady: Boolean = false) {
        if (activity == null || cachedReviewInfo != null || isPreloading) return

        isPreloading = true
        reviewManager = ReviewManagerFactory.create(activity.applicationContext)
        val manager = reviewManager ?: run {
            isPreloading = false
            return
        }

        manager.requestReviewFlow().addOnCompleteListener { task ->
            isPreloading = false
            if (task.isSuccessful) {
                cachedReviewInfo = task.result
                AppLogger.d("ReviewInfo loaded.")
                if (showWhenReady) {
                    launchReviewIfReady()
                }
            } else {
                AppLogger.e("Error loading ReviewInfo", task.exception)
            }
        }
    }

    private fun launchReviewIfReady() {
        val activity = this.activity ?: return
        if (hasLaunchedReviewInSession) return
        val info = cachedReviewInfo ?: return
        val manager = reviewManager ?: ReviewManagerFactory.create(activity.applicationContext)

        hasLaunchedReviewInSession = true
        manager.launchReviewFlow(activity, info).addOnFailureListener { e ->
            AppLogger.e("InApp Review launch failed", e)
        }
    }
}
