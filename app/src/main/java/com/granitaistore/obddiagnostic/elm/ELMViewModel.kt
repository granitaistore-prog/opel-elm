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
            val connection = ElmConnection(device)
            elm = connection

            val ok = connection.connect()
            _connected.value = ok

            if (ok) {
                logger.startSession()
                startPolling()
                startCanListener()
            }
        }
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (_connected.value) {
                try {
                    val rpmValue = elm?.readRpm() ?: 0
                    val speedValue = elm?.readSpeed() ?: 0

                    _rpm.value = rpmValue
                    _speed.value = speedValue

                    logger.log(
                        ecu = "ECU",
                        pid = "0C",
                        value = rpmValue.toString(),
                        unit = "rpm",
                        raw = ""
                    )

                    logger.log(
                        ecu = "ECU",
                        pid = "0D",
                        value = speedValue.toString(),
                        unit = "km/h",
                        raw = ""
                    )
                } catch (_: Exception) {
                }

                delay(500)
            }
        }
    }

    private fun startCanListener() {
        elm?.setCanListener { canId, data ->
            logger.logCanRaw(canId, data)
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            _connected.value = false
            elm?.disconnect()
            elm = null
            logger.stop()
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
