package com.hisabnikash.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisabnikash.app.ui.theme.BrandGold
import com.hisabnikash.app.ui.theme.BrandGreen
import com.hisabnikash.app.ui.theme.ChartSeries
import com.hisabnikash.app.ui.theme.InkFaint
import com.hisabnikash.app.ui.theme.SurfaceAlt
import java.util.Locale

data class ChartPoint(val label: String, val value: Float, val detail: String? = null)

/**
 * Adaptive bar chart with readable labels, tooltip on tap and accessibility
 * description. Bars never clip: the drawing area leaves room for labels.
 */
@Composable
fun BarChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    height: Int = 210,
    color: Color = BrandGreen,
    highlightMax: Boolean = true,
    maxLabels: Int = 8
) {
    if (points.isEmpty()) {
        Text(
            "No data for this period.",
            style = MaterialTheme.typography.bodyMedium,
            color = InkFaint,
            modifier = modifier.padding(12.dp)
        )
        return
    }
    var selected by remember(points) { mutableIntStateOf(-1) }
    val maxValue = remember(points) { (points.maxOf { it.value }).coerceAtLeast(1f) }
    val labelStep = remember(points.size, maxLabels) {
        ((points.size + maxLabels - 1) / maxLabels).coerceAtLeast(1)
    }
    val selectedPoint = points.getOrNull(selected)
    val accessibility = points.joinToString("; ") { "${it.label}: ${it.detail ?: it.value}" }
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val baselineColor = MaterialTheme.colorScheme.outline

    Column(modifier = modifier.fillMaxWidth()) {
        if (selectedPoint != null) {
            Text(
                "${selectedPoint.label} — ${selectedPoint.detail ?: selectedPoint.value}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .semantics { contentDescription = "Chart: $accessibility" }
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        val w = size.width
                        val slot = w / points.size
                        val idx = (offset.x / slot).toInt().coerceIn(0, points.size - 1)
                        selected = if (selected == idx) -1 else idx
                    }
                }
        ) {
            val labelSpace = 18.dp.toPx()
            val topPad = 6.dp.toPx()
            val leftPad = 34.dp.toPx()
            val chartRight = size.width - 4.dp.toPx()
            val chartBottom = size.height - labelSpace
            val chartTop = topPad
            val chartHeight = (chartBottom - chartTop).coerceAtLeast(1f)
            val slot = (chartRight - leftPad) / points.size

            // Grid lines + y labels.
            val gridCount = 3
            for (i in 0..gridCount) {
                val y = chartTop + chartHeight * i / gridCount
                drawLine(
                    color = gridColor,
                    start = Offset(leftPad, y),
                    end = Offset(chartRight, y),
                    strokeWidth = 1f
                )
                val value = maxValue * (gridCount - i) / gridCount
                drawContext.canvas.nativeCanvas.drawText(
                    formatAxis(value),
                    leftPad - 6.dp.toPx(),
                    y + 4.dp.toPx(),
                    android.graphics.Paint().apply {
                        setColor(InkFaint.toArgb())
                        textSize = 9.sp.toPx()
                        textAlign = android.graphics.Paint.Align.RIGHT
                    }
                )
            }
            points.forEachIndexed { index, point ->
                val barWidth = slot * 0.62f
                val x = leftPad + slot * index + (slot - barWidth) / 2
                val barHeight = chartHeight * (point.value / maxValue).coerceIn(0f, 1f)
                val y = chartBottom - barHeight
                drawRoundRect(
                    color = if (highlightMax && index == points.indexOfFirst { it.value == maxValue }) color
                    else color.copy(alpha = 0.55f),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight.coerceAtLeast(2f)),
                    cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                )
                if (index % labelStep == 0) {
                    drawContext.canvas.nativeCanvas.drawText(
                        point.label,
                        x + barWidth / 2,
                        chartBottom + 14.dp.toPx(),
                        android.graphics.Paint().apply {
                            setColor(InkFaint.toArgb())
                            textSize = 9.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            }
            // Baseline
            drawLine(
                color = baselineColor,
                start = Offset(leftPad, chartBottom),
                end = Offset(chartRight, chartBottom),
                strokeWidth = 1.5f
            )
        }
    }
}

@Composable
fun LineChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    height: Int = 190,
    color: Color = BrandGreen
) {
    if (points.isEmpty()) {
        Text(
            "No data for this period.",
            style = MaterialTheme.typography.bodyMedium,
            color = InkFaint,
            modifier = modifier.padding(12.dp)
        )
        return
    }
    var selected by remember(points) { mutableIntStateOf(-1) }
    val maxValue = remember(points) { (points.maxOf { it.value }).coerceAtLeast(1f) }
    val selectedPoint = points.getOrNull(selected)

    Column(modifier = modifier.fillMaxWidth()) {
        if (selectedPoint != null) {
            Text(
                "${selectedPoint.label} — ${selectedPoint.detail ?: selectedPoint.value}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        val slot = size.width / points.size
                        val idx = (offset.x / slot).toInt().coerceIn(0, points.size - 1)
                        selected = if (selected == idx) -1 else idx
                    }
                }
                .semantics { contentDescription = "Line chart of ${points.size} values" }
        ) {
            val leftPad = 34.dp.toPx()
            val rightPad = 8.dp.toPx()
            val topPad = 8.dp.toPx()
            val bottomPad = 20.dp.toPx()
            val w = size.width - leftPad - rightPad
            val h = size.height - topPad - bottomPad
            val slot = w / points.size
            val path = Path()
            points.forEachIndexed { index, point ->
                val x = leftPad + slot * index + slot / 2
                val y = topPad + h * (1f - point.value / maxValue)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color = color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            points.forEachIndexed { index, point ->
                val x = leftPad + slot * index + slot / 2
                val y = topPad + h * (1f - point.value / maxValue)
                drawCircle(
                    color = if (index == selected) color else color.copy(alpha = 0.75f),
                    radius = if (index == selected) 5.dp.toPx() else 3.dp.toPx(),
                    center = Offset(x, y)
                )
            }
            val labelStep = if (points.size <= 9) 1 else (points.size / 6).coerceAtLeast(1)
            points.forEachIndexed { index, point ->
                if (index % labelStep == 0) {
                    val x = leftPad + slot * index + slot / 2
                    drawContext.canvas.nativeCanvas.drawText(
                        point.label,
                        x,
                        size.height - 4.dp.toPx(),
                        android.graphics.Paint().apply {
                            setColor(InkFaint.toArgb())
                            textSize = 9.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun Sparkline(
    values: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = BrandGreen
) {
    if (values.size < 2) {
        Box(
            modifier = modifier.height(28.dp).fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
        )
        return
    }
    Canvas(modifier = modifier.height(28.dp).fillMaxWidth().padding(vertical = 3.dp)) {
        val max = values.max().coerceAtLeast(1f)
        val slot = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = slot * i
            val y = size.height * (1f - v / max)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
fun DonutChart(
    segments: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
    colors: List<Color> = ChartSeries,
    centerLabel: String? = null
) {
    val total = segments.fold(0f) { acc, (_, value) -> acc + value }.coerceAtLeast(1f)
    Row(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(110.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(110.dp)) {
                var start = -90f
                segments.forEachIndexed { index, (_, value) ->
                    val sweep = value / total * 360f
                    drawArc(
                        color = colors[index % colors.size],
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(6.dp.toPx(), 6.dp.toPx()),
                        size = Size(size.width - 12.dp.toPx(), size.height - 12.dp.toPx()),
                        style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Butt)
                    )
                    start += sweep
                }
            }
            Text(
                centerLabel ?: "",
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center
            )
        }
        Column(Modifier.padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            segments.forEachIndexed { index, (label, value) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(10.dp).background(colors[index % colors.size], RoundedCornerShape(3.dp))
                    )
                    Text(
                        "  $label",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Radial health gauge: one premium visualization for the business-health
 * indicator. The arc is deterministic from the score and uses the semantic
 * state colors (emerald healthy / amber watch / red risk).
 */
@Composable
fun HealthGauge(
    score: Int,
    modifier: Modifier = Modifier,
    gaugeSize: Dp = 132.dp
) {
    val track = SurfaceAlt
    val progressColor = when {
        score >= 70 -> BrandGreen
        score >= 50 -> BrandGold
        else -> MaterialTheme.colorScheme.error
    }
    Box(modifier = modifier.size(gaugeSize), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            val inset = stroke / 2 + 2.dp.toPx()
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = track,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = progressColor,
                startAngle = 135f,
                sweepAngle = 270f * score.coerceIn(0, 100) / 100f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$score",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "of 100",
                style = MaterialTheme.typography.labelSmall,
                color = InkFaint
            )
        }
    }
}

private fun formatAxis(value: Float): String {
    return when {
        value >= 1_000_000f -> String.format(Locale.US, "%.1fM", value / 1_000_000f)
        value >= 1_000f -> String.format(Locale.US, "%.0fK", value / 1_000f)
        else -> String.format(Locale.US, "%.0f", value)
    }
}
