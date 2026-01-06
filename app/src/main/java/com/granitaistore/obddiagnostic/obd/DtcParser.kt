package com.granitaistore.obddiagnostic.obd

object DtcParser {
    fun parse(raw: String): List<String> {
        return if (raw.isBlank()) emptyList() else listOf(raw)
    }
}
