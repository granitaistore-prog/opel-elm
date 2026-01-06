package com.granitaistore.obddiagnostic.obd

object CsvExporter {
    fun export(data: List<String>): String {
        return data.joinToString(",")
    }
}
