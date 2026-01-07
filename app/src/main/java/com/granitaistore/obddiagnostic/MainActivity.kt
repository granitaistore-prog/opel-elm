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
    private lateinit var lastDevice: BluetoothDevice

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var liveRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindUi()
        checkPermissions()

        btnSelectElm.setOnClickListener { showElmPicker() }

        startLogBtn.setOnClickListener {
            liveRunning = true
            startFakeLoop()
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
        val perms = arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )

        if (perms.any {
                ActivityCompat.checkSelfPermission(this, it)
                        != PackageManager.PERMISSION_GRANTED
            }) {
            ActivityCompat.requestPermissions(this, perms, 100)
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
                toast("Selected ${lastDevice.name}")
            }
            .show()
    }

    private fun startFakeLoop() {
        scope.launch {
            while (liveRunning) {
                val rpm = (800..3500).random()
                val speed = (0..140).random()

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

    private fun toast(msg: String) {
        runOnUiThread {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }
}
