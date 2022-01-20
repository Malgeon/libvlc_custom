package com.example.libvlc_custom.player.services

import android.app.NotificationManager
import android.app.Service
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.KeyEvent
import android.view.SurfaceView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.libvlc_custom.player.MediaPlayer
import com.example.libvlc_custom.player.VlcMediaPlayer
import com.example.libvlc_custom.player.observables.RendererItemObservable
import com.example.libvlc_custom.player.utils.AudioUtils
import com.example.libvlc_custom.player.utils.NotificationUtils
import dagger.hilt.android.AndroidEntryPoint
import org.videolan.libvlc.Dialog
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.RendererItem
import org.videolan.libvlc.interfaces.IMedia
import org.videolan.libvlc.interfaces.IVLCVout
import java.lang.ref.WeakReference
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
    private var binder: MediaPlayerServiceBinder? = null

    private var audioManager: AudioManager? = null
    private var audioFocusChangeListener: WeakReference<AudioManager.OnAudioFocusChangeListener>? = null
    private var notificationManager: NotificationManager? = null
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
        notificationManager = NotificationUtils.getNotificationManager(applicationContext)
    }

    override fun onDestroy() {
        super.onDestroy()
    }


    override fun onBind(intent: Intent?): IBinder? {
        return binder
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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        NotificationUtils.makeNotificationChannel(
            MediaPlayerServiceChannelId,
            MediaPlayerServiceChannelName,
            notificationManager,
            mEnableSound = false,
            mEnableLights = false,
            mEnableVibration = false
        )
    }


    override fun onPlayerOpening() {
        TODO("Not yet implemented")
    }

    override fun onPlayerSeekStateChange(canSeek: Boolean) {
        TODO("Not yet implemented")
    }

    override fun onPlayerPlaying() {
        TODO("Not yet implemented")
    }

    override fun onPlayerPaused() {
        TODO("Not yet implemented")
    }

    override fun onPlayerStopped() {
        TODO("Not yet implemented")
    }

    override fun onPlayerEndReached() {
        TODO("Not yet implemented")
    }

    override fun onPlayerError() {
        TODO("Not yet implemented")
    }

    override fun onPlayerTimeChange(timeChanged: Long) {
        TODO("Not yet implemented")
    }

    override fun onBuffering(buffering: Float) {
        TODO("Not yet implemented")
    }

    override fun onPlayerPositionChanged(positionChanged: Float) {
        TODO("Not yet implemented")
    }

    override fun onSubtitlesCleared() {
        TODO("Not yet implemented")
    }

    override fun onDisplay(dialog: Dialog.ErrorMessage?) {
        TODO("Not yet implemented")
    }

    override fun onDisplay(dialog: Dialog.LoginDialog?) {
        TODO("Not yet implemented")
    }

    override fun onDisplay(dialog: Dialog.QuestionDialog?) {
        TODO("Not yet implemented")
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
            when (keyEvent.keyCode) {
                KeyEvent.KEYCODE_MEDIA_PLAY -> player?.play()
                KeyEvent.KEYCODE_MEDIA_PAUSE -> player?.pause()
                KeyEvent.KEYCODE_MEDIA_STOP -> player?.stop()
            }
            return true
        }

    }

    private fun setMediaBitmap(mediaUri: Uri): Bitmap {
        if (mediaBitmap != null) {
            return mediaBitmap as Bitmap
        }

        mediaBitmap = BitmapUtil.getBitmap(
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
        val schema = mediaUri.scheme
        setMediaBitmap(mediaUri)

        // Use file descriptor when dealing with content schemas.
        if (schema != null && schema == ContentResolver.SCHEME_CONTENT) {
            player?.setMedia(
                FileUtil.getUriFileDescriptor(
                    context.applicationContext,
                    mediaUri,
                    "r"
                )
            )
            return
        }
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