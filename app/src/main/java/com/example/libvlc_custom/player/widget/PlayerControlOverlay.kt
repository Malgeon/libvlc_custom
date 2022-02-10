package com.example.libvlc_custom.player.widget

import android.animation.ObjectAnimator
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
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.libvlc_custom.R
import com.example.libvlc_custom.player.utils.ThreadUtils
import com.example.libvlc_custom.player.utils.TimeUtils
import com.example.libvlc_custom.player.utils.ViewUtils

class PlayerControlOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), OnSeekBarChangeListener {

    private val root: View
    private var toolbarsAreVisible = true
    private var isTrackingTouch = false
    private var hasSelectedRenderer = false
    private var showSubtitleMenuItem = false
    private var showSubtitle = ""
    private var handlerFlag = false
    private var isPlaying = false

    private val toolbarHeader: Toolbar
    private val seekBarPosition: SeekBar
    private lateinit var callback: Callback
    private val textViewPosition: AppCompatTextView
    private val overlayContainer: ConstraintLayout
    private val textViewLength: AppCompatTextView
    private val imageButtonPlayPause: AppCompatImageButton
    private val imageButtonFullScreen: AppCompatImageButton

    private val overlayHandler: Handler
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

        overlayHandler = Handler()

        toolbarHeader = root.findViewById(R.id.toolbar_header)
        overlayContainer = root.findViewById(R.id.overlay_container)
        seekBarPosition = root.findViewById(R.id.seekbar_position)
        textViewPosition = root.findViewById(R.id.textview_position)
        textViewLength = root.findViewById(R.id.textview_length)
        imageButtonPlayPause = root.findViewById(R.id.imagebutton_play_pause)
        imageButtonFullScreen = root.findViewById(R.id.imagebutton_fullscreen)

        seekBarPosition.setOnSeekBarChangeListener(this)
        imageButtonPlayPause.setOnClickListener {
            callback.onPlayPauseButtonClicked()
        }
        imageButtonFullScreen.setOnClickListener {
            callback.onFullScreenButtonClicked()
        }

        root.setOnClickListener {
            Log.e("OverLay", "Click!")
            overlayHandler.removeCallbacksAndMessages(null)
//            toggleToolbarVisibility()
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

    interface Callback {
        fun onPlayPauseButtonClicked()
        fun onFullScreenButtonClicked()
        fun onCastButtonClicked()
        fun onProgressChanged(progress: Int)
        fun onProgressChangeStarted()
        fun onSubtitlesButtonClicked()
    }

    /**
     * Toggle the visibility of the toolbars by slide animating them.
     */
    private fun toggleToolbarVisibility() {
        // User is sliding seek bar, do not modify visibility.
        if (isTrackingTouch) {
            return
        }
        if (toolbarsAreVisible) {
//            hideToolbars(
            hide()
            return
        }
        show()
//        showToolbars()
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

    /**
     * Hide header and footer toolbars by translating them off the screen vertically.
     */
    private fun hideToolbars() {
        // Already hidden, do nothing.
        if (!toolbarsAreVisible) {
            handlerFlag = false
            return
        }
        handlerFlag = false
        toolbarsAreVisible = false
        ThreadUtils.onMain {
            ViewUtils.fadeOutViewAboveOrBelowParent(overlayContainer)
        }
    }

    private fun isVisible(): Boolean {
        return visibility == VISIBLE
    }

    private fun isOverlayContainerVisible(): Boolean {
        return overlayContainer.visibility == VISIBLE
    }

    private fun hide() {
        if (isOverlayContainerVisible()) {
            overlayContainer.visibility = GONE
//            ViewUtils.fadeOutViewAboveOrBelowParent(overlayContainer)
            removeCallbacks(hideAction)
        }
        hideAtMs = TIME_UNSET
    }

    private fun show() {
        Log.e("show method", "on it")
        if (!isOverlayContainerVisible()) {
//            ThreadUtils.onMain {
//                ViewUtils.fadeInViewAboveOrBelowParent(overlayContainer)
//            }
            overlayContainer.visibility = VISIBLE
        }
        hideAfterTimeout()
    }

    private fun setShowTimeoutMs(showTimeoutMs: Int) {
        this.showTimeoutMs = showTimeoutMs
        if (isVisible()) {
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

    /**
     * Show header and footer toolbars by translating them vertically.
     */
    private fun showToolbars() {
        // Already shown, do nothing.
        if (toolbarsAreVisible) {
            return
        }
        ThreadUtils.onMain {
            toolbarsAreVisible = true
            ViewUtils.fadeInViewAboveOrBelowParent(overlayContainer)
        }
    }


    private fun startToolbarHideTimer() {
        if (!handlerFlag) {
            val timerDelay = 2500L
            handlerFlag = true
            handler.postDelayed(this::hideToolbars, timerDelay)
        }
    }

    private fun removeToolbarHideTimer() {
        if (handlerFlag) {
            handlerFlag = false
            handler.removeCallbacks(this::hideToolbars)
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
            if (time < 0 || length < 0) {
                seekBarPosition.progress = 0
                textViewPosition.text = null
                textViewLength.text = null
                return@onMain
            }
            seekBarPosition.progress = progress
            textViewPosition.text = positionText
            textViewLength.text = lengthText
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
        Log.e("Progress", "change")
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
        isTrackingTouch = true
        callback.onProgressChangeStarted()
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        isTrackingTouch = false
        callback.onProgressChanged(seekBar!!.progress)
    }
}