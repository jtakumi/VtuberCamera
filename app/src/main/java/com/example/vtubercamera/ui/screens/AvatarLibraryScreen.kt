package com.example.vtubercamera.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vtubercamera.data.vrm.ErrorAction
import com.example.vtubercamera.data.vrm.ErrorType
import com.example.vtubercamera.data.vrm.AvatarInfo
import com.example.vtubercamera.ui.components.AvatarListMode
import com.example.vtubercamera.ui.components.AvatarListView
import com.example.vtubercamera.ui.viewmodels.AvatarLibraryViewModel
import com.example.vtubercamera.R

/**
 * Screen for managing the avatar library
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarLibraryScreen(
    onNavigateBack: () -> Unit,
    onAvatarSelected: (AvatarInfo) -> Unit = {},
    viewModel: AvatarLibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val errorNotifications by viewModel.activeErrorNotifications.collectAsStateWithLifecycle()
    var lastShownVrmNotificationId by remember { mutableStateOf<String?>(null) }

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

    val vrmNotification = remember(errorNotifications) {
        errorNotifications.firstOrNull { it.errorType.isVrmError() }
    }

    if (vrmNotification != null && vrmNotification.id != lastShownVrmNotificationId) {
        lastShownVrmNotificationId = vrmNotification.id
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Avatar Library") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshLibrary() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = {
                        viewModel.showImportDialog()
                        importLauncher.launch(
                            arrayOf(
                                "model/gltf-binary",
                                "application/octet-stream",
                                "application/vrm",
                                "application/*"
                            )
                        )
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
                    importLauncher.launch(
                        arrayOf(
                            "model/gltf-binary",
                            "application/octet-stream",
                            "application/vrm",
                            "application/*"
                        )
                    )
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
                        onAvatarClick = {
                            viewModel.selectAvatar(it.id)
                            onAvatarSelected(it)
                        },
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

    if (vrmNotification != null && vrmNotification.id == lastShownVrmNotificationId) {
        val title = getVrmErrorTitle(vrmNotification.errorType)
        val message = getVrmErrorMessage(vrmNotification.errorType)
        val supportedPrimaryAction = vrmNotification.actions.firstOrNull { action ->
            action.action.isSupportedVrmDialogAction()
        }
        AlertDialog(
            onDismissRequest = {
                viewModel.dismissErrorNotification(vrmNotification.id)
            },
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                if (supportedPrimaryAction != null) {
                    TextButton(onClick = {
                        handleVrmNotificationAction(
                            context = context,
                            action = supportedPrimaryAction.action,
                            launchFilePicker = {
                                importLauncher.launch(
                                    arrayOf(
                                        "model/gltf-binary",
                                        "application/octet-stream",
                                        "application/vrm",
                                        "application/*"
                                    )
                                )
                            }
                        )
                        viewModel.dismissErrorNotification(vrmNotification.id)
                    }) {
                        Text(getVrmActionLabel(supportedPrimaryAction.action))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.dismissErrorNotification(vrmNotification.id)
                }) {
                    Text(stringResource(R.string.vrm_action_dismiss))
                }
            }
        )
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

private fun ErrorType.isVrmError(): Boolean {
    return when (this) {
        ErrorType.VRM_FILE_NOT_FOUND,
        ErrorType.VRM_INVALID_FORMAT,
        ErrorType.VRM_FILE_TOO_LARGE,
        ErrorType.VRM_CORRUPTED,
        ErrorType.VRM_UNSUPPORTED_VERSION,
        ErrorType.VRM_INSUFFICIENT_MEMORY,
        ErrorType.VRM_PERMISSION_DENIED,
        ErrorType.VRM_PARSE_ERROR -> true
        else -> false
    }
}

@Composable
private fun getVrmErrorTitle(errorType: ErrorType): String {
    return when (errorType) {
        ErrorType.VRM_FILE_NOT_FOUND -> stringResource(R.string.vrm_error_title_file_not_found)
        ErrorType.VRM_INVALID_FORMAT -> stringResource(R.string.vrm_error_title_invalid_format)
        ErrorType.VRM_FILE_TOO_LARGE -> stringResource(R.string.vrm_error_title_file_too_large)
        ErrorType.VRM_CORRUPTED -> stringResource(R.string.vrm_error_title_corrupted)
        ErrorType.VRM_UNSUPPORTED_VERSION -> stringResource(R.string.vrm_error_title_unsupported_version)
        ErrorType.VRM_INSUFFICIENT_MEMORY -> stringResource(R.string.vrm_error_title_insufficient_memory)
        ErrorType.VRM_PERMISSION_DENIED -> stringResource(R.string.vrm_error_title_permission_denied)
        ErrorType.VRM_PARSE_ERROR -> stringResource(R.string.vrm_error_title_parse_error)
        else -> stringResource(R.string.vrm_error_title_generic)
    }
}

@Composable
private fun getVrmErrorMessage(errorType: ErrorType): String {
    return when (errorType) {
        ErrorType.VRM_FILE_NOT_FOUND -> stringResource(R.string.vrm_error_message_file_not_found)
        ErrorType.VRM_INVALID_FORMAT -> stringResource(R.string.vrm_error_message_invalid_format)
        ErrorType.VRM_FILE_TOO_LARGE -> stringResource(R.string.vrm_error_message_file_too_large)
        ErrorType.VRM_CORRUPTED -> stringResource(R.string.vrm_error_message_corrupted)
        ErrorType.VRM_UNSUPPORTED_VERSION -> stringResource(R.string.vrm_error_message_unsupported_version)
        ErrorType.VRM_INSUFFICIENT_MEMORY -> stringResource(R.string.vrm_error_message_insufficient_memory)
        ErrorType.VRM_PERMISSION_DENIED -> stringResource(R.string.vrm_error_message_permission_denied)
        ErrorType.VRM_PARSE_ERROR -> stringResource(R.string.vrm_error_message_parse_error)
        else -> stringResource(R.string.vrm_error_message_generic)
    }
}

@Composable
private fun getVrmActionLabel(action: ErrorAction?): String {
    return when (action) {
        ErrorAction.SELECT_DIFFERENT_FILE -> stringResource(R.string.vrm_action_select_file)
        ErrorAction.SELECT_SMALLER_FILE -> stringResource(R.string.vrm_action_select_smaller_file)
        ErrorAction.REQUEST_PERMISSIONS -> stringResource(R.string.vrm_action_grant_permission)
        ErrorAction.OPEN_SETTINGS -> stringResource(R.string.vrm_action_open_settings)
        else -> stringResource(R.string.vrm_action_dismiss)
    }
}

private fun ErrorAction?.isSupportedVrmDialogAction(): Boolean {
    return when (this) {
        ErrorAction.SELECT_DIFFERENT_FILE,
        ErrorAction.SELECT_SMALLER_FILE,
        ErrorAction.REQUEST_PERMISSIONS,
        ErrorAction.OPEN_SETTINGS -> true
        else -> false
    }
}

private fun handleVrmNotificationAction(
    context: android.content.Context,
    action: ErrorAction?,
    launchFilePicker: () -> Unit
) {
    when (action) {
        ErrorAction.SELECT_DIFFERENT_FILE,
        ErrorAction.SELECT_SMALLER_FILE -> launchFilePicker()
        ErrorAction.REQUEST_PERMISSIONS,
        ErrorAction.OPEN_SETTINGS -> {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
        else -> {
            // No-op for unsupported actions in this dialog
        }
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
