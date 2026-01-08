package com.granitaistore.obddiagnostic.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(),
        typography = Typography(),
        content = content
    )
}
