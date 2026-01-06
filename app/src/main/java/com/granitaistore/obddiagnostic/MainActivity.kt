package com.granitaistore.obddiagnostic

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var btnSelectElm: Button
    private lateinit var btnReadDtc: Button
    private lateinit var btnClearDtc: Button
    private lateinit var trip: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnSelectElm = findViewById(R.id.btnSelectElm)
        btnReadDtc = findViewById(R.id.btnReadDtc)
        btnClearDtc = findViewById(R.id.btnClearDtc)
        trip = findViewById(R.id.trip)

        btnReadDtc.setOnClickListener {
            trip.text = "Read DTC (stub)"
        }

        btnClearDtc.setOnClickListener {
            trip.text = "Clear DTC (stub)"
        }
    }
}
