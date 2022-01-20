package com.example.libvlc_custom.di

import android.content.Context
import com.example.libvlc_custom.player.VlcMediaPlayer
import dagger.Module
import dagger.Provides
import org.videolan.libvlc.LibVLC

@Module
class VlcModule {

    @Provides
    internal fun provideLibVlc(context: Context): LibVLC {
        val appContext = context.applicationContext

        val options = VlcOptionsProvider
            .getInstance()
            .options

        return if (options == null || options.size == 0)
        // No options provided, build defaults.
            LibVLC(appContext, VlcOptionsProvider.Builder(context).build())
        else
        // Use provided options.
            LibVLC(appContext, options)
    }

    @Provides
    internal fun provideVlcMediaPlayer(libVlc: LibVLC): com.masterwok.simplevlcplayer.contracts.VlcMediaPlayer {
        return VlcMediaPlayer(libVlc)
    }
}
