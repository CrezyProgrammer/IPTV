package com.masum.iptv.data.fileparser

import android.content.Context
import android.util.Log
import com.masum.iptv.models.Channel
import net.bjoernpetersen.m3u.M3uParser
import net.bjoernpetersen.m3u.model.M3uEntry
import java.io.File
import java.io.IOException
import kotlin.random.Random

 fun ParseLocalFile(link: String, id: Int):List<Channel> {
    val fileEntries: List<M3uEntry> = M3uParser.parse(link)
    Log.i("123321", "ParseLocalFile: $fileEntries")
    val resolvedEntries: List<M3uEntry> = M3uParser.resolveNestedPlaylists(fileEntries)
    val list = ArrayList<Channel>()
    resolvedEntries.forEach{
        val channel=Channel(
            it.metadata["tvg-id"] ?:"${Random(1).nextInt()}",
        it.location.url.toString(),
            it.title.toString(),
            it.metadata["group-title"]?:"Unknown",
            it.metadata["tvg-logo"]?:"https://upload.wikimedia.org/wikipedia/en/4/45/BabyTV.png",
            id
        )
        list.add(channel)
    }

    return list
}
@Throws(IOException::class)
fun getFileFromAssets(context: Context, fileName: String): File = File(context.cacheDir, fileName)
    .also {

        if (!it.exists()) {
            it.outputStream().use { cache ->
                context.assets.open(fileName).use { inputStream ->
                    inputStream.copyTo(cache)
                }
            }
        }
    }