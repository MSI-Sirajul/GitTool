package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.ui.navigation.NavGraph
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ThemeViewModel
import kotlinx.coroutines.flow.MutableSharedFlow

class MainActivity : ComponentActivity() {

    companion object {
        // High-performing decoupled channel to capture redirect codes
        val oauthCodeFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Handle incoming OAuth redirection intents if started cold
        handleGitToolCallback(intent)
        
        // 2. Fully leverage MD3 status and navigation bar bleeding
        enableEdgeToEdge()
        
        val container = (applicationContext as GitToolApplication).container
        
        setContent {
            val themeViewModel: ThemeViewModel = viewModel(
                factory = ThemeViewModel.Factory(container.themePreferences)
            )
            val themeMode by themeViewModel.themeMode.collectAsState()

            MyApplicationTheme(themeMode = themeMode) {
                val navController = rememberNavController()
                
                // Track redirect code changes to direct authentication forms
                LaunchedEffect(Unit) {
                    oauthCodeFlow.collect { code ->
                        // Bring app into foreground logic will safely let components verify OAuth
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    NavGraph(
                        navController = navController,
                        themeViewModel = themeViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleGitToolCallback(intent)
    }

    private fun handleGitToolCallback(intent: Intent?) {
        val data: Uri? = intent?.data
        if (data != null && data.toString().startsWith("gittool://callback")) {
            val code = data.getQueryParameter("code")
            if (!code.isNullOrEmpty()) {
                oauthCodeFlow.tryEmit(code)
            }
        }
    }
}
