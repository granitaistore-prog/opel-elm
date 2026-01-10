package com.granitaistore.obddiagnostic.elm

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.*
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class ElmConnection(private val device: BluetoothDevice) {

    companion object {
        private const val TAG = "ElmConnection"
        private val ELM_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private var socket: BluetoothSocket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null

    private var canListener: ((String, String) -> Unit)? = null
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ================= CONNECT =================
    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            socket = device.createRfcommSocketToServiceRecord(ELM_UUID)
            socket!!.connect()
            input = socket!!.inputStream
            output = socket!!.outputStream

            initElm()
            startCanStream()

            true
        } catch (e: Exception) {
            Log.e(TAG, "Connect failed", e)
            false
        }
    }

    fun disconnect() {
        try {
            scope.cancel()
            socket?.close()
        } catch (_: Exception) {}
    }

    // ================= INIT =================
    private suspend fun initElm() {
        send("ATZ")
        delay(800)
        send("ATE0")
        send("ATL0")
        send("ATS0")
        send("ATH1")   // CAN headers ON
        send("ATSP0")  // auto protocol
        send("ATCAF1") // auto formatting
        send("ATMA")   // monitor all CAN (raw stream)
    }

    // ================= SEND =================
    suspend fun send(cmd: String): String = withContext(Dispatchers.IO) {
        output?.write((cmd + "\r").toByteArray())
        output?.flush()
        delay(100)

        val buffer = ByteArray(4096)
        val len = input?.read(buffer) ?: 0
        String(buffer, 0, len)
    }

    // ================= PID HELPERS =================
    suspend fun readRpm(): Int {
        val r = send("010C")
        return parsePid(r) { (it[0] * 256 + it[1]) / 4 }
    }

    suspend fun readSpeed(): Int {
        val r = send("010D")
        return parsePid(r) { it[0] }
    }

    suspend fun readBoostBar(): Float {
        val r = send("010B")
        return parsePid(r) { (it[0] - 100) / 100f }
    }

    // ================= CAN RAW STREAM =================
    private fun startCanStream() {
        scope.launch {
            val buf = ByteArray(1024)
            while (isActive) {
                try {
                    val len = input?.read(buf) ?: continue
                    val data = String(buf, 0, len)
                    parseCanFrames(data)
                } catch (e: Exception) {
                    Log.e(TAG, "CAN stream error", e)
                    break
                }
            }
        }
    }

    fun setCanListener(listener: (canId: String, data: String) -> Unit) {
        canListener = listener
    }

    private fun parseCanFrames(raw: String) {
        raw.lines().forEach { line ->
            if (line.matches(Regex("^[0-9A-F]{3}.*"))) {
                val id = line.substring(0, 3)
                val data = line.substring(3).trim()
                canListener?.invoke(id, data)
            }
        }
    }

    // ================= PID PARSER =================
    private fun <T> parsePid(resp: String, calc: (List<Int>) -> T): T {
        val clean = resp.replace(" ", "")
            .replace("\r", "")
            .replace("\n", "")
            .replace(">", "")

        val idx = clean.indexOf("41")
        if (idx < 0 || clean.length < idx + 8)
            throw IllegalStateException("Invalid PID response: $resp")

        val bytes = clean.substring(idx + 4)
            .chunked(2)
            .map { it.toInt(16) }

        return calc(bytes)
    }
}
