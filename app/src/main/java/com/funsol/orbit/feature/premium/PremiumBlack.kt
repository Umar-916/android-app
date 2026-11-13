package com.funsol.orbit.feature.premium

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.funsol.orbit.R
import com.funsol.orbit.core.common.HideSystemBars
import com.funsol.orbit.core.designsystem.OrbitAmber
import com.funsol.orbit.core.designsystem.OrbitCoral
import com.funsol.orbit.core.designsystem.OrbitViolet
import com.funsol.orbit.core.designsystem.Spacing

private val PureBlack = Color(0xFF000000)
private val BlackMuted = Color(0xFFB4B1D8)
private val BlackBlue = Color(0xFF3B82F6)

private val BlackSidePad = 24.dp

@Composable
internal fun PremiumBlack(actions: PremiumActions) {
    HideSystemBars()
    val state = actions.state
    val ctaBrush = Brush.linearGradient(listOf(OrbitAmber, OrbitAmber))
    val circleColors = listOf(OrbitViolet, BlackBlue, OrbitCoral)
    Box(Modifier.fillMaxSize().background(PureBlack)) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    Modifier
                        .padding(top = 72.dp)
                        .widthIn(max = PremiumContentMaxWidth)
                        .fillMaxWidth()
                        .padding(horizontal = BlackSidePad)
                        .padding(bottom = Spacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        Modifier.fillMaxWidth().revealed(0),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            stringResource(R.string.prem_brand),
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                            color = OrbitAmber,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            stringResource(R.string.prem_black_title),
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Column(
                        Modifier.fillMaxWidth().padding(top = 44.dp, bottom = 36.dp),
                        verticalArrangement = Arrangement.spacedBy(28.dp),
                    ) {
                        premiumFeatures.take(3).forEachIndexed { index, feature ->
                            BlackFeatureRow(feature, circleColors[index], Modifier.revealed(1 + index))
                        }
                    }
                    Text(
                        stringResource(R.string.prem_black_cta).uppercase(),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().revealed(4),
                    )
                    if (!state.canPurchase) UnavailableNote(dark = true, modifier = Modifier.padding(top = Spacing.lg).revealed(5))
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(PureBlack)
                    .navigationBarsPadding()
                    .padding(horizontal = BlackSidePad)
                    .padding(top = 12.dp, bottom = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                PremiumBottomBar(
                    actions,
                    BottomBarStyle(
                        dark = true,
                        brush = ctaBrush,
                        glow = OrbitAmber.copy(alpha = 0.40f),
                        buttonHeight = 56.dp,
                        buttonRadius = 14.dp,
                        arrow = false,
                        gap = 10.dp,
                        showTerms = false,
                        ctaStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        contentColor = Color.Black,
                    ),
                    modifier = Modifier.widthIn(max = PremiumContentMaxWidth),
                    above = {
                        state.price?.let { price ->
                            Text(
                                stringResource(R.string.prem_price_line, price),
                                style = MaterialTheme.typography.labelMedium,
                                color = BlackMuted,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                            )
                        }
                    },
                    below = {
                        Text(
                            stringResource(R.string.prem_black_secure),
                            style = MaterialTheme.typography.labelSmall,
                            color = BlackMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    ctaText = stringResource(R.string.prem_black_go).uppercase(),
                )
            }
        }
        CloseOverlay(actions.close, actions.onClose, plain = true)
    }
}

@Composable
private fun BlackFeatureRow(feature: PremiumFeature, circle: Color, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(circle), contentAlignment = Alignment.Center) {
            Icon(feature.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(feature.title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
            Text(stringResource(feature.body), style = MaterialTheme.typography.bodySmall, color = BlackMuted, maxLines = 2)
        }
    }
}
