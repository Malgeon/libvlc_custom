package com.example.libvlc_custom.player.services

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.KeyEvent
import android.view.SurfaceView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media.session.MediaButtonReceiver
import com.example.libvlc_custom.R
import com.example.libvlc_custom.player.MediaPlayer
import com.example.libvlc_custom.player.VlcMediaPlayer
import com.example.libvlc_custom.player.observables.RendererItemObservable
import com.example.libvlc_custom.player.utils.*
import dagger.hilt.android.AndroidEntryPoint
import org.videolan.libvlc.Dialog
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.RendererItem
import org.videolan.libvlc.interfaces.IMedia
import org.videolan.libvlc.interfaces.IVLCVout
import java.lang.ref.WeakReference
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class MediaPlayerService : Service(), MediaPlayer.Callback, Dialog.Callbacks {

    @Inject
    @JvmField
    var libVlc: LibVLC? = null

    @Inject
    @JvmField
    var player: VlcMediaPlayer? = null

    var rendererItemObservable: RendererItemObservable? = null
    var mediaSession: MediaSessionCompat? = null
    var callback: MediaPlayer.Callback? = null

    private var binder: MediaPlayerServiceBinder? = null

    private var audioManager: AudioManager? = null
    private var audioFocusChangeListener: WeakReference<AudioManager.OnAudioFocusChangeListener>? =
        null
//    private var notificationManager: NotificationManager? = null
    private var stateBuilder: PlaybackStateCompat.Builder? = null
    private var mediaBitmap: Bitmap? = null
    private var defaultBitmap: Bitmap? = null

    private var lastUpdateTime = 0L

    val vOut: IVLCVout? get() = player?.vOut
    val currentVideoTrack: IMedia.VideoTrack? get() = player?.currentVideoTrack

    var selectedRendererItem: RendererItem?
        get() = player?.selectedRendererItem
        set(rendererItem) {
            // No need for local audio focus, abandon it.
            if (rendererItem != null) {
                abandonAudioFocus()
            }

            player?.detachSurfaces()
            player?.setRendererItem(rendererItem)

            sendRendererSelectedBroadcast(rendererItem)
        }

    var selectedSubtitleUri: Uri?
        get() = player?.selectedSubtitleUri
        set(subtitleUri) {
            player?.setSubtitleUri(subtitleUri)
        }

    val isPlaying: Boolean get() = player?.isPlaying == true

    override fun onCreate() {
        super.onCreate()


        audioFocusChangeListener = WeakReference(createAudioFocusListener())
        audioManager = AudioUtils.getAudioManager(applicationContext)

        Dialog.setCallbacks(libVlc, this)
        defaultBitmap = BitmapUtils.drawableToBitmap(
            ResourceUtils.getDrawable(
                applicationContext,
                R.drawable.ic_stream_cover
            )
        )

        binder = MediaPlayerServiceBinder(this)
//        notificationManager = NotificationUtils.getNotificationManager(applicationContext)
        stateBuilder = PlaybackStateCompat.Builder()
            .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_SEEK_TO)
            .setState(PlaybackStateCompat.STATE_PAUSED, 0L, 1f)

        createMediaSession()
//        createNotificationChannel()

        player?.callback = this

        libVlc?.let {
            rendererItemObservable = RendererItemObservable(it)
            rendererItemObservable?.start()
        }

        Log.e("MediaPlayerService", "onCreate")
    }

    override fun onDestroy() {
        Log.e("MediaPlayerService", "onDestroy")
        stopForeground(true)
        Dialog.setCallbacks(libVlc, null)
        player?.release()
        libVlc?.release()
        mediaSession?.release()
        rendererItemObservable?.stop()
        binder = null
        player = null
        libVlc = null
        mediaSession = null
        rendererItemObservable = null
        super.onDestroy()
    }

    override fun onStartCommand(
        intent: Intent?, flags: Int, startId: Int
    ): Int {
        // Pass notification button intents to the media session callback.
        MediaButtonReceiver.handleIntent(mediaSession, intent)

        return START_STICKY
    }


    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    private fun createMediaSession() {
        mediaSession = MediaSessionCompat(this, SimpleVlcSessionTag).apply {
            setMediaButtonReceiver(null)
            setCallback(PlayerSessionCallback())
            setPlaybackState(stateBuilder?.build())
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
        }
    }

    private fun abandonAudioFocus() {
        audioManager?.abandonAudioFocus(audioFocusChangeListener!!.get())
    }

    private fun sendRendererSelectedBroadcast(rendererItem: RendererItem?) {
        val intent =
            rendererItem?.let { Intent(RendererClearedAction) } ?: Intent(RendererSelectionAction)

        LocalBroadcastManager
            .getInstance(applicationContext)
            .sendBroadcast(intent)
    }

    private fun createAudioFocusListener(): AudioManager.OnAudioFocusChangeListener =
        AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> setVolume(100)
                AudioManager.AUDIOFOCUS_LOSS -> pause()
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->
                    // Lower volume, continue playing.
                    setVolume(50)
            }
        }

