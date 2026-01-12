package com.granitaistore.obddiagnostic

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun MainNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "scan"
    ) {

        composable("scan") {
            ScanScreen(
                onConnected = {
                    navController.navigate("dashboard") {
                        popUpTo("scan") { inclusive = true }
                    }
                }
            )
        }

        composable("dashboard") {
            DashboardScreen()
        }
    }
}
