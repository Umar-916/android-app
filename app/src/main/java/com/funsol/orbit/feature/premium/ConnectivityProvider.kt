package com.funsol.orbit.feature.premium

fun interface ConnectivityProvider {
    fun isOnline(): Boolean
}

object AlwaysOnlineConnectivity : ConnectivityProvider {
    override fun isOnline(): Boolean = true
}
