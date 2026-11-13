package com.funsol.orbit.feature.premium

import android.app.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface BillingProvider {
    val isAvailable: Boolean
    val price: StateFlow<String?>
    val isPurchased: StateFlow<Boolean>

    val supportsTrial: Boolean get() = false

    fun selectTrial(enabled: Boolean) = Unit

    fun purchase(activity: Activity)
    fun restore()
}

class UnavailableBillingProvider : BillingProvider {
    override val isAvailable: Boolean = false
    override val price: StateFlow<String?> = MutableStateFlow<String?>(null).asStateFlow()
    override val isPurchased: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()
    override fun purchase(activity: Activity) = Unit
    override fun restore() = Unit
}
