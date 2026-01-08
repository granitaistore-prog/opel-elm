package com.granitaistore.obddiagnostic

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class ElmConnection {

    companion object {
        private const val TAG = "ElmConnection"
        private val ELM_UUID: UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private var socket: BluetoothSocket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null

    private val connected = AtomicBoolean(false)

    // ================= CONNECT =================
    fun connect(device: BluetoothDevice): Boolean {
        return try {
            disconnect()

            socket = device.createRfcommSocketToServiceRecord(ELM_UUID)
            socket!!.connect()

            input = socket!!.inputStream
            output = socket!!.outputStream

            initElm()
            connected.set(true)

            Log.i(TAG, "ELM connected: ${device.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Connect error", e)
            false
        }
    }

    fun disconnect() {
        try {
            connected.set(false)
            input?.close()
            output?.close()
            socket?.close()
        } catch (_: Exception) {
        }
    }

    fun isConnected(): Boolean = connected.get()

    // ================= INIT =================
    private fun initElm() {
        send("ATZ")     // reset
        send("ATE0")    // echo off
        send("ATL0")    // linefeeds off
        send("ATS0")    // spaces off
        send("ATH1")    // headers ON (CAN RAW)
        send("ATSP0")   // auto protocol
        send("ATCAF0")  // raw CAN frames
        send("ATDPN")   // show protocol
    }

    // ================= SEND =================
    @Synchronized
    fun send(cmd: String, timeoutMs: Long = 200): String {
        if (!connected.get()) return ""

        try {
            output?.write((cmd + "\r").toByteArray())
            output?.flush()

            Thread.sleep(timeoutMs)

            val buffer = ByteArray(4096)
            val len = input?.available()?.let {
                if (it > 0) input?.read(buffer) else 0
            } ?: 0

            return if (len > 0)
                String(buffer, 0, len)
                    .replace(">", "")
                    .trim()
            else ""

        } catch (e: Exception) {
            Log.e(TAG, "Send error", e)
            connected.set(false)
            return ""
        }
    }

    // ================= PID =================
    fun readPid(pid: String): String {
        return send(pid)
    }

    // ================= RPM =================
    fun readRpm(): Int {
        val resp = send("010C")
        return parsePid(resp) {
            (it[0] * 256 + it[1]) / 4
        } ?: 0
    }

    // ================= SPEED =================
    fun readSpeed(): Int {
        val resp = send("010D")
        return parsePid(resp) { it[0] } ?: 0
    }

    // ================= BOOST (MAP) =================
    fun readBoostBar(): Float {
        val resp = send("010B")
        return parsePid(resp) {
            val kpa = it[0]
            (kpa - 100) / 100f
        } ?: 0f
    }

    // ================= CAN RAW =================
    fun readCanRaw(): List<String> {
        val raw = send("0100", 300)
        return raw.lines().filter { it.matches(Regex("^[0-9A-F]{3}.*")) }
    }

    // ================= PID PARSER =================
    private fun <T> parsePid(
        resp: String,
        calc: (List<Int>) -> T
    ): T? {
        if (resp.isEmpty()) return null

        val clean = resp.replace(" ", "")
            .replace("\r", "")
            .replace("\n", "")

        val idx = clean.indexOf("41")
        if (idx < 0 || clean.length < idx + 8) return null

        val bytes = clean.substring(idx + 4)
            .chunked(2)
            .mapNotNull {
                try {
                    it.toInt(16)
                } catch (_: Exception) {
                    null
                }
            }

        return if (bytes.isNotEmpty()) calc(bytes) else null
    }

    // ================= DEVICE LIST =================
    fun getBondedDevices(): List<BluetoothDevice> {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        return adapter?.bondedDevices?.toList() ?: emptyList()
    }
}
