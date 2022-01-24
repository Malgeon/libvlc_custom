package com.example.libvlc_custom.ui.player1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.libvlc_custom.databinding.FragmentPlayer1Binding
import com.example.libvlc_custom.player.MediaPlayer
import com.example.libvlc_custom.player.widget.PlayerControlLayout
import dagger.hilt.android.AndroidEntryPoint
import org.videolan.libvlc.interfaces.IVLCVout

@AndroidEntryPoint
class Player1Fragment : MediaPlayerServiceFragment()
    , PlayerControlLayout.Callback
    , MediaPlayer.Callback
    , IVLCVout.OnNewVideoLayoutListener{

    private lateinit var binding: FragmentPlayer1Binding

    override fun onServiceConnected() {
        TODO("Not yet implemented")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentPlayer1Binding.inflate(inflater, container, false)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onPlayPauseButtonClicked() {
        TODO("Not yet implemented")
    }

    override fun onCastButtonClicked() {
        TODO("Not yet implemented")
    }

    override fun onProgressChanged(progress: Int) {
        TODO("Not yet implemented")
    }

    override fun onProgressChangeStarted() {
        TODO("Not yet implemented")
    }

    override fun onSubtitlesButtonClicked() {
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

    override fun onNewVideoLayout(
        vlcVout: IVLCVout?,
        width: Int,
        height: Int,
        visibleWidth: Int,
        visibleHeight: Int,
        sarNum: Int,
        sarDen: Int
    ) {
        TODO("Not yet implemented")
    }
}