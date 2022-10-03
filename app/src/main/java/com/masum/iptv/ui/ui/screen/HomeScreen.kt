package com.masum.iptv.ui.ui.screen

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leinardi.android.speeddial.compose.FabWithLabel
import com.leinardi.android.speeddial.compose.SpeedDial
import com.leinardi.android.speeddial.compose.SpeedDialOverlay
import com.leinardi.android.speeddial.compose.SpeedDialState
import com.masum.iptv.R
import com.masum.iptv.models.Playlist
import com.masum.iptv.ui.ui.theme.*
import com.masum.iptv.viewmodels.MainViewModel
import kotlinx.coroutines.launch
import org.intellij.lang.annotations.JdkConstants.HorizontalAlignment

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun HomeScreen(mainViewModel: MainViewModel= viewModel()) {
    val speedDialState = rememberSaveable { mutableStateOf(SpeedDialState.Collapsed) }
    val speedDialVisible = rememberSaveable { mutableStateOf(true) }
    val reverseAnimationOnClose = rememberSaveable { mutableStateOf(false) }
    val overlayVisible = rememberSaveable { mutableStateOf(speedDialState.value.isExpanded()) }
    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()
    

    isShowDialog= remember { mutableStateOf(false)}
   isUrlDialog= remember { mutableStateOf(true)}
    viewModel=mainViewModel


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
    ) { scaffoldPadding -> ScaffoldContent(scaffoldPadding, overlayVisible, speedDialState) }
}

lateinit var isShowDialog : MutableState<Boolean>
lateinit var viewModel: MainViewModel
lateinit var isUrlDialog : MutableState<Boolean>
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
        val context = LocalContext.current
        SpeedDial(
            state = speedDialState.value,
            onFabClick = { expanded ->
                closeSpeedDial(overlayVisible, speedDialState)
                if (expanded) {
                    showToast(context, "ItemCallback")
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
                FabWithLabel(
                    onClick = {
                       closeSpeedDial(overlayVisible, speedDialState)
                        isUrlDialog.value = false

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
        val playlist = remember { mutableStateOf("") }
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
                        value = playlist.value,
                        onValueChange = {
                            playlist.value = it

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
                    val playlist=Playlist(0,url.value,playlist.value, isUrlDialog.value)
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
private fun ScaffoldContent(
    scaffoldPadding: PaddingValues,
    overlayVisible: MutableState<Boolean>,
    speedDialState: MutableState<SpeedDialState>,
) {
    val playlist by viewModel.getPlaylist().observeAsState(listOf())


    val coroutineScope = rememberCoroutineScope()
    LazyColumn( ) {
     items(playlist){item ->
         ListItem(item.title)

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

private fun showToast(context: Context, text: String) {
    Toast.makeText(context, text, Toast.LENGTH_LONG).show()
}
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun ListItem(title: String="Test",description: String="Test description long line multple line content://com.android.externalstorage.documents/document/primary%3Atest.m3u") {

    ElevatedCard(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom=8.dp,start=16.dp,end=16.dp),
    ) {
        Column(Modifier.padding(8.dp)) {
            Text(text = title, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ){
                Text(
                    modifier = Modifier
                        .weight(1f),
                    text =description,
                    fontSize = 13.sp,
                    color = Color.DarkGray)
                Icon(Icons.Default.Edit, contentDescription ="Edit", tint = Color.Black )

            }
        }


    }


    
}