//    private fun createNotificationChannel() {
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
//            return
//        }
//
//        NotificationUtils.makeNotificationChannel(
//            MediaPlayerServiceChannelId,
//            MediaPlayerServiceChannelName,
//            notificationManager,
//            mEnableSound = false,
//            mEnableLights = false,
//            mEnableVibration = false
//        )
//    }

    private fun onPerformanceWarningDialog(questionDialog: Dialog.QuestionDialog) {
        // Let the user know casting will eat their battery.
        Toast.makeText(
            applicationContext,
            R.string.toast_casting_performance_warning,
            Toast.LENGTH_LONG
        ).show()

        // Accept and dismiss performance warning dialog.
        questionDialog.postAction(1)
        questionDialog.dismiss()
    }

    private fun onInsecureSiteDialog(questionDialog: Dialog.QuestionDialog) {
        if (questionDialog.action1Text.toLowerCase() == "view certificate") {
            questionDialog.postAction(1)
        } else if (questionDialog.action2Text.toLowerCase() == "accept permanently") {
            questionDialog.postAction(2)
        }

        questionDialog.dismiss()
    }

    private fun onBrokenOrMissingIndexDialog(questionDialog: Dialog.QuestionDialog) {
        // Let the user know seeking will not work properly.
        Toast.makeText(
            applicationContext,
            R.string.toast_missing_index_warning,
            Toast.LENGTH_LONG
        ).show()

        questionDialog.postAction(2)
        questionDialog.dismiss()
    }

    private fun enterForeground() {
        mediaSession?.setMetadata(
            getMediaMetadata(mediaBitmap)
        )

//        startForeground(
//            MediaPlayerServiceNotificationId,
//            buildPlaybackNotification()
//        )
    }

    private fun getMediaMetadata(bitmap: Bitmap?): MediaMetadataCompat {
        val builder = MediaMetadataCompat.Builder()

        builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)

        return builder.build()
    }

    private fun updatePlaybackState() {
        stateBuilder?.setBufferedPosition(player?.length ?: 0)
        stateBuilder?.setState(
            if (player?.isPlaying == true)
                PlaybackStateCompat.STATE_PLAYING
            else
                PlaybackStateCompat.STATE_PAUSED,
            player?.time ?: 0,
            1f
        )

        mediaSession?.setPlaybackState(stateBuilder?.build())
    }

//    private fun updateNotification() {
//        if (player?.selectedRendererItem == null) {
//            return
//        }
//
//        notificationManager?.notify(
//            MediaPlayerService.MediaPlayerServiceNotificationId,
//            buildPlaybackNotification()
//        )
//    }

