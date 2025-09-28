package com.example.vtubercamera.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vtubercamera.R
import com.example.vtubercamera.data.vrm.AvatarInfo
import com.example.vtubercamera.ui.viewmodels.AvatarLibraryViewModel
import com.example.vtubercamera.ui.components.AvatarListMode
import com.example.vtubercamera.ui.components.AvatarListView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

/**
 * Screen for managing the avatar library
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarLibraryScreen(
    onNavigateBack: () -> Unit,
    viewModel: AvatarLibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // File picker for VRM import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importAvatar(uri)
        } else {
            viewModel.hideImportDialog()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Avatar Library") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshLibrary() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = {
                        viewModel.showImportDialog()
                        importLauncher.launch(arrayOf(
                            "model/gltf-binary",
                            "application/octet-stream",
                            "application/vrm",
                            "application/*"
                        ))
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Import Avatar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.showImportDialog()
                    importLauncher.launch(arrayOf(
                        "model/gltf-binary",
                        "application/octet-stream",
                        "application/vrm",
                        "application/*"
                    ))
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Import Avatar")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Library stats
            uiState.stats?.let { stats ->
                LibraryStatsCard(
                    stats = stats,
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            // Avatar list
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                uiState.avatars.isEmpty() -> {
                    EmptyLibraryMessage(
                        onImportClick = { viewModel.showImportDialog() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                else -> {
                    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
                    AvatarListView(
                        avatars = uiState.avatars,
                        selectedAvatarId = uiState.selectedAvatarId,
                        mode = AvatarListMode.LIST,
                        onAvatarClick = { viewModel.selectAvatar(it.id) },
                        onFavoriteClick = { viewModel.toggleFavorite(it.id) },
                        onRenameClick = { viewModel.showRenameDialog(it.id) },
                        onDeleteClick = { pendingDeleteId = it.id },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Delete confirmation dialog
                    if (pendingDeleteId != null) {
                        AlertDialog(
                            onDismissRequest = { pendingDeleteId = null },
                            title = { Text("Delete Avatar") },
                            text = { Text("Are you sure you want to delete this avatar?") },
                            confirmButton = {
                                TextButton(onClick = {
                                    val id = pendingDeleteId
                                    pendingDeleteId = null
                                    if (id != null) viewModel.deleteAvatar(id)
                                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                            },
                            dismissButton = {
                                TextButton(onClick = { pendingDeleteId = null }) { Text("Cancel") }
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Error handling
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show error snackbar or dialog
        }
    }

    // Rename dialog
    if (uiState.showRenameDialog && uiState.renameAvatarId != null) {
        var newName by remember(uiState.renameCurrentName) { mutableStateOf(uiState.renameCurrentName) }
        AlertDialog(
            onDismissRequest = { viewModel.hideRenameDialog() },
            title = { Text("Rename Avatar") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true
                )
            },
            confirmButton = {
                val disabled = newName.trim().isEmpty()
                val avatarId = uiState.renameAvatarId
                TextButton(onClick = {
                    if (avatarId != null) {
                        viewModel.renameAvatar(avatarId, newName.trim())
                    }
                }, enabled = !disabled) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideRenameDialog() }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun LibraryStatsCard(
    stats: com.example.vtubercamera.data.vrm.AvatarLibraryStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Library Statistics",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    label = "Total Avatars",
                    value = stats.totalAvatars.toString()
                )
                StatItem(
                    label = "Favorites",
                    value = stats.favoriteCount.toString()
                )
                StatItem(
                    label = "Storage Used",
                    value = stats.getFormattedTotalSize()
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyLibraryMessage(
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No avatars in library",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = "Import VRM files to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(onClick = onImportClick) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Import Avatar")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AvatarListItem(
    avatar: AvatarInfo,
    onAvatarClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRenameClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onAvatarClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar thumbnail placeholder
            Card(
                modifier = Modifier.size(56.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Avatar info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = avatar.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = "${avatar.getFormattedFileSize()} • ${avatar.getFormattedDateAdded()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (avatar.hasExpressions() || avatar.hasPoses()) {
                    Text(
                        text = avatar.getCapabilitySummary(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Action buttons
            Row {
                IconButton(onClick = onFavoriteClick) {
                    Icon(
                        imageVector = if (avatar.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (avatar.isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (avatar.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(onClick = onRenameClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Rename avatar"
                    )
                }
                
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete avatar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
