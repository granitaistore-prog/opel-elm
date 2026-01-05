package com.granitaistore.obddiagnostic

import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context

class BluetoothChooser(private val context: Context) {

    fun show(onSelected: (BluetoothDevice) -> Unit) {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        val devices = adapter?.bondedDevices?.toList() ?: emptyList()

        val names = devices.map { "${it.name}\n${it.address}" }

        AlertDialog.Builder(context)
            .setTitle("Select ELM327")
            .setItems(names.toTypedArray()) { _, index ->
                onSelected(devices[index])
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
