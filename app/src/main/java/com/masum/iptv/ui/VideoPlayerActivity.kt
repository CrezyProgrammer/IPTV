package com.masum.iptv.ui

import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.drawable.Icon
import android.hardware.display.DisplayManager
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.*
import com.masum.iptv.R
import android.support.v4.media.session.MediaSessionCompat
import android.util.DisplayMetrics
import android.util.Log
import android.util.Rational
import android.view.SurfaceView
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.webkit.*
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.ghdsports.india.ui.player.CustomDefaultTrackNameProvider
import com.ghdsports.india.ui.player.Prefs
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.drm.DrmSession
import com.google.android.exoplayer2.drm.DrmSessionManager
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.extractor.ts.DefaultTsPayloadReaderFactory
import com.google.android.exoplayer2.extractor.ts.TsExtractor
import com.google.android.exoplayer2.offline.DownloadHelper
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector.SelectionOverride
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.StyledPlayerControlView
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.ui.TimeBar
import com.google.android.exoplayer2.ui.TimeBar.OnScrubListener
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.EventLogger
import com.google.android.material.snackbar.Snackbar
import com.masum.iptv.databinding.ActivityVideoPlayerBinding
import com.masum.iptv.ui.home.HomeFragment
import com.masum.iptv.ui.player.BrightnessControl
import com.masum.iptv.ui.player.CustomDefaultTimeBar
import com.masum.iptv.ui.player.CustomStyledPlayerView
import com.masum.iptv.ui.player.dtpv.DoubleTapPlayerView
import com.masum.iptv.ui.player.dtpv.youtube.YouTubeOverlay
import com.masum.iptv.utils.*
import com.masum.iptv.utils.GeneralUtils.formatMilisSign
import com.masum.iptv.utils.GeneralUtils.hideSystemUi
import com.masum.iptv.utils.GeneralUtils.isPiPSupported
import com.masum.iptv.utils.GeneralUtils.setButtonEnabled
import com.masum.iptv.utils.GeneralUtils.setViewMargins
import com.masum.iptv.utils.GeneralUtils.setViewParams
import com.masum.iptv.utils.GeneralUtils.showText
import dagger.hilt.android.AndroidEntryPoint
import java.net.*
import java.net.CookieManager
import java.util.*
import java.util.regex.Pattern
import kotlin.math.abs


@AndroidEntryPoint

class VideoPlayerActivity : AppCompatActivity() {
    lateinit var binding: ActivityVideoPlayerBinding
    var mPrefs: Prefs? = null
    private val CONTROL_TYPE_PAUSE = 2
    private val REQUEST_PAUSE = 2
    var shortControllerTimeout = false
    private val REQUEST_PLAY = 1

    private var mReceiver: BroadcastReceiver? = null
    private val CONTROL_TYPE_PLAY = 1
    private var mPictureInPictureParamsBuilder: Any? = null
    private val ACTION_MEDIA_CONTROL = "media_control"
    private val EXTRA_CONTROL_TYPE = "control_type"
    private var errorToShow: ExoPlaybackException? = null
    var apiAccess = false
    var playbackFinished = false
    var displayManager: DisplayManager? = null
    var displayListener: DisplayManager.DisplayListener? = null
    private var buttonPiP: ImageButton? = null
    private var play = false
    private var loadingProgressBar: ProgressBar? = null
    private var videoLoading = false
    var playerView: CustomStyledPlayerView? = null
    private var trackSelector: DefaultTrackSelector? = null
    private var timeBar: CustomDefaultTimeBar? = null
    private var mediaSession: MediaSessionCompat?= null
    var focusPlay = false
    private var restorePlayState = false
    private var isScrubbing = false
    private var scrubbingNoticeable = false
    private var scrubbingStart: Long = 0
    var frameRendered = false
    private var controlView: StyledPlayerControlView? = null
    private var alive = false
    private var mAudioManager: AudioManager? = null
    private var exoPlayPause: ImageButton? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        val DEFAULT_COOKIE_MANAGER =  CookieManager()
        DEFAULT_COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ALL)

        if (CookieHandler.getDefault() != DEFAULT_COOKIE_MANAGER)
        {
            CookieHandler.setDefault(DEFAULT_COOKIE_MANAGER)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)




//

// Add to main Layout

