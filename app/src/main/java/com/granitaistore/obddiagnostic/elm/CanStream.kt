package com.granitaistore.obddiagnostic.elm

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class CanStream(private val elm: ElmConnection) {

    fun stream(): Flow<CanFrame> = flow {
        elm.send("ATMA") // start monitor all

        while (true) {
            val raw = elm.send("")
            val lines = raw.lines()

            for (line in lines) {
                if (line.length > 8 && line.matches(Regex("^[0-9A-F]{3}.*"))) {
                    val id = line.substring(0, 3)
                    val data = line.substring(4)
                    emit(CanFrame(id, data))
                }
            }

            delay(30)
        }
    }
}
