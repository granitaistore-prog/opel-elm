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

    private var file: File
    private var writer: BufferedWriter

    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    init {
        val dir = File(context.getExternalFilesDir(null), "logs")
        if (!dir.exists()) dir.mkdirs()

        val name = "obd_log_${System.currentTimeMillis()}.csv"
        file = File(dir, name)
        writer = BufferedWriter(FileWriter(file, true))

        // Torque-style header
        writer.write("Time,RPM,Speed(km/h),CAN_ID,DATA\n")
        writer.flush()
    }

    fun log(rpm: Int, speed: Int, canId: String? = "", data: String? = "") {
        val time = sdf.format(Date())
        val line = "$time,$rpm,$speed,$canId,$data\n"
        writer.write(line)
        writer.flush()
    }

    fun close() {
        writer.flush()
        writer.close()
    }

    fun getFile(): File = file
}
