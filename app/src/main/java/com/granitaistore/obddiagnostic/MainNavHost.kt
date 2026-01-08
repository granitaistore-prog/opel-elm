package com.granitaistore.obddiagnostic

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable

@Composable
fun MainNavHost() {
    val nav = rememberNavController()

    NavHost(nav, startDestination = "scan") {
        composable("scan") { ScanScreen(nav) }
        composable("dashboard") { DashboardScreen(nav) }
        composable("canraw") { CanRawScreen(nav) }
    }
}
