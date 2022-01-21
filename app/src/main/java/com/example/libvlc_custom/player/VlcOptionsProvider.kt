package com.example.libvlc_custom.player

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.util.Log
import org.videolan.libvlc.util.AndroidUtil
import org.videolan.libvlc.util.VLCUtil
import java.io.File
import java.util.ArrayList


class VlcOptionsProvider  {

    class Builder constructor(context: Context) {

        private var AudioTrackSessionId = 0

        private val keyStoreFile: File = File(
            context.getDir("keystore", Context.MODE_PRIVATE),
            "file"
        )
        private var withChromecastAudioPassthrough = false
        private var chromecastConversionQuality = 2

        private var subtitleEncoding = ""
        private var hasSubtitleBackground = false
        private var isSubtitleBold = false
        private var subtitleBackgroundOpacity = 128
        private var subtitleColor = 16777215
        private var subtitleSize = 16

        private var chroma = ""
        private var timeStretching = true
        private var withFrameSkip = false
        private var networkCaching = 0
        private var deblocking = -1
        private var openGl = -1

        private var isVerbose = false


        init {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager?.let {
                AudioTrackSessionId = it.generateAudioSessionId()
            }
        }

        fun withSubtitleBold(isBold: Boolean): Builder {
            isSubtitleBold = isBold
            return this
        }

        fun withSubtitleSize(size: Int): Builder {
            subtitleSize = size
            return this
        }

        fun withSubtitleColor(color: Int): Builder {
            subtitleColor = color
            return this
        }

        fun withSubtitleBackgroundOpacity(opacity: Int): Builder {
            hasSubtitleBackground = true

            // Keep in bounds (0-255)
            subtitleBackgroundOpacity =
                if (opacity < 0 || opacity > 255) subtitleBackgroundOpacity else opacity
            return this
        }

        fun withSubtitleBackground(hasBackground: Boolean): Builder {
            hasSubtitleBackground = hasBackground
            return this
        }

        fun withChromecastAudioPassthrough(withPassthrough: Boolean): Builder {
            withChromecastAudioPassthrough = withPassthrough
            return this
        }

        fun withChromecastConversionQuality(quality: Int): Builder {
            chromecastConversionQuality = quality
            return this
        }

        fun withTimeStretching(withTimeStretching: Boolean): Builder {
            timeStretching = withTimeStretching
            return this
        }

        fun withNetworkCaching(networkCaching: Int): Builder {
            var networkCaching = networkCaching
            if (networkCaching > 60000) {
                networkCaching = 60000
            } else if (networkCaching < 0) {
                networkCaching = 0
            }
            this.networkCaching = networkCaching
            return this
        }

        fun withDeblocking(deblocking: Int): Builder {
            this.deblocking = deblocking
            return this
        }

        fun withFrameSkip(frameSkip: Boolean): Builder {
            withFrameSkip = frameSkip
            return this
        }

        fun setVerbose(verbose: Boolean): Builder {
            isVerbose = verbose
            return this
        }

        fun withSubtitleEncoding(subtitleEncoding: String): Builder {
            this.subtitleEncoding = subtitleEncoding
            return this
        }

        fun withChroma(chroma: String): Builder {
            var chroma = chroma
            chroma = if (chroma == "YV12") "" else chroma
            this.chroma = chroma
            return this
        }

        fun withOpenGl(openGl: Int): Builder {
            this.openGl = openGl
            return this
        }

        fun build(): ArrayList<String> {
            val options = ArrayList<String>()
            options.add(if (isVerbose) "-vv" else "-v")
            if (hasSubtitleBackground) {
                options.add("--freetype-background-opacity=$subtitleBackgroundOpacity")
            } else {
                options.add("--freetype-background-opacity=0")
            }
            if (isSubtitleBold) {
                options.add("--freetype-bold")
            }
            options.add("--freetype-rel-fontsize=$subtitleSize")
            options.add("--freetype-color=$subtitleColor")
            if (withChromecastAudioPassthrough) {
                options.add("--sout-chromecast-audio-passthrough")
            } else {
                options.add("--no-sout-chromecast-audio-passthrough")
            }
            options.add("--sout-chromecast-conversion-quality=$chromecastConversionQuality")
            options.add("--sout-keep")
            options.add(
                if (timeStretching) "--audio-time-stretch" else "--no-audio-time-stretch"
            )
            if (networkCaching > 0) {
                options.add("--network-caching=$networkCaching")
            }
            if (openGl == 1) {
                options.add("--vOut=gles2,none")
            } else if (openGl == 0) {
                options.add("--vOut=android_display,none")
            }
            options.add("--avcodec-skiploopfilter")
            options.add("" + getDeblocking(deblocking))
            options.add("--avcodec-skip-frame")
            options.add(if (withFrameSkip) "2" else "0")
            options.add("--avcodec-skip-idct")
            options.add(if (withFrameSkip) "2" else "0")
            options.add("--subsdec-encoding")
            options.add(subtitleEncoding)
            options.add("--stats")
            options.add("--android-display-chroma")
            options.add(chroma)
            options.add("--audio-resampler")
            options.add(resampler)
            options.add("--audiotrack-session-id=" + AudioTrackSessionId)
            options.add("--keystore")
            if (AndroidUtil.isMarshMallowOrLater) {
                options.add("file_crypt,none")
            } else {
                options.add("file_plaintext,none")
            }
            options.add("--keystore-file")
            options.add(keyStoreFile.absolutePath)
            Log.e("Provider", keyStoreFile.absolutePath)
            return options
        }

        companion object {
            private var AudioTrackSessionId = 0
            private val resampler: String
                get() {
                    val m = VLCUtil.getMachineSpecs()
                    return if (m == null || m.processors > 2) "soxr" else "ugly"
                }

            private fun getDeblocking(deblocking: Int): Int {
                var ret = deblocking
                if (deblocking > 4) {
                    return 3
                }
                if (deblocking > 0) {
                    return deblocking
                }
                val machineSpecs = VLCUtil.getMachineSpecs() ?: return ret

                // Set some reasonable sDeblocking defaults:
                //
                // Skip all (4) for armv6 and MIPS by default
                // Skip non-ref (1) for all armv7 more than 1.2 Ghz and more than 2 cores
                // Skip non-key (3) for all devices that don't meet anything above
                ret = if (machineSpecs.hasArmV6 && !machineSpecs.hasArmV7 || machineSpecs.hasMips) {
                    4
                } else if (machineSpecs.frequency >= 1200 && machineSpecs.processors > 2) {
                    1
                } else if (machineSpecs.bogoMIPS >= 1200 && machineSpecs.processors > 2) {
                    1
                } else {
                    3
                }
                return ret
            }
        }
    }

}