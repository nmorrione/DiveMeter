package com.nmorrione.divemeter.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nmorrione.divemeter.ui.home.HomeScreen
import com.nmorrione.divemeter.ui.manualentry.ManualEntryScreen
import com.nmorrione.divemeter.ui.settings.SettingsScreen
import com.nmorrione.divemeter.ui.videocalc.VideoCalcScreen

object DiveMeterDestinations {
    const val HOME = "home"
    const val MANUAL_ENTRY = "manual_entry"
    const val VIDEO_CALC = "video_calc"
    const val SETTINGS = "settings"
}

@Composable
fun DiveMeterNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = DiveMeterDestinations.HOME) {
        composable(DiveMeterDestinations.HOME) {
            HomeScreen(
                onNavigateToManualEntry = { navController.navigate(DiveMeterDestinations.MANUAL_ENTRY) },
                onNavigateToVideoCalc = { navController.navigate(DiveMeterDestinations.VIDEO_CALC) },
                onNavigateToSettings = { navController.navigate(DiveMeterDestinations.SETTINGS) }
            )
        }
        composable(DiveMeterDestinations.MANUAL_ENTRY) {
            ManualEntryScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(DiveMeterDestinations.VIDEO_CALC) {
            VideoCalcScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(DiveMeterDestinations.SETTINGS) {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
