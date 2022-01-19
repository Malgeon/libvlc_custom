package com.example.libvlc_custom.player.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.libvlc_custom.player.MediaPlayer
import com.example.libvlc_custom.player.VlcMediaPlayer
import dagger.hilt.android.AndroidEntryPoint
import org.videolan.libvlc.Dialog
import org.videolan.libvlc.LibVLC
import javax.inject.Inject

@AndroidEntryPoint
class MediaPlayerService : Service(), MediaPlayer.Callback, Dialog.Callbacks {

    @Inject
    @JvmField
    var libVlc: LibVLC? = null

    @Inject
    @JvmField
    var player: VlcMediaPlayer? = null


    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
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