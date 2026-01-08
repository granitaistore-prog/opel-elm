package com.granitaistore.obddiagnostic

import kotlin.math.max

class TripCalculator {

    private var lastSpeed = 0       // km/h
    private var distanceKm = 0.0    // km
    private var fuelLiters = 0.0    // L

    private val stoichAFR = 14.7     // gasoline
    private val fuelDensity = 745.0 // g/L

    fun update(speed: Int, maf: Double, dtSec: Double) {
        // distance
        distanceKm += (speed * dtSec) / 3600.0

        // fuel (MAF → fuel flow)
        if (maf > 0) {
            val fuelGramsPerSec = maf / stoichAFR
            val fuelLitersPerSec = fuelGramsPerSec / fuelDensity
            fuelLiters += fuelLitersPerSec * dtSec
        }

        lastSpeed = speed
    }

    fun distance(): Double = distanceKm

    fun fuel(): Double = fuelLiters

    fun avgConsumption(): Double {
        return if (distanceKm > 0)
            (fuelLiters / distanceKm) * 100.0
        else 0.0
    }

    fun reset() {
        distanceKm = 0.0
        fuelLiters = 0.0
        lastSpeed = 0
    }
}
