package com.funsol.orbit.feature.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay

@Composable
internal fun Modifier.staggerIn(index: Int, stepMs: Long = 40L, offsetPx: Float = 40f): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index.coerceAtMost(10) * stepMs)
        progress.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
    }
    return this.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * offsetPx
    }
}

@Composable
internal fun rememberAnimationsEnabled(): Boolean {
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember {
        android.provider.Settings.Global.getFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    }
}

@Composable
internal fun rememberArtLoop(durationMs: Int, staticValue: Float = 0.62f): androidx.compose.runtime.State<Float> {
    val enabled = rememberAnimationsEnabled()
    return if (enabled) rememberLoop(durationMs) else remember { androidx.compose.runtime.mutableStateOf(staticValue) }
}

@Composable
internal fun rememberArtPulse(durationMs: Int = 1800, staticValue: Float = 0.7f): androidx.compose.runtime.State<Float> {
    val enabled = rememberAnimationsEnabled()
    return if (enabled) rememberPulse(durationMs) else remember { androidx.compose.runtime.mutableStateOf(staticValue) }
}
