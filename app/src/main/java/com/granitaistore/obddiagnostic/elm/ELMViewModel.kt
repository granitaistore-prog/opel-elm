package com.granitaistore.obddiagnostic.elm

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.granitaistore.obddiagnostic.logger.CsvLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ElmViewModel(app: Application) : AndroidViewModel(app) {

    private var elm: ElmConnection? = null
    private val logger = CsvLogger(app)

    private val _rpm = MutableStateFlow(0)
    val rpm = _rpm.asStateFlow()

    private val _speed = MutableStateFlow(0)
    val speed = _speed.asStateFlow()

    private val _connected = MutableStateFlow(false)
    val connected = _connected.asStateFlow()

    fun connect(device: BluetoothDevice) {
        viewModelScope.launch {
            elm = ElmConnection(device)
            val ok = elm?.connect() ?: false
            _connected.value = ok

            if (ok) {
                startPolling()
                startCanStream()
            }
        }
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (_connected.value) {
                elm?.sendPid("010C") // RPM
                delay(300)
                elm?.sendPid("010D") // SPEED
                delay(300)
            }
        }
    }

    private fun startCanStream() {
        viewModelScope.launch {
            elm?.startCanStream { canId, data ->
                parsePid(canId, data)
                logger.log(canId, data)
            }
        }
    }

    private fun parsePid(id: String, data: String) {
        val bytes = data.split(" ").mapNotNull { it.toIntOrNull(16) }

        if (bytes.size < 3) return

        when (bytes[1]) {
            0x0C -> { // RPM
                val value = ((bytes[2] shl 8) + bytes[3]) / 4
                _rpm.value = value
            }

            0x0D -> { // SPEED
                _speed.value = bytes[2]
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            _connected.value = false
            elm?.disconnect()
            elm = null
        }
    }

    fun reconnect(device: BluetoothDevice) {
        disconnect()
        viewModelScope.launch {
            delay(1000)
            connect(device)
        }
    }
}
