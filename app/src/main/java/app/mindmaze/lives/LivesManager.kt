package app.mindmaze.lives

import android.content.Context
import android.content.SharedPreferences

object LivesManager {

    const val MAX_LIVES = 5
    const val RECOVERY_TIME_MS = 3_600_000L // 1 hour

    private const val PREF_NAME = "mindmaze_lives"
    private const val KEY_LIVES = "lives_count"
    private const val KEY_LOST_TIMES = "lost_times"

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private fun getLostTimes(context: Context): MutableList<Long> {
        val str = getPrefs(context).getString(KEY_LOST_TIMES, "") ?: ""
        return if (str.isBlank()) mutableListOf()
        else str.split(",").mapNotNull { it.toLongOrNull() }.toMutableList()
    }

    private fun saveLostTimes(context: Context, times: List<Long>) {
        getPrefs(context).edit()
            .putString(KEY_LOST_TIMES, times.joinToString(","))
            .apply()
    }

    // Checks elapsed time and restores lives whose 1h timer has expired
    fun checkAndRestoreLives(context: Context) {
        val prefs = getPrefs(context)
        val now = System.currentTimeMillis()
        val lostTimes = getLostTimes(context)
        val toRestore = lostTimes.filter { now - it >= RECOVERY_TIME_MS }
        if (toRestore.isEmpty()) return

        val currentLives = prefs.getInt(KEY_LIVES, MAX_LIVES)
        val newLives = (currentLives + toRestore.size).coerceAtMost(MAX_LIVES)
        val remaining = lostTimes.filter { now - it < RECOVERY_TIME_MS }

        prefs.edit().putInt(KEY_LIVES, newLives).apply()
        saveLostTimes(context, remaining)
    }

    fun getLives(context: Context): Int {
        checkAndRestoreLives(context)
        return getPrefs(context).getInt(KEY_LIVES, MAX_LIVES)
    }

    fun loseLife(context: Context): Boolean {
        checkAndRestoreLives(context)
        val prefs = getPrefs(context)
        val current = prefs.getInt(KEY_LIVES, MAX_LIVES)
        if (current <= 0) return false

        val lostTimes = getLostTimes(context)
        lostTimes.add(System.currentTimeMillis())
        prefs.edit().putInt(KEY_LIVES, current - 1).apply()
        saveLostTimes(context, lostTimes)
        return true
    }

    fun hasLivesRemaining(context: Context): Boolean = getLives(context) > 0

    fun addLife(context: Context) {
        // Do NOT call checkAndRestoreLives here — video reward must add exactly 1 life
        val prefs = getPrefs(context)
        val current = prefs.getInt(KEY_LIVES, MAX_LIVES)
        if (current < MAX_LIVES) {
            prefs.edit().putInt(KEY_LIVES, current + 1).apply()
        }
    }

    // Returns milliseconds until the next life is restored (0 if no lives lost)
    fun getTimeToNextLife(context: Context): Long {
        val lostTimes = getLostTimes(context)
        if (lostTimes.isEmpty()) return 0L
        val oldest = lostTimes.minOrNull() ?: return 0L
        val elapsed = System.currentTimeMillis() - oldest
        return (RECOVERY_TIME_MS - elapsed).coerceAtLeast(0L)
    }
}
