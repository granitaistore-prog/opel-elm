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
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    // UI
    private lateinit var rpmText: TextView
    private lateinit var speedText: TextView
    private lateinit var rpmGauge: ProgressBar
    private lateinit var speedGauge: ProgressBar
    private lateinit var rpmNeedle: ImageView
    private lateinit var speedNeedle: ImageView

    private lateinit var btnSelectElm: Button
    private lateinit var btnReadDtc: Button
    private lateinit var btnClearDtc: Button

    // Bluetooth
    private var socket: BluetoothSocket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null
    private var lastDevice: BluetoothDevice? = null

    private var live = false

    private val ELM_UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // ================= LIFE =================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // UI bind
        rpmText = findViewById(R.id.rpm)
        speedText = findViewById(R.id.speed)
        rpmGauge = findViewById(R.id.rpmGauge)
        speedGauge = findViewById(R.id.speedGauge)
        rpmNeedle = findViewById(R.id.rpmNeedle)
        speedNeedle = findViewById(R.id.speedNeedle)

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
                val list = readDtc()
                runOnUiThread { showDtcDialog(list) }
            }
        }

        btnClearDtc.setOnClickListener {
            thread {
                send("04")
                runOnUiThread { toast("DTC cleared") }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        live = false
        try { socket?.close() } catch (_: Exception) {}
    }

    // ================= PERMISSIONS =================
    private fun ensurePermissions() {
        val perms = arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val need = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (need.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, need.toTypedArray(), 100)
        }
    }

    // ================= DEVICE =================
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

    // ================= CONNECT =================
    private fun connect(device: BluetoothDevice) {
        thread {
            try {
                socket = device.createRfcommSocketToServiceRecord(ELM_UUID)
                socket!!.connect()
                input = socket!!.inputStream
                output = socket!!.outputStream

                initElm()

                live = true
                startLiveLoop()

                runOnUiThread { toast("ELM connected") }
            } catch (e: Exception) {
                runOnUiThread { toast("ELM connection failed") }
            }
        }
    }

    private fun initElm() {
        send("ATZ")
        send("ATE0")
        send("ATL0")
        send("ATS0")
        send("ATH0")
        send("ATSP0")
    }

    // ================= LIVE LOOP =================
    private fun startLiveLoop() {
        thread {
            while (live) {
                val rpm = readRPM()
                val speed = readSpeed()

                runOnUiThread {
                    updateRpm(rpm)
                    updateSpeed(speed)
                }

                Thread.sleep(500)
            }
        }
    }

    // ================= READ =================
    private fun readRPM(): Int {
        val r = send("010C")
        if (!r.contains("41 0C")) return 0

        val data = r.replace(" ", "")
        val a = data.substringAfter("410C").substring(0, 2).toInt(16)
        val b = data.substringAfter("410C").substring(2, 4).toInt(16)
        return ((a * 256) + b) / 4
    }

    private fun readSpeed(): Int {
        val r = send("010D")
        if (!r.contains("41 0D")) return 0
        return r.replace(" ", "").substringAfter("410D").substring(0, 2).toInt(16)
    }

    // ================= UI UPDATE =================
    private fun updateRpm(value: Int) {
        rpmText.text = "RPM\n$value"
        rpmGauge.progress = value.coerceIn(0, 8000)

        val angle = -135f + (value / 8000f) * 270f
        rpmNeedle.animate().rotation(angle).setDuration(300).start()
    }

    private fun updateSpeed(value: Int) {
        speedText.text = "SPEED\n$value"
        speedGauge.progress = value.coerceIn(0, 240)

        val angle = -135f + (value / 240f) * 270f
        speedNeedle.animate().rotation(angle).setDuration(300).start()
    }

    // ================= SEND =================
    private fun send(cmd: String): String {
        return try {
            output?.write((cmd + "\r").toByteArray())
            Thread.sleep(150)
            val buf = ByteArray(1024)
            val len = input?.read(buf) ?: 0
            String(buf, 0, len)
        } catch (e: Exception) {
            ""
        }
    }

    // ================= DTC =================
    private fun readDtc(): List<String> {
        val raw = send("03")
            .replace(" ", "")
            .replace("\r", "")
            .replace("\n", "")
            .replace(">", "")

        if (!raw.startsWith("43")) return emptyList()

        val list = mutableListOf<String>()
        var i = 2
        while (i + 4 <= raw.length) {
            val a = raw.substring(i, i + 2)
            val b = raw.substring(i + 2, i + 4)
            i += 4
            if (a == "00" && b == "00") break
            list.add(decodeDtc(a, b))
        }
        return list
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
        return "$type${(A shr 4) and 3}${A and 15}${B shr 4}${B and 15}"
    }

    private fun showDtcDialog(list: List<String>) {
        val msg = if (list.isEmpty())
            "No DTC errors"
        else
            list.joinToString("\n") {
                "$it — ${DtcDescriptions.get(it)}"
            }

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
