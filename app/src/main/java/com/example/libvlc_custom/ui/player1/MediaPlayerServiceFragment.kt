package com.example.libvlc_custom.ui.player1

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.IBinder
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.libvlc_custom.player.services.MediaPlayerService
import com.example.libvlc_custom.player.services.MediaPlayerServiceBinder

abstract class MediaPlayerServiceFragment : Fragment() {

    protected var isFullScreen = false

    protected var serviceBinder: MediaPlayerServiceBinder? = null
    private var mediaController: MediaControllerCompat? = null
    protected lateinit var mContext: Context

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(p0: ComponentName?) {
            Log.e("MediaService", "onServiceDisconnected")

            serviceBinder = null
        }

        override fun onServiceConnected(componentName: ComponentName?, binder: IBinder?) {
            serviceBinder = binder as? MediaPlayerServiceBinder
            Log.e("MediaService", "onServiceConnected")
            onServiceConnected()
            registerMediaController(serviceBinder)

        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Toast.makeText(mContext, "세로모드", Toast.LENGTH_SHORT).show()
            closeFullscreen()
        }

        if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Toast.makeText(mContext, "가로모드", Toast.LENGTH_SHORT).show()
            openFullscreen()
        }
    }


    protected abstract fun onServiceConnected()
    protected abstract fun openFullscreen()
    protected abstract fun closeFullscreen()
    protected abstract fun configure(state: PlaybackStateCompat)


    private fun bindMediaPlayerService(): Boolean {
        Log.e("MediaService", "bindMediaPlayerService")

        return requireActivity().bindService(
            Intent(mContext.applicationContext, MediaPlayerService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    private fun registerMediaController(serviceBinder: MediaPlayerServiceBinder?) {
        if (serviceBinder == null) {
            return
        }

        mediaController = MediaControllerCompat(
            mContext,
            serviceBinder.mediaSession!!
        ).apply {
            registerCallback(controllerCallback)
        }

        MediaControllerCompat.setMediaController(requireActivity(), mediaController)
    }

    private val controllerCallback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
            configure(state)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onStart() {
        super.onStart()
        bindMediaPlayerService()
    }

    override fun onStop() {
        activity?.unbindService(serviceConnection)

        super.onStop()
    }

}