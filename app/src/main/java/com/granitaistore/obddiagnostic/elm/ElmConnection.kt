package com.granitaistore.obddiagnostic.elm

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class ElmConnection {

    private val TAG = "ElmConnection"
    private val ELM_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private var socket: BluetoothSocket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null

    var isConnected = false
        private set

    suspend fun connect(device: BluetoothDevice): Boolean = withContext(Dispatchers.IO) {
        try {
            socket = device.createRfcommSocketToServiceRecord(ELM_UUID)
            socket!!.connect()

            input = socket!!.inputStream
            output = socket!!.outputStream

            initElm()
            isConnected = true
            Log.d(TAG, "ELM connected")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Connect error", e)
            disconnect()
            false
        }
    }

    fun disconnect() {
        try {
            isConnected = false
            input?.close()
            output?.close()
            socket?.close()
        } catch (_: Exception) {}
    }

    // ================= AT INIT =================
    private suspend fun initElm() {
        send("ATZ")     // Reset
        send("ATE0")    // Echo off
        send("ATL0")    // Linefeeds off
        send("ATS0")    // Spaces off
        send("ATH1")    // Headers on (CAN IDs)
        send("ATSP0")   // Auto protocol
        send("ATCAF0")  // Raw CAN
        send("ATCRA7E8")// Listen engine ECU
    }

    // ================= SEND =================
    suspend fun send(cmd: String, timeout: Long = 200): String = withContext(Dispatchers.IO) {
        try {
            output?.write((cmd + "\r").toByteArray())
            output?.flush()
            Thread.sleep(timeout)
            read()
        } catch (e: Exception) {
            Log.e(TAG, "Send error", e)
            ""
        }
    }

    // ================= READ =================
    private fun read(): String {
        val buffer = ByteArray(1024)
        val sb = StringBuilder()

        try {
            while (input!!.available() > 0) {
                val len = input!!.read(buffer)
                sb.append(String(buffer, 0, len))
            }
        } catch (_: Exception) {}

        return sb.toString().replace(">", "").trim()
    }

    // ================= OBD PIDS =================
    suspend fun getRPM(): Int {
        val r = send("010C")
        val bytes = parseHex(r)
        return if (bytes.size >= 2) ((bytes[0] * 256) + bytes[1]) / 4 else 0
    }

    suspend fun getSpeed(): Int {
        val r = send("010D")
        val bytes = parseHex(r)
        return if (bytes.isNotEmpty()) bytes[0] else 0
    }

    // ================= CAN RAW STREAM =================
    suspend fun readCanFrame(): CanFrame? {
        val line = read()
        if (line.matches(Regex("^[0-9A-F]{3}.*"))) {
            val id = line.substring(0, 3)
            val data = line.substring(3).trim()
            return CanFrame(id, data)
        }
        return null
    }

    // ================= UTILS =================
    private fun parseHex(resp: String): List<Int> {
        val clean = resp.replace(" ", "").replace("410C", "").replace("410D", "")
        return clean.chunked(2).mapNotNull {
            try { it.toInt(16) } catch (_: Exception) { null }
        }
    }
}

data class CanFrame(
    val id: String,
    val data: String
)
