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

    private val handler = Handler(Looper.getMainLooper())
    private var live = false

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
                runOnUiThread { showDtcDialog(dtc) }
            }
        }

        btnClearDtc.setOnClickListener {
            thread {
                send("04") // CLEAR DTC
                runOnUiThread { toast("DTC cleared") }
            }
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
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 101)
        }
    }

    // ================= DEVICE PICKER =================
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
                startLiveLoop()
                runOnUiThread { toast("ELM connected") }
            } catch (e: Exception) {
                runOnUiThread { toast("ELM connection error") }
            }
        }
    }

    // ================= LIVE LOOP =================
    private fun startLiveLoop() {
        live = true
        handler.post(object : Runnable {
            override fun run() {
                if (!live) return
                try {
                    val rpm = readRPM()
                    val speed = readSpeed()
                    rpmText.text = "RPM\n$rpm"
                    speedText.text = "SPEED\n$speed"
                } catch (_: Exception) {}
                handler.postDelayed(this, 800)
            }
        })
    }

    // ================= ELM INIT =================
    private fun initElm() {
        send("ATZ")
        send("ATE0")
        send("ATL0")
        send("ATS0")
        send("ATH0")
        send("ATSP0")
    }

    // ================= SEND =================
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

    // ================= LIVE DATA =================
    private fun readRPM(): Int {
        val r = send("010C")
        val d = r.replace(" ", "").replace(">", "")
        if (!d.contains("410C")) return 0
        val A = d.substringAfter("410C").substring(0, 2).toInt(16)
        val B = d.substringAfter("410C").substring(2, 4).toInt(16)
        return (A * 256 + B) / 4
    }

    private fun readSpeed(): Int {
        val r = send("010D")
        val d = r.replace(" ", "").replace(">", "")
        if (!d.contains("410D")) return 0
        return d.substringAfter("410D").substring(0, 2).toInt(16)
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
            val code = decodeDtc(a, b)
            list.add("$code – ${DtcDescriptions.get(code)}")
        }
        return list
    }

    private fun decodeDtc(a: String, b: String): String {
        val A = a.toInt(16)
        val B = b.toInt(16)
        val type = listOf("P", "C", "B", "U")[A shr 6]
        return "$type${(A shr 4) and 3}${A and 0xF}${B shr 4}${B and 0xF}"
    }

    // ================= UI =================
    private fun showDtcDialog(list: List<String>) {
        AlertDialog.Builder(this)
            .setTitle("DTC")
            .setMessage(if (list.isEmpty()) "No DTC errors" else list.joinToString("\n"))
            .setPositiveButton("OK", null)
            .show()
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
