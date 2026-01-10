package com.granitaistore.obddiagnostic.logger

import android.content.Context
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CsvLogger(private val context: Context) {

    private var writer: BufferedWriter? = null
    private var file: File? = null

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun start() {
        val dir = File(context.getExternalFilesDir(null), "logs")
        if (!dir.exists()) dir.mkdirs()

        val name = "obd_log_${System.currentTimeMillis()}.csv"
        file = File(dir, name)
        writer = BufferedWriter(FileWriter(file!!, true))

        // Torque compatible header
        writer?.write("Time,RPM,Speed,Boost,Throttle,Load,Coolant,CAN_ID,CAN_DATA\n")
        writer?.flush()
    }

    fun log(
        rpm: Int,
        speed: Int,
        boost: Float,
        throttle: Int,
        load: Int,
        coolant: Int,
        canId: String = "",
        canData: String = ""
    ) {
        val time = dateFormat.format(Date())

        val line = "$time,$rpm,$speed,${"%.2f".format(boost)},$throttle,$load,$coolant,$canId,$canData\n"
        writer?.write(line)
    }

    fun flush() {
        writer?.flush()
    }

    fun stop() {
        writer?.flush()
        writer?.close()
        writer = null
    }

    fun getFile(): File? = file
}
