package com.example.vtubercamera.domain.camera

import android.net.Uri
import android.util.Log
import com.example.vtubercamera.data.MediaRepository
import com.example.vtubercamera.data.PhotoItem
import com.example.vtubercamera.ui.viewmodels.CameraUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class GalleryFeature @Inject constructor(
    private val mediaRepository: MediaRepository,
) {

    fun clearLastCapturedImage(
        scope: CoroutineScope,
        updateUiState: UiStateUpdater,
        uiStateProvider: UiStateProvider,
    ) {
        uiStateProvider().lastCapturedImageUri?.let { uri ->
            scope.launch {
                try {
                    mediaRepository.deletePhoto(uri)
                    Log.d("CameraViewModel", "写真を削除しました: $uri")
                } catch (e: Exception) {
                    Log.e("CameraViewModel", "写真の削除に失敗しました", e)
                }
            }
        }
        updateUiState {
            copy(
                lastCapturedImageUri = null,
                isPreviewMode = false,
                needsCameraRebind = true,
            )
        }
    }

    fun deletePhoto(
        uri: Uri,
        scope: CoroutineScope,
        updateUiState: UiStateUpdater,
        uiStateProvider: UiStateProvider,
        onResult: (Boolean) -> Unit,
    ) {
        scope.launch {
            try {
                val success = mediaRepository.deletePhoto(uri)
                if (success) {
                    Log.d("CameraViewModel", "写真を削除しました: $uri")
                    val currentState = uiStateProvider()
                    val shouldClearLastCaptured = uri == currentState.lastCapturedImageUri
                    val shouldClearLatest = uri == currentState.latestLibraryPhotoUri
                    if (shouldClearLastCaptured || shouldClearLatest) {
                        updateUiState {
                            copy(
                                lastCapturedImageUri = if (shouldClearLastCaptured) null else lastCapturedImageUri,
                                latestLibraryPhotoUri = if (shouldClearLatest) null else latestLibraryPhotoUri,
                                isPreviewMode = if (shouldClearLatest) false else isPreviewMode,
                                needsCameraRebind = if (shouldClearLastCaptured) true else needsCameraRebind,
                            )
                        }
                    }
                } else {
                    Log.w("CameraViewModel", "写真の削除に失敗しました: $uri")
                }
                onResult(success)
            } catch (e: Exception) {
                Log.e("CameraViewModel", "写真の削除中にエラーが発生しました", e)
                onResult(false)
            }
        }
    }

    fun deleteMultiplePhotos(
        uris: List<Uri>,
        scope: CoroutineScope,
        updateUiState: UiStateUpdater,
        onResult: (Int) -> Unit,
    ) {
        scope.launch {
            try {
                val successCount = mediaRepository.deleteMultiplePhotos(uris)
                if (successCount > 0) {
                    updateUiState {
                        copy(
                            selectedPhotos = emptySet(),
                            isSelectionMode = false,
                        )
                    }
                }
                onResult(successCount)
            } catch (e: Exception) {
                Log.e("CameraViewModel", "複数写真の削除中にエラーが発生しました", e)
                onResult(0)
            }
        }
    }

    fun refreshPhotos(
        scope: CoroutineScope,
        updateUiState: UiStateUpdater,
    ) {
        scope.launch {
            updateUiState { copy(isLoadingPhotos = true) }
            try {
                mediaRepository.refreshPhotos()
            } catch (_: Exception) {
            } finally {
                updateUiState { copy(isLoadingPhotos = false) }
            }
        }
    }

    fun togglePhotoSelection(
        uri: Uri,
        updateUiState: UiStateUpdater,
        uiStateProvider: UiStateProvider,
    ) {
        val currentSelection = uiStateProvider().selectedPhotos.toMutableSet()
        if (currentSelection.contains(uri)) {
            currentSelection.remove(uri)
        } else {
            currentSelection.add(uri)
        }

        updateUiState {
            copy(
                selectedPhotos = currentSelection,
                isSelectionMode = if (currentSelection.isEmpty()) false else isSelectionMode,
            )
        }
    }

    fun startSelectionMode(updateUiState: UiStateUpdater) {
        updateUiState {
            copy(
                isSelectionMode = true,
                selectedPhotos = emptySet(),
            )
        }
    }

    fun exitSelectionMode(updateUiState: UiStateUpdater) {
        updateUiState {
            copy(
                isSelectionMode = false,
                selectedPhotos = emptySet(),
                needsCameraRebind = true,
            )
        }
    }

    fun toggleSelectAll(
        updateUiState: UiStateUpdater,
        uiStateProvider: UiStateProvider,
    ) {
        val photosList = uiStateProvider().allPhotos
        val currentSelection = uiStateProvider().selectedPhotos
        val newSelection = if (currentSelection.size == photosList.size) {
            emptySet()
        } else {
            photosList.map { it.uri }.toSet()
        }
        updateUiState { copy(selectedPhotos = newSelection) }
    }

    fun clearSelection(updateUiState: UiStateUpdater) {
        updateUiState {
            copy(
                selectedPhotos = emptySet(),
                isSelectionMode = false,
            )
        }
    }

    fun deleteSelectedPhotos(
        scope: CoroutineScope,
        updateUiState: UiStateUpdater,
        uiStateProvider: UiStateProvider,
        onResult: (Int) -> Unit,
    ) {
        val selectedUris = uiStateProvider().selectedPhotos.toList()
        deleteMultiplePhotos(
            uris = selectedUris,
            scope = scope,
            updateUiState = updateUiState,
            onResult = onResult,
        )
    }

    fun setCurrentViewingPhoto(
        photo: PhotoItem?,
        updateUiState: UiStateUpdater,
    ) {
        updateUiState {
            copy(
                currentViewingPhoto = photo,
                needsCameraRebind = photo == null,
            )
        }
    }

    fun goToNextPhoto(
        updateUiState: UiStateUpdater,
        uiStateProvider: UiStateProvider,
    ) {
        val currentPhoto = uiStateProvider().currentViewingPhoto ?: return
        val photosList = uiStateProvider().allPhotos
        val currentIndex = photosList.indexOfFirst { it.id == currentPhoto.id }
        if (currentIndex >= 0 && currentIndex < photosList.size - 1) {
            updateUiState { copy(currentViewingPhoto = photosList[currentIndex + 1]) }
        }
    }

    fun goToPreviousPhoto(
        updateUiState: UiStateUpdater,
        uiStateProvider: UiStateProvider,
    ) {
        val currentPhoto = uiStateProvider().currentViewingPhoto ?: return
        val photosList = uiStateProvider().allPhotos
        val currentIndex = photosList.indexOfFirst { it.id == currentPhoto.id }
        if (currentIndex > 0) {
            updateUiState { copy(currentViewingPhoto = photosList[currentIndex - 1]) }
        }
    }
}
