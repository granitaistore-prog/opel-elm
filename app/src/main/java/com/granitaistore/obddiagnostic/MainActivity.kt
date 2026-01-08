package com.granitaistore.obddiagnostic

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.*
import android.content.pm.PackageManager
import android.os.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var rpmText: TextView
    private lateinit var speedText: TextView
    private lateinit var rpmNeedle: ImageView
    private lateinit var btnSelectElm: Button
    private lateinit var btnReadDtc: Button
    private lateinit var btnClearDtc: Button
    private lateinit var startLogBtn: Button
    private lateinit var stopLogBtn: Button

    private var socket: BluetoothSocket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null
    private var lastDevice: BluetoothDevice? = null

    private var logger: CsvLogger? = null
    private var logging = false
    private var live = false

    private val handler = Handler(Looper.getMainLooper())

    private val ELM_UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rpmText = findViewById(R.id.rpm)
        speedText = findViewById(R.id.speed)
        rpmNeedle = findViewById(R.id.rpmNeedle)

        btnSelectElm = findViewById(R.id.btnSelectElm)
        btnReadDtc = findViewById(R.id.btnReadDtc)
        btnClearDtc = findViewById(R.id.btnClearDtc)
        startLogBtn = findViewById(R.id.startLogBtn)
        stopLogBtn = findViewById(R.id.stopLogBtn)

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
                clearDtc()
                runOnUiThread { toast("DTC cleared") }
            }
        }

        startLogBtn.setOnClickListener {
            logger = CsvLogger(this)
            logging = true
            toast("LOG started")
        }

        stopLogBtn.setOnClickListener {
            logging = false
            toast("Saved: ${logger?.path()}")
        }
    }

    // ================= PERMISSIONS =================
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

    // ================= DEVICE SELECT =================
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
                startLive()
                runOnUiThread { toast("ELM connected") }
            } catch (e: Exception) {
                runOnUiThread { toast("ELM error") }
            }
        }
    }

    private fun reconnect() {
        try { socket?.close() } catch (_: Exception) {}
        lastDevice?.let { connect(it) }
    }

    // ================= LIVE LOOP =================
    private fun startLive() {
        live = true
        handler.post(object : Runnable {
            override fun run() {
                if (!live) return
                thread {
                    try {
                        val rpm = readRPM()
                        val speed = readSpeed()

                        runOnUiThread {
                            rpmText.text = "RPM\n$rpm"
                            speedText.text = "SPEED\n$speed"

                            val angle = -135f + (rpm / 8000f) * 270f
                            rpmNeedle.animate().rotation(angle).setDuration(300).start()

                            if (logging) logger?.log(rpm, speed)
                        }
                    } catch (e: Exception) {
                        reconnect()
                    }
                }
                handler.postDelayed(this, 1000)
            }
        })
    }

    // ================= ELM =================
    private fun initElm() {
        send("ATZ")
        send("ATE0")
        send("ATL0")
        send("ATS0")
        send("ATH0")
        send("ATSP0")
    }

    private fun send(cmd: String): String {
        output?.write((cmd + "\r").toByteArray())
        Thread.sleep(150)
        val buf = ByteArray(1024)
        val len = input?.read(buf) ?: 0
        return String(buf, 0, len)
            .replace(">", "")
            .replace("\r", "")
            .trim()
    }

    private fun readRPM(): Int {
        val r = send("010C").split(" ")
        return if (r.size >= 4)
            ((r[2].toInt(16) * 256 + r[3].toInt(16)) / 4)
        else 0
    }

    private fun readSpeed(): Int {
        val r = send("010D").split(" ")
        return if (r.size >= 3)
            r[2].toInt(16)
        else 0
    }

    // ================= DTC =================
    private fun readDtc(): List<String> {
        val raw = send("03").replace(" ", "")
        if (!raw.startsWith("43")) return emptyList()
        val list = mutableListOf<String>()
        var i = 2
        while (i + 4 <= raw.length) {
            val a = raw.substring(i, i + 2)
            val b = raw.substring(i + 2, i + 4)
            i += 4
            if (a == "00" && b == "00") break
            val code = decodeDtc(a, b)
            list.add("$code — ${DtcDescriptions.get(code)}")
        }
        return list
    }

    private fun decodeDtc(a: String, b: String): String {
        val A = a.toInt(16)
        val B = b.toInt(16)
        val type = when (A shr 6) {
            0 -> "P"; 1 -> "C"; 2 -> "B"; else -> "U"
        }
        return "$type${(A shr 4) and 3}${A and 0xF}${B shr 4}${B and 0xF}"
    }

    private fun clearDtc() {
        send("04")
    }

    // ================= UI =================
    private fun showDtcDialog(list: List<String>) {
        AlertDialog.Builder(this)
            .setTitle("DTC")
            .setMessage(if (list.isEmpty()) "No errors" else list.joinToString("\n"))
            .setPositiveButton("OK", null)
            .show()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
