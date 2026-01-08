package com.granitaistore.obddiagnostic

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ScanScreen(nav: NavController) {
    var devices by remember { mutableStateOf(listOf<String>()) }
    val elm = remember { ElmConnection() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Scan & Connect", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(24.dp))

        Button(onClick = {
            devices = elm.getBondedDevices().map { it.name + " " + it.address }
        }) {
            Text("Scan Paired")
        }

        Spacer(Modifier.height(16.dp))

        devices.forEach { dev ->
            Button(onClick = {
                // NOTE: you should store device reference
                // elm.connect(it) ...
                nav.navigate("dashboard")
            }) {
                Text(dev)
            }
        }
    }
}
