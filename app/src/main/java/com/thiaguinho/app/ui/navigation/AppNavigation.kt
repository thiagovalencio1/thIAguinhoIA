package com.thiaguinho.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.thiaguinho.app.ui.screens.AiAnalysisScreen
import com.thiaguinho.app.ui.screens.DashboardScreen
import com.thiaguinho.app.ui.screens.DeviceListScreen
import com.thiaguinho.app.ui.viewmodels.MainUiState

@Composable
fun AppNavigation(
    state: MainUiState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onDeviceClick: (String) -> Unit,
    onDisconnect: () -> Unit,
    onGetAiAnalysis: (String) -> Unit,
    onClearAiResult: () -> Unit
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.DeviceList.route) {
        composable(route = Screen.DeviceList.route) {
            DeviceListScreen(
                state = state,
                onStartScan = onStartScan,
                onStopScan = onStopScan,
                onDeviceClick = { address ->
                    onDeviceClick(address)
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.DeviceList.route) { inclusive = true }
                    }
                }
            )
        }
        composable(route = Screen.Dashboard.route) {
            DashboardScreen(
                state = state,
                onDisconnect = {
                    onDisconnect()
                    navController.navigate(Screen.DeviceList.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                },
                onDtcClick = { dtcCode ->
                    navController.navigate(Screen.AiAnalysis.createRoute(dtcCode))
                }
            )
        }
        composable(
            route = Screen.AiAnalysis.route,
            arguments = listOf(navArgument("dtcCode") { type = NavType.StringType })
        ) { backStackEntry ->
            val dtcCode = backStackEntry.arguments?.getString("dtcCode") ?: "N/A"
            AiAnalysisScreen(
                dtcCode = dtcCode,
                state = state,
                onGetAnalysis = onGetAiAnalysis,
                onNavigateBack = {
                    onClearAiResult()
                    navController.popBackStack()
                }
            )
        }
    }
}
