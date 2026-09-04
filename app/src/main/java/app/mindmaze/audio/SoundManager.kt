package app.mindmaze.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.media.ToneGenerator

/**
 * Single shared low-latency SFX player, reused for every tap so we never spin up a new
 * player per touch. Looks for res/raw/boom|error|success (mp3/ogg/wav) at runtime via
 * getIdentifier — if a clip isn't bundled, it falls back to a short ToneGenerator beep so
 * the app still gives audio feedback and always compiles even without real audio assets.
 */
class SoundManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val soundPool: SoundPool
    private val soundIds = mutableMapOf<String, Int>()
    private val loadedIds = mutableSetOf<Int>()
    private var toneGenerator: ToneGenerator? = null

    init {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(attributes)
            .build()

        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) loadedIds += sampleId
        }

        listOf("boom", "error", "success", "drum_hit").forEach { name ->
            val resId = appContext.resources.getIdentifier(name, "raw", appContext.packageName)
            if (resId != 0) {
                soundIds[name] = soundPool.load(appContext, resId, 1)
            }
        }

        toneGenerator = try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 85)
        } catch (_: RuntimeException) {
            null
        }
    }

    fun playBoom() = play("boom", ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 160)

    fun playError() = play("error", ToneGenerator.TONE_CDMA_PIP, 220)

    fun playSuccess() = play("success", ToneGenerator.TONE_PROP_BEEP2, 140)

    /** Short, low-latency celebration hit. Falls back to a tone if the sample is unavailable. */
    fun playDrumHit() = play("drum_hit", ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 180)

    private fun play(name: String, fallbackTone: Int, fallbackDurationMs: Int) {
        val id = soundIds[name]
        if (id != null && id in loadedIds) {
            soundPool.play(id, 1f, 1f, 1, 0, 1f)
        } else {
            toneGenerator?.startTone(fallbackTone, fallbackDurationMs)
        }
    }

    private fun releaseInternal() {
        soundPool.release()
        toneGenerator?.release()
        toneGenerator = null
    }

    companion object {
        @Volatile private var instance: SoundManager? = null

        fun get(context: Context): SoundManager =
            instance ?: synchronized(this) {
                instance ?: SoundManager(context).also { instance = it }
            }

        /** Call once, from the process' single Activity's onDestroy(). */
        fun release() {
            synchronized(this) {
                instance?.releaseInternal()
                instance = null
            }
        }
    }
}
