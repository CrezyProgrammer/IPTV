package com.masum.iptv

import android.net.Uri
import com.google.android.exoplayer2.source.hls.playlist.HlsMediaPlaylist
import com.google.android.exoplayer2.source.hls.playlist.HlsPlaylistParser
import com.google.android.exoplayer2.util.Util
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        val playlistUri: Uri = Uri.parse("https://example.com/test.m3u8")
        val playlistString = """
            #EXTM3U
            #EXT-X-VERSION:3
            #EXT-X-PLAYLIST-TYPE:VOD
            #EXT-X-START:TIME-OFFSET=-25
            #EXT-X-TARGETDURATION:8
            #EXT-X-MEDIA-SEQUENCE:2679
            #EXT-X-DISCONTINUITY-SEQUENCE:4
            #EXT-X-ALLOW-CACHE:YES
            
            #EXTINF:7.975,
            #EXT-X-BYTERANGE:51370@0
            https://priv.example.com/fileSequence2679.ts
            
            #EXT-X-KEY:METHOD=AES-128,URI="https://priv.example.com/key.php?r=2680",IV=0x1566B
            #EXTINF:7.975,segment title
            #EXT-X-BYTERANGE:51501@2147483648
            https://priv.example.com/fileSequence2680.ts
            
            #EXT-X-KEY:METHOD=NONE
            #EXTINF:7.941,segment title .,:/# with interesting chars
            #EXT-X-BYTERANGE:51501
            https://priv.example.com/fileSequence2681.ts
            
            #EXT-X-DISCONTINUITY
            #EXT-X-KEY:METHOD=AES-128,URI="https://priv.example.com/key.php?r=2682"
            #EXTINF:7.975
            #EXT-X-BYTERANGE:51740
            https://priv.example.com/fileSequence2682.ts
            
            #EXTINF:7.975,
            https://priv.example.com/fileSequence2683.ts
            
            #EXTINF:2.002,
            https://priv.example.com/fileSequence2684.ts
            #EXT-X-ENDLIST
            """.trimIndent()
        val inputStream: InputStream = ByteArrayInputStream(Util.getUtf8Bytes(playlistString))
        val playlist = HlsPlaylistParser().parse(playlistUri, inputStream)

        val mediaPlaylist = playlist as HlsMediaPlaylist
        assertThat(mediaPlaylist.version).isEqualTo(3);
    }
}