package com.granitaistore.obddiagnostic

import android.util.Log

class EcuDetector(private val elm: ElmConnection) {

    companion object {
        private const val TAG = "EcuDetector"
    }

    enum class Protocol {
        CAN_11_500,
        CAN_11_250,
        ISO_9141,
        KWP_2000,
        UNKNOWN
    }

    enum class EcuType {
        ENGINE,
        BCM,
        ABS,
        TCM,
        UNKNOWN
    }

    data class EcuInfo(
        val protocol: Protocol,
        val ecuType: EcuType,
        val canId: String
    )

    // ================= PUBLIC =================
    fun detect(): EcuInfo {
        Log.d(TAG, "Starting ECU detect")

        val protocol = detectProtocol()
        Log.d(TAG, "Protocol: $protocol")

        val ecuInfo = detectEcu(protocol)
        Log.d(TAG, "ECU: ${ecuInfo.ecuType} ID=${ecuInfo.canId}")

        return ecuInfo
    }

    // ================= PROTOCOL =================
    private fun detectProtocol(): Protocol {
        val proto = elm.send("ATDPN").trim()

        return when {
            proto.contains("A") || proto.contains("6") -> Protocol.CAN_11_500
            proto.contains("8") -> Protocol.CAN_11_250
            proto.contains("3") -> Protocol.ISO_9141
            proto.contains("4") -> Protocol.KWP_2000
            else -> Protocol.UNKNOWN
        }
    }

    // ================= ECU =================
    private fun detectEcu(protocol: Protocol): EcuInfo {

        // IMPORTANT: reset header before probing
        elm.send("ATSH7E0")
        elm.send("ATH1")

        // -------- ENGINE (standard) --------
        val engineResp = elm.send("0100", 300)
        if (isValid0100(engineResp)) {
            return EcuInfo(
                protocol = protocol,
                ecuType = EcuType.ENGINE,
                canId = extractCanId(engineResp)
            )
        }

        // -------- OPEL ECUs --------
        val opelHeaders = mapOf(
            "6C1" to EcuType.BCM,
            "6C3" to EcuType.ABS,
            "6C8" to EcuType.TCM
        )

        for ((id, type) in opelHeaders) {
            elm.send("ATSH$id")
            val r = elm.send("0100", 300)

            if (isValid0100(r)) {
                return EcuInfo(
                    protocol = protocol,
                    ecuType = type,
                    canId = id
                )
            }
        }

        return EcuInfo(protocol, EcuType.UNKNOWN, "N/A")
    }

    // ================= HELPERS =================
    private fun isValid0100(resp: String): Boolean {
        val clean = resp.replace(" ", "")
        return clean.contains("4100") && !clean.contains("NO DATA")
    }

    private fun extractCanId(resp: String): String {
        return resp.lines()
            .firstOrNull { it.matches(Regex("^[0-9A-F]{3}.*")) }
            ?.substring(0, 3)
            ?: "7E8"
    }
}
