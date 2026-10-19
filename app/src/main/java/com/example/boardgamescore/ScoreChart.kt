package com.example.boardgamescore

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun ScoreChart(
    cumulative: List<List<Long>>,
    players: List<String>
) {
    if (
        cumulative.isEmpty() ||
        cumulative.size != players.size ||
        cumulative.any { it.isEmpty() }
    ) {
        return
    }

    val allValues = cumulative.flatten()

    if (allValues.isEmpty()) {
        return
    }

    val minimum = allValues.minOrNull() ?: 0L
    val maximum = allValues.maxOrNull() ?: 0L
    val range = (maximum - minimum).coerceAtLeast(1L)
    val allEqual = minimum == maximum

    GlowPanel {
        Text(
            text = tr(
                "График по раундам",
                "Round chart"
            ),
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )

        Text(
            text = tr(
                "Диапазон: $minimum…$maximum",
                "Range: $minimum…$maximum"
            ),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val inset = 8.dp.toPx()
            val chartWidth =
                (size.width - inset * 2).coerceAtLeast(0f)
            val chartHeight =
                (size.height - inset * 2).coerceAtLeast(0f)

            cumulative.forEachIndexed { playerIndex, line ->
                val color =
                    ChartColors[playerIndex % ChartColors.size]

                val path = Path()

                line.forEachIndexed { roundIndex, score ->
                    val x = if (line.size == 1) {
                        size.width / 2f
                    } else {
                        inset +
                            roundIndex * chartWidth /
                            (line.size - 1)
                    }

                    val normalized =
                        (score - minimum).toFloat() /
                            range.toFloat()

                    val y = if (allEqual) {
                        size.height / 2f
                    } else {
                        inset + chartHeight * (1f - normalized)
                    }

                    if (roundIndex == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }

                    drawCircle(
                        color = color,
                        radius = 3.dp.toPx(),
                        center = Offset(x, y)
                    )
                }

                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            players.forEachIndexed { index, player ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = ChartColors[
                                    index % ChartColors.size
                                ],
                                shape = RoundedCornerShape(6.dp)
                            )
                    )

                    Text(
                        text = player,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}