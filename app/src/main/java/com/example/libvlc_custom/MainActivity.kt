package com.example.libvlc_custom

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.media.session.MediaControllerCompat
import com.example.libvlc_custom.player.services.MediaPlayerServiceBinder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private var mediaController: MediaControllerCompat? = null
    private var mediaPlayerServiceBinder: MediaPlayerServiceBinder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}