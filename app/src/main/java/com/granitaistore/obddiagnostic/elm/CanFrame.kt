package com.granitaistore.obddiagnostic.elm

data class CanFrame(
    val id: String,
    val data: String,
    val timestamp: Long = System.currentTimeMillis()
)
