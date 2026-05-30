package com.example.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.GitToolApplication
import com.example.ui.login.LoginScreen
import com.example.ui.login.LoginViewModel
import com.example.ui.main.MainScreen
import com.example.ui.main.RepoListViewModel
import com.example.ui.theme.ThemeViewModel
import com.example.ui.upload.UploadViewModel
import kotlinx.coroutines.delay

@Composable
fun NavGraph(
    navController: NavHostController,
    themeViewModel: ThemeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val container = (context.applicationContext as GitToolApplication).container

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = modifier
    ) {
        // 1. SPLASH SCREEN
        composable(Screen.Splash.route) {
            var startAnim by remember { mutableStateOf(false) }
            
            LaunchedEffect(key1 = true) {
                startAnim = true
                delay(1200) // Aesthetic delay for branding
                
                // Inspect stored OAuth or access token session availability
                val authResult = container.authRepository.checkAutoLogin()
                authResult.onSuccess {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }.onFailure {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.primary
                            )
                        )
                    )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AnimatedVisibility(
                        visible = startAnim,
                        enter = scaleIn() + fadeIn(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.size(96.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = "Code bracket icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "GitTool",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Pristine project pushes, automated",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // 2. LOGIN SCREEN
        composable(Screen.Login.route) {
            val loginViewModel: LoginViewModel = viewModel(
                factory = LoginViewModel.Factory(container.authRepository)
            )
            LoginScreen(
                viewModel = loginViewModel,
                onNavigateToMain = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // 3. MAIN DASHBOARD SCREEN (Includes the upload logic)
        composable(Screen.Main.route) {
            val repoViewModel: RepoListViewModel = viewModel(
                factory = RepoListViewModel.Factory(container.repoRepository, container.authRepository)
            )
            val uploadViewModel: UploadViewModel = viewModel(
                factory = UploadViewModel.Factory(container.uploadRepository)
            )

            MainScreen(
                repoViewModel = repoViewModel,
                themeViewModel = themeViewModel,
                uploadViewModel = uploadViewModel,
                onLogoutFinished = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
