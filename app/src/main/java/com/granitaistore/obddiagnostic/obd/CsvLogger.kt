package com.granitaistore.obddiagnostic.obd

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class CsvLogger(private val context: Context) {

    private var writer: FileWriter? = null

    fun start() {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS
            ), "OBD"
        )
        if (!dir.exists()) dir.mkdirs()

        val fileName = "log_" + SimpleDateFormat("yyyyMMdd_HHmmss")
            .format(Date()) + ".csv"

        val file = File(dir, fileName)
        writer = FileWriter(file)
        writer?.append("time,rpm,speed,temp,lph,l100\n")
    }

    fun log(
        rpm: Int,
        speed: Int,
        temp: Int,
        lph: Float,
        l100: Float
    ) {
        val time = System.currentTimeMillis()
        writer?.append(
            "$time,$rpm,$speed,$temp,$lph,$l100\n"
        )
        writer?.flush()
    }

    fun stop() {
        writer?.close()
        writer = null
    }
}
