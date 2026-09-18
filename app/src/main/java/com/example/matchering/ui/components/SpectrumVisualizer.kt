package com.example.matchering.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.matchering.model.AudioData
import com.example.matchering.ui.theme.BorderSubtle
import com.example.matchering.ui.theme.MasteredGold
import com.example.matchering.ui.theme.ReferenceCyan
import com.example.matchering.ui.theme.SurfaceDark
import com.example.matchering.ui.theme.SurfaceElevated
import com.example.matchering.ui.theme.TargetOrange
import com.example.matchering.ui.theme.TextPrimary
import com.example.matchering.ui.theme.TextSecondary
import com.example.matchering.ui.theme.TextTertiary

@Composable
fun SpectrumVisualizer(
    target: AudioData?,
    mastered: AudioData?,
    reference: AudioData?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("spectrum_visualizer")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MATCHING SPECTRUM (FIR EQ)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary,
                    letterSpacing = 1.sp
                )

                // Legend
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LegendItem("TARGET", TargetOrange)
                    Spacer(modifier = Modifier.width(12.dp))
                    if (mastered != null) {
                        LegendItem("MASTERED", MasteredGold)
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    LegendItem("REFERENCE", ReferenceCyan)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Spectrum Graph Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceElevated)
                    .border(0.5.dp, BorderSubtle, RoundedCornerShape(8.dp))
            ) {
                SpectrumCanvas(
                    targetBands = target?.spectrumBands,
                    masteredBands = mastered?.spectrumBands,
                    referenceBands = reference?.spectrumBands,
                    modifier = Modifier.matchParentSize()
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Frequency band labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BandLabel("20Hz")
                BandLabel("SUB")
                BandLabel("BASS")
                BandLabel("MIDS")
                BandLabel("PRESENCE")
                BandLabel("AIR")
                BandLabel("20kHz")
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = color
        )
    }
}

@Composable
private fun BandLabel(text: String) {
    Text(
        text = text,
        fontSize = 9.sp,
        fontFamily = FontFamily.Monospace,
        color = TextTertiary
    )
}

@Composable
fun SpectrumCanvas(
    targetBands: FloatArray?,
    masteredBands: FloatArray?,
    referenceBands: FloatArray?,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Subtle grid lines (-12dB, -24dB, -36dB)
        val gridColor = Color(0xFF33333F)
        for (f in listOf(0.25f, 0.5f, 0.75f)) {
            val y = height * f
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
        }

        // Draw curves
        targetBands?.let { bands ->
            drawBandCurve(bands, TargetOrange, width, height, strokeWidth = 2f, alpha = 0.8f)
        }
        referenceBands?.let { bands ->
            drawBandCurve(bands, ReferenceCyan, width, height, strokeWidth = 2f, alpha = 0.85f)
        }
        masteredBands?.let { bands ->
            drawBandCurve(bands, MasteredGold, width, height, strokeWidth = 3f, alpha = 1.0f)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBandCurve(
    bands: FloatArray,
    color: Color,
    width: Float,
    height: Float,
    strokeWidth: Float,
    alpha: Float
) {
    val count = bands.size
    if (count < 2) return

    val path = Path()
    var maxVal = 0.001f
    for (v in bands) {
        if (v > maxVal) maxVal = v
    }

    val stepX = width / (count - 1)
    for (i in 0 until count) {
        val normalized = (bands[i] / maxVal).coerceIn(0.05f, 1.0f)
        val y = height - (normalized * height * 0.88f) - (height * 0.06f)
        val x = i * stepX
        if (i == 0) {
            path.moveTo(x, y)
        } else {
            val prevX = (i - 1) * stepX
            val prevNorm = (bands[i - 1] / maxVal).coerceIn(0.05f, 1.0f)
            val prevY = height - (prevNorm * height * 0.88f) - (height * 0.06f)
            val cX = (prevX + x) / 2f
            path.cubicTo(cX, prevY, cX, y, x, y)
        }
    }

    drawPath(
        path = path,
        color = color.copy(alpha = alpha),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
}
