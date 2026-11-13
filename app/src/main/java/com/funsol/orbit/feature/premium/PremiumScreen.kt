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
import com.funsol.orbit.core.analytics.PostAnalytic
import com.funsol.orbit.core.analytics.PremiumEvents
import com.funsol.orbit.core.analytics.postAnalytic
import com.funsol.orbit.core.common.rememberClickGuard
import com.funsol.orbit.core.navigation.OrbitNavigator
import com.funsol.orbit.core.startup.continueTo
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PremiumScreen(variant: Int, fromOnboarding: Boolean, nav: OrbitNavigator) {
    val vm: PremiumViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    PostAnalytic(
        when (variant) {
            2 -> PremiumEvents.PREMIUM_COMPARE_SCREEN_DISPLAY
            3 -> PremiumEvents.PREMIUM_BLACK_SCREEN_DISPLAY
            else -> PremiumEvents.PREMIUM_AURORA_SCREEN_DISPLAY
        },
    )
    val context = LocalContext.current
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

    BackHandler(enabled = true) {
        if (closeControl.backEnabled) {
            context.postAnalytic(PremiumEvents.PREMIUM_BACK_CLICK)
            close()
        }
    }

    LaunchedEffect(state.isPurchased) {
        if (state.isPurchased) {
            context.postAnalytic(PremiumEvents.PREMIUM_PURCHASED)
            vm.confirmPurchase { close() }
        }
    }

    val actions = PremiumActions(
        state = state,
        close = closeControl,
        onClose = ::close,
        onPurchase = {
            context.postAnalytic(PremiumEvents.PREMIUM_CONTINUE_CLICK)
            activity?.let(vm::purchase)
        },
        onRestore = {
            context.postAnalytic(PremiumEvents.PREMIUM_RESTORE_CLICK)
            vm.restore()
        },
        onToggleTrial = {
            context.postAnalytic(PremiumEvents.PREMIUM_TRIAL_TOGGLE)
            vm.toggleTrial()
        },
    )
    when (variant) {
        2 -> PremiumCompare(actions)
        3 -> PremiumBlack(actions)
        else -> PremiumAurora(actions)
    }
}
