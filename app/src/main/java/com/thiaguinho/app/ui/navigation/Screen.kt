package com.thiaguinho.app.ui.navigation

sealed class Screen(val route: String) {
    object DeviceList : Screen("device_list")
    object Dashboard : Screen("dashboard")
    object AiAnalysis : Screen("ai_analysis/{dtcCode}") {
        fun createRoute(dtcCode: String) = "ai_analysis/$dtcCode"
    }
}
