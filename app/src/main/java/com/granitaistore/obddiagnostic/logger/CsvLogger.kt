package com.granitaistore.obddiagnostic.logger

import android.content.Context
import android.os.Environment
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CsvLogger(private val context: Context) {

    private var file: File? = null
    private var writer: BufferedWriter? = null

    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun start() {
        val dir = File(
            context.getExternalFilesDir(null),
            "logs"
        )
        if (!dir.exists()) dir.mkdirs()

        val name = "obd_log_${System.currentTimeMillis()}.csv"
        file = File(dir, name)
        writer = BufferedWriter(FileWriter(file!!, true))

        // Header like Torque
        writer?.write("Timestamp,RPM,Speed_kmh,Boost_bar,CAN_ID,CAN_DATA\n")
        writer?.flush()
    }

    fun stop() {
        writer?.flush()
        writer?.close()
        writer = null
    }

    fun logPid(rpm: Int, speed: Int, boost: Float) {
        val ts = timeFormat.format(Date())
        val line = "$ts,$rpm,$speed,${"%.2f".format(boost)},,\n"
        writer?.write(line)
    }

    fun logCan(canId: String, data: String) {
        val ts = timeFormat.format(Date())
        val line = "$ts,,,,${canId},${data}\n"
        writer?.write(line)
    }
}
