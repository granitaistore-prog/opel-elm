package com.granitaistore.obddiagnostic

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*

@Composable
fun CanRawScreen(nav: NavController) {
    var logs by remember { mutableStateOf(listOf<String>()) }

    Column {
        Text("CAN RAW Monitor", style = MaterialTheme.typography.headlineMedium)

        Button(onClick = {
            // elm.readCanRaw() -> logs
        }) {
            Text("Refresh")
        }

        LazyColumn {
            items(logs) {
                Text(it)
            }
        }
    }
}
