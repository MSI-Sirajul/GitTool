package com.example.ui.main

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.RepoItem
import com.example.ui.components.RepoSkeletonItem
import com.example.ui.components.RepoSkeletonList
import com.example.ui.components.TopBarWithMenu
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.ThemeViewModel
import com.example.ui.upload.UploadSheet
import com.example.ui.upload.UploadViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    repoViewModel: RepoListViewModel,
    themeViewModel: ThemeViewModel,
    uploadViewModel: UploadViewModel,
    onLogoutFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by repoViewModel.uiState.collectAsState()
    val themeMode by themeViewModel.themeMode.collectAsState()
    val currentFilter by repoViewModel.currentFilter.collectAsState()
    val filteredRepos by repoViewModel.filteredRepos.collectAsState()
    val showPrivateWarning by repoViewModel.showPrivateWarning.collectAsState()
    
    var showThemeDialog by remember { mutableStateOf(false) }
    var showUploadSheet by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Detect when scrolling gets close to the end to load next pages automatically
    val shouldLoadMore = remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItemIndex >= totalItems - 5 && totalItems > 0 && uiState.hasMore && !uiState.isLoading
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            repoViewModel.loadNextPage()
        }
    }

    Scaffold(
        topBar = {
            TopBarWithMenu(
                user = uiState.user,
                onLogout = {
                    repoViewModel.logout()
                    onLogoutFinished()
                    Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                },
                onThemeSelect = { showThemeDialog = true }
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("repo_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = currentFilter == RepoFilter.PUBLIC,
                    onClick = { repoViewModel.selectFilter(RepoFilter.PUBLIC) },
                    icon = { Icon(imageVector = Icons.Outlined.Public, contentDescription = "Public repositories") },
                    label = { Text("Public") },
                    modifier = Modifier.testTag("public_tab")
                )
                NavigationBarItem(
                    selected = currentFilter == RepoFilter.PRIVATE,
                    onClick = { repoViewModel.selectFilter(RepoFilter.PRIVATE) },
                    icon = { Icon(imageVector = Icons.Outlined.Lock, contentDescription = "Private repositories") },
                    label = { Text("Private") },
                    modifier = Modifier.testTag("private_tab")
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    uploadViewModel.reset()
                    showUploadSheet = true 
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.testTag("app_dashboard_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Upload new local project"
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { repoViewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                if (filteredRepos.isEmpty() && uiState.isLoading) {
                    // Shimmer list skeleton loading at launch and refresh
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 640.dp)
                            .align(Alignment.TopCenter)
                            .padding(16.dp)
                    ) {
                        RepoSkeletonList()
                    }
                } else if (filteredRepos.isEmpty()) {
                    // Styled empty state container
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 480.dp)
                            .align(Alignment.Center)
                            .padding(32.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = "Folder open represent empty",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = if (currentFilter == RepoFilter.PUBLIC) "No Public Repositories" else "No Private Repositories",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (currentFilter == RepoFilter.PUBLIC) "Your public repository list is currently empty. Touch the '+' button below to upload local projects." else "Your private repository list is empty under this account.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = { repoViewModel.refresh() },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Refresh List")
                        }
                    }
                } else {
                    // Paged repo items lazylist
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 640.dp)
                            .align(Alignment.TopCenter)
                    ) {
                        items(
                            items = filteredRepos,
                            key = { it.id }
                        ) { repo ->
                            RepoItem(
                                repo = repo,
                                onOpenInBrowser = { url ->
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "No web browser found on device", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }

                        // Bottom skeleton pagination more items
                        if (uiState.isLoading && filteredRepos.isNotEmpty()) {
                            item {
                                val shimmerColors = listOf(
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f),
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                                )
                                val transition = rememberInfiniteTransition(label = "bottom_shimmer")
                                val translateAnim = transition.animateFloat(
                                    initialValue = 0f,
                                    targetValue = 1000f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(durationMillis = 1200, easing = LinearEasing),
                                        repeatMode = RepeatMode.Restart
                                    ),
                                    label = "bottom_shimmer_anim"
                                )
                                val brush = Brush.linearGradient(
                                    colors = shimmerColors,
                                    start = androidx.compose.ui.geometry.Offset.Zero,
                                    end = androidx.compose.ui.geometry.Offset(x = translateAnim.value, y = translateAnim.value)
                                )
                                RepoSkeletonItem(brush = brush)
                            }
                        }
                    }
                }
            }

            // High priority error presenters
            if (!uiState.errorMessage.isNullOrEmpty()) {
                Snackbar(
                    action = {
                        TextButton(onClick = { repoViewModel.refresh() }) {
                            Text("Retry", color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Text(text = uiState.errorMessage ?: "")
                }
            }
        }
    }

    // Dynamic Theme Selection Custom Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = {
                Text(
                    text = "Theming & Appearance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Customize the interface style to fit your environment preference.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    ThemeOptionRow(
                        title = "System Default",
                        selected = themeMode == ThemeMode.SYSTEM,
                        onClick = { themeViewModel.setThemeMode(ThemeMode.SYSTEM) }
                    )
                    ThemeOptionRow(
                        title = "Light Theme Style",
                        selected = themeMode == ThemeMode.LIGHT,
                        onClick = { themeViewModel.setThemeMode(ThemeMode.LIGHT) }
                    )
                    ThemeOptionRow(
                        title = "Dark Theme Style",
                        selected = themeMode == ThemeMode.DARK,
                        onClick = { themeViewModel.setThemeMode(ThemeMode.DARK) }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Close")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Full Modal Bottom Sheet for Upload form sequences
    if (showUploadSheet) {
        UploadSheet(
            viewModel = uploadViewModel,
            onUploadSuccess = {
                showUploadSheet = false
                repoViewModel.refresh()
            },
            onDismissRequest = { showUploadSheet = false }
        )
    }

    // Warning confirmation dialog for private repositories
    if (showPrivateWarning) {
        AlertDialog(
            onDismissRequest = { repoViewModel.onPrivateWarningResult(false) },
            title = {
                Text(
                    text = "Private Repositories",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "You are about to view your private repositories. These contain sensitive code. Continue?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { repoViewModel.onPrivateWarningResult(true) },
                    modifier = Modifier.testTag("warning_confirm_button")
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { repoViewModel.onPrivateWarningResult(false) },
                    modifier = Modifier.testTag("warning_cancel_button")
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun ThemeOptionRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
