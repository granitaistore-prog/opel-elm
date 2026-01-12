package com.granitaistore.obddiagnostic

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

data class ObdData(
    val rpm: Int = 0,
    val speed: Int = 0,
    val coolantTemp: Int = 0
)

class ElmViewModel : ViewModel() {

    private val elm = ElmConnection()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _obdData = MutableStateFlow(ObdData())
    val obdData: StateFlow<ObdData> = _obdData

    fun connect(device: BluetoothDevice) {
        viewModelScope.launch {
            _connectionState.value = ConnectionState.Connecting

            val success = elm.connect(device)
            if (success) {
                _connectionState.value = ConnectionState.Connected
                startReadingLoop()
            } else {
                _connectionState.value = ConnectionState.Error("Не вдалося підключитись до ELM327")
            }
        }
    }

    private fun startReadingLoop() {
        viewModelScope.launch {
            while (true) {
                try {
                    val rpmRaw = elm.readPID("0C")
                    val speedRaw = elm.readPID("0D")
                    val tempRaw = elm.readPID("05")

                    val rpm = parseRPM(rpmRaw)
                    val speed = parseSpeed(speedRaw)
                    val temp = parseCoolant(tempRaw)

                    _obdData.value = ObdData(rpm, speed, temp)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                delay(1000) // оновлення раз на секунду
            }
        }
    }

    private fun parseRPM(response: String): Int {
        // Формат: 41 0C A B
        val bytes = extractBytes(response)
        if (bytes.size < 2) return 0
        return ((bytes[0] * 256) + bytes[1]) / 4
    }

    private fun parseSpeed(response: String): Int {
        // Формат: 41 0D A
        val bytes = extractBytes(response)
        if (bytes.isEmpty()) return 0
        return bytes[0]
    }

    private fun parseCoolant(response: String): Int {
        // Формат: 41 05 A  => A - 40
        val bytes = extractBytes(response)
        if (bytes.isEmpty()) return 0
        return bytes[0] - 40
    }

    private fun extractBytes(raw: String): List<Int> {
        return raw.replace(" ", "")
            .chunked(2)
            .drop(2) // прибираємо 41XX
            .mapNotNull {
                try { it.toInt(16) } catch (e: Exception) { null }
            }
    }
}