// Add to main Layout




        mPrefs = Prefs(this@VideoPlayerActivity)

        //titleView.setOnTouchListener((v, event) -> true);
        mAudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        playerView=binding.videoView

        exoPlayPause = binding.videoView.findViewById(R.id.exo_play_pause)
        loadingProgressBar = binding.videoView.findViewById(R.id.loadingProgressBar)

        playerView?.setRepeatToggleModes(Player.REPEAT_MODE_ONE)

        playerView?.controllerHideOnTouch = true
        playerView?.controllerAutoShow = true




        binding.youtubeOverlay
            .performListener(object : YouTubeOverlay.PerformListener {
                override fun onAnimationStart() {
                    // Do UI changes when circle scaling animation starts (e.g. hide controller views)
                    binding.youtubeOverlay.visibility = View.VISIBLE
                }

                override fun onAnimationEnd() {
                    // Do UI changes when circle scaling animation starts (e.g. show controller views)
                    binding.youtubeOverlay.visibility = View.GONE
                }
            })
        binding.youtubeOverlay.playerView(binding.videoView)





        (playerView as DoubleTapPlayerView).isDoubleTapEnabled =true
        timeBar = playerView?.findViewById(R.id.exo_progress)
        timeBar!!.addListener(object : OnScrubListener {
            override fun onScrubStart(timeBar: TimeBar, position: Long) {
                if (player == null) {
                    return
                }
                restorePlayState = player!!.isPlaying
                if (restorePlayState) {
                    player!!.pause()
                }
                scrubbingNoticeable = false
                isScrubbing = true
                frameRendered = true
                playerView?.controllerShowTimeoutMs = -1
                scrubbingStart = player?.currentPosition ?:0
                player?.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                reportScrubbing(position)
            }

            override fun onScrubMove(timeBar: TimeBar, position: Long) {
                reportScrubbing(position)
            }

            override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
                playerView?.setCustomErrorMessage(null)
                isScrubbing = false
                if (restorePlayState) {
                    restorePlayState = false
                    playerView?.controllerShowTimeoutMs = CONTROLLER_TIMEOUT
                    player?.playWhenReady = true
                }
            }
        })

        val  buttonAspectRatio = playerView!!.findViewById<ImageButton>(R.id.ratio_button)


        playerView?.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
        buttonAspectRatio.setImageResource(R.drawable.ic_aspect_ratio_24dp)
        buttonAspectRatio.setOnClickListener {
            playerView?.setScale(1f)
            if (playerView?.resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
                playerView?.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                showText(playerView, getString(R.string.video_resize_crop))
            } else {
                // Default mode
                playerView?.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                showText(playerView, getString(R.string.video_resize_fit))
            }
            resetHideCallbacks()
        }
        setButtonEnabled(this@VideoPlayerActivity, buttonAspectRatio, true)


        if (isPiPSupported(this@VideoPlayerActivity)) {
            // TODO: Android 12 improvements:
            // https://developer.android.com/about/versions/12/features/pip-improvements
            mPictureInPictureParamsBuilder = PictureInPictureParams.Builder()
            updatePictureInPictureActions(
                R.drawable.ic_play_arrow_24dp,
                com.google.android.exoplayer2.ui.R.string.exo_controls_play_description,
                CONTROL_TYPE_PLAY,
                REQUEST_PLAY
            )
            buttonPiP = playerView!!.findViewById(R.id.pip_icon)
            buttonPiP?.setOnClickListener { view: View? ->
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    enterPiP()

            }
            setButtonEnabled(this@VideoPlayerActivity, buttonPiP!!, false)
        }




        controlView = playerView!!.findViewById(com.google.android.exoplayer2.ui.R.id.exo_controller)
        controlView?.setOnApplyWindowInsetsListener { view: View, windowInsets: WindowInsets? ->
            if (windowInsets != null) {
                view.setPadding(
                    0, windowInsets.systemWindowInsetTop,
                    0, windowInsets.systemWindowInsetBottom
                )
                val insetLeft = windowInsets.systemWindowInsetLeft
                val insetRight = windowInsets.systemWindowInsetRight
                var paddingLeft = 0
                var marginLeft = insetLeft
                var paddingRight = 0
                var marginRight = insetRight
                if (Build.VERSION.SDK_INT >= 28 && windowInsets.displayCutout != null) {
                    if (windowInsets.displayCutout!!.safeInsetLeft == insetLeft) {
                        paddingLeft = insetLeft
                        marginLeft = 0
                    }
                    if (windowInsets.displayCutout!!.safeInsetRight == insetRight) {
                        paddingRight = insetRight
                        marginRight = 0
                    }
                }
                //    setViewParams(titleView, paddingLeft + titleViewPadding, titleViewPadding, paddingRight + titleViewPadding, titleViewPadding, marginLeft, windowInsets.systemWindowInsetTop, marginRight, 0)
                setViewParams(
                    binding.videoView.findViewById(R.id.exo_bottom_bar), paddingLeft, 0, paddingRight, 0,
                    marginLeft, 0, marginRight, 0
                )
                binding.videoView.findViewById<View>(R.id.exo_progress).setPadding(
                    windowInsets.systemWindowInsetLeft, 0,
                    windowInsets.systemWindowInsetRight, 0
                )
                setViewMargins(
                    binding.videoView.findViewById(com.google.android.exoplayer2.ui.R.id.exo_error_message),
                    0,
                    windowInsets.systemWindowInsetTop / 2,
                    0,
                    resources.getDimensionPixelSize(com.google.android.exoplayer2.ui.R.dimen.exo_error_message_margin_bottom) + windowInsets.systemWindowInsetBottom / 2
                )
                windowInsets.consumeSystemWindowInsets()
            }
            windowInsets!!
        }

        try {
            val customDefaultTrackNameProvider = CustomDefaultTrackNameProvider(
                resources
            )
            val field = StyledPlayerControlView::class.java.getDeclaredField("trackNameProvider")
            field.isAccessible = true
            field[controlView] = customDefaultTrackNameProvider
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }


        this@VideoPlayerActivity.let {
            mBrightnessControl = BrightnessControl(it)
            if (mPrefs!!.brightness >= 0) {
                mBrightnessControl?.currentBrightnessLevel = mPrefs!!.brightness
                mBrightnessControl?.screenBrightness= (mBrightnessControl?.levelToBrightness(
                    mBrightnessControl?.currentBrightnessLevel?:0)?:0f)
            }
        }

        val lock=playerView?.findViewById<ImageView>(R.id.lock_icon)
        lock?.setOnClickListener {
            if (locked || player != null && player!!.isPlaying) {
                locked = !locked

                showText(playerView, "Locked", CustomStyledPlayerView.MESSAGE_TIMEOUT_LONG)

                if (locked && controllerVisible) {
                    playerView?.hideController()
                }
            }
        }


        val exoQuality =  playerView?.findViewById<ImageButton>(R.id.exo_quality)/*
        val exoFfwd =  playerView?.findViewById<ImageButton>(R.id.ffwd)
        val exoRew =  playerView?.findViewById<ImageButton>(R.id.rew)

        exoRew?.setOnClickListener {
            try {
                player?.seekTo(player!!.currentPosition - 5000) // 10000 = 10 Seconds
            } catch (e: java.lang.Exception) {
                Toast.makeText(this, "Error : $e", Toast.LENGTH_SHORT).show()
            }
        }
  exoFfwd?.setOnClickListener {
            try {
                player?.seekTo(player!!.currentPosition + 5000) // 10000 = 10 Seconds
            } catch (e: java.lang.Exception) {
                Toast.makeText(this, "Error : $e", Toast.LENGTH_SHORT).show()
            }
        }
*/


        val fullscreenBtn=playerView?.findViewById<ImageView>(R.id.fullscreen_button)
        fullscreenBtn?.setOnClickListener {
            val orientation = this.resources.configuration.orientation
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            } else {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                // code for landscape mode
            }
        }







        exoQuality?.setOnClickListener {




val v=player!!.currentTracks


            val trackSelectionDialog =
                TrackSelectionDialog.createForTracksAndParameters(
                    /* titleId= */R.string.title_dialog,
                    player!!.currentTracks,
                    DownloadHelper.getDefaultTrackSelectorParameters(this),
                    /* allowAdaptiveSelections= */ false,
                    /* allowMultipleOverrides= */ true,
                    /* onTracksSelectedListener= */
                    {
                        trackSelector?.setParameters(it)
                    },
                    /* onDismissListener= */ null)
            trackSelectionDialog.show(supportFragmentManager, /* tag= */ null)


        }



        playerView!!.setControllerVisibilityListener(StyledPlayerView.ControllerVisibilityListener { visibility ->

            controllerVisible = visibility == View.VISIBLE
            controllerVisibleFully =
                playerView!!.isControllerFullyVisible
            if (restoreControllerTimeout) {

                restoreControllerTimeout = false
                if (player == null || !player!!.isPlaying) {
                    playerView?.controllerShowTimeoutMs = -1
                } else {
                    playerView?.controllerShowTimeoutMs = CONTROLLER_TIMEOUT
                }
            }

            // https://developer.android.com/training/system-ui/immersive
            if (visibility == View.VISIBLE) {
                //  GeneralUtils.showSystemUi(playerView!!)
                // Because when using dpad controls, focus resets to first item in bottom controls bar
                binding.videoView.findViewById<View>(R.id.exo_play_pause).requestFocus()
            } else {
                hideSystemUi(playerView!!)
            }

        })

        var url = intent?.getStringExtra("link")?:intent?.data.toString()

       if(BuildConfig.DEBUG) Log.i("123321", "onCreate: url: " + url)


        if (!url.contains("|") && url.contains("type=web")) getWebLink(url)


        else initializePlayer(url)

    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun enterPiP() {
        val appOpsManager = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager?
        if (AppOpsManager.MODE_ALLOWED != appOpsManager!!.checkOpNoThrow(
                AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                Process.myUid(),
                this@VideoPlayerActivity.packageName
            )
        ) {
            val intent = Intent(
                "android.settings.PICTURE_IN_PICTURE_SETTINGS",
                Uri.fromParts("package", this@VideoPlayerActivity.packageName, null)
            )
            if (this@VideoPlayerActivity.let { intent.resolveActivity(it.packageManager) } != null) {
                startActivity(intent)
            }
            return
        }
        if (player == null) {
            return
        }
        playerView!!.controllerAutoShow = false
        playerView!!.hideController()
        val format: Format? = player?.videoFormat
        // https://github.com/google/ExoPlayer/issues/8611
        // TODO: Test/disable on Android 11+

        format?.let { format ->
            val videoSurfaceView = playerView?.videoSurfaceView
            if (videoSurfaceView is SurfaceView) {
                videoSurfaceView.holder.setFixedSize(format.width, format.height)
            }
            val aspectRatio = Rational(239, 120)

            (mPictureInPictureParamsBuilder as PictureInPictureParams.Builder).setAspectRatio(
                aspectRatio
            )
            this@VideoPlayerActivity.enterPictureInPictureMode((mPictureInPictureParamsBuilder as PictureInPictureParams.Builder).build())

        }
    }

    private fun startExternalBrowserIntent(url: String?) {
        val i = Intent(Intent.ACTION_VIEW)
        i.data = Uri.parse(url)
        startActivity(i)


    }

    companion object {
        var locked: Boolean = false
        var restoreControllerTimeout = false
        val CONTROLLER_TIMEOUT = 3500
        var snackbar: Snackbar? = null
        var player: ExoPlayer? = null
        var controllerVisibleFully = false
        var mBrightnessControl: BrightnessControl? = null
        var boostLevel = 0
        var loudnessEnhancer: LoudnessEnhancer? = null
        var controllerVisible = false
    }

    private fun initializePlayer(passed: String? = null, headers: Map<String, String>? = null) {
       if(BuildConfig.DEBUG) Log.i("123321", " passed: " + passed)
        var url=passed?:""
        var ip=getSharedPreferences("recent",
            MODE_PRIVATE
        ).getString("ip","")
        ip=URLEncoder.encode(ip, "utf-8")
        if(url.contains("type=ghd")){


            url += "&SPORTZFY_TOKEN=$ip"
        }

        url=url.replace("#","")
        url=url.replace("m3u8?|","m3u8?")
        val modified=url.replace("|",
            "&")
        val referer= Uri.parse(modified)


        val userAgentValue= referer.getQueryParameter("User-agent")?:referer.getQueryParameter("user-agent")
        val originValue= referer.getQueryParameter("Origin")?:referer.getQueryParameter("origin")
        val refererValue= referer.getQueryParameter("Referer")?:referer.getQueryParameter("referer")
        val cookieValue= referer.getQueryParameter("cookie")?:referer.getQueryParameter("Cookie")


        val header : HashMap<String, String> = HashMap<String, String> ()

        if(refererValue!= null){
            header["Referer"] = refererValue
           if(BuildConfig.DEBUG) Log.i("123321", "referer: " + refererValue)
        }
        if(originValue!= null){
            header["Origin"] = originValue
           if(BuildConfig.DEBUG) Log.i("123321", "origin: " + originValue)
        }
        if(cookieValue!=null){
            header["cookie"] = cookieValue
           if(BuildConfig.DEBUG) Log.i("123321", "cookie: " + cookieValue)
        }
        if (ip != null) {
            header["Sportzfy_Token"] = ip

           if(BuildConfig.DEBUG) Log.i("123321", "Sportzfy_Token:$ip")

        }
        if (userAgentValue != null){

            header["User-Agent"] = userAgentValue
           if(BuildConfig.DEBUG) Log.i("123321", "User-Agent: $userAgentValue")
        }






        if(headers == null){
        }
        trackSelector = this@VideoPlayerActivity.let { DefaultTrackSelector() }
        if (Build.VERSION.SDK_INT >= 24) {
            val localeList = Resources.getSystem().configuration.locales
            val locales: MutableList<String> = ArrayList()
            for (i in 0 until localeList.size()) {
                locales.add(localeList[i].isO3Language)
            }
            trackSelector!!.buildUponParameters().let {
                trackSelector!!.setParameters(
                    it
                        .setPreferredAudioLanguages(*locales.toTypedArray())
                )
            }
        } else {
            val locale = Resources.getSystem().configuration.locale
            trackSelector!!.buildUponParameters().let {
                trackSelector!!.setParameters(
                    it.setPreferredAudioLanguage(locale.isO3Language)
                )
            }
        }
        val renderersFactory: RenderersFactory = DefaultRenderersFactory(this@VideoPlayerActivity)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
        // https://github.com/google/ExoPlayer/issues/8571
        val extractorsFactory = DefaultExtractorsFactory()
            .setTsExtractorFlags(DefaultTsPayloadReaderFactory.FLAG_ENABLE_HDMV_DTS_AUDIO_STREAMS)
            .setTsExtractorTimestampSearchBytes(1500 * TsExtractor.TS_PACKET_SIZE)
        player =
            ExoPlayer.Builder(this@VideoPlayerActivity, renderersFactory)
                .setTrackSelector(trackSelector!!)
                .setMediaSourceFactory(
                    DefaultMediaSourceFactory(
                        this@VideoPlayerActivity
                    )
                )
                .build()
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        player!!.setAudioAttributes(audioAttributes, true)

        val youTubeOverlay: YouTubeOverlay = binding.root.findViewById(R.id.youtube_overlay)
        youTubeOverlay.player(player)
        playerView!!.player = player




        playerView!!.controllerShowTimeoutMs = -1
        locked = false


        // https://github.com/google/ExoPlayer/issues/5765


        playerView!!.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        playerView!!.setScale(1f)

        // var url="http://31.172.87.56:2200/R-EX/SPORTS_STAR_SPORTS_TAMIL-in/tracks-v1a1/mono.m3u8?token=RED_Hqqv1c3nXjqlzBGFPhFuTg==,165803831455.4814651659=|User-agent=REDLINECLIENT"








        player?.addAnalyticsListener(EventLogger(trackSelector))



     var link=url

// matching streaming link
        val regex = "(http.*?[^|\">]+)"
        val pattern = Pattern.compile(regex)
        val matcher = pattern.matcher(url)

        if (matcher.find()) {
            link = matcher.group(0)
        }





        val defaultHttpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS)
            .setKeepPostFor302Redirects(true)
            .setAllowCrossProtocolRedirects(true)
