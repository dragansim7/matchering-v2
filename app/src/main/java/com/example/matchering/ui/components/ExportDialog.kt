package com.example.matchering.ui.components

import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.matchering.audio.WavBitDepth
import com.example.matchering.model.AudioData
import com.example.matchering.ui.theme.BorderSubtle
import com.example.matchering.ui.theme.MatcheringGold
import com.example.matchering.ui.theme.SurfaceDark
import com.example.matchering.ui.theme.SurfaceElevated
import com.example.matchering.ui.theme.TextPrimary
import com.example.matchering.ui.theme.TextSecondary
import com.example.matchering.ui.theme.TextTertiary
import java.io.File

@Composable
fun ExportDialog(
    masteredAudio: AudioData,
    onExportFile: (WavBitDepth) -> File?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedBitDepth by remember { mutableStateOf(WavBitDepth.BIT_16) }
    var exportedFile by remember { mutableStateOf<File?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                .padding(20.dp)
                .testTag("export_dialog")
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
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = MatcheringGold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "EXPORT MASTERED TRACK",
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

                Text(
                    text = "Select Master Audio Format",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Bit depth options
                WavBitDepth.values().forEach { depth ->
                    BitDepthOption(
                        depth = depth,
                        isSelected = selectedBitDepth == depth,
                        onClick = {
                            selectedBitDepth = depth
                            exportedFile = null
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (exportedFile != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceElevated)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Exported to: ${exportedFile?.name} (${(exportedFile?.length() ?: 0) / 1024} KB)",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MatcheringGold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val file = onExportFile(selectedBitDepth)
                            exportedFile = file
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceElevated,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export WAV", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val file = exportedFile ?: onExportFile(selectedBitDepth)
                            exportedFile = file
                            if (file != null) {
                                shareAudioFile(context, file)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MatcheringGold,
                            contentColor = Color(0xFF16161B)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Audio", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun BitDepthOption(
    depth: WavBitDepth,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) SurfaceElevated else Color.Transparent)
            .border(1.dp, if (isSelected) MatcheringGold else BorderSubtle, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isSelected) MatcheringGold else TextTertiary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = depth.label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) TextPrimary else TextSecondary
        )
    }
}

private fun shareAudioFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/wav"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Mastered Track"))
    } catch (_: Exception) {
        // Fallback simple share
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/wav"
            putExtra(Intent.EXTRA_TEXT, "Mastered with Matchering 2.0: ${file.name}")
        }
        context.startActivity(Intent.createChooser(intent, "Share Mastered Track"))
    }
}
