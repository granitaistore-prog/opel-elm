package com.granitaistore.obddiagnostic

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    var rpm by remember { mutableStateOf(0) }
    var speed by remember { mutableStateOf(0) }
    var boost by remember { mutableStateOf(0f) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Live Dashboard", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(16.dp))

        Text("RPM: $rpm")
        Text("Speed: $speed km/h")
        Text("Boost: $boost bar")

        Spacer(Modifier.height(24.dp))

        Button(onClick = { nav.navigate("canraw") }) {
            Text("CAN RAW Monitor")
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            rpm += 10
            speed += 1
            boost += 0.01f
        }
    }
}
