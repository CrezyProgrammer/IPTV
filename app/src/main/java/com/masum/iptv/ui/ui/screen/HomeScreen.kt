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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leinardi.android.speeddial.compose.FabWithLabel
import com.leinardi.android.speeddial.compose.SpeedDial
import com.leinardi.android.speeddial.compose.SpeedDialState
import com.masum.iptv.R
import com.masum.iptv.models.Playlist
import com.masum.iptv.ui.ui.theme.*
import com.masum.iptv.viewmodels.MainViewModel
import kotlinx.coroutines.launch
import me.rosuh.filepicker.bean.FileItemBeanImpl
import me.rosuh.filepicker.config.AbstractFileFilter
import me.rosuh.filepicker.config.FilePickerManager
import me.rosuh.filepicker.filetype.AudioFileType
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    mainViewModel: MainViewModel = viewModel(),
    drawerState: DrawerState,
    onClick: (Int, String) -> Unit,
) {
    val speedDialState = rememberSaveable { mutableStateOf(SpeedDialState.Collapsed) }
    val speedDialVisible = rememberSaveable { mutableStateOf(true) }
    val reverseAnimationOnClose = rememberSaveable { mutableStateOf(false) }
    val overlayVisible = rememberSaveable { mutableStateOf(speedDialState.value.isExpanded()) }





    isShowDialog= remember { mutableStateOf(false)}
   isUrlDialog= remember { mutableStateOf(true)}
    viewModel=mainViewModel
    click = onClick
    @OptIn(ExperimentalMaterial3Api::class)
    val scope = rememberCoroutineScope()
    // icons to mimic drawer destinations




    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {

                        scope.launch { drawerState.open()}

                    }) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Localized description"
                        )
                    }
                },
                title = {
                    Text(
                    //    style = MaterialTheme.typography.caption.copy(fontSize = 64.sp),
                        text="Playlist",

                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
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
var click: ((Int,String) -> Unit?)? =null

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterialApi::class)
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

            fabOpenedContent = { Icon(Icons.Default.Close,tint= Color.White, contentDescription =  null) },
            fabClosedContent = { Icon(Icons.Default.Add, tint= Color.White, contentDescription = null) },
            fabAnimationRotateAngle = 90f,

            fabOpenedBackgroundColor = MaterialTheme.colorScheme.primary,
            fabClosedBackgroundColor = MaterialTheme.colorScheme.primary,
            labelContent = { Text("Close") },

            reverseAnimationOnClose = reverseAnimationOnClose.value,
            labelBackgroundColor = Color.Transparent,
            labelContainerElevation = 0.dp

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
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,

                    text = "Enter Playlist details")
            },



            text = {
                Column() {

                    OutlinedTextField(
                        singleLine = true,
                        modifier = Modifier.padding(5.dp),
                        label = {Text( "Enter Playlist name") },
                        value = playlistValue.value,
                        trailingIcon = {
                            if (playlistValue.value.isNotBlank())
                                IconButton(
                                    onClick = {
                                        playlistValue.value = ""
                                    },
                                ) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "",
                                        tint = Color.Black
                                    )
                                }
                            else null
                        },

                        onValueChange = {
                            playlistValue.value = it

                        }
                    )
                    OutlinedTextField(
                        singleLine = true,
                        modifier = Modifier.padding(5.dp),
                        label = {Text( "Enter Playlist url") },
                        value = url.value,
                        trailingIcon = {
                            if (url.value.isNotBlank())
                                IconButton(
                                    onClick = {
                                        url.value = ""
                                    },
                                ) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "",
                                        tint = Color.Black
                                    )
                                }
                            else null
                        },
                        onValueChange = {
                            url.value = it

                        }
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { openDialog.value = false })
                { Text(text = "Cancel") }


            },

            confirmButton = {

                TextButton(onClick = {
                    openDialog.value = false
                    val playlist=Playlist(0,url.value,playlistValue.value, isUrlDialog.value,System.currentTimeMillis())
                    coroutineScope.launch {
                        viewModel.insertPlaylist(playlist)
                    }

                })
                { Text(text = "Create")
                }

            })
    }

}

@Composable
private fun ScaffoldContent(scaffoldPadding: PaddingValues) {
    val playlist by viewModel.getPlaylist().observeAsState(listOf())



    LazyColumn(Modifier.padding(scaffoldPadding) ) {
     items(playlist){item ->
         ListItem(playlist = item,
             onClick = {
                 click?.let { it1 -> it1(item.id,URLEncoder.encode(it.location,"utf-8")) }
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
                    fontSize = 13.sp
                )
                Icon(Icons.Default.Edit, contentDescription ="Edit",
                    modifier = Modifier.clickable {
                        urlValue.value=playlist.title
                        locationValue.value=playlist.location
                       openDialog.value =true
                    })
                if(openDialog.value){
                    if(openDialog.value){
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = {
                                openDialog.value = false
                            },
                            title = {
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    text = "Enter Playlist details")
                            },


                            text = {
                                Column() {
                                    OutlinedTextField(
                                        singleLine = true,
                                        modifier = Modifier.padding(5.dp),
                                        label = {Text( "Enter Playlist name") },
                                        value = urlValue.value,
                                        trailingIcon = {
                                            if (urlValue.value.isNotBlank())
                                                IconButton(
                                                    onClick = {
                                                        urlValue.value = ""
                                                    },
                                                ) {
                                                    Icon(
                                                        Icons.Default.Clear,
                                                        contentDescription = ""
                                                    )
                                                }
                                            else null
                                        },
                                        onValueChange = {
                                            urlValue.value = it

                                        }
                                    )
                                    OutlinedTextField(
                                        singleLine = true,
                                        modifier = Modifier.padding(5.dp),
                                        label = {Text( "Enter Playlist url") },
                                        value = locationValue.value,
                                        trailingIcon = {
                                            if (locationValue.value.isNotBlank())
                                                IconButton(
                                                    onClick = {
                                                        locationValue.value = ""
                                                    },
                                                ) {
                                                    Icon(
                                                        Icons.Default.Clear,
                                                        contentDescription = ""
                                                    )
                                                }
                                            else null
                                        },
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
                                    TextButton(onClick = {
                                        openDialog.value = false



                                    })
                                    { Text(text = "Cancel")
                                    }
                                    TextButton(onClick = {
                                        openDialog.value = false

                                        coroutineScope.launch {
                                            viewModel.deletePlaylist(playlist)
                                        }

                                    })
                                    { Text(text = "Delete")
                                    }
                                    TextButton(onClick = {
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
                                    { Text(text = "Update")
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






