package app.mindmaze

object AdConfig {

    val USE_TEST_ADS: Boolean = BuildConfig.USE_TEST_ADS

    // Banner
    const val BANNER_AD_UNIT_ID_TEST = "ca-app-pub-3940256099942544/6300978111"
    const val BANNER_AD_UNIT_ID_REAL = "ca-app-pub-9651830078758870/2032871230"

    // Interstitial (kept for InterstitialAdManager — not shown in game flow)
    const val INTERSTITIAL_AD_UNIT_ID_TEST = "ca-app-pub-3940256099942544/1033173712"
    const val INTERSTITIAL_AD_UNIT_ID_REAL = "ca-app-pub-9651830078758870/8901531919"

    // Rewarded video — +1 life
    const val REWARDED_AD_UNIT_ID_TEST = "ca-app-pub-3940256099942544/5224354917"
    const val REWARDED_AD_UNIT_ID_REAL = "ca-app-pub-9651830078758870/XXXXXXXXXX" // TODO: replace with real ID

    fun getBannerAdId(): String =
        if (USE_TEST_ADS) BANNER_AD_UNIT_ID_TEST else BANNER_AD_UNIT_ID_REAL

    fun getInterstitialAdId(): String =
        if (USE_TEST_ADS) INTERSTITIAL_AD_UNIT_ID_TEST else INTERSTITIAL_AD_UNIT_ID_REAL

    fun getRewardedAdId(): String =
        if (USE_TEST_ADS) REWARDED_AD_UNIT_ID_TEST else REWARDED_AD_UNIT_ID_REAL
}