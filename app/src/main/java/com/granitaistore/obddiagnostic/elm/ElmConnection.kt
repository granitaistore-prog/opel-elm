package com.granitaistore.obddiagnostic.elm

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.UUID

class ElmConnection(private val device: BluetoothDevice) {

    private var socket: BluetoothSocket? = null
    private var reader: BufferedReader? = null
    private var output: OutputStream? = null

    private val uuid: UUID = device.uuids[0].uuid

    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            socket = device.createRfcommSocketToServiceRecord(uuid)
            socket?.connect()

            reader = BufferedReader(InputStreamReader(socket!!.inputStream))
            output = socket!!.outputStream

            initElm()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun send(cmd: String) {
        output?.write((cmd + "\r").toByteArray())
        output?.flush()
        Thread.sleep(100)
    }

    private fun readLine(): String? {
        return reader?.readLine()
    }

    private fun initElm() {
        send("ATZ")      // reset
        send("ATE0")    // echo off
        send("ATL0")    // linefeeds off
        send("ATS0")    // spaces off
        send("ATH1")    // headers on (CAN ID)
        send("ATSP0")   // auto protocol (ISO + CAN)
        send("ATCAF0")  // raw CAN
        send("ATMA")    // start CAN monitor
    }

    suspend fun startCanStream(onFrame: (canId: String, data: String) -> Unit) =
        withContext(Dispatchers.IO) {
            try {
                while (true) {
                    val line = readLine() ?: continue
                    if (line.length > 5 && line.contains(" ")) {
                        val parts = line.split(" ")
                        val id = parts[0]
                        val data = parts.drop(1).joinToString(" ")
                        onFrame(id, data)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    fun sendPid(pid: String) {
        send(pid)
    }

    fun disconnect() {
        try {
            send("ATPC") // protocol close
            socket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
