package com.masum.iptv.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.media.AudioManager
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import android.util.Rational
import android.view.Display
import android.view.View
import android.view.Window
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Format
import com.masum.iptv.BuildConfig
import com.masum.iptv.R
import com.masum.iptv.ui.player.CustomStyledPlayerView
import java.io.File
import java.text.DecimalFormat

const val  LINK="link"
const val REFERER="referer"
const val COOKIE="cookie"
const val USER_AGENT="userAgent"
const val USER_AGENT_VALUE="userAgentValue"

const val ORIGIN="origin"
const val DRM="drm"
const val SCHEME="scheme"

object GeneralUtils {

    fun getStoragePaths(context: Context): List<String> {
        return try {
            val paths: Array<File>? = ContextCompat.getExternalFilesDirs(context, null)
            paths?.map {
                it.path.replace("/Android/data/${context.packageName}/files", "")
            } ?: emptyList()
        } catch (ex: IllegalStateException) {
            emptyList()
        }
    }
    fun dpToPx(dp: Int): Float {
        return (dp * Resources.getSystem().displayMetrics.density)
    }
    fun pxToDp(px: Float): Float {
        return px / Resources.getSystem().displayMetrics.density
    }
    fun formatMilis(time: Long): String? {
        val totalSeconds = Math.abs(time.toInt() / 1000)
        val seconds = totalSeconds % 60
        val minutes = totalSeconds % 3600 / 60
        val hours = totalSeconds / 3600
        return if (hours > 0) String.format(
            "%d:%02d:%02d",
            hours,
            minutes,
            seconds
        ) else String.format("%02d:%02d", minutes, seconds)
    }

    fun formatMilisSign(time: Long): String? {
        return if (time > -1000 && time < 1000) formatMilis(time) else (if (time < 0) "−" else "+") + formatMilis(
            time
        )
    }

    fun isVolumeMax(audioManager: AudioManager): Boolean {
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == audioManager.getStreamMaxVolume(
            AudioManager.STREAM_MUSIC
        )
    }

    private fun isVolumeMin(audioManager: AudioManager): Boolean {
        val min = if (Build.VERSION.SDK_INT >= 28) audioManager.getStreamMinVolume(
            AudioManager.STREAM_MUSIC
        ) else 0
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == min
    }

