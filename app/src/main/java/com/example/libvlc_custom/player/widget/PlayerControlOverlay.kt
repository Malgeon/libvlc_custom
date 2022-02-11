package com.example.libvlc_custom.player.widget

import android.content.Context
import android.os.Handler
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.libvlc_custom.R
import com.example.libvlc_custom.player.utils.ThreadUtils
import com.example.libvlc_custom.player.utils.TimeUtils

class PlayerControlOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), OnSeekBarChangeListener {

    private val root: View
    private var isTrackingTouch = false
    private var hasSelectedRenderer = false
    private var showSubtitleMenuItem = false
    private var showSubtitle = ""
    private var isPlaying = false
    private var isRealTime = false

    private val toolbarHeader: Toolbar
    private val seekBarPosition: SeekBar
    private lateinit var callback: Callback
    private val textViewPosition: AppCompatTextView
    private val overlayContainer: ConstraintLayout
    private val textViewLength: AppCompatTextView
    private val imageButtonPlayPause: AppCompatImageButton
    private val imageButtonFullScreen: AppCompatImageButton
    private val textViewDivide: AppCompatTextView
    private val imageRealtime: AppCompatImageView
    private val textRealtime: AppCompatTextView

    private val hideAction: Runnable
    private var hideAtMs: Long
    private var showTimeoutMs: Int
    private var controllerShowTimeoutMs: Int

    companion object {
        const val TIME_UNSET = Long.MIN_VALUE + 1
        const val DEFAULT_SHOW_TIMEOUT_MS = 2000
    }

    init {
        root = LayoutInflater.from(context)
            .inflate(R.layout.player_overlay, this)

        readStyleAttributes(context, attrs)

        toolbarHeader = root.findViewById(R.id.toolbar_header)
        overlayContainer = root.findViewById(R.id.overlay_container)
        seekBarPosition = root.findViewById(R.id.seekbar_position)
        textViewPosition = root.findViewById(R.id.textview_position)
        textViewLength = root.findViewById(R.id.textview_length)
        imageButtonPlayPause = root.findViewById(R.id.imagebutton_play_pause)
        imageButtonFullScreen = root.findViewById(R.id.imagebutton_fullscreen)
        textViewDivide = root.findViewById(R.id.position_divide)
        imageRealtime = root.findViewById(R.id.imageview_realtime)
        textRealtime = root.findViewById(R.id.textview_realtime)

        seekBarPosition.setOnSeekBarChangeListener(this)
        imageButtonPlayPause.setOnClickListener {
            callback.onPlayPauseButtonClicked()
        }
        imageButtonFullScreen.setOnClickListener {
            callback.onFullScreenButtonClicked()
        }

        root.setOnClickListener {
            handler.removeCallbacksAndMessages(null)
            toggleOverlayVisibility()
        }

        hideAction = Runnable { hide() }
        hideAtMs = TIME_UNSET
        showTimeoutMs = DEFAULT_SHOW_TIMEOUT_MS
        controllerShowTimeoutMs = DEFAULT_SHOW_TIMEOUT_MS
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        toolbarHeader.title = showSubtitle
    }

    private fun toggleOverlayVisibility() {
        if (!isOverlayContainerVisible()) {
            maybeShowController(true)
        } else {
            hide()
        }
    }

    private fun maybeShowController(isForced: Boolean) {
        val wasShowingIndefinitely = isOverlayContainerVisible() && showTimeoutMs <= 0
        val shouldShowIndefinitely = shouldShowControllerIndefinitely()
        if (isForced || wasShowingIndefinitely || shouldShowIndefinitely) {
            showController(shouldShowIndefinitely)
        }
    }

    private fun showController(showIndefinitely: Boolean) {
        setShowTimeoutMs(if (showIndefinitely) 0 else controllerShowTimeoutMs)
        show()
    }

    private fun shouldShowControllerIndefinitely(): Boolean {
        return !isPlaying
    }

    private fun isOverlayContainerVisible(): Boolean {
        return overlayContainer.visibility == VISIBLE
    }

