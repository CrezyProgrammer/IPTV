package com.masum.iptv

import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.masum.iptv.library.M3uParser
import org.junit.Test


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {


    private val COMMENT_START = '#'
    private val EXTENDED_HEADER = "${COMMENT_START}EXTM3U"

    private val EXTENDED_INFO =
        """#EXTINF:(?:| )(?:| )([-]?\d+)(.*),(.+)"""

    private val infoRegex = Regex(EXTENDED_INFO)

    @Test
    fun matchLine(){
        val currentLine ="""#EXTINF:-1 tvg-id="ABTV.bd" tvg-logo="https://i.imgur.com/ytH4Ncu.png" group-title="General",AB TV (720p) [Not 24/7]
https://cdn.appv.jagobd.com:444/c3VydmVyX8RpbEU9Mi8xNy8yMDE0GIDU6RgzQ6NTAgdEoaeFzbF92YWxIZTO0U0ezN1IzMyfvcGVMZEJCTEFWeVN3PTOmdFsaWRtaW51aiPhnPTI/abtvusa.stream/playlist.m3u8"""
        val newMatch = infoRegex.matchEntire(currentLine)
        val testRegex=Regex("/group-title/")
        val test=infoRegex.matchEntire(currentLine)
        assert(newMatch==null)
        assertThat(test).isEqualTo("tu")

    }


    @Test fun test()
    {
        val dummy=      """#EXTINF:(?:| )(?:| )([-]?\d+)(.*),(.+)"""
        val currentLine ="#EXTINF:-1 tvg-id=\"ABTV.bd\" tvg-logo=\"https://i.imgur.com/ytH4Ncu.png\" group-title=\"General\",AB TV (720p) [Not 24/7]"
            val date_pattern = "#EXTINF: ([-]?\\d+)(.*),(.+)"
        val test = Regex(date_pattern).matchEntire(currentLine)


        assertThat(test!=null).isTrue()


    }    @Test
    fun addition_isCorrect() {
        val playlistString = """
          #EXTM3U 

          #EXTINF: -1 tvg-chno="1" tvg-logo="https://telegra.ph/file/56629885c4c8a71d6b097.png" group-title="𝐈𝐍𝐅𝐎𝐑𝐌𝐀𝐓𝐈𝐎𝐍 𝐋𝐎𝐂𝐀𝐋 𝐈𝐏𝐓𝐕", 𝐈𝐍𝐅𝐎𝐑𝐌𝐀𝐓𝐈𝐎𝐍 • ℓινє 1

          https://playlist.localiptv.tk/Information_LIVE_1.m3u8

          #EXTINF: -1 tvg-chno="2" tvg-logo="https://telegra.ph/file/56629885c4c8a71d6b097.png" group-title="𝐈𝐍𝐅𝐎𝐑𝐌𝐀𝐓𝐈𝐎𝐍 𝐋𝐎𝐂𝐀𝐋 𝐈𝐏𝐓𝐕", 𝐈𝐍𝐅𝐎𝐑𝐌𝐀𝐓𝐈𝐎𝐍 • ℓινє 2

          https://playlist.localiptv.tk/Information_LIVE_2.m3u8

          #EXTINF: -1 tvg-chno="3" tvg-logo="https://telegra.ph/file/56629885c4c8a71d6b097.png" group-title="𝐈𝐍𝐅𝐎𝐑𝐌𝐀𝐓𝐈𝐎𝐍 𝐋𝐎𝐂𝐀𝐋 𝐈𝐏𝐓𝐕", 𝐈𝐍𝐅𝐎𝐑𝐌𝐀𝐓𝐈𝐎𝐍 • ℓινє 3

          https://playlist.localiptv.tk/Information_LIVE_3.m3u8

          #EXTINF: -1 tvg-logo="https://telegra.ph/file/1a6235d583851e601c3a8.png" group-title="📺 (ʟɪᴠᴇ ɪᴘ ᴛᴠ sᴛᴏʀᴇ) 📺", 🇱 🇮 🇻 🇪 | ℓοϲαℓ ѕροяτѕ 1

          https://playlist.localiptv.tk/LIVE_SPORTS_1.m3u8

      

          ---------------------------------------------

          LOCAL IPTV LAST Update 15-09-22

          #EXTINF:-1 tvg-logo="xxx" group-title="M3U FILE BY RIFAT HASAN join us TELEGRAM CHANNEL @Local_IPTV_STORE",

          JOIN TELEGRAM CHANNEL: https://t.me/Local_IPTV_STORE

          JOIN TELEGRAM CHANNEL:

          https://t.me/Local_IPTV_SONY_LIV

          JOIN TELEGRAM GROUP:

          https://t.me/Local_IPTV_Discussion_GROUP

            """.trimIndent()

        val v2=com.masum.iptv.library.M3uParser.parse(playlistString)

        assertThat(v2[0].toString()).contains("10");
        assertThat(v2[0].metadata.entries.toString()).contains("1.0")
    }
}