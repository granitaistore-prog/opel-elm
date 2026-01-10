package com.granitaistore.obddiagnostic.elm

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class ElmConnection {

    companion object {
        private const val TAG = "ElmConnection"
        private val ELM_UUID: UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private var socket: BluetoothSocket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null
    private var device: BluetoothDevice? = null

    suspend fun connect(mac: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            device = adapter.getRemoteDevice(mac)

            socket = device!!.createRfcommSocketToServiceRecord(ELM_UUID)
            socket!!.connect()

            input = socket!!.inputStream
            output = socket!!.outputStream

            initElm()
            Log.d(TAG, "ELM connected")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Connect failed", e)
            false
        }
    }

    fun isConnected(): Boolean = socket?.isConnected == true

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            socket?.close()
        } catch (_: Exception) {}
    }

    // ================= ELM INIT =================
    private suspend fun initElm() {
        send("ATZ")
        send("ATE0")
        send("ATL0")
        send("ATS0")
        send("ATH1")   // CAN headers
        send("ATSP0")  // Auto protocol
    }

    // ================= SEND / READ =================
    suspend fun send(cmd: String, timeout: Long = 200): String = withContext(Dispatchers.IO) {
        try {
            output?.write((cmd + "\r").toByteArray())
            output?.flush()

            delay(timeout)

            val buffer = ByteArray(4096)
            val len = input?.read(buffer) ?: 0
            String(buffer, 0, len)
        } catch (e: Exception) {
            Log.e(TAG, "Send error", e)
            ""
        }
    }

    // ================= OBD PIDs =================
    suspend fun readRpm(): Int {
        val resp = send("010C")
        return parsePid(resp) { (it[0] * 256 + it[1]) / 4 }
    }

    suspend fun readSpeed(): Int {
        val resp = send("010D")
        return parsePid(resp) { it[0] }
    }

    suspend fun readBoost(): Float {
        val resp = send("010B")
        return parsePid(resp) {
            val kpa = it[0]
            (kpa - 100) / 100f
        }
    }

    // ================= CAN RAW =================
    suspend fun readCanFrame(): Pair<String, String>? {
        val raw = send("ATMA", 50) // Monitor all CAN
        val line = raw.lines().firstOrNull { it.matches(Regex("^[0-9A-F]{3}.*")) } ?: return null
        val id = line.substring(0, 3)
        val data = line.substring(4)
        return id to data
    }

    // ================= PID PARSER =================
    private fun <T> parsePid(resp: String, calc: (List<Int>) -> T): T {
        val clean = resp.replace(" ", "")
            .replace("\r", "")
            .replace("\n", "")
            .replace(">", "")

        val idx = clean.indexOf("41")
        if (idx < 0 || clean.length < idx + 8)
            throw IllegalStateException("Invalid PID: $resp")

        val bytes = clean.substring(idx + 4)
            .chunked(2)
            .map { it.toInt(16) }

        return calc(bytes)
    }

    // ================= AUTO RECONNECT =================
    suspend fun reconnect(): Boolean {
        delay(1500)
        return device?.address?.let { connect(it) } ?: false
    }
}
