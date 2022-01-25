package com.example.libvlc_custom.player.utils

import android.animation.ObjectAnimator
import android.view.View

object ViewUtils {

    fun fadeInViewAboveOrBelowParent(
        view: View,
        slideAbove: Boolean
    ) {
        val fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        fadeIn.duration = 1500
        fadeIn.start()
    }

    fun fadeOutViewAboveOrBelowParent(
        view: View,
    ) {
        val fadeOut = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f)
        fadeOut.duration = 1500
        fadeOut.start()
    }
}