package com.funsol.orbit.feature.premium

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.funsol.orbit.core.common.rememberClickGuard
import com.funsol.orbit.core.navigation.OrbitNavigator
import com.funsol.orbit.core.startup.continueTo
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PremiumScreen(variant: Int, fromOnboarding: Boolean, nav: OrbitNavigator) {
    val vm: PremiumViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val activity = androidx.activity.compose.LocalActivity.current
    val guard = rememberClickGuard(800L)

    val policy = remember(fromOnboarding, state.billingAvailable) {
        PremiumClosePolicy(homeReached = !fromOnboarding, billingAvailable = state.billingAvailable)
    }
    var delayElapsed by remember(policy) { mutableStateOf(policy.delayMs == 0L) }
    LaunchedEffect(policy) {
        if (policy.delayMs > 0L) {
            delay(policy.delayMs)
            delayElapsed = true
        }
    }
    val closeControl = policy.control(delayElapsed)

    fun close() {
        guard.run {
            vm.close(fromOnboarding) { next -> if (next != null) nav.continueTo(next) else nav.back() }
        }
    }

    BackHandler(enabled = true) { if (closeControl.backEnabled) close() }

    LaunchedEffect(state.isPurchased) {
        if (state.isPurchased) vm.confirmPurchase { close() }
    }

    val actions = PremiumActions(
        state = state,
        close = closeControl,
        onClose = ::close,
        onPurchase = { activity?.let(vm::purchase) },
        onRestore = vm::restore,
        onToggleTrial = vm::toggleTrial,
    )
    when (variant) {
        2 -> PremiumCompare(actions)
        3 -> PremiumBlack(actions)
        else -> PremiumAurora(actions)
    }
}
