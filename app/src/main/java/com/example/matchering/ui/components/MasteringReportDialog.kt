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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.matchering.ui.theme.MasteredGold
import com.example.matchering.ui.theme.MatcheringGold
import com.example.matchering.ui.theme.MeterGreen
import com.example.matchering.ui.theme.ReferenceCyan
import com.example.matchering.ui.theme.SurfaceDark
import com.example.matchering.ui.theme.SurfaceElevated
import com.example.matchering.ui.theme.TargetOrange
import com.example.matchering.ui.theme.TextPrimary
import com.example.matchering.ui.theme.TextSecondary
import com.example.matchering.ui.theme.TextTertiary
import com.example.matchering.viewmodel.MasteringReport
import java.util.Locale

@Composable
fun MasteringReportDialog(
    report: MasteringReport,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                .padding(20.dp)
                .testTag("mastering_report_dialog")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = null,
                            tint = MatcheringGold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MASTERING REPORT",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary,
                            letterSpacing = 1.sp
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Table of comparisons
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceElevated)
                        .padding(12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("METRIC", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextTertiary, modifier = Modifier.weight(1.2f))
                            Text("TARGET", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TargetOrange, modifier = Modifier.weight(1f))
                            Text("REF", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = ReferenceCyan, modifier = Modifier.weight(1f))
                            Text("MASTER", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MasteredGold, modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        MetricRow(
                            name = "RMS Loudness",
                            target = String.format(Locale.US, "%.1f dB", report.targetRmsDbfs),
                            ref = String.format(Locale.US, "%.1f dB", report.referenceRmsDbfs),
                            master = String.format(Locale.US, "%.1f dB", report.masteredRmsDbfs)
                        )

                        MetricRow(
                            name = "True Peak",
                            target = String.format(Locale.US, "%.1f dB", report.targetPeakDbfs),
                            ref = String.format(Locale.US, "%.1f dB", report.referencePeakDbfs),
                            master = String.format(Locale.US, "%.1f dB", report.masteredPeakDbfs)
                        )

                        MetricRow(
                            name = "Stereo Corr",
                            target = String.format(Locale.US, "%.2f", report.targetCorrelation),
                            ref = "-",
                            master = String.format(Locale.US, "%.2f", report.masteredCorrelation)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Summary cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryBadge(
                        title = "LOUDNESS GAIN",
                        value = String.format(Locale.US, "+%.1f dB", report.loudnessGainDb),
                        color = MeterGreen,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryBadge(
                        title = "PROCESSING TIME",
                        value = "${report.processingTimeMs} ms",
                        color = MatcheringGold,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MatcheringGold,
                        contentColor = Color(0xFF16161B)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("OK, Back to Player", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MetricRow(name: String, target: String, ref: String, master: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, fontSize = 11.sp, color = TextSecondary, modifier = Modifier.weight(1.2f))
        Text(target, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextPrimary, modifier = Modifier.weight(1f))
        Text(ref, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextPrimary, modifier = Modifier.weight(1f))
        Text(master, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MasteredGold, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SummaryBadge(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceElevated)
            .padding(10.dp)
    ) {
        Column {
            Text(title, fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextTertiary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
