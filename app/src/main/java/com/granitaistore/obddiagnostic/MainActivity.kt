package com.granitaistore.obddiagnostic

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnSelectElm = findViewById<Button>(R.id.btnSelectElm)
        val btnReadDtc = findViewById<Button>(R.id.btnReadDtc)
        val btnClearDtc = findViewById<Button>(R.id.btnClearDtc)
        val trip = findViewById<TextView>(R.id.trip)

        btnSelectElm.setOnClickListener {
            trip.text = "Select ELM327 (stub)"
        }

        btnReadDtc.setOnClickListener {
            trip.text = "Read DTC (stub)"
        }

        btnClearDtc.setOnClickListener {
            trip.text = "Clear DTC (stub)"
        }
    }
}
package com.granitaistore.obddiagnostic

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.*
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class MainActivity : AppCompatActivity() {

    // ===== UI =====
    private lateinit var rpmView: TextView
    private lateinit var speedView: TextView
    private lateinit var tripView: TextView
    private lateinit var rpmGauge: ProgressBar
    private lateinit var speedGauge: ProgressBar
    private lateinit var startLogBtn: Button
    private lateinit var stopLogBtn: Button
    private lateinit var btnSelectElm: Button
    private lateinit var btnReadDtc: Button
    private lateinit var btnClearDtc: Button

    // ===== Bluetooth / ELM =====
    private val btAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private lateinit var socket: BluetoothSocket
    private lateinit var input: InputStream
    private lateinit var output: OutputStream
    private lateinit var lastDevice: BluetoothDevice

    private val SPP_UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // ===== Coroutine =====
    private val obdScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var liveRunning = false

    // ===== CSV =====
    private lateinit var csvFile: File

    // ===== Permissions =====
    private val BT_PERMS = arrayOf(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN
    )

    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindUi()
        checkPermissions()

        btnSelectElm.setOnClickListener { showElmPicker() }

        startLogBtn.setOnClickListener {
            initCsv()
            startLiveLoop()
        }

        stopLogBtn.setOnClickListener {
            stopLiveLoop()
        }

        btnReadDtc.setOnClickListener {
            obdScope.launch {
                val dtc = readDtc()
                withContext(Dispatchers.Main) {
                    tripView.text = dtc.joinToString("\n")
                }
            }
        }

        btnClearDtc.setOnClickListener {
            send("04")
            tripView.text = "DTC cleared"
        }
    }

    // =========================================================
    // UI
    private fun bindUi() {
        rpmView = findViewById(R.id.rpm)
        speedView = findViewById(R.id.speed)
        tripView = findViewById(R.id.trip)
        rpmGauge = findViewById(R.id.rpmGauge)
        speedGauge = findViewById(R.id.speedGauge)
        startLogBtn = findViewById(R.id.startLogBtn)
        stopLogBtn = findViewById(R.id.stopLogBtn)
        btnSelectElm = findViewById(R.id.btnSelectElm)
        btnReadDtc = findViewById(R.id.btnReadDtc)
        btnClearDtc = findViewById(R.id.btnClearDtc)
    }

    // =========================================================
    // Permissions
    private fun checkPermissions() {
        if (BT_PERMS.any {
                ActivityCompat.checkSelfPermission(this, it)
                        != PackageManager.PERMISSION_GRANTED
            }) {
            ActivityCompat.requestPermissions(this, BT_PERMS, 101)
        }
    }

    // =========================================================
    // Bluetooth picker
    private fun showElmPicker() {
        val devices = btAdapter.bondedDevices.toList()
        if (devices.isEmpty()) {
            toast("No paired ELM devices")
            return
        }

        val names = devices.map { "${it.name}\n${it.address}" }

        AlertDialog.Builder(this)
            .setTitle("Select ELM327")
            .setItems(names.toTypedArray()) { _, i ->
                lastDevice = devices[i]
                connectToElm(lastDevice)
            }
            .show()
    }

    // =========================================================
    // Connect
    private fun connectToElm(device: BluetoothDevice) {
        obdScope.launch {
            try {
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()

                input = socket.inputStream
                output = socket.outputStream

                initElm()

                withContext(Dispatchers.Main) {
                    toast("Connected: ${device.name}")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    toast("ELM connection failed")
                }
            }
        }
    }

    private fun reconnect() {
        try { socket.close() } catch (_: Exception) {}
        Thread.sleep(1000)
        connectToElm(lastDevice)
    }

    // =========================================================
    // ELM
    private fun initElm() {
        send("ATZ")
        send("ATE0")
        send("ATL0")
        send("ATS0")
        send("ATH0")
        send("ATSP0")
    }

    private fun send(cmd: String): String {
        output.write((cmd + "\r").toByteArray())
        Thread.sleep(150)
        val buf = ByteArray(1024)
        val len = input.read(buf)
        return String(buf, 0, len)
    }

    // =========================================================
    // LIVE LOOP
    private fun startLiveLoop() {
        if (!::output.isInitialized) {
            toast("Connect ELM first")
            return
        }

        liveRunning = true
        obdScope.launch {
            while (liveRunning) {
                try {
                    val rpm = readRpm()
                    val speed = readSpeed()
                    logCsv(rpm, speed)

                    withContext(Dispatchers.Main) {
                        rpmView.text = "RPM\n$rpm"
                        speedView.text = "SPEED\n$speed"
                        rpmGauge.progress = rpm
                        speedGauge.progress = speed
                    }

                    delay(500)
                } catch (e: Exception) {
                    reconnect()
                }
            }
        }
    }

    private fun stopLiveLoop() {
        liveRunning = false
    }

    // =========================================================
    // PID
    private fun readRpm(): Int {
        val r = send("010C").replace(" ", "")
        val d = r.substringAfter("410C")
        val A = d.substring(0, 2).toInt(16)
        val B = d.substring(2, 4).toInt(16)
        return ((A * 256) + B) / 4
    }

    private fun readSpeed(): Int {
        val r = send("010D").replace(" ", "")
        return r.substringAfter("410D").toInt(16)
    }

    private fun readDtc(): List<String> {
        val r = send("03").replace(" ", "")
        val data = r.substringAfter("43")
        return data.chunked(4)
            .filter { it != "0000" }
            .map { "P$it" }
    }

    // =========================================================
    // CSV
    private fun initCsv() {
        val dir = File(getExternalFilesDir(null), "logs")
        if (!dir.exists()) dir.mkdirs()

        csvFile = File(dir, "obd_${System.currentTimeMillis()}.csv")
        csvFile.appendText("time,rpm,speed\n")
    }

    private fun logCsv(rpm: Int, speed: Int) {
        csvFile.appendText("${System.currentTimeMillis()},$rpm,$speed\n")
    }

    // =========================================================
    private fun toast(msg: String) {
        runOnUiThread {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }
}
