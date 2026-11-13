package com.funsol.orbit.feature.home

import android.provider.Settings
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.funsol.orbit.R
import com.funsol.orbit.core.common.DateFormats
import com.funsol.orbit.core.common.Progress
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.isLargeFontScale
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.designsystem.orbitGlow
import java.time.LocalDate
import java.time.LocalTime

private val HeroShape = RoundedCornerShape(22.dp)

@Composable
internal fun HomeHero(
    today: LocalDate,
    progress: Progress?,
    onSearch: () -> Unit,
    onCustomize: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hour = remember(today) { LocalTime.now().hour }
    val greeting = stringResource(
        when {
            hour < 12 -> R.string.home_greeting_morning
            hour < 18 -> R.string.home_greeting_afternoon
            else -> R.string.home_greeting_evening
        },
    )
    val extras = Orbit.extras
    Box(
        modifier
            .fillMaxWidth()
            .orbitGlow(HeroShape, extras.actionGlow.copy(alpha = 0.22f), radius = 14.dp, offsetY = 5.dp)
            .clip(HeroShape)
            .background(extras.heroGradient),
    ) {
        OrbitRings(Modifier.matchParentSize())
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Column(Modifier.weight(1f)) {
                    Text(
                        greeting,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        DateFormats.fullDate(today),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.82f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(
                    onClick = onCustomize,
                    modifier = Modifier.size(Spacing.touchTarget).padding(6.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.16f)),
                ) {
                    Icon(Icons.Rounded.Tune, contentDescription = stringResource(R.string.home_customize), tint = Color.White)
                }
            }
            if (progress != null) HeroProgress(progress)
            SearchPill(onSearch)
        }
    }
}

@Composable
private fun HeroProgress(progress: Progress) {
    Row(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        if (progress.total == 0) {
            Box(
                Modifier.size(52.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.EventAvailable, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Text(
                    stringResource(R.string.home_progress_empty_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
                Text(
                    stringResource(R.string.home_progress_empty_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.82f),
                )
            }
        } else {
            val allDone = progress.done >= progress.total
            WhiteRing(progress.fraction, stringResource(R.string.home_percent, progress.percent), if (isLargeFontScale()) 76.dp else 56.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Text(
                    pluralStringResource(R.plurals.home_tasks_done_of, progress.total, progress.done, progress.total),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
                Text(
                    stringResource(if (allDone) R.string.home_progress_all_done else R.string.home_progress_keep_going),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.82f),
                )
            }
        }
    }
}

@Composable
private fun WhiteRing(fraction: Float, label: String, size: Dp) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(800), label = "heroRing")
    val compact = size <= 60.dp
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            val inset = stroke.width / 2
            val arcSize = Size(this.size.width - stroke.width, this.size.height - stroke.width)
            drawArc(Color.White.copy(alpha = 0.25f), 0f, 360f, false, topLeft = Offset(inset, inset), size = arcSize, style = stroke)
            if (animated > 0f) {
                drawArc(Color.White, -90f, 360f * animated, false, topLeft = Offset(inset, inset), size = arcSize, style = stroke)
            }
        }
        // Keep a comfortable gap between the label and the ring stroke, most visible at "100%".
        Text(
            label,
            style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleSmall,
            color = Color.White,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

private val SearchHintRes = listOf(
    R.string.home_search_hint_tasks,
    R.string.home_search_hint_notes,
    R.string.home_search_hint_projects,
)

private const val SEARCH_HINT_INTERVAL_MS = 3_000L

@Composable
private fun SearchPill(onClick: () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    val context = LocalContext.current
    val animationsOff = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    var hintIndex by rememberSaveable { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    if (!animationsOff) {
        LaunchedEffect(lifecycleOwner) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    delay(SEARCH_HINT_INTERVAL_MS)
                    hintIndex = (hintIndex + 1) % SearchHintRes.size
                }
            }
        }
    }
    val staticHint = stringResource(R.string.home_search_hint)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 46.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.16f))
            .border(0.8.dp, Color.White.copy(alpha = 0.32f), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.md)
            .semantics(mergeDescendants = true) { contentDescription = staticHint },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.White)
        Crossfade(
            targetState = hintIndex.coerceIn(0, SearchHintRes.lastIndex),
            animationSpec = tween(450),
            label = "searchHint",
            modifier = Modifier.weight(1f),
        ) { index ->
            Text(
                stringResource(SearchHintRes[index]),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun OrbitRings(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val rtl = layoutDirection == LayoutDirection.Rtl
        val cx = if (rtl) size.width * 0.04f else size.width * 0.96f
        val cy = size.height * 0.06f
        val ringColor = Color.White
        listOf(50.dp to 0.12f, 84.dp to 0.09f, 120.dp to 0.06f).forEach { (r, a) ->
            drawCircle(ringColor.copy(alpha = a), radius = r.toPx(), center = Offset(cx, cy), style = Stroke(width = 1.2.dp.toPx()))
        }
        drawCircle(ringColor.copy(alpha = 0.10f), radius = 26.dp.toPx(), center = Offset(cx, cy))
        val px = if (rtl) cx + 84.dp.toPx() * 0.72f else cx - 84.dp.toPx() * 0.72f
        drawCircle(ringColor.copy(alpha = 0.55f), radius = 4.dp.toPx(), center = Offset(px, cy + 84.dp.toPx() * 0.69f))
        val bx = if (rtl) size.width * 0.92f else size.width * 0.08f
        drawCircle(ringColor.copy(alpha = 0.10f), radius = 40.dp.toPx(), center = Offset(bx, size.height * 1.02f))
    }
}
