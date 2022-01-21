package com.example.libvlc_custom.di

import android.content.Context
import com.example.libvlc_custom.player.VlcMediaPlayer
import com.example.libvlc_custom.player.VlcOptionsProvider
import dagger.Module
import dagger.Provides
import org.videolan.libvlc.LibVLC
import javax.inject.Singleton

@Module
class VlcModule {

    @Provides
    @Singleton
    fun provideVlcOptionsProvider(
        context: Context
    ): ArrayList<String> =
        VlcOptionsProvider.Builder(context)
            .setVerbose(true)
            .withSubtitleEncoding("KOI8-R")
            .build()

    @Provides
    @Singleton
    fun provideLibVlcTest(
        context: Context,
        vlcOptionsProvider: ArrayList<String>,
    ): LibVLC {
        val appContext = context.applicationContext
        return LibVLC(appContext, vlcOptionsProvider)
    }


    @Provides
    internal fun provideVlcMediaPlayer(libVlc: LibVLC): com.masterwok.simplevlcplayer.contracts.VlcMediaPlayer {
        return VlcMediaPlayer(libVlc)
    }
}
