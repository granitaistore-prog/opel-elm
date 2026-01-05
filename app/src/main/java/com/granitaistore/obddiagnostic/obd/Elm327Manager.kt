package com.granitaistore.obddiagnostic.obd

class Elm327Manager(private val bt: BluetoothService) {

    fun initOpel() {
        bt.send("ATZ")
        bt.send("ATE0")
        bt.send("ATL0")
        bt.send("ATS0")
        bt.send("ATH0")
        bt.send("ATSP0")
    }

    fun readDTC(): List<String> {
        val raw = bt.send("03")
        return DtcParser.parse(raw)
    }

    fun clearDTC(): Boolean {
        val resp = bt.send("04")
        return resp.contains("OK") || resp.contains("44")
    }

    fun rpm(): Int = bt.pidInt("010C", 4) / 4
    fun speed(): Int = bt.pidInt("010D", 1)
    fun coolantTemp(): Int = bt.pidInt("0105", 1) - 40
}
