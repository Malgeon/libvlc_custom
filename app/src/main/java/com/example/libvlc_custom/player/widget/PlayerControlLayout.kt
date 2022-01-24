package com.example.libvlc_custom.player.widget

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import android.widget.SeekBar
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.Toolbar
import com.example.libvlc_custom.R

class PlayerControlLayout : RelativeLayout,
    SeekBar.OnSeekBarChangeListener {

    private val toolbarHeader: Toolbar
    private val toolbarFooter: Toolbar
    private val seekBarPosition: SeekBar
    private val textViewPosition: AppCompatTextView
    private val textViewLength: AppCompatTextView
    private val imageButtonPlayPause: AppCompatImageButton
    private val toolbarsAreVisible
    private val isTrackingTouch
    private val callback: Callback
    private var hasSelectedRenderer
    private var showSubtitleMenuItem

    init {

    }

    constructor(context: Context) : super(context) {
        inflate(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        readStyleAttributes(context, attrs)
    }

    constructor (context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        readStyleAttributes(context!!, attrs)
    }

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr) {
        readStyleAttributes(context!!, attrs)
        inflate(context)
    }

    interface Callback {
        fun onPlayPauseButtonClicked()

        fun onCastButtonClicked()

        fun onProgressChanged(progress: Int)

        fun onProgressChangeStarted()

        fun onSubtitlesButtonClicked()
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

    private fun inflate(context: Context) {
        inflate(context, R.layout.component_player_control, this)
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