package com.example.matchering.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.example.matchering.audio.PlaybackSource
import com.example.matchering.audio.PlayerState
import com.example.matchering.model.AudioData
import com.example.matchering.ui.theme.BorderSubtle
import com.example.matchering.ui.theme.MasteredGold
import com.example.matchering.ui.theme.MatcheringGold
import com.example.matchering.ui.theme.MeterGreen
import com.example.matchering.ui.theme.ReferenceCyan
import com.example.matchering.ui.theme.SurfaceDark
import com.example.matchering.ui.theme.SurfaceElevated
import com.example.matchering.ui.theme.SurfaceVariantDark
import com.example.matchering.ui.theme.TargetOrange
import com.example.matchering.ui.theme.TextPrimary
import com.example.matchering.ui.theme.TextSecondary
import com.example.matchering.ui.theme.TextTertiary
import java.util.Locale

@Composable
fun PlayerControls(
    playerState: PlayerState,
    hasMastered: Boolean,
    target: AudioData?,
    mastered: AudioData?,
    reference: AudioData?,
    onSwitchSource: (PlaybackSource) -> Unit,
    onTogglePlay: () -> Unit,
    onSeek: (Float) -> Unit,
    onToggleLoop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("player_controls")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: A/B/C Instant Monitor Switcher
            Text(
                text = "A/B/C COMPARISON MONITOR",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Three-way source selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceElevated)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SourceTabButton(
                    label = "A: TARGET",
                    subLabel = "Unmastered",
                    isSelected = playerState.activeSource == PlaybackSource.TARGET,
                    activeColor = TargetOrange,
                    onClick = { onSwitchSource(PlaybackSource.TARGET) },
                    modifier = Modifier.weight(1f),
                    testTag = "source_target_button"
                )

                SourceTabButton(
                    label = "B: MASTERED",
                    subLabel = if (hasMastered) "Matchering" else "Not Ready",
                    isSelected = playerState.activeSource == PlaybackSource.MASTERED,
                    activeColor = MasteredGold,
                    enabled = hasMastered,
                    onClick = { onSwitchSource(PlaybackSource.MASTERED) },
                    modifier = Modifier.weight(1f),
                    testTag = "source_mastered_button"
                )

                SourceTabButton(
                    label = "C: REFERENCE",
                    subLabel = "Commercial",
                    isSelected = playerState.activeSource == PlaybackSource.REFERENCE,
                    activeColor = ReferenceCyan,
                    onClick = { onSwitchSource(PlaybackSource.REFERENCE) },
                    modifier = Modifier.weight(1f),
                    testTag = "source_reference_button"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scrub bar
            val progressFraction = if (playerState.durationSec > 0f) {
                (playerState.currentPositionSec / playerState.durationSec).coerceIn(0f, 1f)
            } else 0f

            Slider(
                value = progressFraction,
                onValueChange = { onSeek(it) },
                colors = SliderDefaults.colors(
                    thumbColor = when (playerState.activeSource) {
                        PlaybackSource.TARGET -> TargetOrange
                        PlaybackSource.MASTERED -> MasteredGold
                        PlaybackSource.REFERENCE -> ReferenceCyan
                    },
                    activeTrackColor = when (playerState.activeSource) {
                        PlaybackSource.TARGET -> TargetOrange
                        PlaybackSource.MASTERED -> MasteredGold
                        PlaybackSource.REFERENCE -> ReferenceCyan
                    },
                    inactiveTrackColor = SurfaceElevated
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("player_scrub_slider")
            )

            // Timecode & active stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${formatTime(playerState.currentPositionSec)} / ${formatTime(playerState.durationSec)}",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                val activeAudio = when (playerState.activeSource) {
                    PlaybackSource.TARGET -> target
                    PlaybackSource.MASTERED -> mastered ?: target
                    PlaybackSource.REFERENCE -> reference
                }

                if (activeAudio != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Loudness: ",
                            fontSize = 11.sp,
                            color = TextTertiary
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f dBFS", activeAudio.rmsDbfs),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = when (playerState.activeSource) {
                                PlaybackSource.TARGET -> TargetOrange
                                PlaybackSource.MASTERED -> MasteredGold
                                PlaybackSource.REFERENCE -> ReferenceCyan
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Transport buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Loop toggle
                IconButton(
                    onClick = onToggleLoop,
                    modifier = Modifier.testTag("player_loop_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Toggle Loop",
                        tint = if (playerState.isLooping) MatcheringGold else TextTertiary
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Rewind
                IconButton(
                    onClick = { onSeek(0f) },
                    modifier = Modifier.testTag("player_restart_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Restart",
                        tint = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Play / Pause main button
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(
                            when (playerState.activeSource) {
                                PlaybackSource.TARGET -> TargetOrange
                                PlaybackSource.MASTERED -> MasteredGold
                                PlaybackSource.REFERENCE -> ReferenceCyan
                            }
                        )
                        .clickable { onTogglePlay() }
                        .testTag("player_play_pause_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                        tint = Color(0xFF16161B),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceTabButton(
    label: String,
    subLabel: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) activeColor.copy(alpha = 0.22f) else Color.Transparent
            )
            .border(
                1.dp,
                if (isSelected) activeColor else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                color = if (isSelected) activeColor else if (enabled) TextSecondary else TextTertiary
            )
            Text(
                text = subLabel,
                fontSize = 10.sp,
                color = if (isSelected) activeColor.copy(alpha = 0.8f) else TextTertiary
            )
        }
    }
}

private fun formatTime(seconds: Float): String {
    val total = seconds.toInt()
    val m = total / 60
    val s = total % 60
    return String.format(Locale.US, "%02d:%02d", m, s)
}
