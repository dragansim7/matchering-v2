package com.example.matchering.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.matchering.model.AudioData
import com.example.matchering.ui.theme.BorderSubtle
import com.example.matchering.ui.theme.SurfaceElevated
import com.example.matchering.ui.theme.TextPrimary
import com.example.matchering.ui.theme.TextSecondary
import com.example.matchering.ui.theme.TextTertiary
import java.util.Locale

@Composable
fun TrackCard(
    titleTag: String,
    subtitle: String,
    audioData: AudioData?,
    accentColor: Color,
    onSelectFile: () -> Unit,
    testTagPrefix: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("${testTagPrefix}_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header row with tag pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(accentColor.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = titleTag,
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = subtitle,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                OutlinedButton(
                    onClick = onSelectFile,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(accentColor.copy(alpha = 0.5f), accentColor))),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("${testTagPrefix}_select_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Select File",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (audioData == null) "Load File" else "Change",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (audioData != null) {
                // Track Info & Waveform
                Text(
                    text = audioData.title,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Waveform canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceElevated)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    WaveformMiniCanvas(
                        points = audioData.waveformPoints,
                        accentColor = accentColor,
                        modifier = Modifier.matchParentSize()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Track Audio Metrics Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricItem(
                        label = "RMS",
                        value = String.format(Locale.US, "%.1f dBFS", audioData.rmsDbfs),
                        accentColor = accentColor
                    )
                    MetricItem(
                        label = "PEAK",
                        value = String.format(Locale.US, "%.1f dBFS", audioData.peakDbfs),
                        accentColor = accentColor
                    )
                    MetricItem(
                        label = "LENGTH",
                        value = formatDuration(audioData.durationSec),
                        accentColor = accentColor
                    )
                    MetricItem(
                        label = "CORR",
                        value = String.format(Locale.US, "%.2f", audioData.stereoCorrelation),
                        accentColor = accentColor
                    )
                }
            } else {
                // Empty state prompt
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceElevated)
                        .clickable { onSelectFile() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tap to load or drag audio file here",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String, accentColor: Color) {
    Column {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            color = TextTertiary
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = TextPrimary
        )
    }
}

@Composable
fun WaveformMiniCanvas(
    points: FloatArray,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val count = points.size
        if (count == 0) return@Canvas

        val barWidth = width / count
        val brush = Brush.verticalGradient(
            colors = listOf(
                accentColor,
                accentColor.copy(alpha = 0.5f)
            ),
            startY = 0f,
            endY = height
        )

        for (i in 0 until count) {
            val amp = points[i].coerceIn(0.05f, 1.0f)
            val barHeight = amp * (height * 0.9f)
            val x = i * barWidth
            drawRect(
                brush = brush,
                topLeft = Offset(x, centerY - barHeight / 2f),
                size = Size(maxOf(1f, barWidth * 0.7f), barHeight)
            )
        }
    }
}

private fun formatDuration(seconds: Float): String {
    val totalSec = seconds.toInt()
    val mins = totalSec / 60
    val secs = totalSec % 60
    return String.format(Locale.US, "%d:%02d", mins, secs)
}
