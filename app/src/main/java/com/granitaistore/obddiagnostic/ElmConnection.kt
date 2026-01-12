package com.granitaistore.obddiagnostic

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.UUID

class ElmConnection {

    private var socket: BluetoothSocket? = null
    private var output: OutputStream? = null
    private var input: BufferedReader? = null

    private val SPP_UUID: UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    suspend fun connect(device: BluetoothDevice): Boolean = withContext(Dispatchers.IO) {
        try {
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket?.connect()

            output = socket?.outputStream
            input = BufferedReader(InputStreamReader(socket?.inputStream))

            initElm()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun initElm() {
        sendCommand("ATZ")    // Reset
        sendCommand("ATE0")   // Echo off
        sendCommand("ATL0")   // Linefeeds off
        sendCommand("ATS0")   // Spaces off
        sendCommand("ATH0")   // Headers off
        sendCommand("ATSP0")  // Auto protocol
    }

    fun sendCommand(cmd: String): String {
        output?.write((cmd + "\r").toByteArray())
        output?.flush()

        val response = StringBuilder()
        var line: String?

        while (true) {
            line = input?.readLine()
            if (line == null || line.contains(">")) break
            response.append(line)
        }
        return response.toString()
    }

    fun readPID(pid: String): String {
        return sendCommand("01$pid")
    }

    fun close() {
        try {
            socket?.close()
        } catch (_: Exception) { }
    }
}
