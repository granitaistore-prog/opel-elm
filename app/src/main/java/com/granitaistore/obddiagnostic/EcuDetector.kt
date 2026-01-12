package com.granitaistore.obddiagnostic

import com.granitaistore.obddiagnostic.elm.ElmConnection

class EcuDetector(private val elm: ElmConnection) {

    enum class Protocol { UNKNOWN }
    enum class EcuType { UNKNOWN }

    data class EcuInfo(
        val protocol: Protocol,
        val ecuType: EcuType,
        val canId: String
    )

    fun detect(): EcuInfo {
        return EcuInfo(
            protocol = Protocol.UNKNOWN,
            ecuType = EcuType.UNKNOWN,
            canId = "N/A"
        )
    }
}
