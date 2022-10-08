package com.masum.iptv.ui.ui.screen

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.ixuea.android.downloader.DownloadService
import com.ixuea.android.downloader.callback.DownloadListener
import com.ixuea.android.downloader.domain.DownloadInfo
import com.ixuea.android.downloader.exception.DownloadException
import com.masum.iptv.R
import com.masum.iptv.data.fileparser.ParseLocalFile
import com.masum.iptv.ui.VideoPlayerActivity
import com.masum.iptv.ui.home.ui.theme.Purple80
import com.masum.iptv.ui.ui.theme.PurpleGrey40
import com.masum.iptv.utils.*
import com.masum.iptv.viewmodels.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter

@OptIn(ExperimentalMaterial3Api::class)
@Composable

fun ChannelScreen(
    navController: NavHostController,
    location: String?,
    mainViewModel: MainViewModel,
    id: Int?
) {
   Scaffold(
        topBar = {
            TopAppBar(
                title = {
                   Text(
                        "Channels",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Localized description"
                        )
                    }
                }
            )
        }){
        Box(Modifier.padding(it)){
            val context=LocalContext.current

            location?.let { location->
                val coroutineScope = rememberCoroutineScope()
                val openDialog = remember { mutableStateOf(false) }
                coroutineScope.launch(Dispatchers.IO){
                    if(!location.contains("http")){
                        viewModel.insertList(ParseLocalFile(location, id!!))
                        openDialog.value = true
                    }
                    else{
                        val targetFile = File(context.cacheDir, "${System.currentTimeMillis()}.m3u")
                        if(location.contains("m3u")){
                            val downloadManager = DownloadService.getDownloadManager(context)

                            val downloadInfo = DownloadInfo.Builder().setUrl(location)
                                .setPath(targetFile.absolutePath)
                                .build()

//set download callback.

//set download callback.
                            downloadInfo!!.downloadListener = object : DownloadListener {
                                override fun onStart() {

                                }

                                override fun onWaited() {
                                }

                                override fun onPaused() {
                                }

                                override fun onDownloading(progress: Long, size: Long) {
                                }

                                override fun onRemoved() {
                                }

                                override fun onDownloadSuccess() {
                                    openDialog.value=true
                                     coroutineScope.launch (
                                         Dispatchers.IO
                                             ){
                                         val list= ParseLocalFile(targetFile.absolutePath,id!!)

                                         viewModel.insertList(list)
                                     }


                                }

                                override fun onDownloadFailed(e: DownloadException) {
                                    Log.i("123321", "onDownloadFailed: ${e.code}")
                                    openDialog.value=true

                                    Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                                }
                            }

//submit download info to download manager.

//submit download info to download manager.
                            downloadManager.download(downloadInfo)

                        }
                        else{
                            val queue: RequestQueue = Volley.newRequestQueue(context)

                            val stringRequest = StringRequest(
                                Request.Method.GET,
                                location, {
                                    Log.i("123321", "playlistDownloader: response :$it")

                                    val writer = FileWriter(targetFile)
                                    writer.append(it)
                                    writer.flush()
                                    writer.close()
                                    coroutineScope.launch(Dispatchers.IO){

                                        val list= ParseLocalFile(targetFile.absolutePath, id!!)

                                        viewModel.insertList(list)
                                        openDialog.value = true
                                    }

                                },
                                {
                                    it.printStackTrace()
                                    Log.i("123321", "playlistDownloader: error:${it.message}")

                                    openDialog.value = true
                                    Toast.makeText(context, "Download failed:${it.message}", Toast.LENGTH_SHORT).show()
                                })
                            queue.add(stringRequest)
                        }
                    }

                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                   if(!openDialog.value){
                       CircularProgressIndicator()
                   }
                }



                Column {
                    val query: MutableState<String> =remember { mutableStateOf("") }
                    SuggestionChipLayout(query,id!!)
                    gridList(query, id, LocalContext.current)
                }

            }


        }
    }
}
    @Preview
    @Composable
    fun gridListPreview() {
       ChannelScreen(
           navController = rememberNavController(),
           location ="test",
           mainViewModel = viewModel,
           id = 0
       )
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun gridList(query: MutableState<String>, id: Int,context: Context) {
    val channels by viewModel.getAllChannels(query.value,id=id).observeAsState(listOf())
        LazyVerticalGrid(
            columns = GridCells.Fixed(3)
        ){

            items(channels){item->
                
               Card(
                   modifier = Modifier.padding(5.dp),
                   border = BorderStroke(2.dp, Purple80),
                   shape = RoundedCornerShape(10.dp),
                   onClick = {
                       val videoIntent=(Intent( context, VideoPlayerActivity::class.java))

                       videoIntent.putExtra(LINK,item.location)


                       context.startActivity(videoIntent)
                   }
               ) {
                   Column(
                       modifier = Modifier
                           .fillMaxSize()
                           .padding(5.dp),
                       horizontalAlignment = Alignment.CenterHorizontally
                   ) {
                       val context= LocalContext.current
                        val coilGetRequestBuilder  =
                               ImageRequest.Builder(context)
                                   .data(item.logo)
                                   .placeholder(R.drawable.demo)
                                   .crossfade(true)
                                   .build()

                       Image(
                           painter = rememberAsyncImagePainter(
                               coilGetRequestBuilder),
                           contentDescription = null,
                           modifier = Modifier
                               .size(75.dp)
                               .padding(5.dp)
                       )
                       Text(
                           modifier = Modifier
                               .padding(5.dp)
                               .fillMaxWidth(),
                           textAlign = TextAlign.Center,
                           fontSize = 15.sp,
                           maxLines = 1,
                           overflow =TextOverflow.Ellipsis,
                           text = item.title)
                   }
               }
                
            }
        }

}

@Composable
fun SuggestionChipLayout(query: MutableState<String>, id: Int) {
    val chips by viewModel.getCategoryList(id=id).observeAsState(listOf())

   var chipState by remember { mutableStateOf("All") }

    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
       LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
        ) {
           item {
               SuggestionChipEachRow(chip = "All", "All" == chipState) { chip ->
                   query.value=""
                   chipState = chip
               }
           }
           items(chips){

               SuggestionChipEachRow(chip = it, it == chipState) { chip ->
                   query.value=chip
                   chipState = chip
               }
           }

        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionChipEachRow(
    chip: String,
    selected: Boolean,
    onChipState: (String) -> Unit
) {

    val systemTheme= androidx.compose.material.MaterialTheme.colors.isLight
    SuggestionChip(onClick = {
        if (!selected)
            onChipState(chip)
        else
            onChipState("")
    }, label = {
        Text(text = chip)
    },
        border = SuggestionChipDefaults.suggestionChipBorder(
            borderWidth = 1.dp,
            borderColor = if (selected) Color.Transparent else MaterialTheme.colorScheme.primary
        ),

        modifier = Modifier.padding(horizontal = 6.dp),
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,

        ),
        shape = RoundedCornerShape(16.dp)
    )

}

