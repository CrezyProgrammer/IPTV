package com.masum.iptv.ui.home

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
import android.support.v4.media.session.MediaSessionCompat
import android.util.DisplayMetrics
import android.util.Log
import android.util.Rational
import android.view.*
import android.webkit.*
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadState
import androidx.recyclerview.widget.GridLayoutManager
import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.ghdsports.india.ui.player.CustomDefaultTrackNameProvider
import com.ghdsports.india.ui.player.Prefs
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.extractor.ts.DefaultTsPayloadReaderFactory
import com.google.android.exoplayer2.extractor.ts.TsExtractor
import com.google.android.exoplayer2.offline.DownloadHelper
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.StyledPlayerControlView
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.ui.TimeBar
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.EventLogger
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.ixuea.android.downloader.DownloadService
import com.ixuea.android.downloader.callback.DownloadListener
import com.ixuea.android.downloader.domain.DownloadInfo
import com.ixuea.android.downloader.exception.DownloadException
import com.masum.iptv.R
import com.masum.iptv.adapters.PlayersAdapter
import com.masum.iptv.data.fileparser.ParseLocalFile
import com.masum.iptv.databinding.FragmentHomeBinding
import com.masum.iptv.models.Channel
import com.masum.iptv.ui.player.BrightnessControl
import com.masum.iptv.ui.player.CustomDefaultTimeBar
import com.masum.iptv.ui.player.CustomStyledPlayerView
import com.masum.iptv.ui.player.dtpv.DoubleTapPlayerView
import com.masum.iptv.ui.player.dtpv.youtube.YouTubeOverlay
import com.masum.iptv.utils.GeneralUtils
import com.masum.iptv.utils.RecyclerViewItemDecoration
import com.masum.iptv.utils.TrackSelectionDialog
import com.masum.iptv.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.ronnie.allplayers.adapters.PlayersLoadingStateAdapter
import dmax.dialog.SpotsDialog
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.rosuh.filepicker.bean.FileItemBeanImpl
import me.rosuh.filepicker.config.AbstractFileFilter
import me.rosuh.filepicker.config.FilePickerManager
import me.rosuh.filepicker.filetype.AudioFileType
import java.io.File
import java.io.FileWriter
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.*
import java.util.regex.Pattern


