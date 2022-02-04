package com.example.libvlc_custom.player.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import androidx.annotation.RequiresApi

object AudioUtils {
    val getAudioManager: (context: Context) -> AudioManager = {
        it.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    fun requestAudioFocus(
        audioManager: AudioManager,
        listener: AudioManager.OnAudioFocusChangeListener?
    ): Boolean {
        val result: Int = audioManager.requestAudioFocus(
            listener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun requestAudioFocusOreo(
        audioManager: AudioManager
    ) {
        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
            setAudioAttributes(AudioAttributes.Builder().run {
                setUsage(AudioAttributes.USAGE_GAME)
                setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                build()
            })
            setAcceptsDelayedFocusGain(true)
//            setOnAudioFocusChangeListener(afChangeListener, handler)
            build()
        }
    }

}