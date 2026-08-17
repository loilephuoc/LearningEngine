package vn.loi.learning.android.autoplay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Single authoritative coordinator for the active Auto Play session.
 * Shared between AutoPlayPlaybackService and foreground AutoPlayViewModel/Screen.
 */
class AutoPlayRuntimeCoordinator(
    val audioPlayer: AutoPlayAudioPlayer,
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
    private val clock: () -> Long = { SystemClock.elapsedRealtime() },
    val shuffleStrategy: AutoPlayShuffleStrategy = DefaultAutoPlayShuffleStrategy()
) {
    private val timerScheduler = CoroutineAutoPlayTimerScheduler(scope)
    val engine = AutoPlayEngine(audioPlayer, timerScheduler, clock, shuffleStrategy)

    val engineState: StateFlow<AutoPlayEngineState> = engine.state

    private val mutableConfig = MutableStateFlow(AutoPlayConfig())
    val config: StateFlow<AutoPlayConfig> = mutableConfig.asStateFlow()

    private val mutableIsMuted = MutableStateFlow(vn.loi.learning.android.media.LearningEngineAudioPolicy.isMuted.value)
    val isMuted: StateFlow<Boolean> = mutableIsMuted.asStateFlow()

    private val mutableSleepDeadline = MutableStateFlow<Long?>(null)
    val sleepDeadlineElapsed: StateFlow<Long?> = mutableSleepDeadline.asStateFlow()

    private val mutableRemainingSleepMillis = MutableStateFlow<Long?>(null)
    val remainingSleepMillis: StateFlow<Long?> = mutableRemainingSleepMillis.asStateFlow()

    private var sleepTimerJob: Job? = null
    private val sleepTimerToken = AtomicLong(0L)

    private var activeItems: List<AutoPlayItem> = emptyList()

    init {
        val initialMuted = vn.loi.learning.android.media.LearningEngineAudioPolicy.isMuted.value
        mutableIsMuted.value = initialMuted
        audioPlayer.setMuted(initialMuted)
        mutableConfig.value = mutableConfig.value.copy(isMuted = initialMuted)

        scope.launch {
            vn.loi.learning.android.media.LearningEngineAudioPolicy.isMuted.collect { muted ->
                if (mutableIsMuted.value != muted) {
                    mutableIsMuted.value = muted
                    audioPlayer.setMuted(muted)
                    if (mutableConfig.value.isMuted != muted) {
                        mutableConfig.value = mutableConfig.value.copy(isMuted = muted)
                    }
                }
            }
        }
    }

    val exoPlayer: ExoPlayer?
        get() = audioPlayer.exoPlayer

    fun updateConfig(newConfig: AutoPlayConfig) {
        mutableConfig.value = newConfig
        if (newConfig.isMuted != vn.loi.learning.android.media.LearningEngineAudioPolicy.isMuted.value) {
            setMuted(newConfig.isMuted)
        }
    }

    fun setMuted(muted: Boolean) {
        mutableIsMuted.value = muted
        audioPlayer.setMuted(muted)
        if (mutableConfig.value.isMuted != muted) {
            mutableConfig.value = mutableConfig.value.copy(isMuted = muted)
        }
        vn.loi.learning.android.media.LearningEngineAudioPolicy.setMuted(muted)
    }

    fun toggleMute() {
        val next = !mutableIsMuted.value
        setMuted(next)
    }

    fun setSleepTimerDurationMs(durationMs: Long?) {
        val token = sleepTimerToken.incrementAndGet()
        sleepTimerJob?.cancel()
        sleepTimerJob = null

        if (durationMs == null || durationMs <= 0L) {
            mutableSleepDeadline.value = null
            mutableRemainingSleepMillis.value = null
            return
        }

        val deadline = clock() + durationMs
        mutableSleepDeadline.value = deadline
        mutableRemainingSleepMillis.value = durationMs

        sleepTimerJob = scope.launch {
            while (isActive) {
                if (sleepTimerToken.get() != token) return@launch
                val remaining = deadline - clock()
                if (remaining <= 0L) {
                    mutableRemainingSleepMillis.value = 0L
                    mutableSleepDeadline.value = null
                    stop()
                    return@launch
                }
                mutableRemainingSleepMillis.value = remaining
                // Sleep short interval for responsive countdown
                delay(500L.coerceAtMost(remaining))
            }
        }
    }

    fun setSleepTimerMinutes(minutes: Double?) {
        val durationMs = minutes?.let { (it * 60_000.0).toLong() }
        setSleepTimerDurationMs(durationMs)
    }

    fun start(items: List<AutoPlayItem>, config: AutoPlayConfig, startIndex: Int = 0) {
        activeItems = items
        mutableConfig.value = config
        setMuted(config.isMuted)

        // Initialize sleep timer if configured
        val sleepDuration = config.sleepTimerDurationMs
        if (sleepDuration != null && sleepDuration > 0L) {
            setSleepTimerDurationMs(sleepDuration)
        } else {
            setSleepTimerDurationMs(null)
        }

        engine.start(items, config, startIndex)
    }

    fun pause() {
        engine.pause()
    }

    fun resume() {
        engine.resume()
    }

    fun next() {
        engine.next()
    }

    fun previous() {
        engine.previous()
    }

    fun replay() {
        engine.replay()
    }

    fun stop() {
        setSleepTimerDurationMs(null)
        engine.stop()
        activeItems = emptyList()
    }

    fun release() {
        stop()
        scope.cancel()
        if (audioPlayer is AutoCloseable) {
            audioPlayer.close()
        }
    }

    companion object {
        @Volatile
        private var instance: AutoPlayRuntimeCoordinator? = null

        fun getInstance(context: Context): AutoPlayRuntimeCoordinator {
            return instance ?: synchronized(this) {
                instance ?: run {
                    val appContext = context.applicationContext
                    val player = Media3AutoPlayAudioPlayer(appContext)
                    AutoPlayRuntimeCoordinator(player).also { instance = it }
                }
            }
        }

        fun getExistingInstance(): AutoPlayRuntimeCoordinator? = instance
    }
}

