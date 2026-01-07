package com.granitaistore.obddiagnostic

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnSelectElm = findViewById<Button>(R.id.btnSelectElm)
        val btnReadDtc = findViewById<Button>(R.id.btnReadDtc)
        val btnClearDtc = findViewById<Button>(R.id.btnClearDtc)
        val trip = findViewById<TextView>(R.id.trip)

        btnSelectElm.setOnClickListener {
            trip.text = "Select ELM327 (stub)"
        }

        btnReadDtc.setOnClickListener {
            trip.text = "Read DTC (stub)"
        }

        btnClearDtc.setOnClickListener {
            trip.text = "Clear DTC (stub)"
        }
    }
}
