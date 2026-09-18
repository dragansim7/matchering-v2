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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.matchering.model.MatcheringConfig
import com.example.matchering.ui.theme.BorderSubtle
import com.example.matchering.ui.theme.MatcheringGold
import com.example.matchering.ui.theme.SurfaceDark
import com.example.matchering.ui.theme.SurfaceElevated
import com.example.matchering.ui.theme.TextPrimary
import com.example.matchering.ui.theme.TextSecondary
import com.example.matchering.ui.theme.TextTertiary
import java.util.Locale

@Composable
fun MasteringControls(
    config: MatcheringConfig,
    onConfigChange: (MatcheringConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("mastering_controls")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MatcheringGold
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    Text(
                        text = "MASTERING ENGINE PARAMETERS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary,
                        letterSpacing = 1.sp
                    )
                }

                TextButton(
                    onClick = { onConfigChange(MatcheringConfig()) },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.testTag("reset_config_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Reset Defaults",
                        tint = TextTertiary,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text("Defaults", color = TextTertiary, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 1. EQ Matching Intensity
            ControlSlider(
                label = "Matching EQ Amount (Wet / Dry)",
                valueText = String.format(Locale.US, "%d%%", (config.matchingStrength * 100).toInt()),
                value = config.matchingStrength,
                onValueChange = { onConfigChange(config.copy(matchingStrength = it)) },
                testTag = "slider_matching_strength"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Stereo Width Matching
            ControlSlider(
                label = "Stereo Image Matching (Side Channel)",
                valueText = String.format(Locale.US, "%d%%", (config.stereoMatchingStrength * 100).toInt()),
                value = config.stereoMatchingStrength,
                onValueChange = { onConfigChange(config.copy(stereoMatchingStrength = it)) },
                testTag = "slider_stereo_strength"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Limiter Ceiling
            ControlSlider(
                label = "Limiter Ceiling / True Peak Threshold",
                valueText = String.format(Locale.US, "%.1f dB", config.limiterThresholdDb),
                value = (config.limiterThresholdDb + 2.0f) / 1.9f, // map -2.0dB..-0.1dB to 0..1
                onValueChange = { norm ->
                    val db = -2.0f + norm * 1.9f
                    onConfigChange(config.copy(limiterThresholdDb = db))
                },
                testTag = "slider_limiter_ceiling"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Limiter Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceElevated)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hyrax Brickwall Peak Limiter",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (config.applyLimiter) "Lookahead gain reduction enabled (no clipping)" else "Bypassed (Normalize only)",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }

                Switch(
                    checked = config.applyLimiter,
                    onCheckedChange = { onConfigChange(config.copy(applyLimiter = it, normalizeWithoutLimiter = !it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MatcheringGold,
                        checkedTrackColor = MatcheringGold.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.testTag("switch_limiter")
                )
            }
        }
    }
}

@Composable
private fun ControlSlider(
    label: String,
    valueText: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    testTag: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 11.sp, color = TextSecondary)
            Text(
                text = valueText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MatcheringGold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(
                thumbColor = MatcheringGold,
                activeTrackColor = MatcheringGold,
                inactiveTrackColor = SurfaceElevated
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )
    }
}
