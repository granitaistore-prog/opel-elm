package com.granitaistore.obddiagnostic.elm

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ElmViewModel(
    private val elm: ElmConnection,
    private val csvLogger: CsvLogger
) : ViewModel() {

    val rpm = MutableStateFlow(0)
    val speed = MutableStateFlow(0)
    val isConnected = MutableStateFlow(false)
    val canFrames = MutableStateFlow<List<CanFrame>>(emptyList())

    private var pollingJob: Job? = null
    private var canJob: Job? = null

    // ================= CONNECT =================
    fun connect(device: BluetoothDevice) {
        viewModelScope.launch {
            isConnected.value = elm.connect(device)
            if (isConnected.value) {
                startLive()
            }
        }
    }

    // ================= RECONNECT =================
    fun reconnect(device: BluetoothDevice) {
        stop()
        connect(device)
    }

    // ================= LIVE LOOP =================
    private fun startLive() {
        pollingJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive && elm.isConnected) {
                try {
                    val r = elm.getRPM()
                    val s = elm.getSpeed()

                    rpm.value = r
                    speed.value = s

                    csvLogger.log(r, s)
                } catch (_: Exception) {}

                delay(200) // 5Hz like Torque
            }
        }

        canJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive && elm.isConnected) {
                val frame = elm.readCanFrame()
                frame?.let {
                    canFrames.value = (canFrames.value + it).takeLast(100)
                }
                delay(10)
            }
        }
    }

    // ================= STOP =================
    fun stop() {
        pollingJob?.cancel()
        canJob?.cancel()
        elm.disconnect()
        isConnected.value = false
    }

    override fun onCleared() {
        super.onCleared()
        stop()
    }
}
