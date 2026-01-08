package com.granitaistore.obddiagnostic

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CsvLogger(context: Context) {

    private val file: File
    private val sdf = SimpleDateFormat("HH:mm:ss", Locale.US)

    init {
        val dir = context.getExternalFilesDir(null)!!
        file = File(dir, "obd_log_${System.currentTimeMillis()}.csv")
        file.writeText("time,rpm,speed\n")
    }

    fun log(rpm: Int, speed: Int) {
        file.appendText("${sdf.format(Date())},$rpm,$speed\n")
    }

    fun path(): String = file.absolutePath
}
