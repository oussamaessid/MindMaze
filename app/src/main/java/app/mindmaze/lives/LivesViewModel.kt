package app.mindmaze.lives

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LivesViewModel(application: Application) : AndroidViewModel(application) {

    val lives = mutableStateOf(LivesManager.MAX_LIVES)
    val timeToNextLife = mutableStateOf(0L)

    init {
        refresh()
        startCountdown()
    }

    fun refresh() {
        val ctx = getApplication<Application>()
        lives.value = LivesManager.getLives(ctx)
        timeToNextLife.value = LivesManager.getTimeToNextLife(ctx)
    }

    fun loseLife(): Boolean {
        val ctx = getApplication<Application>()
        val lost = LivesManager.loseLife(ctx)
        refresh()
        return lost
    }

    fun addLife() {
        val ctx = getApplication<Application>()
        LivesManager.addLife(ctx)
        refresh()
    }

    private fun startCountdown() {
        viewModelScope.launch {
            while (true) {
                delay(1_000L)
                refresh()
            }
        }
    }
}
