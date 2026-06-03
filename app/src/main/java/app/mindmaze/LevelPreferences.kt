package app.mindmaze

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

object LevelPreferences {
    private const val PREF_NAME = "mindmaze_preferences"
    private const val KEY_LAST_LEVEL = "last_level_index"
    private const val KEY_BOARD_STATE = "board_state"
    private const val KEY_ALL_LEVELS_COMPLETED = "all_levels_completed"
    private const val TAG = "LevelPreferences"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveLastLevel(context: Context, levelIndex: Int) {
        getPrefs(context).edit().putInt(KEY_LAST_LEVEL, levelIndex).apply()
        Log.d(TAG, "✅ Saved last level: $levelIndex")
    }

    fun loadLastLevel(context: Context): Int {
        val level = getPrefs(context).getInt(KEY_LAST_LEVEL, 0)
        Log.d(TAG, "📂 Loaded last level: $level")
        return level
    }

    fun saveBoardState(context: Context, levelIndex: Int, boardState: List<List<Int>>) {
        val stateString = boardState.joinToString(";") { row ->
            row.joinToString(",")
        }
        getPrefs(context).edit()
            .putString("${KEY_BOARD_STATE}_$levelIndex", stateString)
            .apply()

        val bombCount = boardState.sumOf { row -> row.count { it == 2 } }
        Log.d(TAG, "💾 Saved board state for level $levelIndex - Bombs: $bombCount")
    }

    fun loadBoardState(context: Context, levelIndex: Int, defaultSize: Int): List<List<Int>>? {
        val stateString = getPrefs(context).getString("${KEY_BOARD_STATE}_$levelIndex", null)
        return if (stateString.isNullOrEmpty()) {
            Log.d(TAG, "❌ No saved state for level $levelIndex")
            null
        } else {
            try {
                val board = stateString.split(";").map { row ->
                    row.split(",").map { it.toInt() }
                }
                val bombCount = board.sumOf { row -> row.count { it == 2 } }
                Log.d(TAG, "📥 Loaded board state for level $levelIndex - Bombs: $bombCount")
                board
            } catch (e: Exception) {
                Log.e(TAG, "⚠️ Error loading board state for level $levelIndex: ${e.message}")
                null
            }
        }
    }

    fun clearBoardState(context: Context, levelIndex: Int) {
        getPrefs(context).edit()
            .remove("${KEY_BOARD_STATE}_$levelIndex")
            .apply()
        Log.d(TAG, "🗑️ Cleared board state for level $levelIndex")
    }

    fun setAllLevelsCompleted(context: Context) {
        getPrefs(context).edit()
            .putBoolean(KEY_ALL_LEVELS_COMPLETED, true)
            .apply()
        Log.d(TAG, "🏆 All levels completed saved")
    }

    fun isAllLevelsCompleted(context: Context): Boolean {
        val completed = getPrefs(context).getBoolean(KEY_ALL_LEVELS_COMPLETED, false)
        Log.d(TAG, "🔍 All levels completed: $completed")
        return completed
    }

    fun resetAllLevelsCompleted(context: Context) {
        getPrefs(context).edit()
            .putBoolean(KEY_ALL_LEVELS_COMPLETED, false)
            .apply()
        Log.d(TAG, "🔄 All levels completed reset")
    }

    fun resetAll(context: Context) {
        getPrefs(context).edit().clear().apply()
        Log.d(TAG, "🔄 Reset all preferences")
    }
}