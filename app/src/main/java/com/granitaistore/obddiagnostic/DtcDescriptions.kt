package com.granitaistore.obddiagnostic

object DtcDescriptions {

    private val map = mapOf(
        // ---- POWERTRAIN ----
        "P0100" to "Mass or Volume Air Flow Circuit Malfunction",
        "P0101" to "Mass Air Flow Circuit Range/Performance Problem",
        "P0102" to "Mass Air Flow Circuit Low Input",
        "P0103" to "Mass Air Flow Circuit High Input",

        "P0130" to "O2 Sensor Circuit (Bank 1 Sensor 1)",
        "P0131" to "O2 Sensor Circuit Low Voltage (B1S1)",
        "P0132" to "O2 Sensor Circuit High Voltage (B1S1)",
        "P0133" to "O2 Sensor Circuit Slow Response (B1S1)",
        "P0134" to "O2 Sensor Circuit No Activity Detected (B1S1)",

        "P0170" to "Fuel Trim Malfunction (Bank 1)",
        "P0171" to "System Too Lean (Bank 1)",
        "P0172" to "System Too Rich (Bank 1)",

        "P0300" to "Random/Multiple Cylinder Misfire Detected",
        "P0301" to "Cylinder 1 Misfire Detected",
        "P0302" to "Cylinder 2 Misfire Detected",
        "P0303" to "Cylinder 3 Misfire Detected",
        "P0304" to "Cylinder 4 Misfire Detected",

        "P0400" to "Exhaust Gas Recirculation Flow Malfunction",
        "P0401" to "Exhaust Gas Recirculation Flow Insufficient",
        "P0402" to "Exhaust Gas Recirculation Flow Excessive",

        "P0420" to "Catalyst System Efficiency Below Threshold (Bank 1)",

        "P0500" to "Vehicle Speed Sensor Malfunction",

        // ---- TRANSMISSION ----
        "P0700" to "Transmission Control System Malfunction",

        // ---- GENERIC ----
        "U0100" to "Lost Communication With ECM/PCM",
        "U0121" to "Lost Communication With ABS Control Module"
    )

    fun get(code: String): String {
        return map[code] ?: "Unknown DTC"
    }
}