@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val adapter =
        PlayersAdapter { name: String -> snackBarClickedPlayer(name) }
    private val viewModel: MainViewModel by viewModels()
    private var searchJob: Job? = null


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
    private var WrapHeight=0


    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    @OptIn(ExperimentalPagingApi::class)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
        WrapHeight=((getScreenWidth(requireActivity()) * 0.30).toInt())
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)



        initPlayerView()

        setUpAdapter()
        startSearchJob()
        getCategoryList()
        binding.file.setOnClickListener {
            binding.expandableFabLayout.close()
            FilePickerManager
                .from(this)

                .filter(fileFilter)

                .enableSingleChoice()
                .forResult(FilePickerManager.REQUEST_CODE)


        }
        binding.chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
           val text= binding.chipGroup.children.toList().filter { (it as Chip).isChecked }.joinToString(", ") { (it as Chip).text }
            startSearchJob(if(text=="All") "" else text)
            Toast.makeText(requireActivity(),text, Toast.LENGTH_SHORT).show()
        }
        binding.link.setOnClickListener{

            val alert = AlertDialog.Builder(requireActivity())
            val edittext = EditText(requireActivity())
            edittext.setPadding(10)
            edittext.minLines=2
            edittext.hint="Enter  URL"

            alert.setTitle("Playlist URL")

            alert.setView(edittext)

            alert.setPositiveButton("Load"
            ) { dialog, _ -> //What ever you want to do with the value

                //OR
                val url: String = edittext.text.toString()
                playlistDownloader(url)
                dialog.dismiss()
            }

            alert.setNegativeButton("Cancel"
            ) { dialog, whichButton ->
                // what ever you want to do with No option.
                dialog.dismiss()
            }

            alert.show()
        }



        return root
    }

    private fun getCategoryList() {
        lifecycleScope.launch {
            viewModel.getCategoryList(0).observe(viewLifecycleOwner) { it ->
                if(it.isNotEmpty())binding.chipGroup.addView(createTagChip("All"))
                it.forEach {
                binding.chipGroup.addView(createTagChip(it))

            }
            }
        }

    }
    private fun createTagChip(chipName: String): Chip {

            val chip = layoutInflater.inflate(R.layout.single_chip_layout, binding.chipGroup, false) as Chip

        chip.id = ViewCompat.generateViewId()
        val split=chipName.split(";")
        chip.text = split.last()
        return chip

    }
    private fun playlistDownloader(url: String) {
        val dialog: android.app.AlertDialog? = SpotsDialog.Builder()
            .setContext(requireActivity())
            .setMessage("Downloading...")
            .setCancelable(false)

        .build()
            .apply {
                show()
            }



    }

    @OptIn(ExperimentalPagingApi::class)
    private fun insertList(list: List<Channel>) {
     lifecycleScope.launch {
         viewModel.insertList(list)
     }
        startSearchJob()
        getCategoryList()

    }

    private val fileFilter = object : AbstractFileFilter() {
        override fun doFilter(listData: ArrayList<FileItemBeanImpl>): ArrayList<FileItemBeanImpl> {
            val iterator = listData.iterator()
            while (iterator.hasNext()) {
                val item = iterator.next()
                // 如果是文件夹则略过
                if (item.isDir) continue
                // 判断文件类型是否是图片
                if (item.fileType !is AudioFileType) {
                    iterator.remove()
                }
            }


            return listData
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            FilePickerManager.REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    val list = FilePickerManager.obtainData()
                   val channelList= ParseLocalFile(list[0], id!!)
                  //  insertList(channelList)
                    getCategoryList()
                    // do your work
                } else {
                    Toast.makeText(context, "You didn't choose anything~", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    @ExperimentalPagingApi
    private fun startSearchJob(query: String="") {
        binding.progressBar.isVisible = true
        searchJob?.cancel()

  }
    private fun snackBarClickedPlayer(name: String) {
        player?.pause()
        binding.videoView.isVisible = true
        initializePlayer(name)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun setUpAdapter() {

        binding.recycler.apply {
            layoutManager = GridLayoutManager(requireActivity(),3)
            setHasFixedSize(true)
            addItemDecoration(RecyclerViewItemDecoration())
        }
        binding.recycler.adapter = adapter.withLoadStateFooter(
            footer = PlayersLoadingStateAdapter { retry() }
        )

        adapter.addLoadStateListener { loadState ->
            if ( loadState.append.endOfPaginationReached )
            {
                binding.errorMsg.isVisible=adapter.itemCount < 1
            }

            if (loadState.mediator?.refresh is LoadState.Loading) {

                if (adapter.snapshot().isEmpty()) {
                    binding.progressBar.isVisible = true
                }

            } else {

                binding.progressBar.isVisible = false
                binding.errorMsg.isVisible=adapter.itemCount < 1
             //   binding.swipeRefreshLayout.isRefreshing = false

                val error = when {
                    loadState.mediator?.prepend is LoadState.Error -> loadState.mediator?.prepend as LoadState.Error
                    loadState.mediator?.append is LoadState.Error -> loadState.mediator?.append as LoadState.Error
                    loadState.mediator?.refresh is LoadState.Error -> loadState.mediator?.refresh as LoadState.Error

                    else -> null
                }
                error?.let {
                    if (adapter.snapshot().isEmpty()) {
                //        binding.errorTxt.isVisible = true
                  //      binding.errorTxt.text = it.error.localizedMessage
                    }

                }

            }
        }

    }

    private fun retry() {
        adapter.retry()
    }


    private fun initPlayerView() {
        binding.videoView.isVisible = false
        binding.videoView.layoutParams=
            ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, WrapHeight)

        mPrefs = Prefs(requireContext())

        //titleView.setOnTouchListener((v, event) -> true);
        mAudioManager = requireActivity().getSystemService(Context.AUDIO_SERVICE) as AudioManager
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
        timeBar!!.addListener(object : TimeBar.OnScrubListener {
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
                GeneralUtils.showText(playerView, getString(R.string.video_resize_crop))
            } else {
                // Default mode
                playerView?.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                GeneralUtils.showText(playerView, getString(R.string.video_resize_fit))
            }
            resetHideCallbacks()
        }
        GeneralUtils.setButtonEnabled(requireActivity(), buttonAspectRatio, true)


        if (GeneralUtils.isPiPSupported(requireActivity())) {
            // TODO: Android 12 improvements:
            // https://developer.android.com/about/versions/12/features/pip-improvements
            mPictureInPictureParamsBuilder = PictureInPictureParams.Builder()
            updatePictureInPictureActions(
                R.drawable.ic_play_arrow_24dp, R.string.exo_controls_play_description,
                CONTROL_TYPE_PLAY,
                REQUEST_PLAY
            )
            buttonPiP = playerView!!.findViewById(R.id.pip_icon)
            buttonPiP?.setOnClickListener { view: View? ->
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    enterPiP()

            }
            GeneralUtils.setButtonEnabled(requireActivity(), buttonPiP!!, false)
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
                GeneralUtils.setViewParams(
                    binding.videoView.findViewById(R.id.exo_bottom_bar),
                    paddingLeft,
                    0,
                    paddingRight,
                    0,
                    marginLeft,
                    0,
                    marginRight,
                    0
                )
                binding.videoView.findViewById<View>(R.id.exo_progress).setPadding(
                    windowInsets.systemWindowInsetLeft, 0,
                    windowInsets.systemWindowInsetRight, 0
                )
                GeneralUtils.setViewMargins(
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


        requireActivity().let {
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

                GeneralUtils.showText(
                    playerView,
                    "Locked",
                    CustomStyledPlayerView.MESSAGE_TIMEOUT_LONG
                )

                if (locked && controllerVisible) {
                    playerView?.hideController()
                }
            }
        }


        val exoQuality =  playerView?.findViewById<ImageButton>(R.id.exo_quality)


        val fullscreenBtn=playerView?.findViewById<ImageView>(R.id.fullscreen_button)
        fullscreenBtn?.setOnClickListener {
            val orientation = this.resources.configuration.orientation
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                // code for landscape mode
            }
        }






        exoQuality?.setOnClickListener {






            val trackSelectionDialog =
                TrackSelectionDialog.createForTracksAndParameters(
                    R.string.title_dialog,
                    player!!.currentTracks,
                    DownloadHelper.getDefaultTrackSelectorParameters(requireActivity()),
                    false,
                    true,

                    {
                        trackSelector?.setParameters(it)
                    }, null)
            trackSelectionDialog.show(requireActivity().supportFragmentManager, null)


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
                GeneralUtils.hideSystemUi(playerView!!)
            }

        })

        val url = requireActivity().intent?.getStringExtra("link")?:requireActivity().intent?.data.toString()



        if (!url.contains("|") && url.contains("type=web")) getWebLink(url)


        else initializePlayer(url)

    }



    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun enterPiP() {
        val appOpsManager = requireActivity().getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager?
        if (AppOpsManager.MODE_ALLOWED != appOpsManager!!.checkOpNoThrow(
                AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                Process.myUid(),
                requireActivity().packageName
            )
        ) {
            val intent = Intent(
                "android.settings.PICTURE_IN_PICTURE_SETTINGS",
                Uri.fromParts("package", requireActivity().packageName, null)
            )
            if (requireActivity().let { intent.resolveActivity(it.packageManager) } != null) {
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
            requireActivity().enterPictureInPictureMode((mPictureInPictureParamsBuilder as PictureInPictureParams.Builder).build())

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
        if(com.masum.iptv.BuildConfig.DEBUG) Log.i("123321", " passed: " + passed)
        var url=passed?:""
        var ip=requireActivity().getSharedPreferences("recent",
            AppCompatActivity.MODE_PRIVATE
        ).getString("ip","")
        ip= URLEncoder.encode(ip, "utf-8")
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


        val params: MutableMap<String, String> = HashMap()
        if(refererValue!= null){
            params["Referer"] = refererValue
            if(com.masum.iptv.BuildConfig.DEBUG) Log.i("123321", "referer: " + refererValue)
        }
        if(originValue!= null){
            params["Origin"] = originValue
            if(com.masum.iptv.BuildConfig.DEBUG) Log.i("123321", "origin: " + originValue)
        }
        if(cookieValue!=null){
            params["cookie"] = cookieValue
            if(com.masum.iptv.BuildConfig.DEBUG) Log.i("123321", "cookie: " + cookieValue)
        }
        if (ip != null) {
            params["Sportzfy_Token"] = ip

            if(com.masum.iptv.BuildConfig.DEBUG) Log.i("123321", "Sportzfy_Token:$ip")

        }
        if (userAgentValue != null){

            params["User-Agent"] = userAgentValue
            if(com.masum.iptv.BuildConfig.DEBUG) Log.i("123321", "User-Agent: $userAgentValue")
        }





        if(headers == null){
        }
        trackSelector = requireActivity().let { DefaultTrackSelector() }
        if (Build.VERSION.SDK_INT >= 24) {
            val localeList = Resources.getSystem().configuration.locales
            val locales: MutableList<String> = java.util.ArrayList()
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
        val renderersFactory: RenderersFactory = DefaultRenderersFactory(requireActivity())
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
        // https://github.com/google/ExoPlayer/issues/8571
        val extractorsFactory = DefaultExtractorsFactory()
            .setTsExtractorFlags(DefaultTsPayloadReaderFactory.FLAG_ENABLE_HDMV_DTS_AUDIO_STREAMS)
            .setTsExtractorTimestampSearchBytes(1500 * TsExtractor.TS_PACKET_SIZE)
        player =
            ExoPlayer.Builder(requireActivity(), renderersFactory)
                .setTrackSelector(trackSelector!!)
                .setMediaSourceFactory(
                    DefaultMediaSourceFactory(
                        requireActivity()
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


        playerView!!.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
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

        try {
            var header : HashMap<String, String> = HashMap<String, String> ()
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
            if (!header.containsKey("referer")){
                if (refererValue != null) {
                    header.put("referer",refererValue)
                }
            }
            header.forEach { (name, value) ->
                if(com.masum.iptv.BuildConfig.DEBUG) Log.i("123321", "initializePlayer: header: " +name + " value: " +value)
            }
            defaultHttpDataSourceFactory.setDefaultRequestProperties(header)


        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            if(com.masum.iptv.BuildConfig.DEBUG) Log.i("123321", "initializePlayer: exception: " + e.message)
            defaultHttpDataSourceFactory.setDefaultRequestProperties(params)
        }

        // Player
        val mediaItem =
            MediaItem.fromUri(link)

        val hlsMediaSource=HlsMediaSource.Factory(defaultHttpDataSourceFactory).createMediaSource(mediaItem)
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
        if (buttonPiP != null) GeneralUtils.setButtonEnabled(requireActivity(), buttonPiP!!, true)

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
                if (GeneralUtils.isPiPSupported(requireActivity())) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (isPlaying) {
                            updatePictureInPictureActions(
                                R.drawable.ic_pause_24dp,
                                R.string.exo_controls_pause_description,
                                CONTROL_TYPE_PAUSE,
                                REQUEST_PAUSE
                            )
                        } else {
                            updatePictureInPictureActions(
                                R.drawable.ic_play_arrow_24dp,
                                R.string.exo_controls_play_description,
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

                        if(com.masum.iptv.BuildConfig.DEBUG) Log.i("123321", "onPlaybackStateChanged: video near end")
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
                                        requireActivity().getSystemService(Context.DISPLAY_SERVICE) as DisplayManager?
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
                    if(com.masum.iptv.BuildConfig.DEBUG) Log.i("123321", "onPlaybackStateChanged: video ended")


                }

                if(com.masum.iptv.BuildConfig.DEBUG) Log.i("123321", "is video ended:$playbackFinished")
            }

            override fun onPlayerError(error: PlaybackException) {
                if(com.masum.iptv.BuildConfig.DEBUG) Log.i("123321", "onPlayerError: ${error.message}")
                val message = error.message
                Log.i("123321", "onPlayerError: ${error.message}")
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


        val actions = java.util.ArrayList<RemoteAction>()
        val intent = PendingIntent.getBroadcast(
            requireActivity(), requestCode,
            Intent(ACTION_MEDIA_CONTROL).putExtra(
                EXTRA_CONTROL_TYPE,
                controlType
            ), PendingIntent.FLAG_IMMUTABLE
        )
        val icon: Icon = Icon.createWithResource(requireActivity(), iconId)
        val title = getString(resTitle)
        actions.add(RemoteAction(icon, title, title, intent))
        (mPictureInPictureParamsBuilder as PictureInPictureParams.Builder).setActions(actions)
        try {
            requireActivity().setPictureInPictureParams((mPictureInPictureParamsBuilder as PictureInPictureParams.Builder).build())
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
                            val selectionOverride =
                                DefaultTrackSelector.SelectionOverride(trackIndex, *tracks)
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
        if(com.masum.iptv.BuildConfig.DEBUG) Log.i("123321", "showError: ${error.message}")
        if (errorGeneral!=null){
            val errorDetailed: String? = when (error.type) {
                ExoPlaybackException.TYPE_SOURCE -> error.sourceException.localizedMessage
                ExoPlaybackException.TYPE_RENDERER -> error.rendererException.localizedMessage
                ExoPlaybackException.TYPE_UNEXPECTED -> error.unexpectedException.localizedMessage
                ExoPlaybackException.TYPE_REMOTE -> errorGeneral
                else -> errorGeneral
            }
       //     showSnack(errorGeneral, errorDetailed)
        }
    }

    fun showSnack(textPrimary: String?, textSecondary: String?) {
        snackbar = Snackbar.make(
            binding.main,
            textPrimary!!, Snackbar.LENGTH_LONG
        )
        if (textSecondary != null) {
            snackbar?.setAction(R.string.error_details) { v ->
                val builder =
                    android.app.AlertDialog.Builder(requireActivity())
                builder.setMessage(textSecondary)
                builder.setPositiveButton(
                    android.R.string.ok
                ) { dialogInterface: DialogInterface, i: Int -> dialogInterface.dismiss() }
                val dialog = builder.create()
                dialog.show()
            }
        }

        //  snackbar?.show()
    }

    private fun notifyAudioSessionUpdate(active: Boolean) {
        val intent =
            Intent(if (active) AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION else AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
        intent.putExtra(
            AudioEffect.EXTRA_AUDIO_SESSION,
            player?.audioSessionId
        )
        intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, requireActivity().packageName)
        if (active) {
            intent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MOVIE)
        }
        requireActivity().sendBroadcast(intent)
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
        if (Math.abs(diff) > 1000) {
            scrubbingNoticeable = true
        }
        if (scrubbingNoticeable) {
            playerView!!.clearIcon()
            playerView!!.setCustomErrorMessage(GeneralUtils.formatMilisSign(diff))
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
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O&&!requireActivity().isInPictureInPictureMode){
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

            player?.pause()
        }
        else {player?.pause()}
        super.onPause()
    }


    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {


        if (isInPictureInPictureMode) {
            // On Android TV it is required to hide controller in this PIP change callback
            playerView!!.hideController()
            playerView!!.setScale(1f)
            player?.play()
            mReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (ACTION_MEDIA_CONTROL != intent.action || player == null) {
                        if(com.masum.iptv.BuildConfig.DEBUG) Log.i("123321", "onReceive: null trigger")
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
            requireActivity().registerReceiver(
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
                requireActivity().unregisterReceiver(mReceiver)
                mReceiver = null
            }
            playerView!!.controllerAutoShow = true
            if (player != null) {
                if (player?.isPlaying == true) GeneralUtils.hideSystemUi(
                    playerView!!
                )
                else playerView!!.showController()
            }
        }
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)

    }

    override fun onDestroy() {
        super.onDestroy()
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O&&!requireActivity().isInPictureInPictureMode){
            requireActivity().let {it.requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT}

            player?.release()
        }
        else{
            player?.release()

        }
    }


    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val newOrientation = newConfig.orientation

        binding.expandableFabLayout.isVisible=newOrientation ==Configuration.ORIENTATION_PORTRAIT




        if (newOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            binding.videoView.layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                WindowCompat.setDecorFitsSystemWindows(  requireActivity().window, false)
                requireActivity().window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
                requireActivity(). window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;}
            requireActivity(). window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                    or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
            requireActivity(). window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)



        }
        else if (newOrientation == Configuration.ORIENTATION_PORTRAIT) {
            requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

            val decorView: View =  requireActivity().window.decorView
            decorView.systemUiVisibility = 0
            binding.videoView.layoutParams= ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, WrapHeight)
            requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            requireActivity().window.clearFlags( WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            requireActivity().window.clearFlags( View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
            requireActivity().window.clearFlags( View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)




        }

    }

    fun getScreenWidth(context: Context): Int {
        val displayMetrics = DisplayMetrics()
        (context as Activity).windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }
/*    fun onBackPressed() {

        val orientation = this.resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            super.onBackPressed()
            // code for portrait mode
        } else {
            // code for landscape mode

            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

        }

    }*/


    private fun getWebLink(url: String) {
        if(com.masum.iptv.BuildConfig.DEBUG) Log.i("123321", "getWebLink: $url")
        WebView(requireActivity()).apply {
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
                            UiThreadStatement.runOnUiThread {
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
                            UiThreadStatement.runOnUiThread {
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
                    UiThreadStatement.runOnUiThread {
                        view.destroy()
                        initializePlayer(url)
                    }
                }
            }
            loadUrl(url)
        }
    }

/*    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        //setIntent(intent)
        if(com.masum.iptv.BuildConfig.DEBUG) Log.i("123321", "onNewIntent: ${intent?.data}")
        intent?.data?.let {url->
            if (!url.toString().contains("|") && url.toString().contains("type=web")) getWebLink(url.toString())
            else initializePlayer(url.toString())
        }
    }*/

    val callback: OnBackPressedCallback =
        object : OnBackPressedCallback(true /* enabled by default */) {
            override fun handleOnBackPressed() {
                val orientation =   requireActivity().resources.configuration.orientation
                if (orientation == Configuration.ORIENTATION_PORTRAIT) {

                    // code for portrait mode
                } else {
                    // code for landscape mode

                    requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

                }
            }
        }





}