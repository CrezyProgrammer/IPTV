package com.masum.iptv.ui.ui.screen

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.leinardi.android.speeddial.compose.FabWithLabel
import com.leinardi.android.speeddial.compose.SpeedDial
import com.leinardi.android.speeddial.compose.SpeedDialState
import com.masum.iptv.R
import com.masum.iptv.models.Playlist
import com.masum.iptv.ui.BottomNavItem
import com.masum.iptv.ui.ui.theme.*
import com.masum.iptv.viewmodels.MainViewModel
import kotlinx.coroutines.launch
import me.rosuh.filepicker.bean.FileItemBeanImpl
import me.rosuh.filepicker.config.AbstractFileFilter
import me.rosuh.filepicker.config.FilePickerManager
import me.rosuh.filepicker.filetype.AudioFileType
import okio.ByteString.Companion.encode
import okio.ByteString.Companion.encodeUtf8
import java.net.URLEncoder
import java.util.ArrayList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(mainViewModel: MainViewModel = viewModel(),  onClick: (String) -> Unit) {

    val speedDialState = rememberSaveable { mutableStateOf(SpeedDialState.Collapsed) }
    val speedDialVisible = rememberSaveable { mutableStateOf(true) }
    val reverseAnimationOnClose = rememberSaveable { mutableStateOf(false) }
    val overlayVisible = rememberSaveable { mutableStateOf(speedDialState.value.isExpanded()) }
    val scaffoldState = rememberScaffoldState()




    isShowDialog= remember { mutableStateOf(false)}
   isUrlDialog= remember { mutableStateOf(true)}
    viewModel=mainViewModel
    click = onClick


    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = {
                    Text(
                        "Simple TopAppBar",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Localized description"
                        )
                    }
                }
            )
        },

       floatingActionButton = {
            FloatingActionButton(
                speedDialState,
                overlayVisible,
                speedDialVisible,
                reverseAnimationOnClose,
            )
        },
    ) { scaffoldPadding -> ScaffoldContent(scaffoldPadding) }
}

lateinit var isShowDialog : MutableState<Boolean>
lateinit var viewModel: MainViewModel
lateinit var isUrlDialog : MutableState<Boolean>
var click: ((String) -> Unit?)? =null

@OptIn(ExperimentalMaterialApi::class, ExperimentalAnimationApi::class)
@Composable
private fun FloatingActionButton(
    speedDialState: MutableState<SpeedDialState>,
    overlayVisible: MutableState<Boolean>,
    speedDialVisible: MutableState<Boolean>,
    reverseAnimationOnClose: MutableState<Boolean>,
) {
    val openDialog = remember { mutableStateOf(false) }

    ShowDialog(openDialog)
    BackHandler(enabled = speedDialState.value.isExpanded()) {
        closeSpeedDial(overlayVisible, speedDialState)
    }
    AnimatedVisibility(
        visible = speedDialVisible.value,
        enter = scaleIn(),
        exit = scaleOut(),
    ) {
        SpeedDial(
            state = speedDialState.value,
            onFabClick = { expanded ->
                closeSpeedDial(overlayVisible, speedDialState)
                if (expanded) {
                }
            },

            fabOpenedContent = { Icon(Icons.Default.Close,tint= Purple40, contentDescription =  null) },
            fabClosedContent = { Icon(Icons.Default.Add, tint= Purple40, contentDescription = null) },
            fabAnimationRotateAngle = 90f,

            fabOpenedBackgroundColor = Purple80,
            fabClosedBackgroundColor = Purple80,
            labelContent = { Text("Close") },
            reverseAnimationOnClose = reverseAnimationOnClose.value,

        ) {
            item {
                FabWithLabel(
                    labelContent = { Text(stringResource(R.string.label_add_action)) },
                    labelBackgroundColor = Color.Transparent,
                    labelContainerElevation = 0.dp,
                    fabBackgroundColor = Color(0XFF4CAF50),
                    onClick = {
                        closeSpeedDial(overlayVisible, speedDialState)
                        isUrlDialog.value =true
                        openDialog.value =true
                    },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_insert_link_24),
                        contentDescription = null,
                        tint = MaterialTheme.colors.onPrimary,

                        )
                }
            }
            item {
                val context=LocalContext.current as ComponentActivity
                FabWithLabel(
                    onClick = {
                       closeSpeedDial(overlayVisible, speedDialState)
                        FilePickerManager
                            .from(context)

                            .filter(fileFilter)

                            .enableSingleChoice()
                            .forResult(FilePickerManager.REQUEST_CODE)

                    },
                    labelContent = {
                        Text(
                            text = stringResource(R.string.label_custom_color)
                        )
                    },
                    labelBackgroundColor = Color.Transparent,
                    labelContainerElevation = 0.dp,

                    fabBackgroundColor = Color(0XFFFFFFFF),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_audio_file_24),
                        contentDescription = null,
                        tint = Color(0XFF4285F4),
                    )
                }
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable

