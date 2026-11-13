package com.funsol.orbit.feature.premium

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.funsol.orbit.R
import com.funsol.orbit.core.designsystem.FeatureAccent
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.Radius
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.designsystem.orbitGlow
import com.funsol.orbit.feature.onboarding.rememberLoop
import com.funsol.orbit.feature.onboarding.rememberPulse

@Composable
fun PremiumUpgradeCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val extras = Orbit.extras
    val shape = RoundedCornerShape(Radius.card)
    val stacked = LocalDensity.current.fontScale > 1.4f
    Column(
        modifier
            .fillMaxWidth()
            .orbitGlow(shape, extras.actionGlow, radius = 12.dp, offsetY = 5.dp)
            .clip(shape)
            .background(extras.heroGradient)
            .drawWithContent {
                val corner = Offset(if (layoutDirection == LayoutDirection.Rtl) 0f else size.width, 0f)
                drawCircle(Color.White.copy(alpha = 0.10f), 56.dp.toPx(), corner, style = Stroke(1.dp.toPx()))
                drawCircle(Color.White.copy(alpha = 0.07f), 86.dp.toPx(), corner, style = Stroke(1.dp.toPx()))
                drawContent()
            }
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 76.dp)
            .padding(horizontal = Spacing.md, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterVertically),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ShimmerTile()
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(R.string.prem_card_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(R.string.prem_card_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.88f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!stacked) CtaPill()
        }
        if (stacked) CtaPill(Modifier.align(Alignment.End))
    }
}

@Composable
private fun ShimmerTile() {
    val extras = Orbit.extras
    val pulse = rememberPulse(1_800)
    val sweep = rememberLoop(2_800)
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier
            .size(48.dp)
            .graphicsLayer {
                val s = 1f + 0.04f * pulse.value
                scaleX = s
                scaleY = s
            }
            .orbitGlow(shape, extras.accent(FeatureAccent.PREMIUM).copy(alpha = 0.18f), radius = 6.dp, offsetY = 2.dp)
            .clip(shape)
            .background(extras.accentGradient(FeatureAccent.PREMIUM))
            .drawWithContent {
                drawContent()
                val w = size.width
                val x = -0.8f * w + sweep.value * 2.4f * w
                drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.38f), Color.Transparent), startX = x, endX = x + 0.8f * w))
            }
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.WorkspacePremium, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun CtaPill(modifier: Modifier = Modifier) {
    Box(
        modifier.heightIn(min = 36.dp).clip(CircleShape).background(Color.White).padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(R.string.prem_card_cta),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
        )
    }
}
