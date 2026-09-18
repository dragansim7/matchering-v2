package com.example.matchering.ui.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.matchering.ui.theme.BorderSubtle
import com.example.matchering.ui.theme.MatcheringGold
import com.example.matchering.ui.theme.MeterGreen
import com.example.matchering.ui.theme.SurfaceDark
import com.example.matchering.ui.theme.SurfaceElevated
import com.example.matchering.ui.theme.TextPrimary
import com.example.matchering.ui.theme.TextSecondary
import com.example.matchering.ui.theme.TextTertiary

@Composable
fun ProcessingDialog(
    stageText: String,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = {}) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceDark)
                .border(1.5.dp, MatcheringGold.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .padding(24.dp)
                .testTag("processing_dialog")
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = "MATCHERENG 2.0 PROCESSING",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MatcheringGold,
                    letterSpacing = 1.2.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(64.dp),
                    color = MatcheringGold,
                    trackColor = SurfaceElevated,
                    strokeWidth = 6.dp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stageText,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Progress steps checklist
                StepItem(title = "Audio Matrixing (Mid/Side)", isCompleted = progress > 0.25f, isCurrent = progress in 0.05f..0.25f)
                StepItem(title = "Loudest Pieces & Level Matching", isCompleted = progress > 0.40f, isCurrent = progress in 0.25f..0.40f)
                StepItem(title = "Matching FIR Spectral Filtering", isCompleted = progress > 0.65f, isCurrent = progress in 0.40f..0.65f)
                StepItem(title = "Multi-step RMS Convergence", isCompleted = progress > 0.80f, isCurrent = progress in 0.65f..0.80f)
                StepItem(title = "Hyrax Brickwall Limiter", isCompleted = progress > 0.95f, isCurrent = progress in 0.80f..0.95f)
            }
        }
    }
}

@Composable
private fun StepItem(title: String, isCompleted: Boolean, isCurrent: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isCompleted) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MeterGreen,
                modifier = Modifier.size(16.dp)
            )
        } else if (isCurrent) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(MatcheringGold),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(SurfaceElevated)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = if (isCompleted) TextPrimary else if (isCurrent) MatcheringGold else TextTertiary
        )
    }
}
