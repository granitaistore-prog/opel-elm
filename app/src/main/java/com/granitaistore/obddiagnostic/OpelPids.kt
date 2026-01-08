package com.granitaistore.obddiagnostic

object OpelPids {

    fun coolantTemp(a: Int) = a - 40
    fun intakeTemp(a: Int) = a - 40

    fun batteryVoltage(a: Int, b: Int) =
        (a * 256 + b) / 1000.0

    fun throttle(a: Int) = a * 100 / 255
    fun engineLoad(a: Int) = a * 100 / 255

    fun boost(a: Int, b: Int) =
        ((a * 256 + b) / 10) - 100
}
