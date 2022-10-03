package com.masum.iptv.ui.player

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.media.AudioManager
import android.os.Build
import android.util.AttributeSet
import android.view.*
import android.widget.TextView
import androidx.core.view.GestureDetectorCompat
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.SeekParameters
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.masum.iptv.MainActivity
import com.masum.iptv.R
import com.masum.iptv.utils.GeneralUtils.adjustVolume
import com.masum.iptv.utils.GeneralUtils.dpToPx
import com.masum.iptv.utils.GeneralUtils.formatMilisSign
import com.masum.iptv.utils.GeneralUtils.isVolumeMax
import com.masum.iptv.utils.GeneralUtils.pxToDp
import com.masum.iptv.utils.GeneralUtils.showText
import com.skydoves.progressview.ProgressView
import kotlin.math.abs

open class CustomStyledPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : StyledPlayerView(context, attrs, defStyleAttr), GestureDetector.OnGestureListener,
    ScaleGestureDetector.OnScaleGestureListener {
    private val mDetector: GestureDetectorCompat = GestureDetectorCompat(context, this)
    private var gestureOrientation = Orientation.UNKNOWN
    private var gestureScrollY = 0f
    private var gestureScrollX = 0f
    private var handleTouch = false
    private var seekStart: Long = 0
    private var seekChange: Long = 0
    private var seekMax: Long = 0
    private var canBoostVolume = false
    private var canSetAutoBrightness = false
    private val IGNORE_BORDER: Float = dpToPx(24)
    private val SCROLL_STEP: Float =   dpToPx(16)
    private val SCROLL_STEP_SEEK: Float =dpToPx(8)
    private val SEEK_STEP: Long = 1000
    private var restorePlayState = false
    private var canScale = true
    private var isHandledLongPress = false
    private val mScaleDetector: ScaleGestureDetector = ScaleGestureDetector(context, this)
    private var mScaleFactor = 1f
    private var mScaleFactorFit = 0f
    var systemGestureExclusionRect = Rect()
    val textClearRunnable = Runnable {
        setCustomErrorMessage(null)
        clearIcon()
    }
    private val mAudioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val exoErrorMessage: TextView = findViewById(com.google.android.exoplayer2.ui.R.id.exo_error_message)
    private val exoProgress: View = findViewById(R.id.exo_progress)
    private val volumeProgress:ProgressView=findViewById(R.id.volume_progress)
    private val brightnessProgress:ProgressView=findViewById(R.id.brightness_progress)
    fun clearIcon() {
        volumeProgress.visibility= GONE
        brightnessProgress.visibility= GONE
        exoErrorMessage.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        setHighlight(false)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {

        if (MainActivity.restoreControllerTimeout) {
            controllerShowTimeoutMs =MainActivity.CONTROLLER_TIMEOUT
            MainActivity.restoreControllerTimeout = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && gestureOrientation == Orientation.UNKNOWN) mScaleDetector.onTouchEvent(
            ev
        )
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> handleTouch =
                if (MainActivity.snackbar != null && MainActivity.snackbar!!.isShown) {
                    MainActivity.snackbar?.dismiss()
                    false
                } else {
                    removeCallbacks(textClearRunnable)
                    true
                }
            MotionEvent.ACTION_UP -> if (handleTouch) {
                if (gestureOrientation == Orientation.HORIZONTAL) {
                    setCustomErrorMessage(null)
                } else {
                    postDelayed(
                        textClearRunnable,
                        if (isHandledLongPress) MESSAGE_TIMEOUT_LONG.toLong() else MESSAGE_TIMEOUT_TOUCH.toLong()
                    )
                }
                if (restorePlayState) {
                    restorePlayState = false
                    MainActivity.player?.play()
                }
                controllerAutoShow = true
              
            }
        }
        if (handleTouch) mDetector.onTouchEvent(ev)

        // Handle all events to avoid conflict with internal handlers
        return true
    }

    override fun onDown(motionEvent: MotionEvent): Boolean {
        gestureScrollY = 0f
        gestureScrollX = 0f
        gestureOrientation = Orientation.UNKNOWN
        isHandledLongPress = false
        return false
    }

    override fun onShowPress(motionEvent: MotionEvent) {}
    override fun onSingleTapUp(motionEvent: MotionEvent): Boolean {
        return false
    }

    fun tap(): Boolean {
        if (MainActivity.locked) {
           showText(this, "Screen Locked", MESSAGE_TIMEOUT_LONG)
            setIconLock(true)
            return true
        }
        if (!MainActivity.controllerVisibleFully) {
            showController()
            return true
        } else if ( MainActivity.player != null && MainActivity.player?.isPlaying == true) {
            hideController()
            return true
        }
        return false
    }

    override fun onScroll(
        motionEvent: MotionEvent,
        motionEvent1: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {

        
        if (mScaleDetector.isInProgress || MainActivity.player == null || MainActivity.locked) return false

        // Exclude edge areas
        if (motionEvent.y < IGNORE_BORDER || motionEvent.x < IGNORE_BORDER || motionEvent.y > height - IGNORE_BORDER || motionEvent.x > width - IGNORE_BORDER) return false
        if (gestureScrollY == 0f || gestureScrollX == 0f) {
            gestureScrollY = 0.0001f
            gestureScrollX = 0.0001f
            return false
        }
        if (gestureOrientation == Orientation.HORIZONTAL || gestureOrientation == Orientation.UNKNOWN) {
            gestureScrollX += distanceX
            if (abs(gestureScrollX) > SCROLL_STEP || gestureOrientation == Orientation.HORIZONTAL && abs(
                    gestureScrollX
                ) > SCROLL_STEP_SEEK
            ) {
                // Do not show controller if not already visible
                controllerAutoShow = false
                if (gestureOrientation == Orientation.UNKNOWN) {
                    if (MainActivity.player?.isPlaying == true) {
                        restorePlayState = true
                        MainActivity.player?.pause()
                    }
                    clearIcon()
                    seekStart = MainActivity.player?.currentPosition ?:0
                    seekChange = 0L
                    seekMax = MainActivity.player?.duration ?:0
                }
                gestureOrientation = Orientation.HORIZONTAL
                val position: Long
                val distanceDiff =
                    0.5f.coerceAtLeast(abs(pxToDp(distanceX) / 4).coerceAtMost(10f))

                    if (gestureScrollX > 0) {
                        if (seekStart + seekChange - SEEK_STEP * distanceDiff >= 0) {
                            MainActivity.player?.setSeekParameters(SeekParameters.PREVIOUS_SYNC)
                            seekChange -= (SEEK_STEP * distanceDiff).toLong()
                            position = seekStart + seekChange
                            MainActivity.player?.seekTo(position)
                        }
                    } else {
                        MainActivity.player?.setSeekParameters(SeekParameters.NEXT_SYNC)
                        if (seekMax == C.TIME_UNSET) {
                            seekChange += (SEEK_STEP * distanceDiff).toLong()
                            position = seekStart + seekChange
                            MainActivity.player?.seekTo(position)
                        } else if (seekStart + seekChange + SEEK_STEP < seekMax) {
                            seekChange += (SEEK_STEP * distanceDiff).toLong()
                            position = seekStart + seekChange
                            MainActivity.player?.seekTo(position)
                        }
                    }
                    setCustomErrorMessage(formatMilisSign(seekChange))
                    gestureScrollX = 0.0001f

            }
        }

        // LEFT = Brightness  |  RIGHT = Volume
        if (gestureOrientation == Orientation.VERTICAL || gestureOrientation == Orientation.UNKNOWN) {
            gestureScrollY += distanceY
            if (abs(gestureScrollY) > SCROLL_STEP) {
                if (gestureOrientation == Orientation.UNKNOWN) {
                    canBoostVolume = isVolumeMax(mAudioManager)
                    canSetAutoBrightness = MainActivity.mBrightnessControl?.currentBrightnessLevel ?:  0<= 0
                }
                gestureOrientation = Orientation.VERTICAL
                if (motionEvent.x < (width / 2).toFloat()) {
                    MainActivity.mBrightnessControl?.changeBrightness(
                        this,
                        gestureScrollY > 0,
                        canSetAutoBrightness
                    )
                } else {
                adjustVolume(mAudioManager, this, gestureScrollY > 0, canBoostVolume,MainActivity.loudnessEnhancer!!)
                }
                gestureScrollY = 0.0001f
            }
        }
        return true
    }

    override fun onLongPress(motionEvent: MotionEvent) {  if (MainActivity.locked || player != null && player!!.isPlaying) {
        MainActivity.locked = !MainActivity.locked
        isHandledLongPress = true
        showText(this, "", MESSAGE_TIMEOUT_LONG)
        setIconLock(MainActivity.locked)
        if (MainActivity.locked && MainActivity.controllerVisible) {
            hideController()
        }
    }

    }

    override fun onFling(
        motionEvent: MotionEvent,
        motionEvent1: MotionEvent,
        v: Float,
        v1: Float
    ): Boolean {
        return false
    }

    override fun onScale(scaleGestureDetector: ScaleGestureDetector): Boolean {
        if (MainActivity.locked) return false
        if (canScale) {
            val previousScaleFactor = mScaleFactor
            mScaleFactor *= scaleGestureDetector.scaleFactor
            mScaleFactor = 0.25f.coerceAtLeast(mScaleFactor.coerceAtMost(2.0f))
            if (isCrossingThreshold(previousScaleFactor, mScaleFactor, 1.0f) ||
                isCrossingThreshold(previousScaleFactor, mScaleFactor, mScaleFactorFit)
            ) performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY
            )
            setScale(mScaleFactor)
            restoreSurfaceView()
            clearIcon()
            setCustomErrorMessage("${(mScaleFactor * 100).toInt()}  %")
            return true
        }
        return false
    }

    private fun isCrossingThreshold(val1: Float, val2: Float, threshold: Float): Boolean {
        return val1 < threshold && val2 >= threshold || val1 > threshold && val2 <= threshold
    }

    override fun onScaleBegin(scaleGestureDetector: ScaleGestureDetector): Boolean {
        if (MainActivity.locked) return false
        mScaleFactor = videoSurfaceView!!.scaleX
        if (resizeMode != AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
            canScale = false
            setAspectRatioListener { targetAspectRatio: Float, naturalAspectRatio: Float, aspectRatioMismatch: Boolean ->
                setAspectRatioListener(null)
                mScaleFactorFit = scaleFit
                mScaleFactor = mScaleFactorFit
                canScale = true
            }
            videoSurfaceView!!.alpha = 0f
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        } else {
            mScaleFactorFit = scaleFit
            canScale = true
        }
        return true
    }

    override fun onScaleEnd(scaleGestureDetector: ScaleGestureDetector) {
        if (MainActivity.locked) return
        restoreSurfaceView()
    }

    private fun restoreSurfaceView() {
        if (videoSurfaceView!!.alpha != 1f) {
            videoSurfaceView!!.alpha = 1f
        }
    }

    private val scaleFit: Float
        private get() = (height.toFloat() / videoSurfaceView!!.height.toFloat()).coerceAtMost(width.toFloat() / videoSurfaceView!!.width.toFloat())

    private enum class Orientation {
        HORIZONTAL, VERTICAL, UNKNOWN
    }

    fun setIconVolume(volumeActive: Boolean) {
        volumeProgress.visibility= VISIBLE
        exoErrorMessage.setCompoundDrawablesWithIntrinsicBounds(
            if (volumeActive) R.drawable.ic_volume_up_24dp else R.drawable.ic_volume_off_24dp, 0, 0, 0)
    }

    fun setHighlight(active: Boolean) {
        if (active) exoErrorMessage.setTextColor(Color.RED) else exoErrorMessage.setTextColor(Color.WHITE)
    }

    fun setIconBrightness() {
        exoErrorMessage.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.ic_brightness_medium_24,
            0,
            0,
            0
        )
    }

    fun setIconBrightnessAuto() {
        exoErrorMessage.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.ic_brightness_auto_24dp,
            0,
            0,
            0
        )
    }

    private fun setIconLock(locked: Boolean) {
        exoErrorMessage.setCompoundDrawablesWithIntrinsicBounds(
            if (locked) R.drawable.ic_lock_24dp else R.drawable.ic_lock_open_24dp,
            0,
            0,
            0
        )
    }

    fun setScale(scale: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val videoSurfaceView = videoSurfaceView
            videoSurfaceView!!.scaleX = scale
            videoSurfaceView.scaleY = scale
            //videoSurfaceView.animate().setStartDelay(0).setDuration(0).scaleX(scale).scaleY(scale).start();
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (Build.VERSION.SDK_INT >= 29) {
            exoProgress.getGlobalVisibleRect(systemGestureExclusionRect)
            systemGestureExclusionRect.left = left
            systemGestureExclusionRect.right = right
            systemGestureExclusionRects = listOf(systemGestureExclusionRect)
        }
    }

    fun setVolumeProgress(volumeNew: Int) {
        volumeProgress.progress=volumeNew.toFloat()
    }
 fun setBrightnessProgress(volumeNew: Int) {
     brightnessProgress.visibility= VISIBLE
        brightnessProgress.progress=volumeNew.toFloat()
    }

    companion object {
        const val MESSAGE_TIMEOUT_TOUCH = 400
        const val MESSAGE_TIMEOUT_KEY = 800
        const val MESSAGE_TIMEOUT_LONG = 1400
    }

    init {

        exoErrorMessage.setOnClickListener { v: View? ->
                if (MainActivity.locked) {
                    MainActivity.locked = false
                   showText(this@CustomStyledPlayerView, "", MESSAGE_TIMEOUT_LONG)
                    setIconLock(false)

            }
        }
    }
}