package com.granitaistore.obddiagnostic

import android.os.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var rpm: TextView
    private lateinit var speed: TextView
    private lateinit var boost: TextView
    private lateinit var trip: TextView
    private lateinit var rpmGauge: ProgressBar
    private lateinit var speedGauge: ProgressBar
    private lateinit var boostGauge: ProgressBar

    private val tripCalc = TripCalculator()
    private var lastTick = System.currentTimeMillis()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setContentView(R.layout.activity_main)

        rpm = findViewById(R.id.rpm)
        speed = findViewById(R.id.speed)
        boost = findViewById(R.id.boost)
        trip = findViewById(R.id.trip)

        rpmGauge = findViewById(R.id.rpmGauge)
        speedGauge = findViewById(R.id.speedGauge)
        boostGauge = findViewById(R.id.boostGauge)

        startLive()
    }

    private fun startLive() = thread {
        while (true) {
            val r = (800..3000).random()
            val s = (0..120).random()
            val b = (0..120).random()

            val now = System.currentTimeMillis()
            val dt = (now - lastTick) / 1000.0
            lastTick = now

            tripCalc.update(s, 0.0, dt)

            runOnUiThread {
                rpm.text = "RPM\n$r"
                speed.text = "SPEED\n$s"
                boost.text = "BOOST\n$b kPa"

                rpmGauge.progress = r
                speedGauge.progress = s
                boostGauge.progress = b

                trip.text = String.format(
                    "TRIP\n%.2f km | %.2f L | %.1f L/100km",
                    tripCalc.distance(),
                    tripCalc.fuel(),
                    tripCalc.avgConsumption()
                )
            }
            Thread.sleep(500)
        }
    }
}
