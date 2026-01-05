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

        // ===== Views =====
        rpmView = findViewById(R.id.rpm)
        speedView = findViewById(R.id.speed)
        tempView = findViewById(R.id.temp)
        fuelView = findViewById(R.id.fuel)
        tripView = findViewById(R.id.trip)
        startLogBtn = findViewById(R.id.startLogBtn)
        stopLogBtn = findViewById(R.id.stopLogBtn)

        // ===== OBD =====
        val bt = BluetoothService() // заглушка
        val elm = Elm327Manager(bt)
        val trip = TripManager()
        val logger = CsvLogger(this)

        startLogBtn.setOnClickListener { logger.start() }
        stopLogBtn.setOnClickListener { logger.stop() }

        Thread {
            elm.initOpel()

            handler.post(object : Runnable {
                override fun run() {
                    try {
                        val rpm = elm.rpm()
                        val speed = elm.speed()
                        val temp = elm.coolantTemp()
                        val l100 = elm.fuelL100km()
                        val lph = elm.fuelLph()

                        trip.update(speed, lph)

                        rpmView.text = "RPM\n$rpm"
                        speedView.text = "SPEED\n$speed"
                        tempView.text = "TEMP\n$temp°C"
                        fuelView.text = "FUEL\n%.1f L/100km".format(l100)
                        tripView.text = "TRIP\n%.1f km | %.1f L/100km"
                            .format(trip.distance(), trip.avgFuel())

                    } catch (_: Exception) { }

                    handler.postDelayed(this, 1000)
                }
            })
        }.start()
    }
}
