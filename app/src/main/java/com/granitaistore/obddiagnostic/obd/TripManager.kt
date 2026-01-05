package com.granitaistore.obddiagnostic.obd

class TripManager {

    private var lastTime = System.currentTimeMillis()
    private var distanceKm = 0.0
    private var fuelLiters = 0.0

    fun update(speedKmh: Int, fuelLph: Double) {
        val now = System.currentTimeMillis()
        val dt = (now - lastTime) / 1000.0
        lastTime = now

        distanceKm += speedKmh * dt / 3600.0
        fuelLiters += fuelLph * dt / 3600.0
    }

    fun distance(): Double = distanceKm

    fun avgFuel(): Double =
        if (distanceKm > 0) fuelLiters / distanceKm * 100 else 0.0
}
