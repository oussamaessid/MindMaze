package app.mindmaze

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import app.mindmaze.audio.SoundManager
import app.mindmaze.data.repositoryImp.PuzzleLevels
import app.mindmaze.screens.HelpScreen
import app.mindmaze.ui.theme.BoomdukuTheme
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {

    private var appOpenAdManager: AppOpenAdManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        enableEdgeToEdge()

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
        }

        checkAndClearCacheOnUpdate(this)
        preloadLevels(this)

        // Register emulator/device as test device so test ads always deliver
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setTestDeviceIds(listOf(com.google.android.gms.ads.AdRequest.DEVICE_ID_EMULATOR))
                .build()
        )

        MobileAds.initialize(this) {
            println("✅ AdMob initialized")
            if (appOpenAdManager == null) {
                appOpenAdManager = AppOpenAdManager(application)
            }
        }

        setContent {
            BoomdukuTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BoomdukuApp()
                }
            }
        }
    }

    private fun preloadLevels(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                println("🔄 Pré-chargement des niveaux...")
                PuzzleLevels.loadLevelsFromRemote(context)
                println("✅ Niveaux pré-chargés avec succès")
            } catch (e: Exception) {
                println("⚠️ Erreur pré-chargement: ${e.message}")
            }
        }
    }

    private fun checkAndClearCacheOnUpdate(context: Context) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val currentVersion = getCurrentVersionCode(context)
        val savedVersion = prefs.getLong("saved_version_code", -1)

        if (savedVersion != -1L && savedVersion != currentVersion) {
            println("🔄 Nouvelle version détectée: $savedVersion → $currentVersion")
            clearAppCache(context)
            prefs.edit().putLong("saved_version_code", currentVersion).apply()
        } else if (savedVersion == -1L) {
            prefs.edit().putLong("saved_version_code", currentVersion).apply()
        }
    }

    private fun getCurrentVersionCode(context: Context): Long {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            -1L
        }
    }

    private fun clearAppCache(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                context.cacheDir?.deleteRecursively()
                context.externalCacheDir?.deleteRecursively()
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    context.codeCacheDir?.deleteRecursively()
                }
                println("✅ Cache vidé avec succès")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun File.deleteRecursively(): Boolean {
        return if (isDirectory) {
            listFiles()?.forEach { it.deleteRecursively() }
            delete()
        } else {
            delete()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        appOpenAdManager?.cleanup()
        SoundManager.release()
    }
}

@Composable
fun BoomdukuApp() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    BackHandler(enabled = currentScreen != Screen.Home) {
        currentScreen = Screen.Home
    }

    when (currentScreen) {
        Screen.Home -> HomeScreen(
            onPlayClicked = { currentScreen = Screen.Game },
            onHelpClicked = { currentScreen = Screen.Help }
        )
        Screen.Game -> GameScreen(
            onBack = { currentScreen = Screen.Home }
        )
        Screen.Help -> HelpScreen(
            onBack = { currentScreen = Screen.Home }
        )
    }
}

sealed class Screen {
    object Home : Screen()
    object Game : Screen()
    object Help : Screen()
}
