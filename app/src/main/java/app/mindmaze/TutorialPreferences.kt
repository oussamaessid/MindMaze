package app.mindmaze

import android.content.Context
import android.content.SharedPreferences

object TutorialPreferences {
    private const val PREF_NAME = "mindmaze_preferences"
    private const val KEY_TUTORIAL_SHOWN = "tutorial_shown"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun isTutorialShown(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_TUTORIAL_SHOWN, false)
    }

    fun setTutorialShown(context: Context) {
        getPrefs(context).edit().putBoolean(KEY_TUTORIAL_SHOWN, true).apply()
    }
}