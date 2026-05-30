package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.remote.GitHubUser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarWithMenu(
    user: GitHubUser?,
    onLogout: () -> Unit,
    onThemeSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    CenterAlignedTopAppBar(
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "GitTool",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (user != null) {
                    val displayName = if (!user.name.isNullOrBlank()) user.name else user.login
                    Text(
                        text = "@${user.login} ($displayName)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        navigationIcon = {
            if (user?.avatar_url != null) {
                AsyncImage(
                    model = user.avatar_url,
                    contentDescription = "User GitHub circular profile picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                )
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
        },
        actions = {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "GitTool overflow configuration keys"
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Theme Settings") },
                    onClick = {
                        showMenu = false
                        onThemeSelect()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Theme palette configuration trigger"
                        )
                    }
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Log Out") },
                    onClick = {
                        showMenu = false
                        onLogout()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout current application session"
                        )
                    }
                )
            }
        },
        modifier = modifier
    )
}
