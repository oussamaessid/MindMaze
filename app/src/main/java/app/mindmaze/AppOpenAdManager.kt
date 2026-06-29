package app.mindmaze

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.Date

class AppOpenAdManager(
    private val application: Application
) : Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var loadTime: Long = 0
    private var currentActivity: Activity? = null
    private var isShowingAd = false
    private val appLaunchTime: Long = System.currentTimeMillis()

    private val prefs = application.getSharedPreferences("admob_prefs", Context.MODE_PRIVATE)
    private var lastAdShownTime: Long
        get() = prefs.getLong("last_app_open_ad_time", 0L)
        set(value) = prefs.edit().putLong("last_app_open_ad_time", value).apply()

    companion object {
        private const val TAG = "AppOpenAdManager"
        private val USE_TEST_AD: Boolean = BuildConfig.USE_TEST_ADS
        private const val APP_OPEN_AD_UNIT_ID_REAL = "ca-app-pub-9651830078758870/8274845951"
        private const val APP_OPEN_AD_UNIT_ID_TEST = "ca-app-pub-3940256099942544/9257395921"
        private const val AD_TIMEOUT_MS = 4 * 3600 * 1000L
        private const val MIN_AD_INTERVAL_MS = 30 * 60 * 1000L
        private const val MIN_LAUNCH_DELAY_MS = 5000L
        private const val FOREGROUND_DELAY_MS = 2000L
    }

    init {
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        Log.d(TAG, "🚀 AppOpenAdManager initialized — ${if (USE_TEST_AD) "TEST" else "PRODUCTION"}")
        loadAd()
    }

    fun loadAd() {
        if (isLoadingAd || isAdAvailable()) return
        isLoadingAd = true

        val adUnitId = if (USE_TEST_AD) APP_OPEN_AD_UNIT_ID_TEST else APP_OPEN_AD_UNIT_ID_REAL
        Log.d(TAG, "📡 Loading App Open Ad — $adUnitId")

        // SDK 21+: no orientation parameter
        AppOpenAd.load(
            application,
            adUnitId,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.d(TAG, "✅ App Open Ad loaded")
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = Date().time
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "❌ App Open Ad failed — ${error.code}: ${error.message}")
                    isLoadingAd = false
                }
            }
        )
    }

    private fun isAdAvailable(): Boolean {
        val available = appOpenAd != null && (Date().time - loadTime) < AD_TIMEOUT_MS
        if (!available && appOpenAd != null) appOpenAd = null
        return available
    }

    private fun canShowAd(): Boolean {
        if (System.currentTimeMillis() - appLaunchTime < MIN_LAUNCH_DELAY_MS) return false
        if (lastAdShownTime == 0L) return true
        return System.currentTimeMillis() - lastAdShownTime >= MIN_AD_INTERVAL_MS
    }

    fun showAdIfAvailable(activity: Activity) {
        if (!canShowAd() || isShowingAd || !isAdAvailable()) {
            if (!isAdAvailable()) loadAd()
            return
        }

        Log.d(TAG, "🎬 Showing App Open Ad")

        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "✅ App Open Ad dismissed")
                appOpenAd = null
                isShowingAd = false
                lastAdShownTime = System.currentTimeMillis()
                loadAd()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.e(TAG, "❌ App Open Ad show failed: ${error.message}")
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

    // DefaultLifecycleObserver — replaces deprecated @OnLifecycleEvent
    override fun onStart(owner: LifecycleOwner) {
        Log.d(TAG, "📱 App foregrounded")
        Handler(Looper.getMainLooper()).postDelayed({
            currentActivity?.let { if (!isShowingAd) showAdIfAvailable(it) }
        }, FOREGROUND_DELAY_MS)
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
