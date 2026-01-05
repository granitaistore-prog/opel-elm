package com.granitaistore.obddiagnostic.obd

object DtcParser {

    fun parse(raw: String): List<String> {
        val clean = raw.replace(" ", "").replace(">", "")
        if (clean.length < 4) return emptyList()

        val dtcs = mutableListOf<String>()
        var i = 4

        while (i + 3 < clean.length) {
            val code = clean.substring(i, i + 4)
            if (code == "0000") break
            dtcs.add(decode(code))
            i += 4
        }
        return dtcs
    }

    private fun decode(hex: String): String {
        val b = hex.substring(0, 1).toInt(16)
        val type = when (b shr 2) {
            0 -> "P"
            1 -> "C"
            2 -> "B"
            else -> "U"
        }
        return type + hex.substring(1)
    }
}
