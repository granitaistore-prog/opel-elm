package your.package.name.obd

class TripManager {

    private var currentTrip: Trip? = null
    private var lastTickTime: Long = 0L

    /**
     * Викликати кожну 1 секунду
     */
    fun onTick(
        mafGps: Float,
        speedKmh: Float,
        deltaTimeSec: Float
    ): Trip? {

        // ▶️ старт поїздки
        if (speedKmh > 5f && currentTrip == null) {
            currentTrip = Trip(
                id = System.currentTimeMillis(),
                startTime = System.currentTimeMillis()
            )
        }

        currentTrip?.let { trip ->
            val lph = FuelCalculator.mafToLitersPerHour(mafGps)

            val distance = speedKmh * (deltaTimeSec / 3600f)
            val fuel = lph * (deltaTimeSec / 3600f)

            trip.distanceKm += distance
            trip.totalFuelLiters += fuel

            // ⏹ завершення поїздки
            if (speedKmh < 1f) {
                trip.endTime = System.currentTimeMillis()
                val finished = trip
                currentTrip = null
                return finished
            }
        }
        return null
    }
}
