package com.example.vtubercamera.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vtubercamera.data.vrm.AvatarInfo
import com.example.vtubercamera.data.vrm.AvatarSortBy
import com.example.vtubercamera.data.vrm.VRMModel

/**
 * Avatar selector panel for choosing and managing avatars
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarSelectorPanel(
    avatars: List<AvatarInfo>,
    currentAvatar: VRMModel?,
    selectedAvatarId: String?,
    sortBy: AvatarSortBy,
    isLoading: Boolean,
    onAvatarSelected: (String) -> Unit,
    onSortByChanged: (AvatarSortBy) -> Unit,
    onRefresh: () -> Unit,
    onImportAvatar: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSortOptions by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(AvatarViewMode.GRID) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp)
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header with controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Avatar Selection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row {
                    // View mode toggle
                    IconButton(
                        onClick = {
                            viewMode = if (viewMode == AvatarViewMode.GRID)
                                AvatarViewMode.LIST else AvatarViewMode.GRID
                        }
                    ) {
                        Icon(
                            imageVector = if (viewMode == AvatarViewMode.GRID)
                                Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle View Mode"
                        )
                    }

                    // Sort options
                    IconButton(
                        onClick = { showSortOptions = !showSortOptions }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort Options"
                        )
                    }

                    // Refresh
                    IconButton(
                        onClick = onRefresh,
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }

                    // Import
                    IconButton(
                        onClick = onImportAvatar
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Import Avatar"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sort options dropdown
            AnimatedVisibility(
                visible = showSortOptions,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                SortOptionsPanel(
                    currentSortBy = sortBy,
                    onSortByChanged = { newSortBy ->
                        onSortByChanged(newSortBy)
                        showSortOptions = false
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Current avatar info
            currentAvatar?.let { avatar ->
                CurrentAvatarCard(
                    avatar = avatar,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Loading indicator
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Avatar list/grid
                when (viewMode) {
                    AvatarViewMode.GRID -> {
                        AvatarGrid(
                            avatars = avatars,
                            selectedAvatarId = selectedAvatarId,
                            onAvatarSelected = onAvatarSelected,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    AvatarViewMode.LIST -> {
                        AvatarList(
                            avatars = avatars,
                            selectedAvatarId = selectedAvatarId,
                            onAvatarSelected = onAvatarSelected,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Footer with stats
            if (avatars.isNotEmpty()) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "${avatars.size} avatars available",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CurrentAvatarCard(
    avatar: VRMModel,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar icon/thumbnail
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Current: ${avatar.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "${avatar.expressions.size} expressions • ${avatar.poses.size} poses",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }

            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Current Avatar",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SortOptionsPanel(
    currentSortBy: AvatarSortBy,
    onSortByChanged: (AvatarSortBy) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = "Sort by:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(AvatarSortBy.values()) { sortOption ->
                    FilterChip(
                        onClick = { onSortByChanged(sortOption) },
                        label = {
                            Text(
                                text = sortOption.displayName,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        selected = currentSortBy == sortOption
                    )
                }
            }
        }
    }
}

@Composable
private fun AvatarGrid(
    avatars: List<AvatarInfo>,
    selectedAvatarId: String?,
    onAvatarSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(avatars.chunked(2)) { rowAvatars ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowAvatars.forEach { avatar ->
                    AvatarGridItem(
                        avatar = avatar,
                        isSelected = avatar.id == selectedAvatarId,
                        onSelected = { onAvatarSelected(avatar.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill remaining space if odd number of items
                if (rowAvatars.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AvatarList(
    avatars: List<AvatarInfo>,
    selectedAvatarId: String?,
    onAvatarSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(avatars) { avatar ->
            AvatarListItem(
                avatar = avatar,
                isSelected = avatar.id == selectedAvatarId,
                onSelected = { onAvatarSelected(avatar.id) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AvatarGridItem(
    avatar: AvatarInfo,
    isSelected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { onSelected() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected)
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Avatar thumbnail/icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = avatar.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurface
            )

            // Favorite indicator
            if (avatar.isFavorite) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Favorite",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun AvatarListItem(
    avatar: AvatarInfo,
    isSelected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onSelected() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected)
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar icon
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = avatar.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (avatar.isFavorite) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Favorite",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Text(
                    text = "Added ${avatar.dateAdded}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * View mode for avatar display
 */
enum class AvatarViewMode {
    GRID, LIST
}

/**
 * Extension to get display name for sort options
 */
private val AvatarSortBy.displayName: String
    get() = when (this) {
        AvatarSortBy.NAME_ASC -> "Name A-Z"
        AvatarSortBy.NAME_DESC -> "Name Z-A"
        AvatarSortBy.DATE_ADDED_ASC -> "Oldest First"
        AvatarSortBy.DATE_ADDED_DESC -> "Newest First"
        AvatarSortBy.DATE_USED_ASC -> "Oldest Used"
        AvatarSortBy.DATE_USED_DESC -> "Recently Used"
        AvatarSortBy.SIZE_ASC -> "Smallest First"
        AvatarSortBy.SIZE_DESC -> "Largest First"
        AvatarSortBy.USAGE_COUNT_ASC -> "Least Used"
        AvatarSortBy.USAGE_COUNT_DESC -> "Most Used"
        AvatarSortBy.FAVORITES_FIRST -> "Favorites First"
    }