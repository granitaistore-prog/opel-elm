
package com.granitaistore.obddiagnostic.obd

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CsvLogger(private val context: Context) {

    private val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
    private var file: File? = null

    fun start() {
        val name = "trip_${sdf.format(Date())}.csv"
        file = File(context.getExternalFilesDir(null), name)
        file?.writeText("time,rpm,speed,temp,maf,lph,l100km,stft,ltft\n")
    }

    fun log(
        rpm: Int,
        speed: Int,
        temp: Int,
        maf: Double,
        lph: Double,
        l100: Double,
        stft: Double,
        ltft: Double
    ) {
        val line = "${System.currentTimeMillis()},$rpm,$speed,$temp," +
                "${"%.2f".format(maf)}," +
                "${"%.2f".format(lph)}," +
                "${"%.2f".format(l100)}," +
                "${"%.1f".format(stft)}," +
                "${"%.1f".format(ltft)}\n"
        file?.appendText(line)
    }

    fun stop() { file = null }
}
