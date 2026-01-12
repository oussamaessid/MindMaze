package app.mindmaze

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import app.mindmaze.data.repositoryImp.PuzzleLevels
import app.mindmaze.ui.theme.MindMazeTheme
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {

    private var appOpenAdManager: AppOpenAdManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔒 Verrouiller l'orientation en mode portrait
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        enableEdgeToEdge()

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
        }

        checkAndClearCacheOnUpdate(this)
        preloadLevels(this)

        MobileAds.initialize(this) {
            println("✅ AdMob initialized")
        }

        // Initialisation UNIQUE du gestionnaire App Open Ad
        if (appOpenAdManager == null) {
            appOpenAdManager = AppOpenAdManager(application)
        }

        setContent {
            MindMazeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MindMazeApp()
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
    }
}

@Composable
fun MindMazeApp() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    when (currentScreen) {
        Screen.Home -> HomeScreen(onPlayClicked = { currentScreen = Screen.Game })
        Screen.Game -> GameScreen(onBack = { currentScreen = Screen.Home })
    }
}

sealed class Screen {
    object Home : Screen()
    object Game : Screen()
}