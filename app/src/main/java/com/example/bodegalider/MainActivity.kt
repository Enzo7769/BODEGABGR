package com.example.bodegalider

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.bodegalider.ui.screens.LoginScreen
import com.example.bodegalider.ui.screens.MainScreen
import com.example.bodegalider.ui.theme.BodegaLiderTheme
import com.example.bodegalider.ui.viewmodel.InventoryViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: InventoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            val currentUser by viewModel.currentUser.collectAsState()

            BodegaLiderTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    if (currentUser == null) {
                        LoginScreen(viewModel = viewModel)
                    } else {
                        MainScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
