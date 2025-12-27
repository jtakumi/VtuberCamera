package com.example.vtubercamera.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.vtubercamera.data.PhotoItem

@Composable
fun GalleryView(
    photos: List<PhotoItem>,
    isLoading: Boolean,
    selectedPhotos: Set<android.net.Uri>,
    isSelectionMode: Boolean,
    onPhotoClick: (PhotoItem) -> Unit,
    onPhotoLongClick: (PhotoItem) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> LoadingIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
            photos.isEmpty() -> EmptyGalleryMessage(
                modifier = Modifier.align(Alignment.Center)
            )
            else -> PhotoGrid(
                photos = photos,
                selectedPhotos = selectedPhotos,
                isSelectionMode = isSelectionMode,
                onPhotoClick = onPhotoClick,
                onPhotoLongClick = onPhotoLongClick
            )
        }
    }
}

@Composable
private fun LoadingIndicator(modifier: Modifier = Modifier) {
    CircularProgressIndicator(modifier = modifier)
}

@Composable
private fun EmptyGalleryMessage(modifier: Modifier = Modifier) {
    Text(
        text = "写真がありません",
        modifier = modifier,
        style = MaterialTheme.typography.bodyLarge
    )
}

@Composable
private fun PhotoGrid(
    photos: List<PhotoItem>,
    selectedPhotos: Set<android.net.Uri>,
    isSelectionMode: Boolean,
    onPhotoClick: (PhotoItem) -> Unit,
    onPhotoLongClick: (PhotoItem) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(photos) { photo ->
            PhotoGridItem(
                photo = photo,
                isSelected = selectedPhotos.contains(photo.uri),
                isSelectionMode = isSelectionMode,
                onPhotoClick = onPhotoClick,
                onPhotoLongClick = onPhotoLongClick
            )
        }
    }
}

@Composable
private fun PhotoGridItem(
    photo: PhotoItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onPhotoClick: (PhotoItem) -> Unit,
    onPhotoLongClick: (PhotoItem) -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onPhotoLongClick(photo) },
                    onTap = { onPhotoClick(photo) }
                )
            }
    ) {
        PhotoImage(photo = photo)
        
        if (isSelectionMode) {
            SelectionOverlay(isSelected = isSelected)
            SelectionIcon(
                isSelected = isSelected,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(24.dp)
            )
        }
    }
}

@Composable
private fun PhotoImage(photo: PhotoItem) {
    AsyncImage(
        model = photo.uri,
        contentDescription = photo.displayName,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun SelectionOverlay(isSelected: Boolean) {
    if (isSelected) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Blue.copy(alpha = 0.3f))
        )
    }
}

@Composable
private fun SelectionIcon(isSelected: Boolean, modifier: Modifier = Modifier) {
    Icon(
        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
        contentDescription = if (isSelected) "選択済み" else "未選択",
        tint = if (isSelected) Color.Blue else Color.White,
        modifier = modifier
    )
}