/**
 * Media3 ExoPlayer-backed implementation of AutoPlayAudioPlayer.
 * Handles audio attributes (SPEECH/MEDIA), audio focus, volume muting, and audio becoming noisy.
 */
@OptIn(UnstableApi::class)
class Media3AutoPlayAudioPlayer(
    private val context: Context
) : AutoPlayAudioPlayer, AutoCloseable {

    override var isMuted: Boolean = false
        private set

    override val exoPlayer: ExoPlayer by lazy {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .setUsage(C.USAGE_MEDIA)
            .build()

        ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus= */ true)
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                playWhenReady = true
                volume = if (isMuted) 0f else 1f
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) {
                            val cb = activeCallback
                            activeCallback = null
                            cb?.invoke()
                        }
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        val cb = activeCallback
                        activeCallback = null
                        cb?.invoke()
                    }
                })
            }
    }

    private var activeCallback: (() -> Unit)? = null

    override fun setMuted(muted: Boolean) {
        isMuted = muted
        try {
            exoPlayer.volume = if (muted) 0f else 1f
        } catch (_: Throwable) {}
    }

    override fun play(path: String?, onComplete: () -> Unit) {
        if (path.isNullOrBlank()) {
            onComplete()
            return
        }

        activeCallback = onComplete

        try {
            val file = File(path)
            val uri = if (file.exists()) Uri.fromFile(file) else Uri.parse(path)
            val mediaItem = MediaItem.fromUri(uri)
            exoPlayer.volume = if (isMuted) 0f else 1f
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
        } catch (_: Throwable) {
            activeCallback = null
            onComplete()
        }
    }

    override fun stop() {
        activeCallback = null
        try {
            if (exoPlayer.isPlaying) {
                exoPlayer.stop()
            }
        } catch (_: Throwable) {}
    }

    override fun close() {
        stop()
        try {
            exoPlayer.release()
        } catch (_: Throwable) {}
    }
}

object AutoPlayArtworkLoader {
    suspend fun loadArtworkBytes(imagePath: String?, maxDimension: Int = 512): ByteArray? = withContext(Dispatchers.IO) {
        if (imagePath.isNullOrBlank()) return@withContext null
        try {
            val file = File(imagePath)
            if (!file.exists()) return@withContext null

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)

            var sampleSize = 1
            while (options.outWidth / sampleSize > maxDimension || options.outHeight / sampleSize > maxDimension) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val bitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions) ?: return@withContext null

            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
            bitmap.recycle()
            stream.toByteArray()
        } catch (_: Throwable) {
            null
        }
    }
}
