package com.granitaistore.obddiagnostic

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var rpmText: TextView
    private lateinit var speedText: TextView
    private lateinit var btnSelectElm: Button
    private lateinit var btnReadDtc: Button
    private lateinit var btnClearDtc: Button

    private var socket: BluetoothSocket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null
    private var lastDevice: BluetoothDevice? = null

    private val ELM_UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rpmText = findViewById(R.id.rpm)
        speedText = findViewById(R.id.speed)
        btnSelectElm = findViewById(R.id.btnSelectElm)
        btnReadDtc = findViewById(R.id.btnReadDtc)
        btnClearDtc = findViewById(R.id.btnClearDtc)

        ensurePermissions()

        btnSelectElm.setOnClickListener {
            selectElmDevice {
                lastDevice = it
                connect(it)
            }
        }

        btnReadDtc.setOnClickListener {
            thread {
                val dtc = readDtc()
                runOnUiThread {
                    showDtcDialog(dtc)
                }
            }
        }

        btnClearDtc.setOnClickListener {
            thread {
                clearDtc()
                runOnUiThread {
                    toast("DTC cleared")
                }
            }
        }
    }

    // ================= Permissions =================
    private fun ensurePermissions() {
        val perms = arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val missing = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 101)
        }
    }

    // ================= Device chooser =================
    private fun selectElmDevice(onSelected: (BluetoothDevice) -> Unit) {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        val devices = adapter.bondedDevices.toList()
        val names = devices.map { "${it.name}\n${it.address}" }

        AlertDialog.Builder(this)
            .setTitle("Select ELM327")
            .setItems(names.toTypedArray()) { _, i ->
                onSelected(devices[i])
            }
            .show()
    }

    // ================= Connect =================
    private fun connect(device: BluetoothDevice) {
        thread {
            try {
                socket = device.createRfcommSocketToServiceRecord(ELM_UUID)
                socket!!.connect()
                input = socket!!.inputStream
                output = socket!!.outputStream
                initElm()
                runOnUiThread { toast("ELM connected") }
            } catch (e: Exception) {
                runOnUiThread { toast("ELM error") }
            }
        }
    }

    // ================= ELM init =================
    private fun initElm() {
        send("ATZ")
        send("ATE0")
        send("ATL0")
        send("ATS0")
        send("ATH0")
        send("ATSP0")
    }

    // ================= Send =================
    private fun send(cmd: String): String {
        return try {
            output?.write((cmd + "\r").toByteArray())
            Thread.sleep(200)
            val buf = ByteArray(1024)
            val len = input?.read(buf) ?: 0
            String(buf, 0, len)
        } catch (e: Exception) {
            ""
        }
    }

    // ================= READ DTC =================
    private fun readDtc(): List<String> {
        val raw = send("03")
            .replace(" ", "")
            .replace("\r", "")
            .replace("\n", "")
            .replace(">", "")

        if (!raw.startsWith("43")) return emptyList()

        val dtc = mutableListOf<String>()
        var i = 2

        while (i + 4 <= raw.length) {
            val a = raw.substring(i, i + 2)
            val b = raw.substring(i + 2, i + 4)
            i += 4

            if (a == "00" && b == "00") break

            val code = decodeDtc(a, b)
            dtc.add(code)
        }

        return dtc
    }

    private fun decodeDtc(a: String, b: String): String {
        val A = a.toInt(16)
        val B = b.toInt(16)

        val type = when (A shr 6) {
            0 -> "P"
            1 -> "C"
            2 -> "B"
            else -> "U"
        }

        val digit1 = (A shr 4) and 0x3
        val digit2 = A and 0xF
        val digit3 = B shr 4
        val digit4 = B and 0xF

        return "$type$digit1$digit2$digit3$digit4"
    }

    // ================= CLEAR DTC =================
    private fun clearDtc() {
        send("04")
    }

    // ================= UI =================
    private fun showDtcDialog(list: List<String>) {
        val msg = if (list.isEmpty())
            "No DTC errors"
        else
            list.joinToString("\n")

        AlertDialog.Builder(this)
            .setTitle("DTC")
            .setMessage(msg)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
