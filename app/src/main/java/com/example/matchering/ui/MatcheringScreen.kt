package com.example.matchering.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.matchering.R
import com.example.matchering.audio.PlaybackSource
import com.example.matchering.ui.components.ExportDialog
import com.example.matchering.ui.components.InfoDialog
import com.example.matchering.ui.components.MasteringControls
import com.example.matchering.ui.components.MasteringReportDialog
import com.example.matchering.ui.components.PlayerControls
import com.example.matchering.ui.components.ProcessingDialog
import com.example.matchering.ui.components.SpectrumVisualizer
import com.example.matchering.ui.components.TrackCard
import com.example.matchering.ui.theme.BackgroundDark
import com.example.matchering.ui.theme.BorderSubtle
import com.example.matchering.ui.theme.MasteredGold
import com.example.matchering.ui.theme.MatcheringGold
import com.example.matchering.ui.theme.ReferenceCyan
import com.example.matchering.ui.theme.SurfaceDark
import com.example.matchering.ui.theme.TargetOrange
import com.example.matchering.ui.theme.TextPrimary
import com.example.matchering.ui.theme.TextSecondary
import com.example.matchering.ui.theme.TextTertiary
import com.example.matchering.viewmodel.MatcheringViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatcheringScreen(
    viewModel: MatcheringViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // File pickers for Target and Reference
    val targetPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.loadAudioFromUri(context, it, isTarget = true) }
    }

    val referencePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.loadAudioFromUri(context, it, isTarget = false) }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BackgroundDark,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_matchering_logo),
                            contentDescription = "Matchering Logo",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "MATCH",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = MatcheringGold
                                )
                                Text(
                                    text = "ERING",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "2.0",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = MatcheringGold
                                )
                            }
                            Text(
                                text = "Audio Matching & Mastering",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadSampleTracks() },
                        modifier = Modifier.testTag("load_samples_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.LibraryMusic,
                            contentDescription = "Load Sample Tracks",
                            tint = MatcheringGold
                        )
                    }

                    if (uiState.lastReport != null) {
                        IconButton(
                            onClick = { viewModel.setReportDialogVisible(true) },
                            modifier = Modifier.testTag("show_report_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = "Mastering Report",
                                tint = TextPrimary
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.setInfoDialogVisible(true) },
                        modifier = Modifier.testTag("info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About Matchering",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark,
                    titleContentColor = TextPrimary
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dual Track Input Cards
                item {
                    TrackCard(
                        titleTag = "TARGET",
                        subtitle = "Track to master (sound will change)",
                        audioData = uiState.targetAudio,
                        accentColor = TargetOrange,
                        onSelectFile = { targetPickerLauncher.launch("audio/*") },
                        testTagPrefix = "target"
                    )
                }

                item {
                    TrackCard(
                        titleTag = "REFERENCE",
                        subtitle = "Commercial song (target will sound like it)",
                        audioData = uiState.referenceAudio,
                        accentColor = ReferenceCyan,
                        onSelectFile = { referencePickerLauncher.launch("audio/*") },
                        testTagPrefix = "reference"
                    )
                }

                // Process Button Card
                item {
                    val canProcess = uiState.targetAudio != null && uiState.referenceAudio != null && !uiState.isProcessing
                    Button(
                        onClick = { viewModel.startMastering() },
                        enabled = canProcess,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MatcheringGold,
                            contentColor = Color(0xFF141418),
                            disabledContainerColor = SurfaceDark,
                            disabledContentColor = TextTertiary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("master_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.masteredAudio != null) "RE-MASTER WITH MATCHERENG" else "MASTER WITH MATCHERENG 2.0",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // A/B/C Listening Station
                item {
                    PlayerControls(
                        playerState = playerState,
                        hasMastered = uiState.masteredAudio != null,
                        target = uiState.targetAudio,
                        mastered = uiState.masteredAudio,
                        reference = uiState.referenceAudio,
                        onSwitchSource = { viewModel.audioPlayer.switchSource(it) },
                        onTogglePlay = { viewModel.audioPlayer.togglePlayPause() },
                        onSeek = { viewModel.audioPlayer.seekToFraction(it) },
                        onToggleLoop = { viewModel.audioPlayer.toggleLoop() }
                    )
                }

                // Spectrum Visualizer
                item {
                    SpectrumVisualizer(
                        target = uiState.targetAudio,
                        mastered = uiState.masteredAudio,
                        reference = uiState.referenceAudio
                    )
                }

                // Mastering Engine Parameters
                item {
                    MasteringControls(
                        config = uiState.config,
                        onConfigChange = { viewModel.updateConfig(it) }
                    )
                }

                // Export Button (active when mastered)
                if (uiState.masteredAudio != null) {
                    item {
                        Button(
                            onClick = { viewModel.setExportDialogVisible(true) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SurfaceDark,
                                contentColor = MatcheringGold
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.horizontalGradient(listOf(MatcheringGold.copy(alpha = 0.6f), MatcheringGold))
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("export_master_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = MatcheringGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "EXPORT MASTERED WAV / SHARE",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Dialogs
    if (uiState.isProcessing) {
        ProcessingDialog(
            stageText = uiState.processingStage,
            progress = uiState.processingProgress
        )
    }

    if (uiState.showReportDialog && uiState.lastReport != null) {
        MasteringReportDialog(
            report = uiState.lastReport!!,
            onDismiss = { viewModel.setReportDialogVisible(false) }
        )
    }

    if (uiState.showExportDialog && uiState.masteredAudio != null) {
        ExportDialog(
            masteredAudio = uiState.masteredAudio!!,
            onExportFile = { depth -> viewModel.exportMasteredWav(context, depth) },
            onDismiss = { viewModel.setExportDialogVisible(false) }
        )
    }

    if (uiState.showInfoDialog) {
        InfoDialog(
            onDismiss = { viewModel.setInfoDialogVisible(false) }
        )
    }
}
