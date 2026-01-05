
package com.granitaistore.obddiagnostic.obd

class TripManager {
    private var last = System.currentTimeMillis()
    private var dist = 0.0
    private var fuel = 0.0

    fun reset() {
        last = System.currentTimeMillis()
        dist = 0.0
        fuel = 0.0
    }

    fun update(speed: Int, fuelLph: Double) {
        val now = System.currentTimeMillis()
        val dt = (now - last) / 1000.0
        last = now
        dist += speed * dt / 3600.0
        fuel += fuelLph * dt / 3600.0
    }

    fun distance() = dist
    fun avgFuel() = if (dist > 0) fuel / dist * 100 else 0.0
}