// get headers from url and set to datasourcefactory
        userAgentValue?.let { defaultHttpDataSourceFactory.setUserAgent(it)}
        userAgentValue?:intent.getStringExtra(USER_AGENT)?.let {

            defaultHttpDataSourceFactory.setUserAgent(it)
        }

        try {
            val mainUrl = URL(url.replace("|", "#"))
            val query = mainUrl.ref

            for (q in query.split("&").toTypedArray()) {
                val qa = q.split("=").toTypedArray()
                val name = URLDecoder.decode(qa[0])
                var value: String? = ""
                if (qa.size == 2) {
                    value = URLDecoder.decode(qa[1])

                }
                if (value != null) {

                    header.put(name,value)

                }
//
            }




        } catch (e: java.lang.Exception) {
            e.printStackTrace()
           if(BuildConfig.DEBUG) Log.i("123321", "initializePlayer: exception: " + e.message)
        }
        intent.getStringExtra(REFERER)?.let { header["Referer"] = it}
        intent.getStringExtra(ORIGIN)?.let { header["Origin"] = it}
        intent.getStringExtra(COOKIE)?.let { header["Cookie"] = it}

        defaultHttpDataSourceFactory.setDefaultRequestProperties(header)
        if(headers!=null) defaultHttpDataSourceFactory.setDefaultRequestProperties(headers)
        val uuid=when(intent.getStringExtra(SCHEME)){
                "widevine"->C.WIDEVINE_UUID
            "playready"->C.PLAYREADY_UUID
            "clearkey"->C.CLEARKEY_UUID
            else->C.WIDEVINE_UUID

        }


        // Player
        val mediaItem =
            MediaItem.Builder()
                .setUri(Uri.parse(link))
                .setDrmLicenseUri(intent.getStringExtra(DRM))
                .setDrmUuid(uuid)
                .build()

        val hlsMediaSource= HlsMediaSource.Factory(defaultHttpDataSourceFactory).createMediaSource(mediaItem)
      player?.setMediaSource(hlsMediaSource)
        playVideos()

    }

    private fun playVideos() {
        //                    player?.seekTo(videos.indexOf(video),C.TIME_UNSET)


        if (loudnessEnhancer != null) {
            loudnessEnhancer!!.release()
        }
        try {
            loudnessEnhancer =
                LoudnessEnhancer(player!!.audioSessionId)
        } catch (e: RuntimeException) {
            e.printStackTrace()
        }
        notifyAudioSessionUpdate(true)
        videoLoading = true
        updateLoading(true)
        if (mPrefs?.position == 0L || apiAccess) {
            play = true
        }
        if (buttonPiP != null) setButtonEnabled(this@VideoPlayerActivity, buttonPiP!!, true)

        (playerView as DoubleTapPlayerView).isDoubleTapEnabled = true
        // if (!apiAccess) nextUri = findNext()
        player?.setHandleAudioBecomingNoisy(true)
        mediaSession?.isActive = true

        player?.addListener(object : Player.Listener {


            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (loudnessEnhancer != null) {
                    loudnessEnhancer?.release()
                }
                try {
                    loudnessEnhancer =
                        LoudnessEnhancer(audioSessionId)
                } catch (e: RuntimeException) {
                    e.printStackTrace()
                }
                notifyAudioSessionUpdate(true)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playerView?.keepScreenOn = isPlaying
                if (isPiPSupported(this@VideoPlayerActivity)) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (isPlaying) {
                            updatePictureInPictureActions(
                                R.drawable.ic_pause_24dp,
                                com.google.android.exoplayer2.ui.R.string.exo_controls_pause_description,
                                CONTROL_TYPE_PAUSE,
                                REQUEST_PAUSE
                            )
                        } else {
                            updatePictureInPictureActions(
                                R.drawable.ic_play_arrow_24dp,
                                com.google.android.exoplayer2.ui.R.string.exo_controls_play_description,
                                CONTROL_TYPE_PLAY,
                                REQUEST_PLAY
                            )
                        }
                    },1000)
                }
                if (!isScrubbing) {
                    if (isPlaying) {
                        if (shortControllerTimeout) {
                            playerView?.controllerShowTimeoutMs = CONTROLLER_TIMEOUT / 3
                            shortControllerTimeout = false
                            restoreControllerTimeout = true
                        } else {
                            playerView?.controllerShowTimeoutMs = CONTROLLER_TIMEOUT
                        }
                    } else {
                        playerView?.controllerShowTimeoutMs = -1
                    }
                }
                if (!isPlaying) {
                    locked = false
                }
            }



            @SuppressLint("SourceLockedOrientationActivity")
            override fun onPlaybackStateChanged(state: Int) {

                val duration: Long = player?.duration ?: 0
                if (duration != C.TIME_UNSET) {
                    val position: Long =
                        player?.currentPosition ?: 0
                    if (position + 4000 >= duration) {

                       if(BuildConfig.DEBUG) Log.i("123321", "onPlaybackStateChanged: video near end")
                    }
                }
                if (state == Player.STATE_READY) {

                    frameRendered = true
                    if (videoLoading) {
                        videoLoading = false



                        if (mPrefs!!.frameRateMatching) {
                            if (play) {
                                if (displayManager == null) {
                                    displayManager =
                                        getSystemService(Context.DISPLAY_SERVICE) as DisplayManager?
                                }
                                if (displayListener == null) {
                                    displayListener = object : DisplayManager.DisplayListener {
                                        override fun onDisplayAdded(displayId: Int) {}
                                        override fun onDisplayRemoved(displayId: Int) {}
                                        override fun onDisplayChanged(displayId: Int) {
                                            if (play) {
                                                play = false
                                                displayManager?.unregisterDisplayListener(this)
                                                if (player != null) {
                                                    player?.play()
                                                }
                                                if (playerView != null) {
                                                    playerView?.hideController()
                                                }
                                            }
                                        }
                                    }
                                }
                                displayManager?.registerDisplayListener(displayListener, null)
                            }

                        }
                        if (displayManager != null) {
                            displayManager?.unregisterDisplayListener(displayListener)
                        }
                        if (play) {
                            play = false
                            player?.play()
                            playerView?.hideController()
                        }
                        updateLoading(false)
                        if (mPrefs!!.audioTrack != -1 && mPrefs!!.audioTrackFfmpeg != -1) {
                            setSelectedTrackAudio(mPrefs!!.audioTrack, false)
                            setSelectedTrackAudio(mPrefs!!.audioTrackFfmpeg, true)
                        }

                    }
                } else if (state == Player.STATE_ENDED) {
                   if(BuildConfig.DEBUG) Log.i("123321", "onPlaybackStateChanged: video ended")


                }

               if(BuildConfig.DEBUG) Log.i("123321", "is video ended:$playbackFinished")
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.i("123321", "onPlayerError: ${error.message}")
               if(BuildConfig.DEBUG) Log.i("123321", "onPlayerError:- ${error.message}")
                val message = error.message
                updateLoading(false)
                if (error is ExoPlaybackException) {
                    if (controllerVisible && controllerVisibleFully) {
                        showError(error)
                    } else {
                        errorToShow = error
                    }
                }
            }


        })
        Handler(Looper.getMainLooper()).postDelayed({
            player?.prepare()

            if (restorePlayState) {
                restorePlayState = false
                playerView?.showController()
                player?.play()
            }
        },1000)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updatePictureInPictureActions(iconId: Int, resTitle: Int, controlType: Int, requestCode: Int) {


        val actions = ArrayList<RemoteAction>()
        val intent = PendingIntent.getBroadcast(
            this@VideoPlayerActivity, requestCode,
            Intent(ACTION_MEDIA_CONTROL).putExtra(
                EXTRA_CONTROL_TYPE,
                controlType
            ), PendingIntent.FLAG_IMMUTABLE
        )
        val icon: Icon = Icon.createWithResource(this@VideoPlayerActivity, iconId)
        val title = getString(resTitle)
        actions.add(RemoteAction(icon, title, title, intent))
        (mPictureInPictureParamsBuilder as PictureInPictureParams.Builder).setActions(actions)
        try {
            this@VideoPlayerActivity.setPictureInPictureParams((mPictureInPictureParamsBuilder as PictureInPictureParams.Builder).build())
        } catch (e: Exception) {
        }

    }


    fun setSelectedTrackAudio(trackIndex: Int, ffmpeg: Boolean) {
        val mappedTrackInfo = trackSelector!!.currentMappedTrackInfo
        if (mappedTrackInfo != null) {
            val parameters = trackSelector!!.parameters
            val parametersBuilder = parameters.buildUpon()
            for (rendererIndex in 0 until mappedTrackInfo.rendererCount) {
                if (mappedTrackInfo.getRendererType(rendererIndex) == C.TRACK_TYPE_AUDIO) {
                    val rendererName = mappedTrackInfo.getRendererName(rendererIndex)
                    if (rendererName.lowercase(Locale.getDefault()).contains("ffmpeg") && !ffmpeg ||
                        !rendererName.lowercase(Locale.getDefault()).contains("ffmpeg") && ffmpeg
                    ) continue
                    if (trackIndex == Int.MIN_VALUE) {
                        parametersBuilder.setRendererDisabled(rendererIndex, true)
                    } else {
                        parametersBuilder.setRendererDisabled(rendererIndex, false)
                        if (trackIndex == -1) {
                            parametersBuilder.clearSelectionOverrides(rendererIndex)
                        } else {
                            val tracks = intArrayOf(0)
                            val selectionOverride = SelectionOverride(trackIndex, *tracks)
                            parametersBuilder.setSelectionOverride(
                                rendererIndex,
                                mappedTrackInfo.getTrackGroups(rendererIndex),
                                selectionOverride
                            )
                        }
                    }
                }
            }
            trackSelector!!.setParameters(parametersBuilder)
        }
    }


    fun showError(error: ExoPlaybackException) {
        val errorGeneral = error.localizedMessage
       if(BuildConfig.DEBUG) Log.i("123321", "showError: ${error.message}")
        if (errorGeneral!=null){
            val errorDetailed: String? = when (error.type) {
                ExoPlaybackException.TYPE_SOURCE -> error.sourceException.localizedMessage
                ExoPlaybackException.TYPE_RENDERER -> error.rendererException.localizedMessage
                ExoPlaybackException.TYPE_UNEXPECTED -> error.unexpectedException.localizedMessage
                ExoPlaybackException.TYPE_REMOTE -> errorGeneral
                else -> errorGeneral
            }
            showSnack(errorGeneral, errorDetailed)
        }
    }

    fun showSnack(textPrimary: String?, textSecondary: String?) {
        snackbar = Snackbar.make(
            binding.coordinatorLayout,
            textPrimary!!, Snackbar.LENGTH_LONG
        )
        if (textSecondary != null) {
            snackbar?.setAction(R.string.error_details) { v ->
                val builder =
                    AlertDialog.Builder(this@VideoPlayerActivity)
                builder.setMessage(textSecondary)
                builder.setPositiveButton(
                    android.R.string.ok
                ) { dialogInterface: DialogInterface, i: Int -> dialogInterface.dismiss() }
                val dialog = builder.create()
                dialog.show()
            }
        }
        snackbar?.setAnchorView(R.id.exo_bottom_bar)
      //  snackbar?.show()
    }

    private fun notifyAudioSessionUpdate(active: Boolean) {
        val intent =
            Intent(if (active) AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION else AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
        intent.putExtra(
            AudioEffect.EXTRA_AUDIO_SESSION,
            player?.audioSessionId
        )
        intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, this@VideoPlayerActivity.packageName)
        if (active) {
            intent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MOVIE)
        }
        sendBroadcast(intent)
    }


    private fun updateLoading(enableLoading: Boolean) {
        if (enableLoading) {
            exoPlayPause!!.visibility = View.GONE
            loadingProgressBar?.visibility = View.VISIBLE
        } else {
            loadingProgressBar?.visibility = View.GONE
            exoPlayPause!!.visibility = View.VISIBLE
            if (focusPlay) {
                focusPlay = false
                exoPlayPause!!.requestFocus()
            }
        }
    }
    fun reportScrubbing(position: Long) {
        val diff = position - scrubbingStart
        if (abs(diff) > 1000) {
            scrubbingNoticeable = true
        }
        if (scrubbingNoticeable) {
            playerView!!.clearIcon()
            playerView!!.setCustomErrorMessage(formatMilisSign(diff))
        }
        if (frameRendered) {
            frameRendered = false
            player?.seekTo(position)
        }
    }
    private fun resetHideCallbacks() {
        if (player != null && player!!.isPlaying) {
            // Keep controller UI visible - alternative to resetHideCallbacks()
            playerView!!.controllerShowTimeoutMs = CONTROLLER_TIMEOUT
        }
    }

    override fun onStart() {
        super.onStart()
        alive = true

    }

    override fun onResume() {
        super.onResume()
        player?.play()
    }

    override fun onPause() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O&&!isInPictureInPictureMode){
            this@VideoPlayerActivity.let {it.requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT}

            player?.pause()
        }
        else {player?.pause()}
        super.onPause()
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean,
                                               newConfig: Configuration
    ) {


        if (isInPictureInPictureMode) {
            // On Android TV it is required to hide controller in this PIP change callback
            playerView!!.hideController()
            playerView!!.setScale(1f)
            player?.play()
            mReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (ACTION_MEDIA_CONTROL != intent.action || player == null) {
                       if(BuildConfig.DEBUG) Log.i("123321", "onReceive: null trigger")
                        return
                    }
                    when (intent.getIntExtra(
                        EXTRA_CONTROL_TYPE,
                        0
                    )) {
                        CONTROL_TYPE_PLAY -> player?.play()
                        CONTROL_TYPE_PAUSE -> player?.pause()
                    }
                }
            }
            registerReceiver(
                mReceiver,
                IntentFilter(ACTION_MEDIA_CONTROL)
            )
        }
        else {
            player?.pause()

            if (mPrefs!!.resizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
                playerView!!.setScale(mPrefs!!.scale)
            }
            if (mReceiver != null) {
                unregisterReceiver(mReceiver)
                mReceiver = null
            }
            playerView!!.controllerAutoShow = true
            if (player != null) {
                if (player?.isPlaying == true) hideSystemUi(
                    playerView!!
                )
                else playerView!!.showController()
            }
        }
        super.onPictureInPictureModeChanged(isInPictureInPictureMode,newConfig)

    }

    override fun onDestroy() {
        super.onDestroy()
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O&&!isInPictureInPictureMode){
            this@VideoPlayerActivity.let {it.requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT}

            player?.release()
            binding.videoView.player=null
        }
        else{
            player?.release()
            binding.videoView.player=null
        }
    }


    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val newOrientation = newConfig.orientation




        if (newOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
                window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;}
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                    or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)



        }
        else if (newOrientation == Configuration.ORIENTATION_PORTRAIT) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

            val decorView: View = getWindow().getDecorView()
            decorView.systemUiVisibility = 0
            binding.videoView.layoutParams= ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT)
            window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            window.clearFlags( WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            window.clearFlags( View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
            window.clearFlags( View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)




        }

    }

    fun getScreenWidth(context: Context): Int {
        val displayMetrics = DisplayMetrics()
        (context as Activity).windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }
    override fun onBackPressed() {

        val orientation = this.resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
        finish()
            // code for portrait mode
        } else {
            // code for landscape mode

            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            finish()
        }

    }


    private fun getWebLink(url: String) {
       if(BuildConfig.DEBUG) Log.i("123321", "getWebLink: $url")
        WebView(this).apply {
            webChromeClient = WebChromeClient()
            settings.apply {
                mediaPlaybackRequiresUserGesture = false
                javaScriptEnabled = true
            }
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest
                ): WebResourceResponse? {
                    request.url.toString().let {
                        if (it.contains(".m3u8")) {
                            runOnUiThread {
                                stopLoading()
                                destroy()
                                initializePlayer(it, request.requestHeaders)
                            }
                        }
                    }
                    return super.shouldInterceptRequest(view, request)
                }

                override fun onLoadResource(view: WebView?, url: String?) {
                    super.onLoadResource(view, url)
                    url.toString().let {
                        if (it.contains(".m3u8")) {
                            runOnUiThread {
                                stopLoading()
                                destroy()
                                initializePlayer(it)
                            }
                        }
                    }
                }

                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError
                ) {
                    super.onReceivedError(view, request, error)
                    runOnUiThread {
                        view.destroy()
                        initializePlayer(url)
                    }
                }
            }
            loadUrl(url)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        //setIntent(intent)
       if(BuildConfig.DEBUG) Log.i("123321", "onNewIntent: ${intent?.data}")
        intent?.data?.let {url->
            if (!url.toString().contains("|") && url.toString().contains("type=web")) getWebLink(url.toString())
            else initializePlayer(url.toString())
        }
    }


}


