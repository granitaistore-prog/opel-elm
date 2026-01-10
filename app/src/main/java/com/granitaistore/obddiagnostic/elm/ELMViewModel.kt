package com.granitaistore.obddiagnostic.elm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.granitaistore.obddiagnostic.logger.CsvLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ElmViewModel(app: Application) : AndroidViewModel(app) {

    private val elm = ElmConnection()
    private val logger = CsvLogger(app)

    val rpm = MutableStateFlow(0)
    val speed = MutableStateFlow(0)
    val boost = MutableStateFlow(0f)
    val canFrames = MutableStateFlow<List<String>>(emptyList())
    val connected = MutableStateFlow(false)
    val logging = MutableStateFlow(false)

    fun connect(mac: String) {
        viewModelScope.launch {
            connected.value = elm.connect(mac)
            if (connected.value) startLiveLoop()
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            elm.disconnect()
            connected.value = false
        }
    }

    fun startLogging() {
        logging.value = true
        logger.start()
    }

    fun stopLogging() {
        logging.value = false
        logger.stop()
    }

    private fun startLiveLoop() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    if (!elm.isConnected()) {
                        connected.value = elm.reconnect()
                        delay(1500)
                        continue
                    }

                    val r = elm.readRpm()
                    val s = elm.readSpeed()
                    val b = elm.readBoost()
                    val can = elm.readCanFrame()

                    rpm.value = r
                    speed.value = s
                    boost.value = b

                    if (can != null) {
                        val line = "${can.first},${can.second}"
                        canFrames.value = (canFrames.value + line).takeLast(200)
                        if (logging.value) logger.logCan(can.first, can.second)
                    }

                    if (logging.value) logger.logPid(r, s, b)

                    delay(200)

                } catch (e: Exception) {
                    connected.value = false
                    delay(2000)
                }
            }
        }
    }
}
