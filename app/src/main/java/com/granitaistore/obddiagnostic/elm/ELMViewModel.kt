package com.granitaistore.obddiagnostic.elm

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.UUID

class ElmConnection(private val device: BluetoothDevice) {

    private val TAG = "ElmConnection"
    private val ELM_UUID: UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private var socket: BluetoothSocket? = null
    private var reader: BufferedReader? = null
    private var output: OutputStream? = null
    private var isRunning = false

    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            socket = device.createRfcommSocketToServiceRecord(ELM_UUID)
            socket?.connect()

            reader = BufferedReader(InputStreamReader(socket!!.inputStream))
            output = socket!!.outputStream

            initElm()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Connect error", e)
            false
        }
    }

    private fun initElm() {
        send("ATZ")
        send("ATE0")
        send("ATL0")
        send("ATS0")
        send("ATH1")      // CAN headers ON
        send("ATSP0")    // Auto protocol
        send("ATCAF0")   // RAW CAN
        send("ATAL")     // Allow long frames
    }

    fun disconnect() {
        try {
            isRunning = false
            reader?.close()
            output?.close()
            socket?.close()
        } catch (_: Exception) {}
    }

    fun send(cmd: String, delayMs: Long = 100): String {
        return try {
            output?.write((cmd + "\r").toByteArray())
            Thread.sleep(delayMs)

            val sb = StringBuilder()
            while (reader?.ready() == true) {
                sb.append(reader!!.readLine()).append("\n")
            }
            sb.toString().trim()
        } catch (e: Exception) {
            Log.e(TAG, "Send error: $cmd", e)
            ""
        }
    }

    // ============ OBD стандартні PID ============
    fun readRpm(): Int {
        val resp = send("010C")
        val bytes = parseBytes(resp)
        return if (bytes.size >= 2)
            ((bytes[0] * 256) + bytes[1]) / 4
        else 0
    }

    fun readSpeed(): Int {
        val resp = send("010D")
        val bytes = parseBytes(resp)
        return if (bytes.isNotEmpty()) bytes[0] else 0
    }

    // ============ RAW CAN STREAM ============
    suspend fun startCanStream(onFrame: (id: String, data: String) -> Unit) =
        withContext(Dispatchers.IO) {
            isRunning = true
            send("ATMA") // Monitor all CAN

            while (isRunning) {
                val line = reader?.readLine() ?: continue
                if (line.length >= 5 && line.contains(" ")) {
                    val parts = line.split(" ", limit = 2)
                    onFrame(parts[0], parts[1])
                }
            }
        }

    fun stopCanStream() {
        isRunning = false
        send("AT") // stop monitor
    }

    // ============ PARSER ============
    private fun parseBytes(resp: String): List<Int> {
        val clean = resp.replace(" ", "").replace("\n", "")
        if (!clean.contains("41")) return emptyList()

        val data = clean.substringAfter("41").substring(2)
        return data.chunked(2).mapNotNull {
            try { it.toInt(16) } catch (_: Exception) { null }
        }
    }
}
