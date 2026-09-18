package com.example.matchering

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.matchering.ui.MatcheringScreen
import com.example.matchering.ui.theme.MatcheringTheme
import com.example.matchering.viewmodel.MatcheringViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MatcheringTheme {
                val viewModel: MatcheringViewModel = viewModel()
                MatcheringScreen(viewModel = viewModel)
            }
        }
    }
}
