package com.granitaistore.obddiagnostic.obd

class Elm327Manager {

    fun connect(): Boolean = true

    fun readDtc(): List<String> {
        return listOf("P0001")
    }

    fun clearDtc(): Boolean = true

    // 🔧 заглушка, щоб не падала збірка
    private fun pidInt(cmd: String): Int = 0
}
