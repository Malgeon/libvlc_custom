package com.example.libvlc_custom.player.widget

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import com.example.libvlc_custom.R

class PlayerControlOverlay : FrameLayout, OnSeekBarChangeListener {

    constructor(context: Context) : super(context)

    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        defStyleRes: Int = 0
    ) : super(context, attrs, defStyleAttr) {
        readStyleAttributes(context, attrs)
    }

    init {

    }


    private fun readStyleAttributes(context: Context, attrs: AttributeSet?) {
        if (attrs == null) {
            return
        }
        val styledAttributes = context.obtainStyledAttributes(
            attrs,
            R.styleable.PlayerControlComponent,
            0,
            0
        )
        hasSelectedRenderer = styledAttributes.getBoolean(
            R.styleable.PlayerControlComponent_showSubtitleMenuItem,
            false
        )
        showSubtitleMenuItem = styledAttributes.getBoolean(
            R.styleable.PlayerControlComponent_showSubtitleMenuItem,
            true
        )
        styledAttributes.recycle()
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        TODO("Not yet implemented")
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
        TODO("Not yet implemented")
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        TODO("Not yet implemented")
    }

}