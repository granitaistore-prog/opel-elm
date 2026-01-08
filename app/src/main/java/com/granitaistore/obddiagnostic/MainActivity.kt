package com.granitaistore.obddiagnostic

import android.Manifest
import android.bluetooth.*
import android.content.pm.PackageManager
import android.os.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlin.concurrent.thread
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    // ===== UI =====
    private lateinit var rpmText: TextView
    private lateinit var speedText: TextView
    private lateinit var boostText: TextView
    private lateinit var boostNeedle: ImageView

    private lateinit var btnSelectElm: Button
    private lateinit var btnReadDtc: Button
    private lateinit var btnClearDtc: Button

    // ===== Bluetooth =====
    private var socket: BluetoothSocket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null
    private var lastDevice: BluetoothDevice? = null

    private val ELM_UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // ===== Live loop =====
    @Volatile private var running = false

    // smoothing
    private var boostSmooth = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // UI bind
        rpmText = findViewById(R.id.rpmText)
        speedText = findViewById(R.id.speedText)
        boostText = findViewById(R.id.boostText)
        boostNeedle = findViewById(R.id.boostNeedle)

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
                runOnUiThread { showDtcDialog(dtc) }
            }
        }

        btnClearDtc.setOnClickListener {
            thread {
                clearDtc()
                runOnUiThread { toast("DTC cleared") }
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
            ContextCompat.checkSelfPermission(this, it)
                    != PackageManager.PERMISSION_GRANTED
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
        running = false
        thread {
            try {
                socket?.close()
                socket = device.createRfcommSocketToServiceRecord(ELM_UUID)
                socket!!.connect()
                input = socket!!.inputStream
                output = socket!!.outputStream
                initElm()
                running = true
                startLiveLoop()
                runOnUiThread { toast("ELM connected") }
            } catch (e: Exception) {
                autoReconnect()
            }
        }
    }

    private fun autoReconnect() {
        Thread.sleep(2000)
        lastDevice?.let { connect(it) }
    }

    // ================= ELM init =================
    private fun initElm() {
        send("ATZ")
        send("ATE0")
        send("ATL0")
        send("ATS0")
        send("ATH1")   // headers ON (CAN raw)
        send("ATSP0")  // auto protocol
    }

    // ================= Live loop =================
    private fun startLiveLoop() {
        thread {
            while (running) {
                try {
                    val rpm = readRpm()
                    val speed = readSpeed()
                    val boost = readBoost()

                    runOnUiThread {
                        rpmText.text = "RPM\n$rpm"
                        speedText.text = "SPEED\n$speed"
                        updateBoost(boost)
                    }

                    Thread.sleep(250)
                } catch (e: Exception) {
                    running = false
                    autoReconnect()
                }
            }
        }
    }

    // ================= PIDs =================
    private fun readRpm(): Int {
        val r = send("010C")
        return parsePid(r) { (it[0] * 256 + it[1]) / 4 }
    }

    private fun readSpeed(): Int {
        val r = send("010D")
        return parsePid(r) { it[0] }
    }

    // MAP → BOOST (bar)
    private fun readBoost(): Float {
        val r = send("010B")
        return parsePid(r) {
            val kpa = it[0]
            (kpa - 100) / 100f
        }
    }

    // ================= Needle physics =================
    private fun updateBoost(target: Float) {
        val alpha = 0.15f
        boostSmooth += (target - boostSmooth) * alpha

        val angle = 135f + boostSmooth * 90f
        boostNeedle.rotation = angle
        boostText.text = "BOOST\n${"%.2f".format(boostSmooth)} bar"
    }

    // ================= CAN RAW =================
    private fun parsePid(resp: String, calc: (List<Int>) -> Any): Any {
        val clean = resp.replace(" ", "")
            .replace("\r", "")
            .replace("\n", "")
            .replace(">", "")

        val idx = clean.indexOf("41")
        if (idx < 0 || clean.length < idx + 8) return 0

        val bytes = clean.substring(idx + 4)
            .chunked(2)
            .map { it.toInt(16) }

        return calc(bytes)
    }

    // ================= Send =================
    private fun send(cmd: String): String {
        output?.write((cmd + "\r").toByteArray())
        Thread.sleep(120)
        val buf = ByteArray(1024)
        val len = input?.read(buf) ?: 0
        return String(buf, 0, len)
    }

    // ================= DTC =================
    private fun readDtc(): List<String> {
        val raw = send("03")
        return raw.lines().filter { it.startsWith("43") }
    }

    private fun clearDtc() {
        send("04")
    }

    private fun showDtcDialog(list: List<String>) {
        val msg = if (list.isEmpty()) "No DTC"
        else list.joinToString("\n")

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
