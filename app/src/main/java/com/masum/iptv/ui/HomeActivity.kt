package com.masum.iptv.ui

import android.app.Activity
import android.content.Intent
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.masum.iptv.models.Playlist
import com.masum.iptv.ui.ui.screen.ChannelScreen
import com.masum.iptv.ui.ui.screen.HomeScreen
import com.masum.iptv.ui.ui.screen.StreamScreen
import com.masum.iptv.ui.ui.theme.IPTVTheme
import com.masum.iptv.ui.ui.theme.Pink80
import com.masum.iptv.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import me.rosuh.filepicker.config.FilePickerManager
import java.io.File

@AndroidEntryPoint
class HomeActivity : ComponentActivity() {
     val viewModel: MainViewModel by viewModels()
    @OptIn(ExperimentalMaterial3Api::class)
    lateinit var drawerState: DrawerState
    lateinit var checked: MutableState<Boolean>

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        super.onCreate(savedInstanceState)
        setContent {
            MainScreenView()
        }
    }

    @Preview
    @Composable
    private fun MainScreenView() {
        val context= LocalContext.current
        val preference=context.getSharedPreferences("theme", MODE_PRIVATE)
        val systemTheme=isSystemInDarkTheme()
        checked = remember { mutableStateOf(preference.getBoolean("theme", systemTheme)) }


        IPTVTheme(checked.value) {
            // A surface container using the 'background' color from the theme
            // MainContent()
            DrawerContent()
        }
    }
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainContent() {

            val navController = rememberNavController()
            val bottomBarState = rememberSaveable { (mutableStateOf(true)) }
            val scope = rememberCoroutineScope()


            Scaffold(


                bottomBar = { BottomNavigation(navController = navController,bottomBarState) }
            ) { innerPadding ->
                // Apply the padding globally to the whole BottomNavScreensController
                Box(modifier = Modifier.padding(innerPadding)) {

                    NavigationGraph(navController = navController,bottomBarState)
                }

            }
        }


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun NavigationGraph(
        navController: NavHostController,
        bottomBarState: MutableState<Boolean>
    ) {
        NavHost(navController = navController, startDestination = BottomNavItem.Home.screen_route){
            composable(BottomNavItem.Home.screen_route){
                bottomBarState.value=true
                HomeScreen(viewModel,drawerState=drawerState) {id,location->
                    navController.navigate("${BottomNavItem.Channel.screen_route}/$id/$location")
                }
            }
            composable(BottomNavItem.Stream.screen_route){
                StreamScreen(drawerState=drawerState)
            }
            composable("${BottomNavItem.Channel.screen_route}/{id}/{location}",
            arguments = listOf(
                navArgument("id"){type= NavType.IntType},
                navArgument("location"){type= NavType.StringType},

                )){
                bottomBarState.value=false
                ChannelScreen(navController=navController,id=it.arguments?.getInt("id") ,location = it.arguments?.getString("location"),
                mainViewModel=viewModel)
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
            backgroundColor =MaterialTheme.colorScheme.primary,
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun DrawerContent() {

        drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()
// icons to mimic drawer destinations
        BackHandler(enabled = drawerState.isOpen) {
            scope.launch {
                drawerState.close()
            }
        }

        DismissibleNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                DismissibleDrawerSheet {
                    Spacer(Modifier.height(12.dp))

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.DarkMode, contentDescription = null) },
                        label = {   Row(modifier = Modifier
                            .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("Night Mode", fontWeight = FontWeight.Bold,fontSize =14.sp)

                            val context= LocalContext.current
                            val preference=context.getSharedPreferences("theme", MODE_PRIVATE).edit()
                            Switch(
                                modifier = Modifier.semantics { contentDescription = "Demo" },
                                checked = checked.value,
                                onCheckedChange = { checked.value = it
                                    preference.putBoolean("theme",it).apply()
                                    scope.launch { drawerState.close() }

                                })


                        }},
                        onClick = {
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp),
                        selected =false)

                }
            },
            content = {
                MainContent()
            }
        )

    }

}





