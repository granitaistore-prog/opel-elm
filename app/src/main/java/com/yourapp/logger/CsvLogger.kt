class CsvLogger(context: Context) {

    private val file: File
    private val writer: BufferedWriter
    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    init {
        val dir = File(context.getExternalFilesDir(null), "logs")
        if (!dir.exists()) dir.mkdirs()

        file = File(dir, "obd_${System.currentTimeMillis()}.csv")
        writer = BufferedWriter(FileWriter(file, true))
        writer.write("Time,RPM,Speed,Boost\n")
        writer.flush()
    }

    fun log(rpm: Int, speed: Int, boost: Float) {
        val line = "${sdf.format(Date())},$rpm,$speed,${"%.2f".format(boost)}\n"
        writer.write(line)
    }

    fun close() {
        writer.flush()
        writer.close()
    }
}