fun ShowDialog(openDialog: MutableState<Boolean>) {
    if (openDialog.value) {
        val coroutineScope = rememberCoroutineScope()
        val playlistValue = remember { mutableStateOf("") }
        val url = remember { mutableStateOf("") }

        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                openDialog.value = false
            },
            title = {
                androidx.compose.material3.Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    text = "Enter Playlist details")
            },


            text = {
                Column() {
                    androidx.compose.material3.TextField(
                        modifier = Modifier.padding(5.dp),
                        label = {Text( "Enter Playlist name") },
                        value = playlistValue.value,
                        onValueChange = {
                            playlistValue.value = it

                        }
                    )
                    androidx.compose.material3.TextField(
                        modifier = Modifier.padding(5.dp),
                        label = {Text( "Enter Playlist url") },
                        value = url.value,
                        onValueChange = {
                            url.value = it

                        }
                    )
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { openDialog.value = false })
                { androidx.compose.material3.Text(text = "Cancel") }


            },

            confirmButton = {

                androidx.compose.material3.TextButton(onClick = {
                    openDialog.value = false
                    val playlist=Playlist(0,url.value,playlistValue.value, isUrlDialog.value,System.currentTimeMillis())
                    coroutineScope.launch {
                        viewModel.insertPlaylist(playlist)
                    }

                })
                { androidx.compose.material3.Text(text = "Create")
                }

            })
    }

}

@Composable
private fun ScaffoldContent(scaffoldPadding: PaddingValues) {
    val playlist by viewModel.getPlaylist().observeAsState(listOf())



    LazyColumn( ) {
     items(playlist){item ->
         ListItem(playlist = item,
             onClick = {
                 click?.let { it1 -> it1(URLEncoder.encode(it.location,"utf-8")) }
         })

     }
    }

}

private fun closeSpeedDial(
    overlayVisible: MutableState<Boolean>,
    speedDialState: MutableState<SpeedDialState>,
) {
    speedDialState.value = speedDialState.value.toggle()
    overlayVisible.value = speedDialState.value.isExpanded()
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListItem(
    playlist: Playlist = Playlist(
        id = 0,
        title = "Test",
        location = "Test description long line multple line content://com.android.externalstorage.documents/document/primary%3Atest.m3u",
        isURL = true,
        lastModified = System.currentTimeMillis()
    ),
    onClick: (Playlist)-> Unit

) {

    ElevatedCard(
        onClick={
            onClick(playlist)
        },
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
    ) {
        val openDialog = remember { mutableStateOf(false) }
        val urlValue= remember { mutableStateOf(playlist.title) }
        val locationValue= remember { mutableStateOf(playlist.location) }
        val coroutineScope = rememberCoroutineScope()
        Column(Modifier.padding(8.dp)) {
            Text(text = playlist.title, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ){


                Text(
                    modifier = Modifier
                        .weight(1f),
                    text =playlist.location,
                    fontSize = 13.sp,
                    color = Color.DarkGray)
                Icon(Icons.Default.Edit, contentDescription ="Edit", tint = Color.Black,
                    modifier = Modifier.clickable {
                       openDialog.value =true
                    })
                if(openDialog.value){
                    if(openDialog.value){
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = {
                                openDialog.value = false
                            },
                            title = {
                                androidx.compose.material3.Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    text = "Enter Playlist details")
                            },


                            text = {
                                Column() {
                                    OutlinedTextField(
                                        modifier = Modifier.padding(5.dp),
                                        label = {Text( "Enter Playlist name") },
                                        value = urlValue.value,
                                        onValueChange = {
                                            urlValue.value = it

                                        }
                                    )
                                    OutlinedTextField(
                                        modifier = Modifier.padding(5.dp),
                                        label = {Text( "Enter Playlist url") },
                                        value = locationValue.value,
                                        onValueChange = {
                                            locationValue.value = it

                                        }
                                    )
                                }
                            },

                            confirmButton = {

                                Row(modifier = Modifier
                                    .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween) {
                                    androidx.compose.material3.TextButton(onClick = {
                                        openDialog.value = false



                                    })
                                    { androidx.compose.material3.Text(text = "Cancel")
                                    }
                                    androidx.compose.material3.TextButton(onClick = {
                                        openDialog.value = false

                                        coroutineScope.launch {
                                            viewModel.deletePlaylist(playlist)
                                        }

                                    })
                                    { androidx.compose.material3.Text(text = "Delete")
                                    }
                                    androidx.compose.material3.TextButton(onClick = {
                                        openDialog.value = false

                                        coroutineScope.launch {
                                            val newPlaylist =    Playlist(
                                                playlist.id,
                                                locationValue.value,
                                                urlValue.value,
                                                playlist.isURL,
                                                System.currentTimeMillis()
                                            )

                                            viewModel.updatePlaylist(
                                            newPlaylist
                                            )
                                        }

                                    })
                                    { androidx.compose.material3.Text(text = "Update")
                                    }
                                }

                            })
                    }

                }

            }
        }


    }





}

val fileFilter = object : AbstractFileFilter() {
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






