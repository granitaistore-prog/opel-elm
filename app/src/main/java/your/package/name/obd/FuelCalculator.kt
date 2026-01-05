package your.package.name.obd

object FuelCalculator {

    // ===== БЕНЗИН =====
    private const val AFR = 14.7f
    private const val FUEL_DENSITY = 745f // g/L
    private const val SECONDS_PER_HOUR = 3600f

    /** MAF (g/s) -> Fuel (L/h) */
    fun mafToLitersPerHour(mafGps: Float): Float {
        return (mafGps * SECONDS_PER_HOUR) / (AFR * FUEL_DENSITY)
    }

    /** Fuel (L/h) + Speed (km/h) -> L/100km */
    fun litersPer100km(lph: Float, speedKmh: Float): Float {
        if (speedKmh < 1f) return 0f
        return (lph / speedKmh) * 100f
    }
}
