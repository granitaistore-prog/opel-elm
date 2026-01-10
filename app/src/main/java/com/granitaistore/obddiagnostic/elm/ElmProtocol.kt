package com.granitaistore.obddiagnostic.elm

class ElmProtocol(private val elm: ElmConnection) {

    suspend fun detect(): String {
        val p = elm.send("ATDPN")
        return when {
            p.contains("A") || p.contains("6") -> "CAN 11bit 500kbps"
            p.contains("8") -> "CAN 11bit 250kbps"
            p.contains("3") -> "ISO 9141"
            p.contains("4") -> "KWP2000"
            else -> "UNKNOWN"
        }
    }
}
