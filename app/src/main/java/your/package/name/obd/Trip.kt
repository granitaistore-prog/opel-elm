package your.package.name.obd

data class Trip(
    val id: Long,
    val startTime: Long,
    var endTime: Long = 0L,
    var distanceKm: Float = 0f,
    var totalFuelLiters: Float = 0f
) {
    fun avgConsumption(): Float {
        if (distanceKm <= 0f) return 0f
        return (totalFuelLiters / distanceKm) * 100f
    }
}
