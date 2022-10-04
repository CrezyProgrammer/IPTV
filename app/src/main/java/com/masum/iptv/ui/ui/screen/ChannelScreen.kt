package com.masum.iptv.ui.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.masum.iptv.R
import com.masum.iptv.data.fileparser.ParseLocalFile
import com.masum.iptv.models.Channel
import com.masum.iptv.ui.home.ui.theme.Purple80
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable

fun ChannelScreen(navController: NavHostController, location: String?) {
    androidx.compose.material.Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    androidx.compose.material.Text(
                        "Simple TopAppBar",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Localized description"
                        )
                    }
                }
            )
        }){
        Box(modifier = Modifier.padding(it)){
            location?.let { location->
                var mainList :MutableState<List<Channel>> = mutableStateOf(listOf())
                val coroutineScope = rememberCoroutineScope()
                val openDialog = remember { mutableStateOf(false) }
                coroutineScope.launch(Dispatchers.IO){

                     mainList.value=ParseLocalFile(location)
                    if(mainList.value.isNotEmpty())openDialog.value =true

                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                   if(!openDialog.value){
                       CircularProgressIndicator()
                   }
                }



                    gridList(mainList)

            }


        }
    }
}
    @Preview
    @Composable
    fun gridListPreview() {
       ChannelScreen(navController = rememberNavController(),location ="test")
    }

@Composable
fun gridList(list: MutableState<List<Channel>>) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3)
        ){
            items(list.value){item->
                
               Card(
                   modifier = Modifier.padding(5.dp),
                   border = BorderStroke(2.dp, Purple80),
                   shape = RoundedCornerShape(10.dp),
                   colors = CardDefaults.cardColors(Color.White)
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

