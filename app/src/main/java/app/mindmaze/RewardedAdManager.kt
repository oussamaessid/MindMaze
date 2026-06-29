package app.mindmaze

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class RewardedAdManager(context: Context) {

    // Activity context used for BOTH loading and showing — required by some ad networks
    private val activity: Activity? = context.findActivity()
    private val appContext: Context = context.applicationContext
    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    companion object {
        private const val TAG = "RewardedAdManager"
    }

    init {
        loadAd()
    }

    fun loadAd() {
        if (isLoading || rewardedAd != null) return
        val act = activity ?: run {
            Log.e(TAG, "No Activity context — cannot load ad")
            return
        }
        isLoading = true
        val adId = AdConfig.getRewardedAdId()
        Log.d(TAG, "📡 Loading rewarded ad with Activity context — $adId")

        RewardedAd.load(
            act,                          // Activity context as recommended by Google
            adId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "✅ Rewarded ad loaded")
                    rewardedAd = ad
                    isLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "❌ Failed — code:${error.code} msg:${error.message}")
                    rewardedAd = null
                    isLoading = false
                    Handler(Looper.getMainLooper()).postDelayed({ loadAd() }, 5_000L)
                }
            }
        )
    }

    fun showAd(
        onRewarded: () -> Unit,
        onDismissed: () -> Unit = {}
    ) {
        val act = activity
        val ad = rewardedAd

        if (act == null) {
            Log.e(TAG, "No Activity — cannot show ad")
            onDismissed()
            return
        }

        if (ad == null) {
            Toast.makeText(appContext, "Ad is loading, please try again in a moment.", Toast.LENGTH_SHORT).show()
            if (!isLoading) loadAd()
            onDismissed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Ad dismissed")
                rewardedAd = null
                loadAd()
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.e(TAG, "❌ Show failed: ${error.message}")
                Toast.makeText(appContext, "Ad unavailable, please try again.", Toast.LENGTH_SHORT).show()
                rewardedAd = null
                loadAd()
                onDismissed()
            }
        }

        ad.show(act) { rewardItem ->
            Log.d(TAG, "🎁 Reward earned: ${rewardItem.amount} ${rewardItem.type}")
            onRewarded()
        }
    }

    private fun Context.findActivity(): Activity? {
        var ctx = this
        repeat(10) {
            if (ctx is Activity) return ctx as Activity
            ctx = (ctx as? ContextWrapper)?.baseContext ?: return null
        }
        return null
    }
}
