package com.granitaistore.obddiagnostic.logger

import android.content.Context
import android.os.Environment
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class CsvLogger(private val context: Context) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val file: File
    private val writer: BufferedWriter

    init {
        val dir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            "OBDLogs"
        )
        if (!dir.exists()) dir.mkdirs()

        val name = "torque_log_${System.currentTimeMillis()}.csv"
        file = File(dir, name)
        writer = BufferedWriter(FileWriter(file, true))

        // Torque-style header
        writer.write("Time,RPM,Speed_kmh\n")
        writer.flush()
    }

    fun log(rpm: Int, speed: Int) {
        val time = dateFormat.format(Date())
        writer.write("$time,$rpm,$speed\n")
        writer.flush()
    }

    fun logCan(id: String, data: String) {
        val time = dateFormat.format(Date())
        writer.write("$time,CAN,$id,$data\n")
        writer.flush()
    }

    fun close() {
        try {
            writer.flush()
            writer.close()
        } catch (_: Exception) { }
    }
}
