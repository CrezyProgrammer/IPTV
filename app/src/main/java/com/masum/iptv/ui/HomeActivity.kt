package com.masum.iptv.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.masum.iptv.ui.ui.screen.HomeScreen
import com.masum.iptv.ui.ui.screen.StreamScreen
import com.masum.iptv.ui.ui.theme.IPTVTheme
import com.masum.iptv.ui.ui.theme.Pink80
import com.masum.iptv.ui.ui.theme.Purple40
import com.masum.iptv.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

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
                Scaffold(
                    bottomBar = { BottomNavigation(navController = navController) }
                ) { innerPadding ->
                    // Apply the padding globally to the whole BottomNavScreensController
                    Box(modifier = Modifier.padding(innerPadding)) {

                        NavigationGraph(navController = navController)
                    }

                }
            }
        }
    }

    @Composable
    private fun NavigationGraph(navController: NavHostController) {
        NavHost(navController = navController, startDestination = BottomNavItem.Home.screen_route){
            composable(BottomNavItem.Home.screen_route){
                HomeScreen(viewModel)
            }
            composable(BottomNavItem.Stream.screen_route){
                StreamScreen()
            }
        }

    }

    @Composable
    private fun BottomNavigation(navController: NavHostController) {
        val items=listOf(
            BottomNavItem.Home,
            BottomNavItem.Stream
        )
        androidx.compose.material.BottomNavigation(
            backgroundColor = Purple40,
            contentColor = Pink80
        ){
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute=navBackStackEntry?.destination?.route
            items.forEach{item->
                BottomNavigationItem(
                    icon={Icon(painterResource(id = item.icon), tint = Color.White,contentDescription=item.title)},
                    label={Text(text = item.title,
                        color = Color.White,
                          fontSize=10.sp)},
                    selectedContentColor = Pink80,
                    unselectedContentColor = Pink80.copy(0.4f),
                    alwaysShowLabel = true,
                    selected = currentRoute==item.screen_route,
                    onClick = {
                        navController.navigate(item.screen_route){
                            navController.graph.startDestinationRoute?.let { screen_route ->
                                popUpTo(screen_route){
                                    saveState=true
                                }
                            }
                            launchSingleTop=true
                            restoreState=true
                        }
                    }
                )

        }

        }
    }
}

@Composable
fun Greeting2(name: String) {
    Text(text = "Hello $name!")
}


@Composable
fun DefaultPreview2() {
    IPTVTheme {
        Greeting2("Android")
    }
}