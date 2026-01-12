package app.mindmaze

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.Date

class AppOpenAdManager(
    private val application: Application
) : Application.ActivityLifecycleCallbacks, LifecycleObserver {

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var loadTime: Long = 0
    private var currentActivity: Activity? = null
    private var isShowingAd = false
    private var lastAdShownTime: Long = 0

    companion object {
        private const val TAG = "AppOpenAdManager"

        private const val USE_TEST_AD = false

        private const val APP_OPEN_AD_UNIT_ID_REAL = "ca-app-pub-4161995857939030/3809442902"

        private const val APP_OPEN_AD_UNIT_ID_TEST = "ca-app-pub-3940256099942544/9257395921"

        private const val AD_TIMEOUT_MS = 4 * 3600 * 1000L
        private const val MIN_AD_INTERVAL_MS = 4 * 3600 * 1000L
    }

    init {
        application.registerActivityLifecycleCallbacks(this)
        val addObserver = ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        val adUnitId = if (USE_TEST_AD) APP_OPEN_AD_UNIT_ID_TEST else APP_OPEN_AD_UNIT_ID_REAL

        Log.d(TAG, "════════════════════════════════════")
        Log.d(TAG, "🚀 AppOpenAdManager initialized - MindMaze")
        Log.d(TAG, "📱 Mode: ${if (USE_TEST_AD) "TEST" else "PRODUCTION"}")
        Log.d(TAG, "📱 Ad Unit ID: $adUnitId")
        Log.d(TAG, "════════════════════════════════════")

        loadAd()
    }

    fun loadAd() {
        if (isLoadingAd || isAdAvailable()) return

        isLoadingAd = true
        val adUnitId = if (USE_TEST_AD) APP_OPEN_AD_UNIT_ID_TEST else APP_OPEN_AD_UNIT_ID_REAL
        val request = AdRequest.Builder().build()

        Log.d(TAG, "📡 LOADING APP OPEN AD")

        AppOpenAd.load(
            application,
            adUnitId,
            request,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.d(TAG, "✅ APP OPEN AD LOADED!")
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = Date().time

                    if (!isShowingAd && lastAdShownTime == 0L) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            currentActivity?.let { showAdIfAvailable(it) }
                        }, 800)
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "❌ APP OPEN AD FAILED TO LOAD - Code: ${error.code}")
                    isLoadingAd = false
                }
            }
        )
    }

    private fun isAdAvailable(): Boolean {
        val available = appOpenAd != null && (Date().time - loadTime) < AD_TIMEOUT_MS
        if (!available && appOpenAd != null) {
            appOpenAd = null
        }
        return available
    }

    private fun canShowAd(): Boolean {
        if (lastAdShownTime == 0L) return true
        val elapsed = System.currentTimeMillis() - lastAdShownTime
        return elapsed >= MIN_AD_INTERVAL_MS
    }

    fun showAdIfAvailable(activity: Activity) {
        if (!canShowAd() || isShowingAd || !isAdAvailable()) {
            if (!isAdAvailable()) loadAd()
            return
        }

        Log.d(TAG, "🎬 SHOWING APP OPEN AD")

        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "✅ Ad dismissed")
                appOpenAd = null
                isShowingAd = false
                lastAdShownTime = System.currentTimeMillis()
                loadAd()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.e(TAG, "❌ Ad failed to show: ${error.message}")
                appOpenAd = null
                isShowingAd = false
                loadAd()
            }

            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
            }
        }

        appOpenAd?.show(activity)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        Log.d(TAG, "📱 App to FOREGROUND")
        Handler(Looper.getMainLooper()).postDelayed({
            currentActivity?.let {
                if (!isShowingAd) showAdIfAvailable(it)
            }
        }, 500)
    }

    override fun onActivityStarted(activity: Activity) {
        if (!isShowingAd) currentActivity = activity
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) currentActivity = null
    }

    fun cleanup() {
        appOpenAd = null
        currentActivity = null
    }
}