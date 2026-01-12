package com.example.vtubercamera.ui.viewmodels

import android.util.Log
import com.example.vtubercamera.data.MediaRepository
import com.example.vtubercamera.domain.avatar.AvatarFeature
import com.example.vtubercamera.domain.camera.LensSwitchFeature
import com.example.vtubercamera.domain.camera.UiStateProvider
import com.example.vtubercamera.domain.camera.UiStateUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class CameraViewModelBootstrapper @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val lensSwitchFeature: LensSwitchFeature,
    private val avatarFeature: AvatarFeature,
) {
    companion object{
       private const val TAG = "CameraViewModelBootstrapper"
    }


    fun start(
        scope: CoroutineScope,
        updateUiState: UiStateUpdater,
        uiStateProvider: UiStateProvider,
    ) {
        scope.launch {
            mediaRepository.getAllPhotos().collect { photos ->
                updateUiState { copy(gallery = gallery.copy(allPhotos = photos)) }
            }
        }

        scope.launch {
            mediaRepository.getARPhotos().collect { photos ->
                updateUiState { copy(gallery = gallery.copy(arPhotos = photos)) }
            }
        }

        scope.launch {
            mediaRepository.getNormalPhotos().collect { photos ->
                updateUiState { copy(gallery = gallery.copy(normalPhotos = photos)) }
            }
        }

        scope.launch {
            mediaRepository.getLatestPhotoUri().collect { latestUri ->
                updateUiState { copy(camera = camera.copy(latestLibraryPhotoUri = latestUri)) }
            }
        }

        lensSwitchFeature.initializeCameraCapabilities(
            scope = scope,
            updateUiState = updateUiState,
            uiStateProvider = uiStateProvider,
        )

        scope.launch {
            try {
                avatarFeature.initializeAvatarLibrary(
                    scope = scope,
                    updateUiState = updateUiState,
                    uiStateProvider = uiStateProvider,
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize avatar library", e)
                updateUiState {
                    copy(
                        avatarLibraryState = avatarLibraryState.copy(
                            avatarLibraryError = "Failed to initialize avatar library: ${e.message}"
                        )
                    )
                }
            }
        }

        avatarFeature.startAvatarControlObservers(
            scope = scope,
            updateUiState = updateUiState,
        )
    }
}
