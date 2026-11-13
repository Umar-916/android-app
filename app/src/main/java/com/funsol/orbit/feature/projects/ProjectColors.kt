package com.funsol.orbit.feature.projects

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import com.funsol.orbit.R
import com.funsol.orbit.core.designsystem.OrbitWell
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.designsystem.orbitGlow

object ProjectColors {
    val palette: List<Int> = listOf(
        0xFF7167F5,
        0xFF3B82F6,
        0xFF0E8A96,
        0xFF2E9E6B,
        0xFFB87400,
        0xFFD9622B,
        0xFFD64545,
        0xFFC2417C,
        0xFF8E44AD,
        0xFF5C6B7A,
    ).map { it.toInt() }

    val default: Int get() = palette[0]

    private val names = listOf(
        R.string.planning_color_violet,
        R.string.planning_color_blue,
        R.string.planning_color_teal,
        R.string.planning_color_green,
        R.string.planning_color_amber,
        R.string.planning_color_orange,
        R.string.planning_color_red,
        R.string.planning_color_pink,
        R.string.planning_color_purple,
        R.string.planning_color_slate,
    )

    fun nameRes(color: Int): Int = names[palette.indexOf(color).takeIf { it >= 0 } ?: 0]
}

fun Color.lighten(amount: Float = 0.18f): Color = lerp(this, Color.White, amount)

fun Color.deepen(amount: Float = 0.30f): Color = lerp(this, Color.Black, amount)

fun Color.toGradient(): Brush = Brush.linearGradient(listOf(lighten(), deepen()))

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerRow(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.planning_color_label),
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OrbitWell(Modifier.fillMaxWidth()) {
            FlowRow(
                Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
                verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
            ) {
                ProjectColors.palette.forEach { argb ->
                    val isSelected = argb == selected
                    val color = Color(argb)
                    val name = stringResource(ProjectColors.nameRes(argb))
                    val description = if (isSelected) stringResource(R.string.planning_color_selected, name) else name
                    val swatchSize by animateDpAsState(if (isSelected) 38.dp else 32.dp, label = "swatch")
                    Box(
                        modifier = Modifier
                            .size(Spacing.touchTarget)
                            .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(argb) })
                            .clearAndSetSemantics {
                                contentDescription = description
                                this.selected = isSelected
                                role = Role.RadioButton
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .size(swatchSize)
                                .then(
                                    if (isSelected) Modifier.orbitGlow(CircleShape, color.copy(alpha = 0.55f), radius = 8.dp, offsetY = 2.dp)
                                    else Modifier,
                                )
                                .clip(CircleShape)
                                .background(color.toGradient())
                                .then(
                                    if (isSelected) Modifier.border(BorderStroke(2.5.dp, MaterialTheme.colorScheme.surface), CircleShape)
                                    else Modifier,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}
