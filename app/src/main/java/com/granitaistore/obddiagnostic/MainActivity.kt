package com.granitaistore.obddiagnostic

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var rpmText: TextView
    private lateinit var speedText: TextView
    private lateinit var rpmGauge: ProgressBar
    private lateinit var speedGauge: ProgressBar

    private val handler = Handler(Looper.getMainLooper())
    private var running = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rpmText = findViewById(R.id.rpm)
        speedText = findViewById(R.id.speed)
        rpmGauge = findViewById(R.id.rpmGauge)
        speedGauge = findViewById(R.id.speedGauge)

        findViewById<Button>(R.id.startLogBtn).setOnClickListener {
            startFakeLoop()
            toast("LOG started")
        }

        findViewById<Button>(R.id.stopLogBtn).setOnClickListener {
            running = false
            toast("LOG stopped")
        }

        findViewById<Button>(R.id.btnSelectElm).setOnClickListener {
            toast("ELM327 select (stub)")
        }

        findViewById<Button>(R.id.btnReadDtc).setOnClickListener {
            toast("Read DTC (stub)")
        }

        findViewById<Button>(R.id.btnClearDtc).setOnClickListener {
            toast("Clear DTC (stub)")
        }
    }

    private fun startFakeLoop() {
        if (running) return
        running = true

        handler.post(object : Runnable {
            override fun run() {
                if (!running) return

                val rpm = Random.nextInt(700, 5000)
                val speed = Random.nextInt(0, 160)

                rpmText.text = "RPM\n$rpm"
                speedText.text = "SPEED\n$speed"

                rpmGauge.progress = rpm
                speedGauge.progress = speed

                handler.postDelayed(this, 800)
            }
        })
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
