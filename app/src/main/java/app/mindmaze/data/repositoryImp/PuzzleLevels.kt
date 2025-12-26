package app.mindmaze.data.repositoryImp

import android.content.Context
import androidx.compose.ui.graphics.Color
import app.mindmaze.data.model.PuzzleLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object PuzzleLevels {

    val colors = listOf(
        Color(0xFFE8C1FF), Color(0xFFFFD4A3), Color(0xFFD3D3D3), Color(0xFF9DB4C0),
        Color(0xFFB4E7B4), Color(0xFFA3E4DB), Color(0xFFFFB3E6), Color(0xFFC8C8C8),
        Color(0xFFADD8E6), Color(0xFFFFB6A3)
    )

    private const val LEVELS_URL = "https://raw.githubusercontent.com/oussamaessid/Queen/main/levels.json"
    private const val CACHE_FILE_NAME = "cached_levels.json"

    suspend fun loadLevelsFromRemote(context: Context): List<PuzzleLevel> = withContext(Dispatchers.IO) {
        try {
            // 1. Essayer de charger depuis Internet
            val url = URL(LEVELS_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val text = connection.inputStream.bufferedReader().use { it.readText() }
            val levels = parseLevelsFromJson(text)

            if (levels.isNotEmpty()) {
                // Sauvegarder dans le cache
                saveLevelsToCache(context, text)
                println("✅ Niveaux chargés depuis Internet")
                return@withContext levels
            }
        } catch (e: Exception) {
            println("⚠️ Erreur réseau: ${e.message}")
        }

        // 2. Charger depuis le cache si Internet échoue
        try {
            val cachedText = loadCacheFile(context)
            if (cachedText != null) {
                val levels = parseLevelsFromJson(cachedText)
                if (levels.isNotEmpty()) {
                    println("📦 Niveaux chargés depuis le cache")
                    return@withContext levels
                }
            }
        } catch (e: Exception) {
            println("⚠️ Erreur cache: ${e.message}")
        }

        // 3. Utiliser les niveaux de secours
        println("🔄 Utilisation des niveaux de secours")
        getFallbackLevels()
    }

    private fun saveLevelsToCache(context: Context, jsonText: String) {
        try {
            val cacheFile = File(context.cacheDir, CACHE_FILE_NAME)
            cacheFile.writeText(jsonText)
            println("💾 Cache sauvegardé")
        } catch (e: Exception) {
            println("❌ Erreur sauvegarde cache: ${e.message}")
        }
    }

    private fun loadCacheFile(context: Context): String? {
        return try {
            val cacheFile = File(context.cacheDir, CACHE_FILE_NAME)
            if (cacheFile.exists()) cacheFile.readText() else null
        } catch (e: Exception) {
            null
        }
    }

    private fun parseLevelsFromJson(jsonText: String): List<PuzzleLevel> {
        val jsonArray = JSONArray(jsonText)
        val levels = mutableListOf<PuzzleLevel>()

        for (i in 0 until jsonArray.length()) {
            val jsonLevel = jsonArray.getJSONObject(i)
            val levelMap = mutableMapOf<Int, List<Pair<Int, Int>>>()

            val keys = jsonLevel.keys()
            while (keys.hasNext()) {
                val keyStr = keys.next()
                val colorIndex = keyStr.toIntOrNull() ?: continue

                val pairsArray = jsonLevel.getJSONArray(keyStr)
                val pairs = mutableListOf<Pair<Int, Int>>()

                for (j in 0 until pairsArray.length()) {
                    val pair = pairsArray.getJSONArray(j)
                    val row = pair.getInt(0)
                    val col = pair.getInt(1)
                    pairs.add(row to col)
                }
                levelMap[colorIndex] = pairs
            }
            levels.add(levelMap)
        }

        return levels
    }

    private fun getFallbackLevels(): List<PuzzleLevel> {
        return listOf(
            mapOf(
                0 to listOf(0 to 0, 0 to 1, 1 to 0, 1 to 1),
                1 to listOf(0 to 4, 0 to 5, 1 to 4, 1 to 5),
                2 to listOf(4 to 0, 4 to 1, 5 to 0, 5 to 1),
                3 to listOf(4 to 4, 4 to 5, 5 to 4, 5 to 5)
            ),
            mapOf(
                0 to listOf(0 to 0, 0 to 1, 1 to 0, 1 to 1, 0 to 2),
                1 to listOf(0 to 5, 1 to 5, 2 to 5),
                2 to listOf(5 to 0, 5 to 1, 4 to 0),
                3 to listOf(5 to 5, 4 to 5, 4 to 4)
            )
        )
    }

    fun getBoardSize(level: PuzzleLevel): Int {
        if (level.isEmpty()) return 6
        val all = level.values.flatten()
        if (all.isEmpty()) return 6
        val maxRow = all.maxOf { it.first }
        val maxCol = all.maxOf { it.second }
        return maxOf(maxRow, maxCol) + 1
    }

    fun buildMatrix(level: PuzzleLevel, size: Int): Array<IntArray> {
        val matrix = Array(size) { IntArray(size) { -1 } }
        level.forEach { (colorIdx, positions) ->
            positions.forEach { (r, c) ->
                if (r < size && c < size) {
                    matrix[r][c] = colorIdx
                }
            }
        }
        return matrix
    }
}