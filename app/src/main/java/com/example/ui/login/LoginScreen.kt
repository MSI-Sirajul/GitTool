package com.example.ui.login

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainActivity
import com.example.ui.components.LoadingAnimation
import com.example.util.Constants

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onNavigateToMain: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    // 0 = OAuth, 1 = Token
    var selectedTab by remember { mutableIntStateOf(0) }
    var obscureToken by remember { mutableStateOf(true) }

    // Subscribe to decoupled Activity redirection stream
    LaunchedEffect(Unit) {
        MainActivity.pendingOAuthCode?.let { code ->
            MainActivity.pendingOAuthCode = null
            viewModel.handleOAuthRedirectCode(code)
        }
        MainActivity.oauthCodeFlow.collect { code ->
            MainActivity.pendingOAuthCode = null
            viewModel.handleOAuthRedirectCode(code)
        }
    }

    LaunchedEffect(uiState.isLoginSuccess) {
        if (uiState.isLoginSuccess) {
            onNavigateToMain()
        }
    }

    if (uiState.isLoading) {
        LoadingAnimation(message = "Verifying authentication credentials...")
    } else {
        Scaffold { innerPadding ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        )
                    )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 480.dp)
                        .align(Alignment.Center)
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Styled application branding
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "App logo key",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "GitTool",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Manage and upload your development projects to GitHub securely in real time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Tab selector
                    TabRow(
                        selectedTabIndex = selectedTab,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("GitHub OAuth", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("oauth_tab")
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Access Token", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("token_tab")
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Auth Panel contents depend on selection
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "Auth selection panels"
                    ) { targetTab ->
                        if (targetTab == 0) {
                            // OAuth Panel
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Button(
                                    onClick = {
                                        viewModel.startOAuthFlow { url ->
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            context.startActivity(intent)
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .testTag("oauth_login_button")
                                ) {
                                    Text(
                                        text = "Login with GitHub",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Redirects to GitHub's authorization portal safely via custom web intents.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            // Access Token Panel
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                OutlinedTextField(
                                    value = uiState.tokenInput,
                                    onValueChange = { viewModel.updateTokenInput(it) },
                                    label = { Text("Personal Access Token") },
                                    placeholder = { Text("ghp_...") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Key,
                                            contentDescription = "Token field entry key"
                                        )
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { obscureToken = !obscureToken }) {
                                            Icon(
                                                imageVector = if (obscureToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = "Obscure input toggle"
                                            )
                                        }
                                    },
                                    visualTransformation = if (obscureToken) PasswordVisualTransformation() else VisualTransformation.None,
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("token_input_field")
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = { viewModel.loginWithToken() },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .testTag("token_login_button")
                                ) {
                                    Text(
                                        text = "Verify & Access",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Shared "Remember me" option
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) {
                        Checkbox(
                            checked = uiState.rememberMe,
                            onCheckedChange = { viewModel.updateRememberMe(it) },
                            modifier = Modifier.testTag("remember_me_checkbox")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Keep me signed in",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Encrypts session key in Android Secure storage.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Error presentation
                    if (!uiState.errorMessage.isNullOrEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Authentication Issue",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = uiState.errorMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
