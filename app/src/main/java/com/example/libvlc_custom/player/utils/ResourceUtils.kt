package com.example.libvlc_custom.player.utils

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat

object ResourceUtils {

    fun getDrawable(
        context: Context,
        id: Int
    ) : Drawable? {
        return ContextCompat.getDrawable(context, id)
    }
}