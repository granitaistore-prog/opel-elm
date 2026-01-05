package com.granitaistore.obddiagnostic

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.granitaistore.obddiagnostic.obd.*

class MainActivity : AppCompatActivity() {

    private lateinit var rpmView: TextView
    private lateinit var speedView: TextView
    private lateinit var tempView: TextView
    private lateinit var fuelView: TextView
    private lateinit var tripView: TextView
    private lateinit var startLogBtn: Button
    private lateinit var stopLogBtn: Button

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rpmView = findViewById(R.id.rpm)
        speedView = findViewById(R.id.speed)
        tempView = findViewById(R.id.temp)
        fuelView = findViewById(R.id.fuel)
        tripView = findViewById(R.id.trip)
        startLogBtn = findViewById(R.id.startLogBtn)
        stopLogBtn = findViewById(R.id.stopLogBtn)

        val logger = CsvLogger(this)
        val trip = TripManager()

        startLogBtn.setOnClickListener { logger.start() }
        stopLogBtn.setOnClickListener { logger.stop() }

        Thread {
            val bt = BluetoothService()
            if (!bt.connect()) return@Thread

            val elm = Elm327Manager(bt)
            elm.initOpel()

            handler.post(object : Runnable {
                override fun run() {
                    try {
                        // === ЯВНІ ТИПИ ===
                        val rpm: Int = elm.rpm()
                        val speed: Int = elm.speed()
                        val temp: Int = elm.coolantTemp()

                        val fuelL100: Double = elm.fuelL100km()
                        val fuelLph: Double = elm.fuelLph()

                        // TripManager очікує (Int, Double)
                        trip.update(speed, fuelLph)

                        // UI
                        rpmView.text = "RPM\n$rpm"
                        speedView.text = "SPEED\n$speed"
                        tempView.text = "TEMP\n$temp°C"
                        fuelView.text =
                            "FUEL\n%.1f L/100km".format(fuelL100)

                        tripView.text =
                            "TRIP\n%.1f km | %.1f L/100km"
                                .format(trip.distance(), trip.avgFuel())

                        // CSV
                        logger.log(
                            rpm,
                            speed,
                            temp,
                            fuelLph,
                            fuelL100
                        )

                    } catch (_: Exception) {
                    }

                    handler.postDelayed(this, 1000)
                }
            })
        }.start()
    }
}
