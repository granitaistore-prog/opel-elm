package com.granitaistore.obddiagnostic.obd

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class BluetoothService {

    private val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var socket: BluetoothSocket? = null
    var input: InputStream? = null
    var output: OutputStream? = null

    private val ELM_UUID: UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun connect(): Boolean {
        val device = findElmDevice() ?: return false
        socket = device.createRfcommSocketToServiceRecord(ELM_UUID)
        adapter?.cancelDiscovery()
        socket?.connect()

        input = socket?.inputStream
        output = socket?.outputStream
        return true
    }

    fun send(cmd: String): String {
        output?.write((cmd + "\r").toByteArray())
        Thread.sleep(200)

        val buffer = ByteArray(1024)
        val len = input?.read(buffer) ?: 0
        return String(buffer, 0, len)
    }

    private fun findElmDevice(): BluetoothDevice? {
        return adapter?.bondedDevices?.firstOrNull {
            it.name.contains("ELM", true) ||
            it.name.contains("OBD", true)
        }
    }

    fun close() {
        socket?.close()
    }
}
