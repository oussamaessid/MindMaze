package app.mindmaze

import android.content.Context
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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


    /**
     * Pré-charge les niveaux dès le lancement de l'app
     */
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
            println("✅ Cache vidé et version mise à jour")
        } else if (savedVersion == -1L) {
            prefs.edit().putLong("saved_version_code", currentVersion).apply()
            println("📱 Première installation détectée, version: $currentVersion")
        } else {
            println("✓ Même version: $currentVersion")
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
                context.cacheDir?.let { cacheDir ->
                    deleteDir(cacheDir)
                    println("🗑️ Cache directory vidé")
                }
                context.externalCacheDir?.let { externalCacheDir ->
                    deleteDir(externalCacheDir)
                    println("🗑️ External cache directory vidé")
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    context.codeCacheDir?.let { codeCacheDir ->
                        deleteDir(codeCacheDir)
                        println("🗑️ Code cache directory vidé")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                println("❌ Erreur lors du vidage du cache: ${e.message}")
            }
        }
    }

    private fun deleteDir(dir: File): Boolean {
        if (dir.isDirectory) {
            val children = dir.list()
            children?.forEach { child ->
                val success = deleteDir(File(dir, child))
                if (!success) {
                    return false
                }
            }
        }
        return dir.delete()
    }
}

@Composable
fun MindMazeApp() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    when (currentScreen) {
        Screen.Home -> {
            HomeScreen(
                onPlayClicked = {
                    currentScreen = Screen.Game
                }
            )
        }
        Screen.Game -> {
            GameScreen(
                onBack = {
                    currentScreen = Screen.Home
                }
            )
        }
    }
}

sealed class Screen {
    object Home : Screen()
    object Game : Screen()
}