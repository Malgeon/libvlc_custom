package com.example.libvlc_custom

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.media.session.MediaControllerCompat
import com.example.libvlc_custom.player.services.MediaPlayerService
import com.example.libvlc_custom.player.services.MediaPlayerServiceBinder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        startService(Intent(applicationContext, MediaPlayerService::class.java))
    }
}