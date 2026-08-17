package vn.loi.learning.android.autoplay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaStyleNotificationHelper
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import vn.loi.learning.android.MainActivity
import vn.loi.learning.android.R

@OptIn(UnstableApi::class)
class AutoPlayPlaybackService : MediaSessionService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var stateObservationJob: Job? = null
    private var muteObservationJob: Job? = null

    private lateinit var coordinator: AutoPlayRuntimeCoordinator
    private var mediaSession: MediaSession? = null
    private var fallbackPlayer: ExoPlayer? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        coordinator = AutoPlayRuntimeCoordinator.getInstance(this)

        val player = coordinator.exoPlayer ?: run {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                .setUsage(C.USAGE_MEDIA)
                .build()

            ExoPlayer.Builder(this)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .build().also { fallbackPlayer = it }
        }

        val forwardingPlayer = AutoPlayForwardingPlayer(player, coordinator)

        val isMuted = coordinator.isMuted.value
        val customLayout = listOf(buildMuteButton(isMuted), buildStopButton())
        val mediaButtonPrefs = buildMediaButtonPreferences(isMuted)

        val customCommandToggleMute = SessionCommand(COMMAND_AUTO_PLAY_TOGGLE_MUTE, Bundle.EMPTY)
        val customCommandStop = SessionCommand(COMMAND_AUTO_PLAY_STOP, Bundle.EMPTY)

        val callback = object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val connectionResult = super.onConnect(session, controller)
                val playerCommands = connectionResult.availablePlayerCommands.buildUpon()
                    .add(Player.COMMAND_PLAY_PAUSE)
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .add(Player.COMMAND_STOP)
                    .build()
                val sessionCommands = connectionResult.availableSessionCommands.buildUpon()
                    .add(customCommandToggleMute)
                    .add(customCommandStop)
                    .build()
                return MediaSession.ConnectionResult.accept(
                    sessionCommands,
                    playerCommands
                )
            }

            override fun onPostConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ) {
                super.onPostConnect(session, controller)
                val currentMuted = coordinator.isMuted.value
                session.setCustomLayout(listOf(buildMuteButton(currentMuted), buildStopButton()))
                session.setMediaButtonPreferences(buildMediaButtonPreferences(currentMuted))
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult> {
                when (customCommand.customAction) {
                    COMMAND_AUTO_PLAY_TOGGLE_MUTE -> {
                        coordinator.toggleMute()
                        val newMuted = coordinator.isMuted.value
                        session.setCustomLayout(listOf(buildMuteButton(newMuted), buildStopButton()))
                        session.setMediaButtonPreferences(buildMediaButtonPreferences(newMuted))
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    COMMAND_AUTO_PLAY_STOP -> {
                        coordinator.stop()
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                }
                return super.onCustomCommand(session, controller, customCommand, args)
            }

            override fun onPlaybackResumption(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo
            ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                return Futures.immediateFuture(MediaSession.MediaItemsWithStartPosition(emptyList(), 0, 0L))
            }

            @Deprecated("Deprecated in Java")
            override fun onPlayerCommandRequest(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                playerCommand: Int
            ): Int {
                when (playerCommand) {
                    Player.COMMAND_PLAY_PAUSE -> {
                        val state = coordinator.engineState.value
                        if (state is AutoPlayEngineState.Running) {
                            if (state.isPaused) coordinator.resume() else coordinator.pause()
                        }
                    }
                    Player.COMMAND_SEEK_TO_NEXT,
                    Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> {
                        coordinator.next()
                    }
                    Player.COMMAND_SEEK_TO_PREVIOUS,
                    Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> {
                        coordinator.previous()
                    }
                    Player.COMMAND_STOP -> {
                        coordinator.stop()
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
                return super.onPlayerCommandRequest(session, controller, playerCommand)
            }
        }

        val sessionActivityIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builtSession = MediaSession.Builder(this, forwardingPlayer)
            .setCallback(callback)
            .setCustomLayout(customLayout)
            .setMediaButtonPreferences(mediaButtonPrefs)
            .setSessionActivity(sessionActivityIntent)
            .build()
        mediaSession = builtSession

        observeCoordinatorState()
    }

    private fun buildMuteButton(isMuted: Boolean): CommandButton {
        val customCommandToggleMute = SessionCommand(COMMAND_AUTO_PLAY_TOGGLE_MUTE, Bundle.EMPTY)
        return CommandButton.Builder(if (isMuted) R.drawable.ic_autoplay_unmute else R.drawable.ic_autoplay_mute)
            .setDisplayName(if (isMuted) "Unmute" else "Mute")
            .setSessionCommand(customCommandToggleMute)
            .setSlots(CommandButton.SLOT_OVERFLOW)
            .setEnabled(true)
            .build()
    }

    private fun buildStopButton(): CommandButton {
        val customCommandStop = SessionCommand(COMMAND_AUTO_PLAY_STOP, Bundle.EMPTY)
        return CommandButton.Builder(R.drawable.ic_autoplay_stop)
            .setDisplayName("Stop")
            .setSessionCommand(customCommandStop)
            .setSlots(CommandButton.SLOT_OVERFLOW)
            .setEnabled(true)
            .build()
    }

    private fun buildMediaButtonPreferences(isMuted: Boolean): List<CommandButton> {
        return listOf(
            buildMuteButton(isMuted),
            buildStopButton()
        )
    }

    private fun observeCoordinatorState() {
        stateObservationJob?.cancel()
        stateObservationJob = serviceScope.launch {
            coordinator.engineState.collectLatest { state ->
                when (state) {
                    is AutoPlayEngineState.Running -> {
                        updateNotificationAndMetadata(state)
                    }
                    is AutoPlayEngineState.Completed -> {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                    is AutoPlayEngineState.Empty,
                    is AutoPlayEngineState.Idle -> {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
            }
        }

        muteObservationJob?.cancel()
        muteObservationJob = serviceScope.launch {
            coordinator.isMuted.collectLatest { currentMuted ->
                mediaSession?.setCustomLayout(listOf(buildMuteButton(currentMuted), buildStopButton()))
                mediaSession?.setMediaButtonPreferences(buildMediaButtonPreferences(currentMuted))
                val state = coordinator.engineState.value
                if (state is AutoPlayEngineState.Running) {
                    updateNotificationAndMetadata(state)
                }
            }
        }
    }

    private fun updateNotificationAndMetadata(state: AutoPlayEngineState.Running) {
        val item = state.item
        val isPaused = state.isPaused
        val isMuted = coordinator.isMuted.value

        serviceScope.launch {
            val artworkBytes = AutoPlayArtworkLoader.loadArtworkBytes(item.imagePath)

            val notification = buildMediaNotification(item, state.stage, state.currentIndex + 1, state.totalCount, isPaused, isMuted, artworkBytes)
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildMediaNotification(
        item: AutoPlayItem,
        stage: AutoPlayStage,
        index: Int,
        total: Int,
        isPaused: Boolean,
        isMuted: Boolean,
        artworkBytes: ByteArray?
    ): Notification {
        val session = mediaSession

        val activityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Dynamic Subtitle based on semantic playback stage
        val semanticSubtitle = when (stage) {
            AutoPlayStage.EXAMPLE_EN_AUDIO,
            AutoPlayStage.POST_EXAMPLE_EN_DELAY -> item.englishExample?.takeIf { it.isNotBlank() } ?: item.vietnameseMeaning

            AutoPlayStage.EXAMPLE_VI_AUDIO,
            AutoPlayStage.POST_EXAMPLE_VI_DELAY -> item.vietnameseExample?.takeIf { it.isNotBlank() } ?: item.vietnameseMeaning

            else -> item.vietnameseMeaning
        }

        // Custom intent actions for notification buttons
        val muteIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, AutoPlayPlaybackService::class.java).apply { action = ACTION_TOGGLE_MUTE },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val prevIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, AutoPlayPlaybackService::class.java).apply { action = ACTION_PREVIOUS },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val playPauseIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, AutoPlayPlaybackService::class.java).apply {
                action = if (isPaused) ACTION_RESUME else ACTION_PAUSE
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val nextIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, AutoPlayPlaybackService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this,
            4,
            Intent(this, AutoPlayPlaybackService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val subText = buildString {
            if (!item.partOfSpeech.isNullOrBlank()) {
                append("${item.partOfSpeech} • ")
            }
            append("$index / $total")
            val remainingSleep = coordinator.remainingSleepMillis.value
            if (remainingSleep != null && remainingSleep > 0L) {
                val mins = (remainingSleep + 59_999L) / 60_000L
                append(" • Sleep ${mins}m")
            }
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(item.headword)
            .setContentText(semanticSubtitle)
            .setSubText(subText)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(activityPendingIntent)
            .setDeleteIntent(stopIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(!isPaused)
            // Action 0: Mute/Unmute
            .addAction(
                if (isMuted) R.drawable.ic_autoplay_unmute else R.drawable.ic_autoplay_mute,
                if (isMuted) "Unmute" else "Mute",
                muteIntent
            )
            // Action 1: Previous
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevIntent)
            // Action 2: Play/Pause
            .addAction(
                if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
                if (isPaused) "Play" else "Pause",
                playPauseIntent
            )
            // Action 3: Next
            .addAction(android.R.drawable.ic_media_next, "Next", nextIntent)
            // Action 4: Stop
            .addAction(R.drawable.ic_autoplay_stop, "Stop", stopIntent)

        if (session != null) {
            builder.setStyle(
                MediaStyleNotificationHelper.MediaStyle(session)
                    .setShowActionsInCompactView(1, 2, 3) // Previous, Play/Pause, Next
            )
        }

        if (artworkBytes != null) {
            val bitmap = BitmapFactory.decodeByteArray(artworkBytes, 0, artworkBytes.size)
            builder.setLargeIcon(bitmap)
        }

        return builder.build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE_MUTE -> coordinator.toggleMute()
            ACTION_PAUSE -> coordinator.pause()
            ACTION_RESUME -> coordinator.resume()
            ACTION_NEXT -> coordinator.next()
            ACTION_PREVIOUS -> coordinator.previous()
            ACTION_STOP -> {
                coordinator.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Auto Play Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls and status for background Auto Play vocabulary playback"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stateObservationJob?.cancel()
        muteObservationJob?.cancel()
        serviceScope.cancel()
        mediaSession?.run {
            release()
            mediaSession = null
        }
        fallbackPlayer?.release()
        fallbackPlayer = null
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "autoplay_playback_channel"
        const val NOTIFICATION_ID = 2026

        const val ACTION_TOGGLE_MUTE = "vn.loi.learning.android.autoplay.ACTION_TOGGLE_MUTE"
        const val ACTION_PAUSE = "vn.loi.learning.android.autoplay.ACTION_PAUSE"
        const val ACTION_RESUME = "vn.loi.learning.android.autoplay.ACTION_RESUME"
        const val ACTION_NEXT = "vn.loi.learning.android.autoplay.ACTION_NEXT"
        const val ACTION_PREVIOUS = "vn.loi.learning.android.autoplay.ACTION_PREVIOUS"
        const val ACTION_STOP = "vn.loi.learning.android.autoplay.ACTION_STOP"

        const val COMMAND_AUTO_PLAY_TOGGLE_MUTE = "COMMAND_AUTO_PLAY_TOGGLE_MUTE"
        const val COMMAND_AUTO_PLAY_STOP = "COMMAND_AUTO_PLAY_STOP"

        fun startService(context: Context) {
            val intent = Intent(context, AutoPlayPlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, AutoPlayPlaybackService::class.java)
            context.stopService(intent)
        }
    }
}

@OptIn(UnstableApi::class)
class AutoPlayForwardingPlayer(
    player: Player,
    private val coordinator: AutoPlayRuntimeCoordinator
) : androidx.media3.common.ForwardingPlayer(player) {

    override fun getAvailableCommands(): Player.Commands {
        val base = super.getAvailableCommands()
        val builder = base.buildUpon()
            .add(Player.COMMAND_PLAY_PAUSE)
            .add(Player.COMMAND_STOP)

        val state = coordinator.engineState.value
        if (state is AutoPlayEngineState.Running) {
            if (coordinator.engine.hasPrevious) {
                builder.add(Player.COMMAND_SEEK_TO_PREVIOUS)
                builder.add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
            } else {
                builder.remove(Player.COMMAND_SEEK_TO_PREVIOUS)
                builder.remove(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
            }

            if (coordinator.engine.hasNext) {
                builder.add(Player.COMMAND_SEEK_TO_NEXT)
                builder.add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
            } else {
                builder.remove(Player.COMMAND_SEEK_TO_NEXT)
                builder.remove(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
            }
        }
        return builder.build()
    }

    override fun isCommandAvailable(command: Int): Boolean {
        return when (command) {
            Player.COMMAND_PLAY_PAUSE,
            Player.COMMAND_STOP -> true
            Player.COMMAND_SEEK_TO_PREVIOUS,
            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> {
                val state = coordinator.engineState.value
                state is AutoPlayEngineState.Running && coordinator.engine.hasPrevious
            }
            Player.COMMAND_SEEK_TO_NEXT,
            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> {
                val state = coordinator.engineState.value
                state is AutoPlayEngineState.Running && coordinator.engine.hasNext
            }
            else -> super.isCommandAvailable(command)
        }
    }

    override fun isPlaying(): Boolean {
        val state = coordinator.engineState.value
        return state is AutoPlayEngineState.Running && !state.isPaused
    }

    override fun getPlayWhenReady(): Boolean {
        val state = coordinator.engineState.value
        return state is AutoPlayEngineState.Running && !state.isPaused
    }

    override fun getPlaybackState(): Int {
        val state = coordinator.engineState.value
        return when (state) {
            is AutoPlayEngineState.Running -> Player.STATE_READY
            is AutoPlayEngineState.Completed -> Player.STATE_ENDED
            else -> Player.STATE_IDLE
        }
    }

    override fun seekToNext() {
        coordinator.next()
    }

    override fun seekToNextMediaItem() {
        coordinator.next()
    }

    override fun seekToPrevious() {
        coordinator.previous()
    }

    override fun seekToPreviousMediaItem() {
        coordinator.previous()
    }

    override fun play() {
        coordinator.resume()
    }

    override fun pause() {
        coordinator.pause()
    }

    override fun stop() {
        coordinator.stop()
    }
}
