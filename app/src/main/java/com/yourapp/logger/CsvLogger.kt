package com.yourapp.logger

import java.text.SimpleDateFormat
import java.util.*

object LogSession {

    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun timestamp(): String {
        return formatter.format(Date())
    }

    fun fileName(): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        return "obd_log_${sdf.format(Date())}.csv"
    }
}
