package com.granitaistore.obddiagnostic

class OpelCanSniffer(private val send: (String) -> String) {

    fun enable() {
        send("ATCAF0")
        send("ATSP6")
        send("ATMA")
    }
}
