package com.example.ui.main

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.GitHubRepo
import com.example.ui.components.RepoItem
import com.example.ui.components.RepoSkeletonList
import com.example.ui.components.TopBarWithMenu
import com.example.ui.theme.ThemeViewModel
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.ui.upload.UploadSheet
import com.example.ui.upload.UploadViewModel
import com.example.util.NetworkUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    repoViewModel: RepoListViewModel,
    themeViewModel: ThemeViewModel,
    uploadViewModel: UploadViewModel,
    onLogoutFinished: () -> Unit,
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onRepoClick: (owner: String, repo: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by repoViewModel.uiState.collectAsState()
    val currentFilter by repoViewModel.currentFilter.collectAsState()
    val filteredRepos by repoViewModel.filteredRepos.collectAsState()
    val showPrivateWarning by repoViewModel.showPrivateWarning.collectAsState()
    val bookmarksList by repoViewModel.bookmarksFlow.collectAsState()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showUploadSheet by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var repoToDelete by remember { mutableStateOf<GitHubRepo?>(null) }
    var showAvatarChangeDialog by remember { mutableStateOf(false) }
    
    // Dialog input controls
    var showImportDialog by remember { mutableStateOf(false) }
    var importUrlInput by remember { mutableStateOf("") }
    var showForkDialog by remember { mutableStateOf(false) }
    var forkRepoNameInput by remember { mutableStateOf("") } // e.g., "owner/repo"

    // FAB Speed-dial state
    var isFabExpanded by remember { mutableStateOf(false) }

    // Read general network connectivity state
    val isOnline = remember(uiState.isLoading, uiState.isRefreshing) {
        NetworkUtil.isInternetAvailable(context)
    }

    val listState = rememberLazyListState()

    // 1. Double check and demand POST_NOTIFICATIONS & Storage access permission sequence
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // Graceful completion
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    // Trigger initial cached/online loading on startup
    LaunchedEffect(Unit) {
        repoViewModel.loadUserAndRepos(context, forceRefresh = false)
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
                onThemeSelect = { showThemeDialog = true },
                onProfileClick = { showProfileSheet = true },
                onSearchClick = onSearchClick,
                onNotificationsClick = onNotificationsClick
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
                    selected = currentFilter == RepoFilter.BOOKMARKS,
                    onClick = { repoViewModel.selectFilter(RepoFilter.BOOKMARKS) },
                    icon = { Icon(imageVector = Icons.Outlined.Star, contentDescription = "Bookmarked repositories") },
                    label = { Text("Bookmarks") },
                    modifier = Modifier.testTag("bookmarks_tab")
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
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Expanded Action 1: Upload
                AnimatedVisibility(
                    visible = isFabExpanded,
                    enter = fadeIn() + expandVertically() + slideInVertically(initialOffsetY = { 50 }),
                    exit = fadeOut() + shrinkVertically() + slideOutVertically(targetOffsetY = { 50 })
                ) {
                    FloatingActionButton(
                        onClick = {
                            isFabExpanded = false
                            uploadViewModel.reset()
                            showUploadSheet = true
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(48.dp).testTag("fab_upload_repo")
                    ) {
                        Icon(imageVector = Icons.Default.Backup, contentDescription = "Scan & Upload")
                    }
                }

                // Expanded Action 2: Import
                AnimatedVisibility(
                    visible = isFabExpanded,
                    enter = fadeIn() + expandVertically() + slideInVertically(initialOffsetY = { 50 }),
                    exit = fadeOut() + shrinkVertically() + slideOutVertically(targetOffsetY = { 50 })
                ) {
                    FloatingActionButton(
                        onClick = {
                            isFabExpanded = false
                            importUrlInput = ""
                            showImportDialog = true
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(48.dp).testTag("fab_import_repo")
                    ) {
                        Icon(imageVector = Icons.Default.VerticalAlignBottom, contentDescription = "Import External Git")
                    }
                }

                // Expanded Action 3: Fork
                AnimatedVisibility(
                    visible = isFabExpanded,
                    enter = fadeIn() + expandVertically() + slideInVertically(initialOffsetY = { 50 }),
                    exit = fadeOut() + shrinkVertically() + slideOutVertically(targetOffsetY = { 50 })
                ) {
                    FloatingActionButton(
                        onClick = {
                            isFabExpanded = false
                            forkRepoNameInput = ""
                            showForkDialog = true
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(48.dp).testTag("fab_fork_repo")
                    ) {
                        Icon(imageVector = Icons.Default.CallSplit, contentDescription = "Fork Repository")
                    }
                }

                // Main Speed dial Fab controller toggle
                FloatingActionButton(
                    onClick = { isFabExpanded = !isFabExpanded },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.testTag("app_dashboard_fab")
                ) {
                    Icon(
                        imageVector = if (isFabExpanded) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "Expand Speed dial settings panel"
                    )
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // If offline, display a beautiful high-contrast banner indicating cache mode is active
                if (!isOnline) {
                    Surface(
                        color = Color(0xFFFFB300),
                        contentColor = Color.Black,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WifiOff,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Viewing Local Database Cache (Offline Mode)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { repoViewModel.loadUserAndRepos(context, forceRefresh = true) },
                    modifier = Modifier.fillMaxSize().weight(1f)
                ) {
                    if (filteredRepos.isEmpty() && uiState.isLoading) {
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
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = when (currentFilter) {
                                    RepoFilter.PUBLIC -> "No Public Repositories"
                                    RepoFilter.BOOKMARKS -> "No Bookmarked Items"
                                    RepoFilter.PRIVATE -> "No Private Repositories"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = when (currentFilter) {
                                    RepoFilter.PUBLIC -> "Your public repository list is empty. Touch the '+' button below to upload local projects."
                                    RepoFilter.BOOKMARKS -> "Bookmarks show up here as quick access anchors to view files offline."
                                    RepoFilter.PRIVATE -> "Your private repository list is empty under this account."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Button(
                                onClick = { repoViewModel.loadUserAndRepos(context, forceRefresh = true) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Refresh List")
                            }
                        }
                    } else {
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
                                val isBookmarked = bookmarksList.any { it.id == repo.id }
                                RepoItem(
                                    repo = repo,
                                    isBookmarked = isBookmarked,
                                    onToggleBookmark = { repoViewModel.toggleBookmark(repo) },
                                    onOpenInBrowser = { url ->
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "No web browser found on device.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onDownloadZip = {
                                        val parts = repo.full_name.split("/")
                                        if (parts.size >= 2) {
                                            Toast.makeText(context, "Initiating download sequence...", Toast.LENGTH_SHORT).show()
                                            repoViewModel.downloadRepositoryZip(
                                                context = context,
                                                owner = parts[0],
                                                repoName = parts[1],
                                                branch = repo.default_branch ?: "main"
                                            )
                                        }
                                    },
                                    onCardClick = {
                                        val parts = repo.full_name.split("/")
                                        if (parts.size >= 2) {
                                            onRepoClick(parts[0], parts[1])
                                        }
                                    },
                                    onShareClick = {
                                        try {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_SUBJECT, repo.name)
                                                putExtra(Intent.EXTRA_TEXT, "Check out this GitHub repository: ${repo.html_url}")
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share Repository"))
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Cannot share repository.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onDeleteClick = {
                                        val parts = repo.full_name.split("/")
                                        if (parts.size >= 2) {
                                            repoToDelete = repo
                                            showDeleteDialog = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Error display Snackbar banner
            if (!uiState.errorMessage.isNullOrEmpty()) {
                Snackbar(
                    action = {
                        TextButton(onClick = { repoViewModel.loadUserAndRepos(context, forceRefresh = true) }) {
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

    // Modal dialogue to select core app light/dark styles
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = {
                Text(
                    text = "Theming Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeOptionRow(
                        title = "System Default",
                        selected = themeViewModel.themeMode.collectAsState().value == com.example.ui.theme.ThemeMode.SYSTEM,
                        onClick = { themeViewModel.setThemeMode(com.example.ui.theme.ThemeMode.SYSTEM) }
                    )
                    ThemeOptionRow(
                        title = "Light Theme Style",
                        selected = themeViewModel.themeMode.collectAsState().value == com.example.ui.theme.ThemeMode.LIGHT,
                        onClick = { themeViewModel.setThemeMode(com.example.ui.theme.ThemeMode.LIGHT) }
                    )
                    ThemeOptionRow(
                        title = "Dark Theme Style",
                        selected = themeViewModel.themeMode.collectAsState().value == com.example.ui.theme.ThemeMode.DARK,
                        onClick = { themeViewModel.setThemeMode(com.example.ui.theme.ThemeMode.DARK) }
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

    // Modal sheet for scanning project folder uploads
    if (showUploadSheet) {
        UploadSheet(
            viewModel = uploadViewModel,
            onUploadSuccess = {
                showUploadSheet = false
                repoViewModel.loadUserAndRepos(context, forceRefresh = true)
            },
            onDismissRequest = { showUploadSheet = false }
        )
    }

    // Dialog for Repository forks
    if (showForkDialog) {
        AlertDialog(
            onDismissRequest = { showForkDialog = false },
            title = { Text("Fork Repository", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Forks copy upstream repository structures under your current GitTool authorization.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = forkRepoNameInput,
                        onValueChange = { forkRepoNameInput = it },
                        placeholder = { Text("owner/repo (e.g. google/gson)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("repo_fork_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val input = forkRepoNameInput.trim()
                        if (input.contains("/") && input.split("/").size >= 2) {
                            showForkDialog = false
                            val parts = input.split("/")
                            repoViewModel.forkRepo(parts[0], parts[1], context) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "Enter correct format: owner/repo_name", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("repo_fork_confirm_btn")
                ) {
                    Text("Fork")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForkDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Dialog for Repository Import actions
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Remote Git Repo", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Supports importing projects from HTTP Git clone locations. GitHub links will be automatically forked.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = importUrlInput,
                        onValueChange = { importUrlInput = it },
                        placeholder = { Text("Git Clone Link (https://github.com/...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("repo_import_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val url = importUrlInput.trim()
                        if (url.startsWith("http://") || url.startsWith("https://")) {
                            showImportDialog = false
                            repoViewModel.importExternalRepo(url, context) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "Please enter valid clone http link.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("repo_import_confirm_btn")
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Modal sheet / Dialog to edit User profile metrics
    if (showProfileSheet) {
        var profileName by remember { mutableStateOf(uiState.user?.name ?: "") }
        var profileBio by remember { mutableStateOf(uiState.user?.bio ?: "") }
        var profileBlog by remember { mutableStateOf(uiState.user?.blog ?: "") }
        var profileLocation by remember { mutableStateOf(uiState.user?.location ?: "") }

        AlertDialog(
            onDismissRequest = { showProfileSheet = false },
            title = { Text("My GitHub Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    // Avatar display with camera overlay
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .padding(bottom = 8.dp)
                    ) {
                        if (uiState.user?.avatar_url != null) {
                            AsyncImage(
                                model = uiState.user?.avatar_url,
                                contentDescription = "User profile picture",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(80.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "No avatar",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = { showAvatarChangeDialog = true },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(28.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Edit avatar",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Profile info fields
                    Text(
                        text = "Customize profile fields on GitHub.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    OutlinedTextField(
                        value = profileName,
                        onValueChange = { profileName = it },
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
                    )
                    OutlinedTextField(
                        value = profileBio,
                        onValueChange = { profileBio = it },
                        label = { Text("Bio description") },
                        modifier = Modifier.fillMaxWidth().testTag("profile_bio_input")
                    )
                    OutlinedTextField(
                        value = profileLocation,
                        onValueChange = { profileLocation = it },
                        label = { Text("Location") },
                        modifier = Modifier.fillMaxWidth().testTag("profile_location_input")
                    )
                    OutlinedTextField(
                        value = profileBlog,
                        onValueChange = { profileBlog = it },
                        label = { Text("Personal Link / Blog") },
                        trailingIcon = {
                            if (profileBlog.isNotBlank()) {
                                IconButton(onClick = {
                                    try {
                                        var cleanUrl = profileBlog.trim()
                                        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                                            cleanUrl = "https://$cleanUrl"
                                        }
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Invalid website link", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = "Open personal website in browser"
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("profile_blog_input")
                    )

                    // Stats summary panel
                    HorizontalDivider()
                    Row(
                        horizontalArrangement = Arrangement.SpaceAround,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${uiState.user?.followers ?: 0}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text("Followers", style = MaterialTheme.typography.bodySmall)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${uiState.user?.following ?: 0}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text("Following", style = MaterialTheme.typography.bodySmall)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${uiState.user?.public_repos ?: 0}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text("Repos", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showProfileSheet = false
                        repoViewModel.updateProfile(
                            name = profileName,
                            bio = profileBio,
                            blog = profileBlog,
                            location = profileLocation
                        ) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("profile_save_btn")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileSheet = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Modal warning Dialog confirmation for reading private items
    if (showPrivateWarning) {
        AlertDialog(
            onDismissRequest = { repoViewModel.onPrivateWarningResult(false) },
            title = {
                Text(
                    text = "Private Repositories Check",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "You are about to view your private repositories. These contain sensitive source code files. Continue?",
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

    if (showDeleteDialog && repoToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false; repoToDelete = null },
            title = { Text("Delete Repository") },
            text = { Text("Are you sure you want to delete ${repoToDelete!!.full_name}? This action cannot be undone on GitHub.") },
            confirmButton = {
                Button(
                    onClick = {
                        val repo = repoToDelete!!
                        showDeleteDialog = false
                        repoToDelete = null
                        val parts = repo.full_name.split("/")
                        if (parts.size >= 2) {
                            repoViewModel.deleteRepository(context, parts[0], parts[1]) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false; repoToDelete = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showAvatarChangeDialog) {
        AlertDialog(
            onDismissRequest = { showAvatarChangeDialog = false },
            title = { Text("Change Profile Picture") },
            text = { Text("Direct avatar photo upload is not supported by standard GitHub REST API. Would you like to open GitHub's official profile configuration pages in your standard browser?") },
            confirmButton = {
                Button(
                    onClick = {
                        showAvatarChangeDialog = false
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/settings/profile"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "No browser detected.", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Open Profile Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAvatarChangeDialog = false }) {
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
