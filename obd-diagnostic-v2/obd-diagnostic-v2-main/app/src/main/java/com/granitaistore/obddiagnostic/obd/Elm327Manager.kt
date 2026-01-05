package com.granitaistore.obddiagnostic.obd

class Elm327Manager(private val bt: BluetoothService) {

    fun initOpel() {
        bt.send("ATZ")
        bt.send("ATE0")
        bt.send("ATL0")
        bt.send("ATS0")
        bt.send("ATH1")
        bt.send("ATSP3")
    }

    // ===== BASIC PIDs =====

    fun rpm(): Int {
        val b = parse(bt.send("010C"))
        return ((b[0] shl 8) + b[1]) / 4
    }

    fun speed(): Int {
        return parse(bt.send("010D"))[0]
    }

    fun coolantTemp(): Int {
        return parse(bt.send("0105"))[0] - 40
    }

    // ===== MAF / FUEL =====

    fun maf(): Double {
        val b = parse(bt.send("0110"))
        return ((b[0] shl 8) + b[1]) / 100.0
    }

    fun fuelLph(): Double {
        val afr = 14.7
        val density = 0.745
        return (maf() * 3600) / (afr * density)
    }

    fun fuelL100km(): Double {
        val spd = speed()
        if (spd < 5) return 0.0
        return fuelLph() * 100 / spd
    }

    // ===== TRIMS =====

    fun stft(): Double {
        val a = parse(bt.send("0106"))[0]
        return (a - 128) * 100.0 / 128.0
    }

    fun ltft(): Double {
        val a = parse(bt.send("0107"))[0]
        return (a - 128) * 100.0 / 128.0
    }

    // ===== PARSER =====

    private fun parse(r: String): List<Int> =
        r.replace(">", "")
            .replace("\r", " ")
            .replace("\n", " ")
            .trim()
            .split(" ")
            .filter { it.length == 2 }
            .map { it.toInt(16) }
}
