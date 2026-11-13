package com.funsol.orbit.feature.premium

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.funsol.orbit.R
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitAmber
import com.funsol.orbit.core.designsystem.OrbitViolet
import com.funsol.orbit.core.designsystem.Spacing

private val SliderSidePad = 20.dp

private val SliderRowHeight = 40.dp

private val sliderRows = listOf(
    R.string.prem_feat_ads_title,
    R.string.prem_feat_focus_title,
    R.string.prem_feat_support_title,
    R.string.prem_row_all,
    R.string.prem_row_private,
    R.string.prem_row_backup,
)

@Composable
internal fun PremiumAurora(actions: PremiumActions) {
    val extras = Orbit.extras
    val background = MaterialTheme.colorScheme.background
    val state = actions.state
    Box(
        Modifier
            .fillMaxSize()
            .background(background)
            .drawBehind { drawRect(Brush.verticalGradient(listOf(OrbitViolet.copy(alpha = 0.12f), Color.Transparent), endY = 320.dp.toPx())) },
    ) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    Modifier
                        .statusBarsPadding()
                        .padding(top = 56.dp)
                        .widthIn(max = PremiumContentMaxWidth)
                        .fillMaxWidth()
                        .padding(horizontal = SliderSidePad)
                        .padding(bottom = Spacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    Column(
                        Modifier.fillMaxWidth().revealed(0),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        PremiumHero()
                        Text(
                            stringResource(R.string.prem_slider_title),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            stringResource(R.string.prem_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = extras.textMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = Spacing.sm),
                        )
                    }
                    Text(
                        stringResource(R.string.prem_included_header),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = Spacing.xs).semantics { heading() }.revealed(1),
                    )
                    Column(Modifier.fillMaxWidth().revealed(2), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        PremiumOutlinedCard {
                            Column(Modifier.fillMaxWidth()) {
                                sliderRows.forEach { IncludedRow(it) }
                            }
                        }
                        if (state.canPurchase && state.trialAvailable) TrialCard(state.trialEnabled, actions.onToggleTrial)
                        if (state.canPurchase) PlanCard(state)
                    }
                    if (!state.canPurchase) UnavailableNote(dark = false, modifier = Modifier.revealed(3))
                    else Text(
                        stringResource(R.string.prem_terms),
                        style = MaterialTheme.typography.labelSmall,
                        color = extras.textMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(background)
                    .navigationBarsPadding()
                    .padding(horizontal = SliderSidePad)
                    .padding(top = Spacing.sm, bottom = Spacing.md),
                contentAlignment = Alignment.Center,
            ) {
                PremiumBottomBar(
                    actions,
                    BottomBarStyle(dark = false, gap = 10.dp, showTerms = false, ctaStyle = MaterialTheme.typography.titleMedium),
                    modifier = Modifier.widthIn(max = PremiumContentMaxWidth),
                    below = {
                        DotLine(
                            stringResource(if (state.trialOn) R.string.prem_no_payment_now else R.string.prem_cancel_anytime),
                            showDot = state.trialOn,
                        )
                    },
                    ctaText = stringResource(if (state.trialOn) R.string.prem_continue_free else R.string.action_continue).uppercase(),
                )
            }
        }
        CloseOverlay(actions.close, actions.onClose)
    }
}

@Composable
private fun DotLine(text: String, showDot: Boolean) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showDot) Box(Modifier.size(8.dp).clip(CircleShape).background(OrbitAmber))
        Text(text, style = MaterialTheme.typography.labelSmall, color = Orbit.extras.textMuted, textAlign = TextAlign.Center)
    }
}

@Composable
private fun IncludedRow(@StringRes title: Int) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = SliderRowHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(stringResource(title), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        PremiumCheckBox()
    }
}

@Composable
internal fun PremiumCheckBox(modifier: Modifier = Modifier) {
    Box(
        modifier.size(18.dp).clip(RoundedCornerShape(5.dp)).background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
    }
}

@Composable
private fun TrialCard(enabled: Boolean, onToggle: () -> Unit) {
    PremiumOutlinedCard {
        Row(
            Modifier
                .fillMaxWidth()
                .toggleable(value = enabled, role = Role.Switch, onValueChange = { onToggle() }),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.prem_trial_title), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.prem_trial_subtitle), style = MaterialTheme.typography.bodySmall, color = Orbit.extras.textMuted)
            }
            PremiumTrialSwitch(checked = enabled)
        }
    }
}

@Composable
private fun PlanCard(state: PremiumUiState) {
    val extras = Orbit.extras
    PremiumOutlinedCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.prem_brand), style = MaterialTheme.typography.titleSmall)
                Text(state.price ?: stringResource(R.string.prem_plan_unavailable), style = MaterialTheme.typography.bodySmall, color = extras.textMuted)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (state.trialOn) {
                    Text(stringResource(R.string.prem_trial_on), style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.End)
                    Text(stringResource(R.string.prem_no_payment_now), style = MaterialTheme.typography.bodySmall, color = extras.textMuted, textAlign = TextAlign.End)
                } else {
                    Text(stringResource(R.string.prem_plan_managed), style = MaterialTheme.typography.bodySmall, color = extras.textMuted, textAlign = TextAlign.End)
                }
            }
        }
    }
}
