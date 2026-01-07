package com.granitaistore.obddiagnostic

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
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
    private lateinit var rpmGauge: ProgressBar
    private lateinit var speedGauge: ProgressBar
    private lateinit var btnSelectElm: Button

    private var socket: BluetoothSocket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null

    private val ELM_UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rpmText = findViewById(R.id.rpm)
        speedText = findViewById(R.id.speed)
        rpmGauge = findViewById(R.id.rpmGauge)
        speedGauge = findViewById(R.id.speedGauge)
        btnSelectElm = findViewById(R.id.btnSelectElm)

        ensureBluetoothPermissions()

        btnSelectElm.setOnClickListener {
            selectElmDevice { device ->
                connectElm(device)
            }
        }
    }

    // ===== Permissions =====
    private fun ensureBluetoothPermissions(): Boolean {
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
            return false
        }
        return true
    }

    // ===== Device chooser =====
    private fun selectElmDevice(onSelected: (BluetoothDevice) -> Unit) {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null || !adapter.isEnabled) {
            toast("Bluetooth OFF")
            return
        }

        val devices = adapter.bondedDevices.toList()
        if (devices.isEmpty()) {
            toast("No paired ELM327")
            return
        }

        val names = devices.map { "${it.name}\n${it.address}" }

        AlertDialog.Builder(this)
            .setTitle("Select ELM327")
            .setItems(names.toTypedArray()) { _, i ->
                onSelected(devices[i])
            }
            .show()
    }

    // ===== Connect =====
    private fun connectElm(device: BluetoothDevice) {
        thread {
            try {
                socket = device.createRfcommSocketToServiceRecord(ELM_UUID)
                socket!!.connect()

                input = socket!!.inputStream
                output = socket!!.outputStream

                runOnUiThread {
                    toast("ELM connected")
                }

                initElm()
                startLiveLoop()

            } catch (e: Exception) {
                runOnUiThread {
                    toast("ELM error: ${e.message}")
                }
            }
        }
    }

    // ===== ELM init =====
    private fun initElm() {
        send("ATZ")
        send("ATE0")
        send("ATL0")
        send("ATS0")
        send("ATH0")
        send("ATSP0")
    }

    // ===== Send command =====
    private fun send(cmd: String): String {
        output?.write((cmd + "\r").toByteArray())
        Thread.sleep(200)

        val buf = ByteArray(1024)
        val len = input?.read(buf) ?: 0
        return String(buf, 0, len)
    }

    // ===== Read RPM =====
    private fun readRpm(): Int {
        val r = send("010C").replace(" ", "")
        val i = r.indexOf("410C")
        if (i == -1) return 0
        val A = r.substring(i + 4, i + 6).toInt(16)
        val B = r.substring(i + 6, i + 8).toInt(16)
        return ((A * 256) + B) / 4
    }

    // ===== Read SPEED =====
    private fun readSpeed(): Int {
        val r = send("010D").replace(" ", "")
        val i = r.indexOf("410D")
        if (i == -1) return 0
        return r.substring(i + 4, i + 6).toInt(16)
    }

    // ===== Live loop =====
    private fun startLiveLoop() {
        thread {
            while (socket?.isConnected == true) {
                val rpm = readRpm()
                val speed = readSpeed()

                runOnUiThread {
                    rpmText.text = "RPM\n$rpm"
                    speedText.text = "SPEED\n$speed"
                    rpmGauge.progress = rpm
                    speedGauge.progress = speed
                }

                Thread.sleep(800)
            }
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
