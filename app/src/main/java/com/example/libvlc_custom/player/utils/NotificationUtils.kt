package com.example.libvlc_custom.player.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService

object NotificationUtils {

    val getNotificationManager: (context: Context) -> NotificationManager = {
        it.getSystemService() ?: throw Exception("Notification Manager not found")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun makeNotificationChannel(
        channelId: String,
        channelName: String,
        notificationManager: NotificationManager?,
        mEnableSound: Boolean,
        mEnableLights: Boolean,
        mEnableVibration: Boolean
    ) {
        notificationManager?.createNotificationChannel(
            NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                if (!mEnableSound) {
                    setSound(null, null)
                }
                enableLights(mEnableLights)
                enableVibration(mEnableVibration)
            }
        )
    }
}