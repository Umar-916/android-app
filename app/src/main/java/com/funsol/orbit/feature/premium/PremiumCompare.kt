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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.funsol.orbit.R
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.Spacing

private val CompareSidePad = 20.dp

private val CompareRowHeight = 44.dp

private class CompareRow(
    @StringRes val label: Int,
    val icon: ImageVector,
    val inBasic: Boolean,
)

private val compareRows = listOf(
    CompareRow(R.string.prem_row_ads, Icons.Outlined.Block, inBasic = false),
    CompareRow(R.string.prem_row_support, Icons.Outlined.FavoriteBorder, inBasic = false),
    CompareRow(R.string.prem_row_all, Icons.Outlined.TaskAlt, inBasic = true),
    CompareRow(R.string.prem_row_private, Icons.Outlined.PrivacyTip, inBasic = true),
    CompareRow(R.string.prem_row_backup, Icons.Outlined.Backup, inBasic = true),
    CompareRow(R.string.prem_row_reminders, Icons.Outlined.Notifications, inBasic = true),
)

@Composable
internal fun PremiumCompare(actions: PremiumActions) {
    val extras = Orbit.extras
    val background = MaterialTheme.colorScheme.background
    val state = actions.state
    val columnWidth = (58f * LocalDensity.current.fontScale.coerceIn(1f, 1.4f)).dp
    Box(Modifier.fillMaxSize().background(background)) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    Modifier
                        .statusBarsPadding()
                        .padding(top = 64.dp)
                        .widthIn(max = PremiumContentMaxWidth)
                        .fillMaxWidth()
                        .padding(horizontal = CompareSidePad)
                        .padding(bottom = Spacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        Modifier.fillMaxWidth().revealed(0),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        Icon(
                            Icons.Outlined.WorkspacePremium,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp),
                        )
                        Text(
                            stringResource(R.string.prem_zippo_title),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            stringResource(R.string.prem_compare_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = extras.textMuted,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Column(Modifier.fillMaxWidth().padding(top = 32.dp).revealed(1)) {
                        ColumnHeaders(columnWidth)
                        FeatureTable(columnWidth)
                    }
                    if (!state.canPurchase) UnavailableNote(dark = false, modifier = Modifier.padding(top = Spacing.lg).revealed(2))
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(background)
                    .navigationBarsPadding()
                    .padding(horizontal = CompareSidePad)
                    .padding(top = Spacing.sm, bottom = Spacing.md),
                contentAlignment = Alignment.Center,
            ) {
                PremiumBottomBar(
                    actions,
                    BottomBarStyle(dark = false, buttonRadius = 12.dp, arrow = false, gap = 10.dp),
                    modifier = Modifier.widthIn(max = PremiumContentMaxWidth),
                    above = {
                        state.price?.let { price ->
                            Text(
                                if (state.trialOn) stringResource(R.string.prem_price_after_trial, price) else stringResource(R.string.prem_price_line, price),
                                style = MaterialTheme.typography.labelMedium,
                                color = extras.textMuted,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                            )
                        }
                    },
                    ctaText = stringResource(if (state.trialOn) R.string.prem_continue_free else R.string.action_continue).uppercase(),
                )
            }
        }
        CloseOverlay(actions.close, actions.onClose, plain = true, tint = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun ColumnHeaders(columnWidth: Dp) {
    val extras = Orbit.extras
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f))
        Box(Modifier.width(columnWidth), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.prem_col_premium).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
            )
        }
        Box(Modifier.width(columnWidth), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.prem_col_basic).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = extras.textMuted,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
            )
        }
    }
}

@Composable
private fun FeatureTable(columnWidth: Dp) {
    val extras = Orbit.extras
    val included = stringResource(R.string.prem_included_cd)
    val notIncluded = stringResource(R.string.prem_not_included_cd)
    Column(Modifier.fillMaxWidth()) {
        compareRows.forEach { row ->
            val label = stringResource(row.label)
            val summary = stringResource(R.string.prem_compare_row_cd, label, if (row.inBasic) included else notIncluded, included)
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = CompareRowHeight)
                    .semantics(mergeDescendants = true) { contentDescription = summary },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(row.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f).padding(start = 16.dp, end = Spacing.sm),
                )
                Box(Modifier.width(columnWidth).clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
                Box(Modifier.width(columnWidth).clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
                    if (row.inBasic) Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    else Icon(Icons.Rounded.Remove, contentDescription = null, tint = extras.textMuted, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
