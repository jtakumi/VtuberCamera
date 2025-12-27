package com.example.vtubercamera.ui.components

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.vtubercamera.data.vrm.AvatarInfo
import java.io.File

enum class AvatarListMode { GRID, LIST }

/**
 * Reusable avatar list/grid component.
 */
@Composable
fun AvatarListView(
    avatars: List<AvatarInfo>,
    selectedAvatarId: String?,
    mode: AvatarListMode,
    onAvatarClick: (AvatarInfo) -> Unit,
    onFavoriteClick: ((AvatarInfo) -> Unit)? = null,
    onRenameClick: ((AvatarInfo) -> Unit)? = null,
    onDeleteClick: ((AvatarInfo) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    when (mode) {
        AvatarListMode.GRID -> AvatarGridView(
            avatars = avatars,
            selectedAvatarId = selectedAvatarId,
            onAvatarClick = onAvatarClick,
            onFavoriteClick = onFavoriteClick,
            onRenameClick = onRenameClick,
            onDeleteClick = onDeleteClick,
            modifier = modifier
        )
        AvatarListMode.LIST -> AvatarLinearListView(
            avatars = avatars,
            selectedAvatarId = selectedAvatarId,
            onAvatarClick = onAvatarClick,
            onFavoriteClick = onFavoriteClick,
            onRenameClick = onRenameClick,
            onDeleteClick = onDeleteClick,
            modifier = modifier
        )
    }
}

@Composable
private fun AvatarGridView(
    avatars: List<AvatarInfo>,
    selectedAvatarId: String?,
    onAvatarClick: (AvatarInfo) -> Unit,
    onFavoriteClick: ((AvatarInfo) -> Unit)?,
    onRenameClick: ((AvatarInfo) -> Unit)?,
    onDeleteClick: ((AvatarInfo) -> Unit)?,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        items(avatars) { avatar ->
            AvatarGridCard(
                avatar = avatar,
                isSelected = avatar.id == selectedAvatarId,
                onClick = { onAvatarClick(avatar) },
                onFavoriteClick = onFavoriteClick?.let { { it(avatar) } },
                onRenameClick = onRenameClick?.let { { it(avatar) } },
                onDeleteClick = onDeleteClick?.let { { it(avatar) } }
            )
        }
    }
}

@Composable
private fun AvatarLinearListView(
    avatars: List<AvatarInfo>,
    selectedAvatarId: String?,
    onAvatarClick: (AvatarInfo) -> Unit,
    onFavoriteClick: ((AvatarInfo) -> Unit)?,
    onRenameClick: ((AvatarInfo) -> Unit)?,
    onDeleteClick: ((AvatarInfo) -> Unit)?,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        items(avatars) { avatar ->
            AvatarListCard(
                avatar = avatar,
                isSelected = avatar.id == selectedAvatarId,
                onClick = { onAvatarClick(avatar) },
                onFavoriteClick = onFavoriteClick?.let { { it(avatar) } },
                onRenameClick = onRenameClick?.let { { it(avatar) } },
                onDeleteClick = onDeleteClick?.let { { it(avatar) } }
            )
        }
    }
}

@Composable
private fun AvatarGridCard(
    avatar: AvatarInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: (() -> Unit)?,
    onRenameClick: (() -> Unit)?,
    onDeleteClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            AvatarThumbnail(avatar, Modifier.size(64.dp))
            Text(
                text = avatar.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onFavoriteClick != null) {
                    IconButton(onClick = onFavoriteClick, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = if (avatar.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (avatar.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (onRenameClick != null) {
                    IconButton(onClick = onRenameClick, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                    }
                }
                if (onDeleteClick != null) {
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun AvatarListCard(
    avatar: AvatarInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: (() -> Unit)?,
    onRenameClick: (() -> Unit)?,
    onDeleteClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarThumbnail(avatar, Modifier.size(48.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = "${avatar.getFormattedFileSize()} • ${avatar.getFormattedDateAdded()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                if (onFavoriteClick != null) {
                    IconButton(onClick = onFavoriteClick) {
                        Icon(
                            imageVector = if (avatar.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (avatar.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (onRenameClick != null) {
                    IconButton(onClick = onRenameClick) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                    }
                }
                if (onDeleteClick != null) {
                    IconButton(onClick = onDeleteClick) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun AvatarThumbnail(avatar: AvatarInfo, modifier: Modifier = Modifier) {
    val hasThumb = avatar.thumbnailPath.isNotBlank()
    if (hasThumb) {
        val uri = toUri(avatar.thumbnailPath)
        Image(
            painter = rememberAsyncImagePainter(uri),
            contentDescription = avatar.name,
            modifier = modifier.clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun toUri(path: String): Uri =
    if (path.startsWith("content://") || path.startsWith("file://")) Uri.parse(path)
    else Uri.fromFile(File(path))

