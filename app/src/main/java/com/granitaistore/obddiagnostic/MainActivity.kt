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

    private val btAdapter = BluetoothAdapter.getDefaultAdapter()
    private lateinit var socket: BluetoothSocket
    private lateinit var input: InputStream
    private lateinit var output: OutputStream
    private lateinit var lastDevice: BluetoothDevice

    private val SPP_UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var liveRunning = false

    private lateinit var csvFile: File

    private val BT_PERMS = arrayOf(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN
    )

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
            liveRunning = false
        }
    }

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

    private fun checkPermissions() {
        if (BT_PERMS.any {
                ActivityCompat.checkSelfPermission(this, it)
                        != PackageManager.PERMISSION_GRANTED
            }) {
            ActivityCompat.requestPermissions(this, BT_PERMS, 100)
        }
    }

    private fun showElmPicker() {
        val devices = btAdapter.bondedDevices.toList()
        if (devices.isEmpty()) {
            toast("No paired devices")
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Select ELM327")
            .setItems(devices.map { it.name }.toTypedArray()) { _, i ->
                lastDevice = devices[i]
                connectToElm(lastDevice)
            }
            .show()
    }

    private fun connectToElm(device: BluetoothDevice) {
        scope.launch {
            try {
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()
                input = socket.inputStream
                output = socket.outputStream
                withContext(Dispatchers.Main) {
                    toast("Connected")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    toast("Connect failed")
                }
            }
        }
    }

    private fun startLiveLoop() {
        if (!::output.isInitialized) {
            toast("Connect ELM first")
            return
        }

        liveRunning = true
        scope.launch {
            while (liveRunning) {
                val rpm = (1000..3000).random()
                val speed = (0..120).random()

                withContext(Dispatchers.Main) {
                    rpmView.text = "RPM\n$rpm"
                    speedView.text = "SPEED\n$speed"
                    rpmGauge.progress = rpm
                    speedGauge.progress = speed
                }
                delay(500)
            }
        }
    }

    private fun initCsv() {
        val dir = File(getExternalFilesDir(null), "logs")
        if (!dir.exists()) dir.mkdirs()
        csvFile = File(dir, "log.csv")
    }

    private fun toast(msg: String) {
        runOnUiThread {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }
}
