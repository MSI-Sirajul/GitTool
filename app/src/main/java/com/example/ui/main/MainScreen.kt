package com.example.ui.main

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.RepoItem
import com.example.ui.components.ShimmerEffectList
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
                if (uiState.repos.isEmpty() && uiState.isLoading) {
                    // Shimmer list skeleton loading at launch
                    Box(modifier = Modifier.padding(16.dp)) {
                        ShimmerEffectList()
                    }
                } else if (uiState.repos.isEmpty()) {
                    // Styled empty state container
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
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
                            text = "No Repositories Found",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Your repository list appears to be empty. Touch the '+' button below to push your first local project directory securely onto GitHub.",
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
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = uiState.repos,
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

                        // Bottom spinner loading more items
                        if (uiState.isLoading && uiState.repos.isNotEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
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
