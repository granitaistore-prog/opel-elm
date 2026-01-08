package com.granitaistore.obddiagnostic

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.ComponentActivity
import com.granitaistore.obddiagnostic.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                MainNavHost()
            }
        }
    }
}
