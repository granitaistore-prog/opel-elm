package com.granitaistore.obddiagnostic.elm

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
            socket?.soTimeout = 3000

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
        send("ATZ")
        Thread.sleep(1200)
        send("ATE0")
        send("ATL0")
        send("ATS0")
        send("ATH0")
        send("ATSP0")
    }

    // Сумісність зі старим кодом
    fun send(cmd: String): String = sendCommand(cmd)

    private fun sendCommand(cmd: String): String {
        output?.write((cmd + "\r").toByteArray())
        output?.flush()

        val sb = StringBuilder()
        while (true) {
            val ch = input?.read() ?: break
            if (ch.toChar() == '>') break
            sb.append(ch.toChar())
        }
        return sb.toString().replace("\r", "").trim()
    }

    fun readPID(pid: String): String {
        return send("01$pid")
    }

    fun close() {
        try {
            input?.close()
            output?.close()
            socket?.close()
        } catch (_: Exception) {}
    }
}
