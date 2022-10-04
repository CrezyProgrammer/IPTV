package com.masum.iptv.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.masum.iptv.data.fileparser.ParseLocalFile
import com.masum.iptv.models.Playlist
import com.masum.iptv.ui.ui.screen.ChannelScreen
import com.masum.iptv.ui.ui.screen.HomeScreen
import com.masum.iptv.ui.ui.screen.StreamScreen
import com.masum.iptv.ui.ui.theme.IPTVTheme
import com.masum.iptv.ui.ui.theme.Pink80
import com.masum.iptv.ui.ui.theme.Purple40
import com.masum.iptv.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import me.rosuh.filepicker.config.FilePickerManager
import java.io.File

@AndroidEntryPoint
class HomeActivity : ComponentActivity() {
     val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreenView()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Preview
    @Composable
    private fun MainScreenView() {
        IPTVTheme {
            // A surface container using the 'background' color from the theme
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                val navController = rememberNavController()
                val bottomBarState = rememberSaveable { (mutableStateOf(true)) }
                Scaffold(
                    bottomBar = { BottomNavigation(navController = navController,bottomBarState) }
                ) { innerPadding ->
                    // Apply the padding globally to the whole BottomNavScreensController
                    Box(modifier = Modifier.padding(innerPadding)) {

                        NavigationGraph(navController = navController,bottomBarState)
                    }

                }
            }
        }
    }

    @Composable
    private fun NavigationGraph(
        navController: NavHostController,
        bottomBarState: MutableState<Boolean>
    ) {
        NavHost(navController = navController, startDestination = BottomNavItem.Home.screen_route){
            composable(BottomNavItem.Home.screen_route){
                bottomBarState.value=true
                HomeScreen(viewModel) {
                    navController.navigate("${BottomNavItem.Channel.screen_route}/$it")
                }
            }
            composable(BottomNavItem.Stream.screen_route){
                StreamScreen()
            }
            composable("${BottomNavItem.Channel.screen_route}/{location}",
            arguments = listOf(navArgument("location"){type= NavType.StringType})){
                bottomBarState.value=false
                ChannelScreen(navController,it.arguments?.getString("location"))
            }
        }

    }

    @Composable
    private fun BottomNavigation(navController: NavHostController, bottomBarState: MutableState<Boolean>) {
        val items = listOf(
            BottomNavItem.Home,
            BottomNavItem.Stream
        )
        AnimatedVisibility(
            visible = bottomBarState.value,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            content = {
        androidx.compose.material.BottomNavigation(
            backgroundColor = Purple40,
            contentColor = Pink80
        ) {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            items.forEach { item ->
                BottomNavigationItem(
                    icon = {
                        Icon(
                            painterResource(id = item.icon),
                            tint = Color.White,
                            contentDescription = item.title
                        )
                    },
                    label = {
                        Text(
                            text = item.title,
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    },
                    selectedContentColor = Pink80,
                    unselectedContentColor = Pink80.copy(0.4f),
                    alwaysShowLabel = true,
                    selected = currentRoute == item.screen_route,
                    onClick = {
                        navController.navigate(item.screen_route) {
                            navController.graph.startDestinationRoute?.let { screen_route ->
                                popUpTo(screen_route) {
                                    saveState = true
                                }
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )

            }

        }
    })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            FilePickerManager.REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    val list = FilePickerManager.obtainData()
                    val item=list[0]
                   lifecycleScope.launch {
                       viewModel.insertPlaylist(Playlist(0,item, File(item).name,false,System.currentTimeMillis()))
                   }
                    //val channelList= ParseLocalFile(list[0])

                    // do your work
                } else {
                    Toast.makeText(this, "You didn't choose anything~", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

}





