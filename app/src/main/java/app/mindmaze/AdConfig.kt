package app.mindmaze

object AdConfig {
    // Banner Ads
    const val BANNER_AD_UNIT_ID_TEST = "ca-app-pub-3940256099942544/6300978111"
    const val BANNER_AD_UNIT_ID_REAL = "ca-app-pub-9651830078758870/2032871230"

    // Interstitial Ads
    const val INTERSTITIAL_AD_UNIT_ID_TEST = "ca-app-pub-3940256099942544/1033173712"
    const val INTERSTITIAL_AD_UNIT_ID_REAL = "ca-app-pub-9651830078758870/8901531919"

    // Automatique : test en debug, production en release
    val USE_TEST_ADS: Boolean = BuildConfig.USE_TEST_ADS

    fun getBannerAdId(): String {
        return if (USE_TEST_ADS) BANNER_AD_UNIT_ID_TEST else BANNER_AD_UNIT_ID_REAL
    }

    fun getInterstitialAdId(): String {
        return if (USE_TEST_ADS) INTERSTITIAL_AD_UNIT_ID_TEST else INTERSTITIAL_AD_UNIT_ID_REAL
    }
}