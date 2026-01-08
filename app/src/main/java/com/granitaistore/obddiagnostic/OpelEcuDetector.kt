package com.granitaistore.obddiagnostic

object OpelEcuDetector {

    fun detect(send: (String) -> String): String {
        val r = send("0902")
        return when {
            r.contains("SIMTEC", true) -> "Simtec"
            r.contains("ME7", true) -> "Bosch ME7"
            r.contains("EDC", true) -> "Bosch EDC"
            r.contains("DELCO", true) -> "Delco"
            else -> "Unknown ECU"
        }
    }
}
