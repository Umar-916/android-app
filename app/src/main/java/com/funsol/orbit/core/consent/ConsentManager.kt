package com.funsol.orbit.core.consent

import android.app.Activity
import android.content.Context
import com.funsol.orbit.core.common.tryCatch
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class ConsentManager(context: Context) {
    private val information: ConsentInformation = UserMessagingPlatform.getConsentInformation(context.applicationContext)

    private val _canRequestAds = MutableStateFlow(information.canRequestAds())
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(isPrivacyOptionsRequired())
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    private val _formShowing = MutableStateFlow(false)
    val formShowing: StateFlow<Boolean> = _formShowing.asStateFlow()

    suspend fun gather(activity: Activity, infoTimeoutMs: Long = INFO_TIMEOUT_MS): Boolean {
        try {
            val updated = withTimeoutOrNull(infoTimeoutMs) { requestInfoUpdate(activity) } == true
            if (updated && information.consentStatus == ConsentInformation.ConsentStatus.REQUIRED) {
                _formShowing.value = true
                try {
                    withTimeoutOrNull(FORM_TIMEOUT_MS) { showFormIfRequired(activity) }
                } finally {
                    _formShowing.value = false
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
        }
        refresh()
        return _canRequestAds.value
    }

    fun showPrivacyOptions(activity: Activity, onDismissed: () -> Unit = {}) {
        tryCatch {
            UserMessagingPlatform.showPrivacyOptionsForm(activity) {
                refresh()
                onDismissed()
            }
        }
    }

    fun refresh() {
        _canRequestAds.value = information.canRequestAds()
        _privacyOptionsRequired.value = isPrivacyOptionsRequired()
    }

    private fun isPrivacyOptionsRequired(): Boolean =
        information.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    private suspend fun requestInfoUpdate(activity: Activity): Boolean = suspendCancellableCoroutine { cont ->
        val params = ConsentRequestParameters.Builder().build()
        information.requestConsentInfoUpdate(
            activity,
            params,
            { if (cont.isActive) cont.resume(true) },
            { if (cont.isActive) cont.resume(false) },
        )
    }

    private suspend fun showFormIfRequired(activity: Activity): Unit = suspendCancellableCoroutine { cont ->
        UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
            if (cont.isActive) cont.resume(Unit)
        }
    }

    companion object {
        const val INFO_TIMEOUT_MS = 6_000L

        const val FORM_TIMEOUT_MS = 5 * 60_000L
    }
}
