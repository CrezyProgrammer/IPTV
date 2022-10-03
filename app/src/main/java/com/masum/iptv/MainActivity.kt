package com.masum.iptv

import android.content.res.Configuration
import android.media.audiofx.LoudnessEnhancer
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.masum.iptv.ui.player.BrightnessControl
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.material.snackbar.Snackbar
import com.masum.iptv.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    companion object {
        var locked: Boolean = false
        var restoreControllerTimeout = false
        val CONTROLLER_TIMEOUT = 3500
        var snackbar: Snackbar? = null
        var player: ExoPlayer? = null
        var controllerVisibleFully = false
        var mBrightnessControl: BrightnessControl? = null
        var boostLevel = 0
        var loudnessEnhancer: LoudnessEnhancer? = null
        var controllerVisible = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
       // setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val newOrientation = newConfig.orientation
        binding.navView.isVisible=newOrientation ==Configuration.ORIENTATION_PORTRAIT
    }
}