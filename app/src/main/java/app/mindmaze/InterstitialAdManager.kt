package app.mindmaze

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class InterstitialAdManager(private val context: Context) {
    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false

    init {
        loadAd()
    }

    private fun loadAd() {
        if (isLoading) {
            println("⏳ Pub déjà en cours de chargement...")
            return
        }

        isLoading = true
        val adRequest = AdRequest.Builder().build()

        println("📡 Chargement de la pub interstitielle...")

        InterstitialAd.load(
            context,
            AdConfig.getInterstitialAdId(),
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading = false
                    println("✅ Pub interstitielle chargée avec succès !")
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    interstitialAd = null
                    isLoading = false
                    println("❌ Échec du chargement de la pub : ${adError.message}")
                    println("   Code d'erreur : ${adError.code}")
                }
            }
        )
    }

    fun showAd(
        onAdDismissed: () -> Unit,
        onAdFailed: () -> Unit = {}
    ) {
        val activity = context.findActivity()

        if (activity == null) {
            println("❌ Impossible de trouver l'Activity")
            onAdFailed()
            onAdDismissed()
            return
        }

        if (interstitialAd != null) {
            println("🎬 Affichage de la pub interstitielle...")

            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    println("✅ Pub fermée par l'utilisateur")
                    interstitialAd = null
                    loadAd()
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    println("❌ Échec de l'affichage : ${adError.message}")
                    interstitialAd = null
                    onAdFailed()
                    onAdDismissed()
                }

                override fun onAdShowedFullScreenContent() {
                    println("✅ Pub affichée en plein écran")
                }

                override fun onAdClicked() {
                    println("👆 Pub cliquée")
                }

                override fun onAdImpression() {
                    println("👁️ Impression de pub enregistrée")
                }
            }

            interstitialAd?.show(activity)
        } else {
            println("⚠️ Pub interstitielle pas encore prête")
            if (!isLoading) {
                println("🔄 Tentative de rechargement...")
                loadAd()
            }
            onAdDismissed()
        }
    }

    private fun Context.findActivity(): Activity? {
        var context = this
        while (context is ContextWrapper) {
            if (context is Activity) return context
            context = context.baseContext
        }
        return null
    }
}