//    private fun getPauseAction(context: Context): NotificationCompat.Action {
//        return NotificationCompat.Action(
//            R.drawable.ic_pause_black_36dp,
//            "Pause",
//            MediaButtonReceiver.buildMediaButtonPendingIntent(
//                context,
//                PlaybackStateCompat.ACTION_PAUSE
//            )
//        )
//    }
//
//    private fun getPlayAction(context: Context): NotificationCompat.Action {
//        return NotificationCompat.Action(
//            R.drawable.ic_play_arrow_black_36dp,
//            "Play",
//            MediaButtonReceiver.buildMediaButtonPendingIntent(
//                context,
//                PlaybackStateCompat.ACTION_PLAY
//            )
//        )
//    }
//
//    private fun getStopAction(context: Context): NotificationCompat.Action {
//        return NotificationCompat.Action(
//            R.drawable.ic_clear_black_36dp,
//            "Stop",
//            MediaButtonReceiver.buildMediaButtonPendingIntent(
//                context,
//                PlaybackStateCompat.ACTION_STOP
//            )
//        )
//    }
//
//    private fun buildPlaybackNotification(): Notification {
//        val context = applicationContext
//
//        val title = player
//            ?.media
//            ?.uri
//            ?.lastPathSegment
//            ?: ""
//
//        val builder = NotificationCompat.Builder(
//            context,
//            MediaPlayerService.MediaPlayerServiceChannelId
//        )
//
//        builder.setCategory(NotificationCompat.CATEGORY_SERVICE)
//            .setSmallIcon(R.drawable.ic_play_arrow_black_36dp)
//            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
//            .setLargeIcon(mediaBitmap)
//            .setContentTitle(title)
//            .setContentText(null)
//            .setTicker(title)
//            .setAutoCancel(false)
//            .setOngoing(true)
//
//        if (player?.isPlaying == true) {
//            builder.addAction(getPauseAction(context))
//        } else {
//            builder.addAction(getPlayAction(context))
//        }
//
//        builder.addAction(getStopAction(context))
//
//        builder.setStyle(
//            androidx.media.app.NotificationCompat.MediaStyle()
//                .setMediaSession(mediaSession!!.sessionToken)
//                .setShowActionsInCompactView(0, 1)
//        )
//        return builder.build()
//    }

    private fun gainAudioFocus() {
        // Only gain audio focus when playing locally.
        if (player?.selectedRendererItem != null) {
            return
        }

        AudioUtils.requestAudioFocus(
            audioManager!!,
            audioFocusChangeListener?.get()
        )
    }

    override fun onPlayerOpening() {
        lastUpdateTime = 0L

        updatePlaybackState()
//        updateNotification()
        callback?.onPlayerOpening()
    }

    override fun onPlayerSeekStateChange(canSeek: Boolean) {
        updatePlaybackState()
        callback?.onPlayerSeekStateChange(canSeek)
    }

    override fun onPlayerPlaying() {
        updatePlaybackState()
//        updateNotification()

        mediaSession?.isActive = true
        if (player?.selectedRendererItem != null) {
            enterForeground()
        }
        callback?.onPlayerPlaying()
    }

    override fun onPlayerPaused() {
        updatePlaybackState()
//        updateNotification()
        callback?.onPlayerPaused()
    }

    override fun onPlayerStopped() {
        updatePlaybackState()
//        updateNotification()
        mediaSession?.isActive = false
        stopForeground(true)
        callback?.onPlayerStopped()
    }

    override fun onPlayerEndReached() {
        updatePlaybackState()
//        updateNotification()
        mediaSession?.isActive = false
        callback?.onPlayerEndReached()
    }

    override fun onPlayerError() {
        updatePlaybackState()
//        updateNotification()
        mediaSession?.isActive = false
        callback?.onPlayerError()
    }

    override fun onPlayerTimeChange(timeChanged: Long) {
        val time = (player?.time ?: 0) / 1000L

        // At least one second has elapsed, update playback state.
        if (time >= lastUpdateTime + 1 || time <= lastUpdateTime) {
            updatePlaybackState()
            lastUpdateTime = time
        }
        callback?.onPlayerTimeChange(timeChanged)
    }

    override fun onBuffering(buffering: Float) {
        updatePlaybackState()
        callback?.onBuffering(buffering)
    }

    override fun onPlayerPositionChanged(positionChanged: Float) {
        callback?.onPlayerPositionChanged(positionChanged)
    }

    override fun onSubtitlesCleared() {
        callback?.onSubtitlesCleared()
    }

    override fun onDisplay(errorMessage: Dialog.ErrorMessage?) {
        Log.e("MediaPlayerService", "onDisplay")
    }

    override fun onDisplay(loginDialog: Dialog.LoginDialog?) {
        TODO("Not yet implemented")
    }

    override fun onDisplay(questionDialog: Dialog.QuestionDialog) {
        val dialogTitle = questionDialog.title ?: return

        when (dialogTitle.lowercase(Locale.getDefault())) {
            "broken or missing index" -> onBrokenOrMissingIndexDialog(questionDialog)
            "insecure site" -> onInsecureSiteDialog(questionDialog)
            "performance warning" -> onPerformanceWarningDialog(questionDialog)
            else -> Log.w(Tag, "Unhandled dialog: $dialogTitle")
        }
    }

    override fun onDisplay(dialog: Dialog.ProgressDialog?) {
        TODO("Not yet implemented")
    }

    override fun onCanceled(dialog: Dialog?) {
        TODO("Not yet implemented")
    }

    override fun onProgressUpdate(dialog: Dialog.ProgressDialog?) {
        TODO("Not yet implemented")
    }

    private inner class PlayerSessionCallback : MediaSessionCompat.Callback() {
        override fun onPlay() {
            player?.play()
        }

        override fun onPause() {
            player?.pause()
        }

        override fun onStop() {
            super.onStop()
        }

        override fun onMediaButtonEvent(mediaButtonEvent: Intent): Boolean {
            val action = mediaButtonEvent.action
            if (action == null || action != Intent.ACTION_MEDIA_BUTTON) {
                return false
            }
            val keyEvent = mediaButtonEvent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
            when (keyEvent?.keyCode) {
                KeyEvent.KEYCODE_MEDIA_PLAY -> player?.play()
                KeyEvent.KEYCODE_MEDIA_PAUSE -> player?.pause()
                KeyEvent.KEYCODE_MEDIA_STOP -> player?.stop()
                else -> {

                }
            }
            return true
        }

    }

    private fun setMediaBitmap(mediaUri: Uri): Bitmap {
        if (mediaBitmap != null) {
            return mediaBitmap as Bitmap
        }

        mediaBitmap = BitmapUtils.getBitmap(
            applicationContext,
            mediaUri
        )

        // Unable to get bitmap, use fallback bitmap.
        if (mediaBitmap == null) {
            mediaBitmap = defaultBitmap
        }

        return mediaBitmap!!
    }


    fun setMedia(
        context: Context?,
        mediaUri: Uri?
    ) {
        if (context == null || mediaUri == null) {
            return
        }
        setMediaBitmap(mediaUri)
        Log.e("MediaPlayerService", "setMedia")
        player?.setMedia(mediaUri)
    }

    fun play() {
        gainAudioFocus()
        player?.play()
    }

    fun stop() {
        abandonAudioFocus()
        player?.stop()
    }

    fun setTime(time: Long) {
        player?.time = time
    }

    fun setProgress(progress: Int) {
        player?.time = (progress.toFloat() / 100 * (player?.length ?: 0)).toLong()
    }

    fun togglePlayback() {
        if (player?.isPlaying == true) {
            pause()
            return
        }
        play()
    }

    fun pause() = player?.pause()

    fun setAspectRatio(aspectRatio: String?) = player?.setAspectRatio(aspectRatio)

    fun setScale(scale: Float) = player?.setScale(scale)

    fun setVolume(volume: Int) = player?.setVolume(volume)

    fun attachSurfaces(
        surfaceMedia: SurfaceView,
        surfaceSubtitle: SurfaceView,
        listener: IVLCVout.OnNewVideoLayoutListener
    ) = player?.attachSurfaces(
        surfaceMedia,
        surfaceSubtitle,
        listener
    )

    fun setSubtitle(subtitleUri: Uri?) = player?.setSubtitleUri(subtitleUri)

    fun detachSurfaces() = player?.detachSurfaces()

    companion object {
        private const val Tag = "MediaPlayerService"
        const val RendererClearedAction = "action.rendererclearedaction"
        const val RendererSelectionAction = "action.rendererselectionaction"
        private const val MediaPlayerServiceChannelName = "Media Player Service"
        private const val MediaPlayerServiceChannelId = "channel.mediaplayerservice"
        private const val SimpleVlcSessionTag = "tag.libvlcsession"
        private const val MediaPlayerServiceNotificationId = 1
    }
}