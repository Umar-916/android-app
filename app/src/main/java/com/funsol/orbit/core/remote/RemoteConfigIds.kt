package com.funsol.orbit.core.remote

object RemoteConfigIds {
    const val API_KEY = ""
    const val APP_ID = ""
    const val PROJECT_ID = ""

    val isConfigured: Boolean get() = API_KEY.isNotBlank() && APP_ID.isNotBlank() && PROJECT_ID.isNotBlank()
}
