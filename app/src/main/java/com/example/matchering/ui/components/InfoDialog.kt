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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.matchering.ui.theme.SurfaceDark
import com.example.matchering.ui.theme.SurfaceElevated
import com.example.matchering.ui.theme.TextPrimary
import com.example.matchering.ui.theme.TextSecondary
import com.example.matchering.ui.theme.TextTertiary

@Composable
fun InfoDialog(
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
                .testTag("info_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MatcheringGold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ABOUT MATCHERENG 2.0",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary,
                            letterSpacing = 1.sp
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                InfoSection(
                    title = "Matching + Mastering = ❤️",
                    description = "Matchering 2.0 is an open-source audio matching and mastering tool. It follows a simple yet powerful idea: you supply TWO audio files (TARGET and REFERENCE), and Matchering gives your Target track the same RMS loudness, tonal balance (FR), peak amplitude, and stereo width as your Reference track."
                )

                Spacer(modifier = Modifier.height(12.dp))

                InfoSection(
                    title = "How it Works",
                    description = "1. Mid/Side decomposition for independent center and stereo image control.\n2. Loudest pieces extraction to capture the chorus / drop dynamic balance.\n3. Log-frequency smoothed matching FIR filter.\n4. Multi-step RMS correction to converge perceptually.\n5. Hyrax lookahead brickwall limiter for transparent peak control without digital clipping."
                )

                Spacer(modifier = Modifier.height(12.dp))

                InfoSection(
                    title = "Original Author & Credits",
                    description = "Created and maintained by Sergree. Featured in UVR5, Songmastr, MVSEP, and reviewed by Benn Jordan."
                )

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
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun InfoSection(title: String, description: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = MatcheringGold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = TextSecondary
        )
    }
}
