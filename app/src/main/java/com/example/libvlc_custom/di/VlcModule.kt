package com.example.libvlc_custom.di

import android.content.Context
import com.example.libvlc_custom.player.VlcMediaPlayer
import com.example.libvlc_custom.player.VlcOptionsProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.videolan.libvlc.LibVLC
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class VlcModule {

    @Provides
    @Singleton
    fun provideVlcOptionsProvider(
        @ApplicationContext context: Context
    ): ArrayList<String> =
        VlcOptionsProvider.Builder(context)
            .setVerbose(true)
            .withSubtitleEncoding("KOI8-R")
            .build()

    @Provides
    @Singleton
    fun provideLibVlcTest(
        @ApplicationContext context: Context,
        vlcOptionsProvider: ArrayList<String>,
    ): LibVLC {
        val appContext = context.applicationContext
        return LibVLC(appContext, vlcOptionsProvider)
    }


    @Provides
    @Singleton
    internal fun provideVlcMediaPlayer(libVlc: LibVLC): VlcMediaPlayer {
        return VlcMediaPlayer(libVlc)
    }
}
