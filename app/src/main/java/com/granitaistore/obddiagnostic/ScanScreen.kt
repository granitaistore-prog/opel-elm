package com.granitaistore.obddiagnostic

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ScanScreen(
    viewModel: ElmViewModel = viewModel(),
    onConnected: () -> Unit
) {
    val pairedDevices = remember { getPairedDevices() }
    val connectionState by viewModel.connectionState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Виберіть ELM327", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(pairedDevices) { device ->
                DeviceItem(device) {
                    viewModel.connect(device)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (connectionState) {
            is ConnectionState.Connected -> {
                Text("Підключено", color = MaterialTheme.colorScheme.primary)
                LaunchedEffect(Unit) { onConnected() }
            }
            is ConnectionState.Connecting -> {
                Text("Підключення...")
            }
            is ConnectionState.Error -> {
                Text("Помилка підключення", color = MaterialTheme.colorScheme.error)
            }
            else -> {}
        }
    }
}

@Composable
fun DeviceItem(device: BluetoothDevice, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(device.name ?: "Невідомий пристрій")
            Text(device.address, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun getPairedDevices(): List<BluetoothDevice> {
    val adapter = BluetoothAdapter.getDefaultAdapter()
    return adapter?.bondedDevices?.toList() ?: emptyList()
}
