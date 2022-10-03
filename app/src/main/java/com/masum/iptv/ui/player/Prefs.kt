package com.ghdsports.india.ui.player

import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.preference.PreferenceManager
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.masum.iptv.utils.GeneralUtils
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class Prefs(val mContext: Context) {
    val mSharedPreferences: SharedPreferences
    var mediaUri: Uri? = null
    var subtitleUri: Uri? = null
    var scopeUri: Uri? = null
    var mediaType: String? = null
    var resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
    var orientation: GeneralUtils.Orientation = GeneralUtils.Orientation.VIDEO
    var scale = 1f
    var subtitleTrack = -1
    var audioTrack = -1
    var audioTrackFfmpeg = -1
    var brightness = -1
    var firstRun = true
    var askScope = true
    var autoPiP = false
    var tunneling = false
    var skipSilence = false
    var frameRateMatching: Boolean
    var repeatToggle = false
    private var positions: LinkedHashMap<String, Long>? = null
    var persistentMode = true
    var nonPersitentPosition = -1L
    private fun loadSavedPreferences() {
        if (mSharedPreferences.contains(PREF_KEY_MEDIA_URI)) mediaUri = Uri.parse(
            mSharedPreferences.getString(
                PREF_KEY_MEDIA_URI, null
            )
        )
        if (mSharedPreferences.contains(PREF_KEY_MEDIA_TYPE)) mediaType =
            mSharedPreferences.getString(
                PREF_KEY_MEDIA_TYPE, null
            )
        brightness = mSharedPreferences.getInt(PREF_KEY_BRIGHTNESS, brightness)
        firstRun = mSharedPreferences.getBoolean(PREF_KEY_FIRST_RUN, firstRun)
        if (mSharedPreferences.contains(PREF_KEY_SUBTITLE_URI)) subtitleUri = Uri.parse(
            mSharedPreferences.getString(
                PREF_KEY_SUBTITLE_URI, null
            )
        )
        if (mSharedPreferences.contains(PREF_KEY_AUDIO_TRACK)) audioTrack =
            mSharedPreferences.getInt(
                PREF_KEY_AUDIO_TRACK, audioTrack
            )
        if (mSharedPreferences.contains(PREF_KEY_AUDIO_TRACK_FFMPEG)) audioTrackFfmpeg =
            mSharedPreferences.getInt(
                PREF_KEY_AUDIO_TRACK_FFMPEG, audioTrackFfmpeg
            )
        if (mSharedPreferences.contains(PREF_KEY_SUBTITLE_TRACK)) subtitleTrack =
            mSharedPreferences.getInt(
                PREF_KEY_SUBTITLE_TRACK, subtitleTrack
            )
        if (mSharedPreferences.contains(PREF_KEY_RESIZE_MODE)) resizeMode =
            mSharedPreferences.getInt(
                PREF_KEY_RESIZE_MODE, resizeMode
            )
        orientation =
            GeneralUtils.Orientation.values().get(mSharedPreferences.getInt(PREF_KEY_ORIENTATION, 1))
        scale = mSharedPreferences.getFloat(PREF_KEY_SCALE, scale)
        if (mSharedPreferences.contains(PREF_KEY_SCOPE_URI)) scopeUri = Uri.parse(
            mSharedPreferences.getString(
                PREF_KEY_SCOPE_URI, null
            )
        )
        askScope = mSharedPreferences.getBoolean(PREF_KEY_ASK_SCOPE, askScope)
        loadUserPreferences()
    }

    fun loadUserPreferences() {
        autoPiP = mSharedPreferences.getBoolean(PREF_KEY_AUTO_PIP, autoPiP)
        tunneling = mSharedPreferences.getBoolean(PREF_KEY_TUNNELING, tunneling)
        skipSilence = mSharedPreferences.getBoolean(PREF_KEY_SKIP_SILENCE, skipSilence)
        frameRateMatching =
            mSharedPreferences.getBoolean(PREF_KEY_FRAMERATE_MATCHING, frameRateMatching)
        repeatToggle = mSharedPreferences.getBoolean(PREF_KEY_REPEAT_TOGGLE, repeatToggle)
    }

    fun updateMedia(context: Context, uri: Uri?, type: String?) {
        mediaUri = uri
        mediaType = type
        updateSubtitle(null)
        updateMeta(-1, -1, -1, AspectRatioFrameLayout.RESIZE_MODE_FIT, 1f)
        if (mediaType != null && mediaType!!.endsWith("/*")) {
            mediaType = null
        }
        if (mediaType == null) {
            if (ContentResolver.SCHEME_CONTENT == mediaUri!!.scheme) {
                mediaType = context.contentResolver.getType(mediaUri!!)
            }
        }
        if (persistentMode) {
            val sharedPreferencesEditor = mSharedPreferences.edit()
            if (mediaUri == null) sharedPreferencesEditor.remove(PREF_KEY_MEDIA_URI) else sharedPreferencesEditor.putString(
                PREF_KEY_MEDIA_URI, mediaUri.toString()
            )
            if (mediaType == null) sharedPreferencesEditor.remove(PREF_KEY_MEDIA_TYPE) else sharedPreferencesEditor.putString(
                PREF_KEY_MEDIA_TYPE, mediaType
            )
            sharedPreferencesEditor.commit()
        }
    }

    fun updateSubtitle(uri: Uri?) {
        subtitleUri = uri
        subtitleTrack = -1
        if (persistentMode) {
            val sharedPreferencesEditor = mSharedPreferences.edit()
            if (uri == null) sharedPreferencesEditor.remove(PREF_KEY_SUBTITLE_URI) else sharedPreferencesEditor.putString(
                PREF_KEY_SUBTITLE_URI, uri.toString()
            )
            sharedPreferencesEditor.remove(PREF_KEY_SUBTITLE_TRACK)
            sharedPreferencesEditor.commit()
        }
    }

    fun updatePosition(position: Long) {
        if (mediaUri == null) return
        while (positions!!.size > 100) positions!!.remove(positions!!.keys.toTypedArray()[0])
        if (persistentMode) {
            positions!![mediaUri.toString()] = position
            savePositions()
        } else {
            nonPersitentPosition = position
        }
    }

    fun updateBrightness(brightness: Int) {
        if (brightness >= -1) {
            this.brightness = brightness
            val sharedPreferencesEditor = mSharedPreferences.edit()
            sharedPreferencesEditor.putInt(PREF_KEY_BRIGHTNESS, brightness)
            sharedPreferencesEditor.commit()
        }
    }

    fun markFirstRun() {
        firstRun = false
        val sharedPreferencesEditor = mSharedPreferences.edit()
        sharedPreferencesEditor.putBoolean(PREF_KEY_FIRST_RUN, false)
        sharedPreferencesEditor.commit()
    }

    fun markScopeAsked() {
        askScope = false
        val sharedPreferencesEditor = mSharedPreferences.edit()
        sharedPreferencesEditor.putBoolean(PREF_KEY_ASK_SCOPE, false)
        sharedPreferencesEditor.commit()
    }

    private fun savePositions() {
        try {
            val fos = mContext.openFileOutput("positions", Context.MODE_PRIVATE)
            val os = ObjectOutputStream(fos)
            os.writeObject(positions)
            os.close()
            fos.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadPositions() {
        try {
            val fis = mContext.openFileInput("positions")
            val `is` = ObjectInputStream(fis)
            positions = `is`.readObject() as LinkedHashMap<String, Long>
            `is`.close()
            fis.close()
        } catch (e: Exception) {
            e.printStackTrace()
            positions = LinkedHashMap<String, Long>(10)
        }
    }

    // Return position for uri from limited scope (loaded after using Next action)
    val position: Long
        get() {
            if (!persistentMode) {
                return nonPersitentPosition
            }
            val `val` = positions!![mediaUri.toString()]
            if (`val` != null) return `val` as Long

            return 0L
        }

    fun updateOrientation() {
        val sharedPreferencesEditor = mSharedPreferences.edit()
        sharedPreferencesEditor.putInt(PREF_KEY_ORIENTATION, orientation.value)
        sharedPreferencesEditor.commit()
    }

    fun updateMeta(
        audioTrack: Int,
        audioTrackFfmpeg: Int,
        subtitleTrack: Int,
        resizeMode: Int,
        scale: Float
    ) {
        this.audioTrack = audioTrack
        this.audioTrackFfmpeg = audioTrackFfmpeg
        this.subtitleTrack = subtitleTrack
        this.resizeMode = resizeMode
        this.scale = scale
        if (persistentMode) {
            val sharedPreferencesEditor = mSharedPreferences.edit()
            if (audioTrack == -1) sharedPreferencesEditor.remove(PREF_KEY_AUDIO_TRACK) else sharedPreferencesEditor.putInt(
                PREF_KEY_AUDIO_TRACK, audioTrack
            )
            if (audioTrackFfmpeg == -1) sharedPreferencesEditor.remove(PREF_KEY_AUDIO_TRACK_FFMPEG) else sharedPreferencesEditor.putInt(
                PREF_KEY_AUDIO_TRACK_FFMPEG, audioTrackFfmpeg
            )
            if (subtitleTrack == -1) sharedPreferencesEditor.remove(PREF_KEY_SUBTITLE_TRACK) else sharedPreferencesEditor.putInt(
                PREF_KEY_SUBTITLE_TRACK, subtitleTrack
            )
            sharedPreferencesEditor.putInt(PREF_KEY_RESIZE_MODE, resizeMode)
            sharedPreferencesEditor.putFloat(PREF_KEY_SCALE, scale)
            sharedPreferencesEditor.commit()
        }
    }

    fun updateScope(uri: Uri?) {
        scopeUri = uri
        val sharedPreferencesEditor = mSharedPreferences.edit()
        if (uri == null) sharedPreferencesEditor.remove(PREF_KEY_SCOPE_URI) else sharedPreferencesEditor.putString(
            PREF_KEY_SCOPE_URI, uri.toString()
        )
        sharedPreferencesEditor.commit()
    }

    fun setPersistent(persistentMode: Boolean) {
        this.persistentMode = persistentMode
    }

    companion object {
        private const val PREF_KEY_MEDIA_URI = "mediaUri"
        private const val PREF_KEY_MEDIA_TYPE = "mediaType"
        private const val PREF_KEY_BRIGHTNESS = "brightness"
        private const val PREF_KEY_FIRST_RUN = "firstRun"
        private const val PREF_KEY_SUBTITLE_URI = "subtitleUri"
        private const val PREF_KEY_AUDIO_TRACK = "audioTrack"
        private const val PREF_KEY_AUDIO_TRACK_FFMPEG = "audioTrackFfmpeg"
        private const val PREF_KEY_SUBTITLE_TRACK = "subtitleTrack"
        private const val PREF_KEY_RESIZE_MODE = "resizeMode"
        private const val PREF_KEY_ORIENTATION = "orientation"
        private const val PREF_KEY_SCALE = "scale"
        private const val PREF_KEY_SCOPE_URI = "scopeUri"
        private const val PREF_KEY_ASK_SCOPE = "askScope"
        private const val PREF_KEY_AUTO_PIP = "autoPiP"
        private const val PREF_KEY_TUNNELING = "tunneling"
        private const val PREF_KEY_SKIP_SILENCE = "skipSilence"
        private const val PREF_KEY_FRAMERATE_MATCHING = "frameRateMatching"
        private const val PREF_KEY_REPEAT_TOGGLE = "repeatToggle"
        fun initDefaults(context: Context?) {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            if (!sharedPreferences.contains(PREF_KEY_FRAMERATE_MATCHING)) {
                val sharedPreferencesEditor = sharedPreferences.edit()
                sharedPreferencesEditor.putBoolean(
                    PREF_KEY_FRAMERATE_MATCHING,
                    false
                )
                sharedPreferencesEditor.commit()
            }
        }
    }

    init {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(
            mContext
        )
        frameRateMatching =false
        loadSavedPreferences()
        loadPositions()
    }
}