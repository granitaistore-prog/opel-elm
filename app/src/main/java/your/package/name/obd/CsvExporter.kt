package your.package.name.obd

import android.content.Context
import java.io.File

object CsvExporter {

    fun exportTrips(context: Context, trips: List<Trip>): File {
        val file = File(
            context.getExternalFilesDir(null),
            "trip_history.csv"
        )

        file.printWriter().use { out ->
            out.println("trip_id,start_time,end_time,distance_km,avg_l_100km,total_fuel_l")

            trips.forEach {
                out.println(
                    "${it.id}," +
                    "${it.startTime}," +
                    "${it.endTime}," +
                    "${"%.2f".format(it.distanceKm)}," +
                    "${"%.2f".format(it.avgConsumption())}," +
                    "${"%.2f".format(it.totalFuelLiters)}"
                )
            }
        }
        return file
    }
}
