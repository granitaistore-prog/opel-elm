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
import java.util.UUID
import kotlin.concurrent.thread
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private lateinit var rpmGauge: ProgressBar
    private lateinit var speedGauge: ProgressBar
    private lateinit var rpmText: TextView
    private lateinit var speedText: TextView
    private lateinit var boostNeedle: ImageView
    private lateinit var boostText: TextView

    private lateinit var btnSelectElm: Button
    private lateinit var btnReadDtc: Button
    private lateinit var btnClearDtc: Button

    private var socket: BluetoothSocket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null
    private var lastDevice: BluetoothDevice? = null

    private val ELM_UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val handler = Handler(Looper.getMainLooper())
    private var running = false

    // smoothing
    private var boostFiltered = 0f
    private val SMOOTH = 0.15f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rpmGauge = findViewById(R.id.rpmGauge)
        speedGauge = findViewById(R.id.speedGauge)
        rpmText = findViewById(R.id.rpmText)
        speedText = findViewById(R.id.speedText)
        boostNeedle = findViewById(R.id.boostNeedle)
        boostText = findViewById(R.id.boostText)

        btnSelectElm = findViewById(R.id.btnSelectElm)
        btnReadDtc = findViewById(R.id.btnReadDtc)
        btnClearDtc = findViewById(R.id.btnClearDtc)

        ensurePermissions()

        btnSelectElm.setOnClickListener {
            selectElm {
                lastDevice = it
                connect(it)
            }
        }

        btnReadDtc.setOnClickListener {
            thread {
                val list = readDtc()
                runOnUiThread { showDtc(list) }
            }
        }

        btnClearDtc.setOnClickListener {
            thread {
                send("04")
                runOnUiThread { toast("DTC cleared") }
            }
        }
    }

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

        if (missing.isNotEmpty())
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 1)
    }

    private fun selectElm(onSelected: (BluetoothDevice) -> Unit) {
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

    private fun initElm() {
        send("ATZ")
        send("ATE0")
        send("ATL0")
        send("ATS0")
        send("ATH0")
        send("ATSP0")
    }

    private fun startLive() {
        running = true
        handler.post(object : Runnable {
            override fun run() {
                if (!running) return

                val rpm = readRpm()
                val speed = readSpeed()
                val boost = readBoost()

                updateUi(rpm, speed, boost)
                handler.postDelayed(this, 300)
            }
        })
    }

    private fun updateUi(rpm: Int, speed: Int, boost: Float) {
        rpmGauge.progress = rpm
        speedGauge.progress = speed

        rpmText.text = "RPM\n$rpm"
        speedText.text = "SPEED\n$speed"

        boostFiltered += (boost - boostFiltered) * SMOOTH
        val angle = 135f + (boostFiltered.coerceIn(0f, 1f) * 270f)
        boostNeedle.rotation = angle
        boostText.text = String.format("BOOST\n%.2f bar", boostFiltered)
    }

    private fun readRpm(): Int {
        val r = send("010C")
        return try {
            val d = r.replace(" ", "")
            val a = d.substringAfter("410C").substring(0, 4)
            ((a.substring(0, 2).toInt(16) * 256 +
                    a.substring(2, 4).toInt(16)) / 4)
        } catch (e: Exception) { 0 }
    }

    private fun readSpeed(): Int {
        val r = send("010D")
        return try {
            r.replace(" ", "").substringAfter("410D")
                .substring(0, 2).toInt(16)
        } catch (e: Exception) { 0 }
    }

    private fun readBoost(): Float {
        val r = send("010B")
        return try {
            val kpa = r.replace(" ", "").substringAfter("410B")
                .substring(0, 2).toInt(16)
            (kpa / 100f) - 1f
        } catch (e: Exception) { 0f }
    }

    private fun send(cmd: String): String {
        return try {
            output?.write((cmd + "\r").toByteArray())
            Thread.sleep(150)
            val buf = ByteArray(1024)
            val len = input?.read(buf) ?: 0
            String(buf, 0, len)
        } catch (e: Exception) { "" }
    }

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
            list.add(decodeDtc(a, b))
        }
        return list
    }

    private fun decodeDtc(a: String, b: String): String {
        val A = a.toInt(16)
        val B = b.toInt(16)
        val type = when (A shr 6) { 0 -> "P"; 1 -> "C"; 2 -> "B"; else -> "U" }
        return "$type${(A shr 4) and 3}${A and 15}${B shr 4}${B and 15}"
    }

    private fun showDtc(list: List<String>) {
        val text = if (list.isEmpty()) "No DTC"
        else list.joinToString("\n") { "$it – ${DtcDescriptions.get(it)}" }

        AlertDialog.Builder(this)
            .setTitle("DTC")
            .setMessage(text)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
