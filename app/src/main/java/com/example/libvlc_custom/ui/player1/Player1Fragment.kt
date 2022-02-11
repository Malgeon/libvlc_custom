package com.example.libvlc_custom.ui.player1

import android.annotation.TargetApi
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.libvlc_custom.R
import com.example.libvlc_custom.databinding.FragmentPlayer1Binding
import com.example.libvlc_custom.player.MediaPlayer
import com.example.libvlc_custom.player.utils.AndroidJob
import com.example.libvlc_custom.player.utils.ResourceUtil
import com.example.libvlc_custom.player.utils.SizePolicy
import com.example.libvlc_custom.player.widget.PlayerControlOverlay
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.videolan.libvlc.interfaces.IMedia
import org.videolan.libvlc.interfaces.IVLCVout

@AndroidEntryPoint
class Player1Fragment : MediaPlayerServiceFragment(), PlayerControlOverlay.Callback,
    MediaPlayer.Callback, IVLCVout.OnNewVideoLayoutListener {

    private val rtspUrl = TestUrl

    companion object {
        const val IsPlayingKey = "bundle.isplaying"
        const val LengthKey = "bundle.length"
        const val TimeKey = "bundle.time"
        const val TempUrl = ""
        const val TestUrl = "rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mp4"
        private const val UI_ANIMATION_DELAY = 300
    }

    private var sizePolicy: SizePolicy = SizePolicy.SURFACE_BEST_FIT
    private var mVideoHeight = 0
    private var mVideoWidth = 0
    private var mVideoVisibleHeight = 0
    private var mVideoVisibleWidth = 0
    private var mVideoSarNum = 0
    private var mVideoSarDen = 0
    private var resumeIsPlaying = true
    private var resumeLength: Long = 0
    private var resumeTime: Long = 0
    private var statusBarHeight = 0

    private val rootJob: AndroidJob = AndroidJob(lifecycle)
    private val handler = Handler()
    private val hideHandler = Handler()

    private val playerViewModel: PlayerViewModel by viewModels()

    private val surfaceLayoutListener = object : View.OnLayoutChangeListener {
        private val mRunnable = { updateVideoSurfaces() }

        override fun onLayoutChange(
            v: View,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
            oldLeft: Int,
            oldTop: Int,
            oldRight: Int,
            oldBottom: Int
        ) {
            if (left != oldLeft
                || top != oldTop
                || right != oldRight
                || bottom != oldBottom
            ) {
                handler.removeCallbacks(mRunnable)
                handler.post(mRunnable)
            }
        }
    }

    private lateinit var binding: FragmentPlayer1Binding

    private val becomingNoisyReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent.action) {
                // Pause playback whenever the user pulls out ( ͡° ͜ʖ ͡°)
                serviceBinder?.pause()
            }
        }
    }

    private fun configureSubtitleSurface() = binding.surfaceViewSubtitle.apply {
        setZOrderMediaOverlay(true)
        holder.setFormat(PixelFormat.TRANSLUCENT)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPlayer1Binding.inflate(inflater, container, false)
        statusBarHeight = getStatusBarHeight()
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subscribeToViewComponents()
        configureSubtitleSurface()
        playerViewModel.isFullScreen.observe(viewLifecycleOwner) {
            activeFullscreen(it)
            binding.componentPlayerControl.setFullscreen(it)
        }
    }

    private fun controlWindowInsets(hide: Boolean) {
        if (hide) {
            hideSystemUI()
        } else {
            showSystemUI()
        }
    }

    private val hideScreenRunnable = Runnable {
        binding.playerContainer.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    private val showScreenRunnable = Runnable {
        binding.toolbar.visibility = View.VISIBLE
        setMargin(statusBarHeight)
    }

    private fun hideSystemUI() {
        binding.toolbar.visibility = View.GONE
        setMargin(0)
        hideHandler.removeCallbacks(showScreenRunnable)
        hideHandler.postDelayed(hideScreenRunnable, UI_ANIMATION_DELAY.toLong())
    }

    private fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else {
            0
        }
    }

    private fun showSystemUI() {
        binding.playerContainer.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)

        hideHandler.removeCallbacks(hideScreenRunnable)
        hideHandler.postDelayed(showScreenRunnable, UI_ANIMATION_DELAY.toLong())
    }

    private fun setMargin(size: Int) {
        val param: FrameLayout.LayoutParams = FrameLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.MATCH_PARENT
        )
        param.topMargin = size
        binding.rootContainer.layoutParams = param
    }


    private fun activeFullscreen(flag: Boolean) {
        if (flag) {
            requireActivity().requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        }
    }

    private fun subscribeToViewComponents() {
        binding.componentPlayerControl.registerCallback(this)
    }

    override fun onServiceConnected() {
        serviceBinder?.callback = this
        startPlayback()
    }

    override fun openFullscreen() {
        val params: ConstraintLayout.LayoutParams =
            binding.playerContainer.layoutParams as ConstraintLayout.LayoutParams
        params.width = ConstraintLayout.LayoutParams.MATCH_PARENT
        params.height = ConstraintLayout.LayoutParams.MATCH_PARENT
        controlWindowInsets(hide = true)
    }

    override fun closeFullscreen() {
        val params: ConstraintLayout.LayoutParams =
            binding.playerContainer.layoutParams as ConstraintLayout.LayoutParams
        params.width = ConstraintLayout.LayoutParams.MATCH_PARENT
        params.height =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 300F, resources.displayMetrics)
                .toInt()
        controlWindowInsets(hide = false)
    }

    override fun onResume() {
        super.onResume()
        context?.registerReceiver(
            becomingNoisyReceiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        )
    }

    override fun onPause() {
        stopPlayback()
        serviceBinder?.callback = null
        context?.unregisterReceiver(becomingNoisyReceiver)
        super.onPause()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateVideoSurfaces()
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState.apply {
            putBoolean(IsPlayingKey, resumeIsPlaying)
            putLong(TimeKey, resumeTime)
            putLong(LengthKey, resumeLength)
        })
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        if (savedInstanceState == null) {
            return
        }
        resumeIsPlaying = savedInstanceState.getBoolean(IsPlayingKey, true)
        resumeTime = savedInstanceState.getLong(TimeKey, 0)
        resumeLength = savedInstanceState.getLong(LengthKey, 0)

        configure(
            resumeIsPlaying,
            resumeTime,
            resumeLength
        )
    }

    private fun updateResumeState() {
        val activity = activity ?: return

        val playbackState = MediaControllerCompat
            .getMediaController(activity)
            .playbackState

        resumeIsPlaying = playbackState.state == PlaybackStateCompat.STATE_PLAYING
        resumeTime = playbackState.position
        resumeLength = playbackState.bufferedPosition
    }

    private fun stopPlayback() {
        binding.surfaceViewSubtitle.removeOnLayoutChangeListener(surfaceLayoutListener)

        updateResumeState()
        serviceBinder?.stop()
        detachSurfaces()
    }

    private fun attachSurfaces() {

        if (serviceBinder?.vOut?.areViewsAttached() == true) {
            return
        }

        serviceBinder?.attachSurfaces(
            binding.surfaceViewMedia, binding.surfaceViewSubtitle, this
        )
    }

    private fun detachSurfaces() = serviceBinder?.detachSurfaces()

    private fun startPlayback() {

        binding.surfaceViewMedia.addOnLayoutChangeListener(surfaceLayoutListener)
        attachSurfaces()
        updateVideoSurfaces()

        serviceBinder?.setMedia(
            mContext, Uri.parse(rtspUrl)
        )

        if (resumeIsPlaying) {
            serviceBinder?.play()
        }
    }

    private fun configure(
        isPlaying: Boolean,
        time: Long,
        length: Long
    ) = binding.componentPlayerControl.configure(
        isPlaying,
        time,
        length
    )

    override fun configure(state: PlaybackStateCompat) = configure(
        state.state == PlaybackStateCompat.STATE_PLAYING,
        state.position,
        state.bufferedPosition
    )

    override fun onPlayPauseButtonClicked() {
        serviceBinder?.togglePlayback()
    }

    override fun onFullScreenButtonClicked() {
        val currentState = playerViewModel.isFullScreen.value
        manageFullscreen(currentState)
    }

    private fun manageFullscreen(flag: Boolean?) {
        flag?.let {
            playerViewModel.setFullscreenState(!it)
        } ?: playerViewModel.setFullscreenState(false)
    }

    override fun onCastButtonClicked() {

    }

    override fun onProgressChanged(progress: Int) {
        serviceBinder?.setProgress(progress)
        serviceBinder?.play()
    }

    override fun onProgressChangeStarted() {
        serviceBinder?.pause()
    }

    override fun onSubtitlesButtonClicked() {

    }

    override fun onPlayerOpening() {
        // Intentionally left blank..
    }

    override fun onPlayerSeekStateChange(canSeek: Boolean) {
        if (!canSeek) {
            return
        }

        serviceBinder?.setTime(resumeTime)
    }

    override fun onPlayerPlaying() {
        // Intentionally left blank..
    }

    override fun onPlayerPaused() {
        // Intentionally left blank..
    }

    override fun onPlayerStopped() {
        // Intentionally left blank..
    }

    override fun onPlayerEndReached() {
        activity?.finish()
    }

    override fun onPlayerError() {
        // Intentionally left blank..
    }

    override fun onPlayerTimeChange(timeChanged: Long) {
        // Intentionally left blank..
    }

    override fun onBuffering(buffering: Float) {
        if (buffering < 99) {
            binding.componentPlayerControl.setBuffering(true)
        } else {
            binding.componentPlayerControl.setBuffering(false)
        }
    }

    override fun onPlayerPositionChanged(positionChanged: Float) {
        // Intentionally left blank..
    }

    override fun onSubtitlesCleared() = startPlayback()

    private fun changeMediaPlayerLayout(displayW: Int, displayH: Int) {
        /* Change the video placement using the MediaPlayer API */
        when (sizePolicy) {
            SizePolicy.SURFACE_BEST_FIT -> {
                serviceBinder?.setAspectRatio(null)
                serviceBinder?.setScale(0f)
            }
            SizePolicy.SURFACE_FIT_SCREEN, SizePolicy.SURFACE_FILL -> {
                val videoTrack = serviceBinder?.currentVideoTrack ?: return
                val videoSwapped =
                    videoTrack.orientation == IMedia.VideoTrack.Orientation.LeftBottom || videoTrack.orientation == IMedia.VideoTrack.Orientation.RightTop
                if (sizePolicy == SizePolicy.SURFACE_FIT_SCREEN) {
                    var videoW = videoTrack.width
                    var videoH = videoTrack.height

                    if (videoSwapped) {
                        val swap = videoW
                        videoW = videoH
                        videoH = swap
                    }
                    if (videoTrack.sarNum != videoTrack.sarDen)
                        videoW = videoW * videoTrack.sarNum / videoTrack.sarDen

                    val ar = videoW / videoH.toFloat()
                    val dar = displayW / displayH.toFloat()

                    val scale: Float = if (dar >= ar)
                        displayW / videoW.toFloat() /* horizontal */
                    else
                        displayH / videoH.toFloat() /* vertical */

                    serviceBinder?.setScale(scale)
                    serviceBinder?.setAspectRatio(null)
                } else {
                    serviceBinder?.setScale(0f)
                    serviceBinder?.setAspectRatio(
                        if (!videoSwapped)
                            "$displayW:$displayH"
                        else
                            "$displayH:$displayW"
                    )
                }
            }
            SizePolicy.SURFACE_16_9 -> {
                serviceBinder?.setAspectRatio("16:9")
                serviceBinder?.setScale(0f)
            }
            SizePolicy.SURFACE_4_3 -> {
                serviceBinder?.setAspectRatio("4:3")
                serviceBinder?.setScale(0f)
            }
            SizePolicy.SURFACE_ORIGINAL -> {
                serviceBinder?.setAspectRatio(null)
                serviceBinder?.setScale(1f)
            }
        }
    }

    private fun updateVideoSurfaces() {
        if (activity == null || serviceBinder == null) {
            return
        }

        val decorView = requireActivity()
            .window
            .decorView

        val sw = decorView.width
        val sh = decorView.height

        // sanity check
        if (sw * sh == 0) {
            return
        }

        serviceBinder?.vOut?.setWindowSize(sw, sh)

        var lp = binding.surfaceViewMedia.layoutParams

        if (mVideoWidth * mVideoHeight == 0) {
            /* Case of OpenGL vouts: handles the placement of the video using MediaPlayer API */
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT
            lp.height = ViewGroup.LayoutParams.MATCH_PARENT
            binding.surfaceViewMedia.layoutParams = lp
            lp = binding.frameLayoutVideoSurface.layoutParams
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT
            lp.height = ViewGroup.LayoutParams.MATCH_PARENT
            binding.frameLayoutVideoSurface.layoutParams = lp
            changeMediaPlayerLayout(sw, sh)
            return
        }

        if (lp.width == lp.height && lp.width == ViewGroup.LayoutParams.MATCH_PARENT) {
            /* We handle the placement of the video using Android View LayoutParams */
            serviceBinder?.setAspectRatio(null)
            serviceBinder?.setScale(0f)
        }

        var dw = sw.toDouble()
        var dh = sh.toDouble()
        val isPortrait = ResourceUtil.deviceIsPortraitOriented(context)

        if (sw > sh && isPortrait || sw < sh && !isPortrait) {
            dw = sh.toDouble()
            dh = sw.toDouble()
        }

        // compute the aspect ratio
        var ar: Double
        val vw: Double
        if (mVideoSarDen == mVideoSarNum) {
            /* No indication about the density, assuming 1:1 */
            vw = mVideoVisibleWidth.toDouble()
            ar = mVideoVisibleWidth.toDouble() / mVideoVisibleHeight.toDouble()
        } else {
            /* Use the specified aspect ratio */
            vw = mVideoVisibleWidth * mVideoSarNum.toDouble() / mVideoSarDen
            ar = vw / mVideoVisibleHeight
        }

        // compute the display aspect ratio
        val dar = dw / dh

        when (sizePolicy) {
            SizePolicy.SURFACE_BEST_FIT -> if (dar < ar)
                dh = dw / ar
            else
                dw = dh * ar
            SizePolicy.SURFACE_FIT_SCREEN -> if (dar >= ar)
                dh = dw / ar /* horizontal */
            else
                dw = dh * ar /* vertical */
            SizePolicy.SURFACE_FILL -> {
            }
            SizePolicy.SURFACE_16_9 -> {
                ar = 16.0 / 9.0
                if (dar < ar)
                    dh = dw / ar
                else
                    dw = dh * ar
            }
            SizePolicy.SURFACE_4_3 -> {
                ar = 4.0 / 3.0
                if (dar < ar)
                    dh = dw / ar
                else
                    dw = dh * ar
            }
            SizePolicy.SURFACE_ORIGINAL -> {
                dh = mVideoVisibleHeight.toDouble()
                dw = vw
            }
        }

        // set display size
        lp.width = Math.ceil(dw * mVideoWidth / mVideoVisibleWidth).toInt()
        lp.height = Math.ceil(dh * mVideoHeight / mVideoVisibleHeight).toInt()
        binding.surfaceViewMedia.layoutParams = lp
        if (binding.surfaceViewSubtitle != null)
            binding.surfaceViewSubtitle.layoutParams = lp

        // set frame size (crop if necessary)
        lp = binding.frameLayoutVideoSurface.layoutParams
        lp.width = Math.floor(dw).toInt()
        lp.height = Math.floor(dh).toInt()
        binding.frameLayoutVideoSurface.layoutParams = lp

        binding.surfaceViewMedia.invalidate()
        if (binding.surfaceViewSubtitle != null)
            binding.surfaceViewSubtitle.invalidate()
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    override fun onNewVideoLayout(
        vOut: IVLCVout,
        width: Int,
        height: Int,
        visibleWidth: Int,
        visibleHeight: Int,
        sarNum: Int,
        sarDen: Int
    ) {
        mVideoWidth = width
        mVideoHeight = height
        mVideoVisibleWidth = visibleWidth
        mVideoVisibleHeight = visibleHeight
        mVideoSarNum = sarNum
        mVideoSarDen = sarDen
        updateVideoSurfaces()
    }
}