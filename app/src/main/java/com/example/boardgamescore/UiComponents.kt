package com.example.boardgamescore

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun Page(
    content: @Composable ColumnScope.() -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val horizontalPadding = if (maxWidth < 360.dp) {
            12.dp
        } else {
            20.dp
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxWidth()
                    .padding(
                        horizontal = horizontalPadding,
                        vertical = 20.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content
            )
        }
    }
}

@Composable
internal fun GlowPanel(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val motion = LocalMotion.current

    val tintAlpha by animateFloatAsState(
        targetValue = if (highlighted) 0.10f else 0.025f,
        animationSpec = tween(
            durationMillis = if (motion) 240 else 0
        ),
        label = "panel_tint"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = colors.surface,
        contentColor = colors.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = if (highlighted) 3.dp else 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = if (highlighted) {
                colors.primary.copy(alpha = 0.35f)
            } else {
                colors.outlineVariant.copy(alpha = 0.55f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            colors.primary.copy(alpha = tintAlpha),
                            Color.Transparent
                        )
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
internal fun Heading(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            letterSpacing = (-0.6).sp
        )

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun MainButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}

@Composable
internal fun Choice(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = if (selected) {
            colors.primary.copy(alpha = 0.09f)
        } else {
            colors.surfaceVariant.copy(alpha = 0.24f)
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) {
                colors.primary.copy(alpha = 0.45f)
            } else {
                colors.outlineVariant.copy(alpha = 0.35f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .selectable(
                    selected = selected,
                    role = Role.RadioButton,
                    onClick = onClick
                )
                .heightIn(min = 56.dp)
                .padding(
                    horizontal = 14.dp,
                    vertical = 10.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RadioButton(
                selected = selected,
                onClick = null
            )

            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) {
                    FontWeight.SemiBold
                } else {
                    FontWeight.Normal
                }
            )
        }
    }
}

@Composable
internal fun ScoreNumber(value: Long?) {
    val motion = LocalMotion.current

    AnimatedContent(
        targetState = value,
        transitionSpec = {
            val duration = if (motion) 220 else 0
            val increasing =
                (targetState ?: 0L) >= (initialState ?: 0L)

            (
                slideInVertically(
                    animationSpec = tween(duration)
                ) { height ->
                    if (increasing) height / 3 else -height / 3
                } + fadeIn(tween(duration))
            ) togetherWith (
                slideOutVertically(
                    animationSpec = tween(duration)
                ) { height ->
                    if (increasing) -height / 3 else height / 3
                } + fadeOut(tween(duration))
            )
        },
        label = "score_number"
    ) { current ->
        Text(
            text = current?.toString() ?: "—",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = if (current == null) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            }
        )
    }
}