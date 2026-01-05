
package com.granitaistore.obddiagnostic.obd

object DtcDescriptions {
    private val map = mapOf(
        "P0171" to "Lean mixture – підсос повітря / MAF",
        "P0172" to "Rich mixture – тиск палива / MAF",
        "P0300" to "Random misfire – свічки / котушка",
        "P0400" to "EGR fault – закоксований EGR"
    )
    fun get(code: String) = map[code] ?: "OBD-II код без опису"
}
