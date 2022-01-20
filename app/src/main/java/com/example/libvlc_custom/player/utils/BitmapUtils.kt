package com.example.libvlc_custom.player.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

object BitmapUtils {

    fun drawableToBitmap(drawable: Drawable?): Bitmap {
        if (drawable != null) {
            if (drawable is BitmapDrawable) {
                return drawable.bitmap
            }

            val bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )

            val canvas = Canvas(bitmap)

            drawable.setBounds(
                0,
                0,
                canvas.width,
                canvas.height
            )
            drawable.draw(canvas)
            return bitmap
        } else {
            return Bitmap.createBitmap(
                0, 0, Bitmap.Config.ARGB_8888
            )
        }
    }

}