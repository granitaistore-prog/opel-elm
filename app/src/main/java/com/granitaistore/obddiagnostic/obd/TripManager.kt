package com.granitaistore.obddiagnostic.obd

class TripManager {
    private var trip = Trip()

    fun reset() {
        trip = Trip()
    }

    fun getTrip(): Trip = trip
}
