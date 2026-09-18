package com.matchering.app

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    private var targetUri: Uri? = null
    private var referenceUri: Uri? = null
    private var resultFile: File? = null
    private var mediaPlayer: MediaPlayer? = null

    private lateinit var targetLabel: TextView
    private lateinit var referenceLabel: TextView
    private lateinit var processButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var resultContainer: LinearLayout
    private lateinit var playButton: Button
    private lateinit var shareButton: Button

    private val pickTarget = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            targetUri = uri
            targetLabel.text = getFileName(uri)
            updateProcessButton()
        }
    }

    private val pickReference = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            referenceUri = uri
            referenceLabel.text = getFileName(uri)
            updateProcessButton()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        targetLabel = findViewById(R.id.targetLabel)
        referenceLabel = findViewById(R.id.referenceLabel)
        processButton = findViewById(R.id.processButton)
        progressBar = findViewById(R.id.progressBar)
        statusText = findViewById(R.id.statusText)
        resultContainer = findViewById(R.id.resultContainer)
        playButton = findViewById(R.id.playButton)
        shareButton = findViewById(R.id.shareButton)

        findViewById<Button>(R.id.selectTargetButton).setOnClickListener {
            pickTarget.launch(arrayOf("audio/*"))
        }

        findViewById<Button>(R.id.selectReferenceButton).setOnClickListener {
            pickReference.launch(arrayOf("audio/*"))
        }

        processButton.setOnClickListener {
            val target = targetUri ?: return@setOnClickListener
            val reference = referenceUri ?: return@setOnClickListener
            runMatchering(target, reference)
        }

        playButton.setOnClickListener {
            val file = resultFile ?: return@setOnClickListener
            playResult(file)
        }

        shareButton.setOnClickListener {
            val file = resultFile ?: return@setOnClickListener
            shareResult(file)
        }
    }

    private fun updateProcessButton() {
        processButton.isEnabled = targetUri != null && referenceUri != null
    }

    private fun runMatchering(target: Uri, reference: Uri) {
        processButton.isEnabled = false
        progressBar.visibility = android.view.View.VISIBLE
        statusText.text = "Processing… This may take a while."
        resultContainer.visibility = android.view.View.GONE

        lifecycleScope.launch {
            try {
                val processor = MatcheringProcessor(this@MainActivity)
                val output = withContext(Dispatchers.IO) {
                    processor.process(target, reference)
                }
                resultFile = output
                statusText.text = "Done! Mastered track is ready."
                resultContainer.visibility = android.view.View.VISIBLE
            } catch (e: Exception) {
                statusText.text = "Error: ${e.message}"
                Toast.makeText(this@MainActivity, "Processing failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                progressBar.visibility = android.view.View.GONE
                processButton.isEnabled = true
            }
        }
    }

    private fun playResult(file: File) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            prepare()
            start()
        }
        Toast.makeText(this, "Playing result…", Toast.LENGTH_SHORT).show()
    }

    private fun shareResult(file: File) {
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/wav"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share mastered track"))
    }

    private fun getFileName(uri: Uri): String {
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && it.moveToFirst()) {
                return it.getString(nameIndex)
            }
        }
        return uri.lastPathSegment ?: "Unknown"
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