    fun adjustVolume(
        audioManager: AudioManager,
        playerView: CustomStyledPlayerView,
        raise: Boolean,
        canBoost: Boolean,
         loudnessEnhancer: LoudnessEnhancer
    ) {
        var canBoost = canBoost
        val volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val volumeMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        var volumeActive = volume != 0
        var boostLevel=0

        // Handle volume changes outside the app (lose boost if volume is not maxed out)
        if (volume != volumeMax) {
        }
        if (loudnessEnhancer == null) canBoost = false
        if (volume != volumeMax || boostLevel == 0 && !raise) {
            if (loudnessEnhancer != null) loudnessEnhancer?.enabled =
                false
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                if (raise) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE
            )
            val volumeNew = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            if (raise && volume == volumeNew && !isVolumeMin(audioManager)) {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE or AudioManager.FLAG_SHOW_UI
                )
            } else {
                volumeActive = volumeNew != 0
                playerView.setCustomErrorMessage(if (volumeActive) "$volumeNew" else "")
                playerView.setVolumeProgress(volumeNew)
            }
        } else {
            if (canBoost && raise && boostLevel < 10) boostLevel++ else if (!raise && boostLevel > 0) boostLevel--
            if (loudnessEnhancer != null) {
                try {
                    loudnessEnhancer?.setTargetGain(boostLevel * 200)
                } catch (e: RuntimeException) {
                    e.printStackTrace()
                }
            }
            playerView.setCustomErrorMessage(" " + (volumeMax + boostLevel))
        }
        playerView.setIconVolume(volumeActive)

        if (loudnessEnhancer != null) loudnessEnhancer?.enabled =
            boostLevel > 0
        playerView.setHighlight(boostLevel > 0)
    }


    fun showText(playerView: CustomStyledPlayerView?, text: String?, timeout: Int) {
        playerView?.removeCallbacks(playerView.textClearRunnable)
        playerView?.clearIcon()
        playerView?.setCustomErrorMessage(text)
        playerView?.postDelayed(playerView.textClearRunnable, timeout.toLong())
    }
    fun setButtonEnabled(context: Context, button: ImageButton, enabled: Boolean) {
        button.isEnabled = enabled
        button.alpha =
            if (enabled) context.resources.getInteger(com.google.android.exoplayer2.ui.R.integer.exo_media_button_opacity_percentage_enabled)
                .toFloat() / 100 else context.resources.getInteger(com.google.android.exoplayer2.ui.R.integer.exo_media_button_opacity_percentage_disabled)
                .toFloat() / 100
    }

    enum class Orientation(val value: Int, val description: Int) {
        VIDEO(0, R.string.video_orientation_video),
        SENSOR(1, R.string.video_orientation_sensor);
    }

    fun getNextOrientation(orientation: Orientation?): Orientation {
        return when (orientation) {
            Orientation.VIDEO -> Orientation.SENSOR
            Orientation.SENSOR -> Orientation.VIDEO
            else -> Orientation.VIDEO
        }
    }

    fun showText(playerView: CustomStyledPlayerView?, text: String?) {
        showText(playerView, text, 1200)
    }

    fun setViewMargins(
        view: View,
        marginLeft: Int,
        marginTop: Int,
        marginRight: Int,
        marginBottom: Int
    ) {
        if (view.layoutParams is ConstraintLayout.LayoutParams){
            val layoutParams = view.layoutParams as ConstraintLayout.LayoutParams
            layoutParams.setMargins(marginLeft, marginTop, marginRight, marginBottom)
            view.layoutParams = layoutParams
        }
        if (view.layoutParams is FrameLayout.LayoutParams)
        {
            val layoutParams = view.layoutParams as FrameLayout.LayoutParams
            layoutParams.setMargins(marginLeft, marginTop, marginRight, marginBottom)
            view.layoutParams = layoutParams
        }
    }

    fun hideSystemUi(playerView: CustomStyledPlayerView) {

        // demo
//        playerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
//                | View.SYSTEM_UI_FLAG_FULLSCREEN
//                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }


    fun showSystemUi(playerView: CustomStyledPlayerView) {
        playerView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }
    fun setViewParams(
        view: View,
        paddingLeft: Int,
        paddingTop: Int,
        paddingRight: Int,
        paddingBottom: Int,
        marginLeft: Int,
        marginTop: Int,
        marginRight: Int,
        marginBottom: Int
    ) {
        view.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
        setViewMargins(
            view,
            marginLeft,
            marginTop,
            marginRight,
            marginBottom
        )
    }

    @SuppressLint("SourceLockedOrientationActivity")
    fun setOrientation(activity: Activity, orientation: Orientation,player: ExoPlayer) {
        when (orientation) {
            Orientation.VIDEO -> if (player != null) {
                val format: Format? = player!!.videoFormat
                if (format != null && isPortrait(format)) activity.requestedOrientation =
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT else activity.requestedOrientation =
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
            Orientation.SENSOR -> activity.requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_SENSOR
        }
    }



    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun handleFrameRate(activity: FragmentActivity, frameRate: Float, play: Boolean,player :ExoPlayer) {
        activity.runOnUiThread {
            var switchingModes = false
            if (BuildConfig.DEBUG) Toast.makeText(
                activity,
                "Video frameRate: $frameRate",
                Toast.LENGTH_LONG
            ).show()
            if (frameRate > 0) {
                val display: Display = activity.window.decorView.display
                    ?: return@runOnUiThread
                val supportedModes =
                    display.supportedModes
                val activeMode = display.mode
                if (supportedModes.size > 1) {
                    // Refresh rate >= video FPS
                    val modesHigh: MutableList<Display.Mode> =
                        ArrayList()
                    // Max refresh rate
                    var modeTop = activeMode
                    var modesResolutionCount = 0

                    // Filter only resolutions same as current
                    for (mode in supportedModes) {
                        if (mode.physicalWidth == activeMode.physicalWidth &&
                            mode.physicalHeight == activeMode.physicalHeight
                        ) {
                            modesResolutionCount++
                            if (normRate(mode.refreshRate) >= normRate(
                                    frameRate
                                )
                            ) modesHigh.add(mode)
                            if (normRate(mode.refreshRate) > normRate(
                                    modeTop.refreshRate
                                )
                            ) modeTop = mode
                        }
                    }
                    if (modesResolutionCount > 1) {
                        var modeBest: Display.Mode? = null
                        for (mode in modesHigh) {
                            if (normRate(mode.refreshRate) % normRate(
                                    frameRate
                                ) <= 0.0001f
                            ) {
                                if (modeBest == null || normRate(mode.refreshRate) > normRate(
                                        modeBest.refreshRate
                                    )
                                ) {
                                    modeBest = mode
                                }
                            }
                        }
                        val window: Window = activity.window
                        val layoutParams =
                            window.attributes
                        if (modeBest == null) modeBest = modeTop
                        switchingModes = modeBest?.modeId != activeMode.modeId
                        if (switchingModes) {
                            layoutParams.preferredDisplayModeId = modeBest?.modeId!!
                            window.attributes = layoutParams
                        }
                        if (BuildConfig.DEBUG) Toast.makeText(
                            activity,
                            """
                            Video frameRate: $frameRate
                            Display refreshRate: ${modeBest?.refreshRate}
                            """.trimIndent(),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            if (!switchingModes) {
                if (play) {
                    if (player != null) player?.play()
                    //   if (playerView != null) activity.playerView.hideController()
                }
            }
        }
    }


    fun getRational(format: Format): Rational {
        return if (isRotated(format)) Rational(
            format.height,
            format.width
        ) else Rational(format.width, format.height)
    }
    fun normRate(rate: Float): Int {
        return (rate * 100f).toInt()
    }


    fun isPortrait(format: Format): Boolean {
        return if (isRotated(format)) {
            format.width > format.height
        } else {
            format.height > format.width
        }
    }
    fun isRotated(format: Format): Boolean {
        return format.rotationDegrees == 90 || format.rotationDegrees == 270
    }


    fun isPiPSupported(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && context.packageManager.hasSystemFeature(
            PackageManager.FEATURE_PICTURE_IN_PICTURE
        )
    }
    fun getStringSizeLengthFile(path: String): String
    {
        val size=File(path).length()
        val df = DecimalFormat("0.00")
        val sizeKb = 1024.0f
        val sizeMb = sizeKb * sizeKb
        val sizeGb = sizeMb * sizeKb
        val sizeTerra = sizeGb * sizeKb
        return when {
            size < sizeMb -> df.format(size / sizeKb)
                .toString() + " Kb"
            size < sizeGb -> df.format(size / sizeMb)
                .toString() + " Mb"
            size < sizeTerra -> df.format(size / sizeGb)
                .toString() + " Gb"
            else -> ""
        }
    }
}