    private fun hide() {
        if (isOverlayContainerVisible()) {
            overlayContainer.animate().alpha(0F).setDuration(500).start()
            overlayContainer.visibility = GONE
            removeCallbacks(hideAction)
        }
        hideAtMs = TIME_UNSET
    }

    private fun show() {
        if (!isOverlayContainerVisible()) {
            overlayContainer.visibility = VISIBLE
            overlayContainer.animate().alpha(1F).setDuration(500).start()
        }
        hideAfterTimeout()
    }

    private fun setShowTimeoutMs(showTimeoutMs: Int) {
        this.showTimeoutMs = showTimeoutMs
        if (isOverlayContainerVisible()) {
            hideAfterTimeout()
        }
    }

    private fun hideAfterTimeout() {
        removeCallbacks(hideAction)
        if (showTimeoutMs > 0) {
            hideAtMs = SystemClock.uptimeMillis() + showTimeoutMs
            if (isAttachedToWindow) {
                postDelayed(hideAction, showTimeoutMs.toLong())
            }
        } else {
            hideAtMs = TIME_UNSET
        }
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
        showSubtitle = styledAttributes.getString(
            R.styleable.PlayerControlComponent_showSubtitle
        ) ?: "초기화"
        styledAttributes.recycle()
    }

    private fun getPlayPauseDrawableResourceId(isPlaying: Boolean): Int {
        return if (isPlaying) R.drawable.ic_pause_white_36dp else R.drawable.ic_play_arrow_white_36dp
    }

    fun configure(
        isPlaying: Boolean,
        time: Long,
        length: Long
    ) {
        this.isPlaying = isPlaying
        maybeShowController(false)
        val lengthText: String = TimeUtils.getTimeString(length)
        val positionText: String = TimeUtils.getTimeString(time)
        val progress = (time.toFloat() / length * 100).toInt()
        ThreadUtils.onMain {
            imageButtonPlayPause.setImageResource(
                getPlayPauseDrawableResourceId(isPlaying)
            )
            if (time <= 0 || length <= 0) {
                setRealTime(true)
                return@onMain
            } else {
                setRealTime(false)
            }
            seekBarPosition.progress = progress
            textViewPosition.text = positionText
            textViewLength.text = lengthText
        }
    }

    private fun setRealTime(isRealTime: Boolean) {
        if (isRealTime xor this.isRealTime) {
            if(isRealTime) {
                seekBarPosition.visibility = GONE
                textViewPosition.visibility = GONE
                textViewDivide.visibility = GONE
                textViewLength.visibility = GONE
                imageRealtime.visibility = VISIBLE
                textRealtime.visibility = VISIBLE
            } else {
                seekBarPosition.visibility = VISIBLE
                textViewPosition.visibility = VISIBLE
                textViewDivide.visibility = VISIBLE
                textViewLength.visibility = VISIBLE
                imageRealtime.visibility = GONE
                textRealtime.visibility = GONE
            }
            this.isRealTime = isRealTime
        } else {
            return
        }
    }

    fun setFullscreen(isFullscreen: Boolean) {
        imageButtonFullScreen.setImageResource(
            getFullscreenDrawableResourceId(isFullscreen)
        )
    }

    private fun getFullscreenDrawableResourceId(isFullscreen: Boolean): Int {
        return if (isFullscreen) R.drawable.ic_fullscreen_exit_24 else R.drawable.ic_fullscreen_24
    }

    fun registerCallback(callback: Callback) {
        this.callback = callback
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        Log.i("onProgressChanged", "change")
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
        isTrackingTouch = true
        callback.onProgressChangeStarted()
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        isTrackingTouch = false
        callback.onProgressChanged(seekBar!!.progress)
    }

    interface Callback {
        fun onPlayPauseButtonClicked()
        fun onFullScreenButtonClicked()
        fun onCastButtonClicked()
        fun onProgressChanged(progress: Int)
        fun onProgressChangeStarted()
        fun onSubtitlesButtonClicked()
    }
}