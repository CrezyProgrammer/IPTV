package com.masum.iptv

import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.exoplayer2.source.hls.playlist.HlsMediaPlaylist
import com.google.android.exoplayer2.source.hls.playlist.HlsPlaylistParser
import com.google.android.exoplayer2.util.Util
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat

import org.junit.Test
import org.junit.runner.RunWith

import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        val playlistUri: Uri = Uri.parse("https://example.com/test.m3u8")
        val playlistString = """
          #EXTM3U
#EXT-X-VERSION:3
#EXT-X-STREAM-INF:PROGRAM-ID=1,CLOSED-CAPTIONS=NONE,BANDWIDTH=250000,RESOLUTION=426x240
don't_try_to_fuck_ur_ass.php?c=Colors_Kannada_Cinema&q=250&e=.m3u8
#EXT-X-STREAM-INF:PROGRAM-ID=1,CLOSED-CAPTIONS=NONE,BANDWIDTH=400000,RESOLUTION=640x360
don't_try_to_fuck_ur_ass.php?c=Colors_Kannada_Cinema&q=400&e=.m3u8
#EXT-X-STREAM-INF:PROGRAM-ID=1,CLOSED-CAPTIONS=NONE,BANDWIDTH=600000,RESOLUTION=842x480
don't_try_to_fuck_ur_ass.php?c=Colors_Kannada_Cinema&q=600&e=.m3u8
#EXT-X-STREAM-INF:PROGRAM-ID=1,CLOSED-CAPTIONS=NONE,BANDWIDTH=800000,RESOLUTION=1024x576
don't_try_to_fuck_ur_ass.php?c=Colors_Kannada_Cinema&q=800&e=.m3u8
#EXT-X-STREAM-INF:PROGRAM-ID=1,CLOSED-CAPTIONS=NONE,BANDWIDTH=1200000,RESOLUTION=1280x720
don't_try_to_fuck_ur_ass.php?c=Colors_Kannada_Cinema&q=1200&e=.m3u8

            """.trimIndent()
        val inputStream: InputStream = ByteArrayInputStream(Util.getUtf8Bytes(playlistString))
        val playlist = HlsPlaylistParser().parse(playlistUri, inputStream)

        val mediaPlaylist = playlist as HlsMediaPlaylist
        assertThat(mediaPlaylist.version).isEqualTo(3);
    }
}