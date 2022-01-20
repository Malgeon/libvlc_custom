package com.example.libvlc_custom.player.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.CancellationSignal
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.lang.Exception

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

    fun getFileBitmap(uri: Uri): Bitmap? {
        return ThumbnailUtils.createVideoThumbnail(
            uri.path!!,
            MediaStore.Images.Thumbnails.MINI_KIND
        )
    }

    fun getDocumentBitmap(
        context: Context,
        uri: Uri?,
        width: Int,
        height: Int,
        cancellationSignal: CancellationSignal?
    ): Bitmap? {
        return if (width < 0 || height < 0) {
            null
        } else try {
            DocumentsContract.getDocumentThumbnail(
                context.contentResolver,
                uri!!,
                Point(width, height),
                cancellationSignal
            )
        } catch (ignored: Exception) {
            null
        }
    }

    fun getBitmap(
        context: Context,
        uri: Uri
    ): Bitmap? {
        val schema = uri.scheme
        if ("file" == schema) {
            return getFileBitmap(uri)
        }
        return if ("content" == schema) {
            getDocumentBitmap(
                context,
                uri,
                500,
                500,
                null
            )
        } else null
    }

}