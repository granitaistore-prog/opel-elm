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
    private lateinit var tripText: TextView
    private lateinit var rpmGauge: ProgressBar
    private lateinit var speedGauge: ProgressBar

    private lateinit var btnSelectElm: Button
    private lateinit var btnReadDtc: Button
    private lateinit var btnClearDtc: Button

    private var socket: BluetoothSocket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null
    private var lastDevice: BluetoothDevice? = null

    private val tripCalc = TripCalculator()
    private var lastTick = System.currentTimeMillis()
    private var live = false

    private val ELM_UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rpmText = findViewById(R.id.rpm)
        speedText = findViewById(R.id.speed)
        tripText = findViewById(R.id.trip)
        rpmGauge = findViewById(R.id.rpmGauge)
        speedGauge = findViewById(R.id.speedGauge)

        btnSelectElm = findViewById(R.id.btnSelectElm)
        btnReadDtc = findViewById(R.id.btnReadDtc)
        btnClearDtc = findViewById(R.id.btnClearDtc)

        ensurePermissions()

        btnSelectElm.setOnClickListener {
            selectElm { d ->
                lastDevice = d
                connect(d)
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

    // ---------------- PERMISSIONS ----------------
    private fun ensurePermissions() {
        val p = arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val m = p.filter {
            ContextCompat.checkSelfPermission(this, it)
                    != PackageManager.PERMISSION_GRANTED
        }
        if (m.isNotEmpty())
            ActivityCompat.requestPermissions(this, m.toTypedArray(), 1)
    }

    // ---------------- DEVICE ----------------
    private fun selectElm(cb: (BluetoothDevice) -> Unit) {
        val devs = BluetoothAdapter.getDefaultAdapter().bondedDevices.toList()
        AlertDialog.Builder(this)
            .setTitle("Select ELM327")
            .setItems(devs.map { it.name }.toTypedArray()) { _, i ->
                cb(devs[i])
            }.show()
    }

    private fun connect(d: BluetoothDevice) {
        thread {
            try {
                socket = d.createRfcommSocketToServiceRecord(ELM_UUID)
                socket!!.connect()
                input = socket!!.inputStream
                output = socket!!.outputStream
                initElm()
                runOnUiThread { toast("ELM connected") }
                startLive()
            } catch (e: Exception) {
                runOnUiThread { toast("ELM error") }
            }
        }
    }

    private fun initElm() {
        listOf("ATZ","ATE0","ATL0","ATS0","ATH0","ATSP0")
            .forEach { send(it) }
    }

    // ---------------- LIVE LOOP ----------------
    private fun startLive() {
        if (live) return
        live = true

        thread {
            while (live) {
                try {
                    val rpm = readRPM()
                    val speed = readSpeed()
                    val maf = readMAF()

                    val now = System.currentTimeMillis()
                    val dt = (now - lastTick) / 1000.0
                    lastTick = now

                    tripCalc.update(speed, maf, dt)

                    runOnUiThread {
                        rpmText.text = "RPM\n$rpm"
                        speedText.text = "SPEED\n$speed"
                        rpmGauge.progress = rpm
                        speedGauge.progress = speed
                        updateTrip()
                    }
                    Thread.sleep(500)
                } catch (e: Exception) {
                    autoReconnect()
                }
            }
        }
    }

    private fun autoReconnect() {
        live = false
        lastDevice?.let { connect(it) }
    }

    // ---------------- PIDS ----------------
    private fun readRPM(): Int {
        val r = send("010C")
        if (!r.contains("41 0C")) return 0
        val d = r.replace(" ", "")
        val a = d.substringAfter("410C").substring(0,2).toInt(16)
        val b = d.substringAfter("410C").substring(2,4).toInt(16)
        return ((a * 256) + b) / 4
    }

    private fun readSpeed(): Int {
        val r = send("010D")
        if (!r.contains("41 0D")) return 0
        return r.replace(" ", "")
            .substringAfter("410D")
            .substring(0,2).toInt(16)
    }

    private fun readMAF(): Double {
        val r = send("010F")
        if (!r.contains("41 0F")) return 0.0
        val d = r.replace(" ", "")
        val a = d.substringAfter("410F").substring(0,2).toInt(16)
        val b = d.substringAfter("410F").substring(2,4).toInt(16)
        return ((a * 256) + b) / 100.0
    }

    // ---------------- DTC ----------------
    private fun readDtc(): List<String> {
        val raw = send("03").replace(" ", "")
        if (!raw.startsWith("43")) return emptyList()
        val out = mutableListOf<String>()
        var i = 2
        while (i + 4 <= raw.length) {
            val a = raw.substring(i, i+2)
            val b = raw.substring(i+2, i+4)
            if (a == "00" && b == "00") break
            out.add(decodeDtc(a,b))
            i += 4
        }
        return out
    }

    private fun decodeDtc(a:String,b:String):String {
        val A=a.toInt(16)
        val B=b.toInt(16)
        val t=when(A shr 6){0->"P";1->"C";2->"B";else->"U"}
        return "$t${(A shr 4)&3}${A and 15}${B shr 4}${B and 15}"
    }

    // ---------------- UI ----------------
    private fun updateTrip() {
        tripText.text = String.format(
            "TRIP\n%.2f km | %.2f L | %.1f L/100km",
            tripCalc.distance(),
            tripCalc.fuel(),
            tripCalc.avgConsumption()
        )
    }

    private fun showDtc(list: List<String>) {
        AlertDialog.Builder(this)
            .setTitle("DTC")
            .setMessage(
                if (list.isEmpty()) "No errors"
                else list.joinToString("\n") {
                    "$it – ${DtcDescriptions.get(it)}"
                }
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun send(cmd:String):String{
        return try{
            output?.write((cmd+"\r").toByteArray())
            Thread.sleep(200)
            val b=ByteArray(1024)
            val l=input?.read(b)?:0
            String(b,0,l)
        }catch(e:Exception){""}
    }

    private fun toast(s:String)=
        Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
}
