package com.example.vtubercamera.domain.avatar

import android.net.Uri
import android.util.Log
import com.example.vtubercamera.data.VRMRepository
import com.example.vtubercamera.data.vrm.ARError
import com.example.vtubercamera.data.vrm.AvatarController
import com.example.vtubercamera.data.vrm.AvatarInfo
import com.example.vtubercamera.data.vrm.AvatarLibraryManager
import com.example.vtubercamera.data.vrm.AvatarState
import com.example.vtubercamera.data.vrm.AvatarSortBy
import com.example.vtubercamera.data.vrm.Expression
import com.example.vtubercamera.data.vrm.ExpressionController
import com.example.vtubercamera.data.vrm.Pose
import com.example.vtubercamera.data.vrm.PoseController
import com.example.vtubercamera.data.vrm.math.Transform
import com.example.vtubercamera.domain.camera.UiStateProvider
import com.example.vtubercamera.domain.camera.UiStateUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

class AvatarFeature @Inject constructor(
    private val vrmRepository: VRMRepository,
    private val avatarLibraryManager: AvatarLibraryManager,
    private val avatarController: AvatarController,
    private val expressionController: ExpressionController,
    private val poseController: PoseController,
) {

    fun startAvatarControlObservers(
        scope: CoroutineScope,
        updateUiState: UiStateUpdater,
    ) {
        scope.launch {
            avatarController.avatarState.collect { avatarControllerState ->
                updateUiState {
                    copy(
                        avatarState = avatarControllerState,
                        avatarTransform = avatarControllerState.transform,
                        currentAvatar = avatarControllerState.model,
                        currentExpression = avatarControllerState.currentExpression,
                        currentPose = avatarControllerState.currentPose,
                    )
                }
            }
        }

        scope.launch {
            expressionController.currentExpression.collect { expression ->
                updateUiState { copy(currentExpression = expression) }
            }
        }

        scope.launch {
            expressionController.activeBlendShapes.collect { blendShapes ->
                updateUiState { copy(activeBlendShapes = blendShapes) }
            }
        }

        scope.launch {
            expressionController.isTransitioning.collect { isTransitioning ->
                updateUiState { copy(isExpressionTransitioning = isTransitioning) }
            }
        }

        scope.launch {
            expressionController.transitionProgress.collect { progress ->
                updateUiState { copy(expressionTransitionProgress = progress) }
            }
        }

        scope.launch {
            poseController.currentPose.collect { pose ->
                updateUiState { copy(currentPose = pose) }
            }
        }

        scope.launch {
            poseController.activeBoneTransforms.collect { transforms ->
                updateUiState { copy(activeBoneTransforms = transforms) }
            }
        }

        scope.launch {
            poseController.boneLocks.collect { locks ->
                updateUiState { copy(boneLocks = locks) }
            }
        }

        scope.launch {
            poseController.isTransitioning.collect { isTransitioning ->
                updateUiState { copy(isPoseTransitioning = isTransitioning) }
            }
        }

        scope.launch {
            poseController.transitionProgress.collect { progress ->
                updateUiState { copy(poseTransitionProgress = progress) }
            }
        }
    }

    fun loadAvatar(
        uri: Uri,
        scope: CoroutineScope,
        updateUiState: UiStateUpdater,
        uiStateProvider: UiStateProvider,
    ) {
        scope.launch {
            try {
                Log.d("CameraViewModel", "Loading avatar from URI: $uri")

                val currentState = uiStateProvider()
                updateUiState {
                    copy(
                        avatarState = currentState.avatarState.copy(
                            isLoading = true,
                            loadingProgress = 0.0f,
                        )
                    )
                }

                val result = vrmRepository.loadVRMFromUri(uri)

                result.fold(
                    onSuccess = { vrmModel ->
                        Log.d("CameraViewModel", "Avatar loaded successfully: ${vrmModel.name}")

                        updateUiState {
                            copy(
                                currentAvatar = vrmModel,
                                avatarState = AvatarState(
                                    model = vrmModel,
                                    transform = currentState.avatarTransform,
                                    currentExpression = currentState.currentExpression,
                                    currentPose = currentState.currentPose,
                                    isVisible = currentState.isARMode,
                                    isLoading = false,
                                    loadingProgress = 1.0f,
                                )
                            )
                        }

                        avatarController.loadModel(vrmModel)

                        if (currentState.autoResetOnAvatarChange) {
                            clearExpression()
                            clearPose()
                        } else {
                            if (vrmModel.expressions.isNotEmpty()) {
                                selectExpression(vrmModel.expressions.first(), smoothTransitions = currentState.smoothTransitions)
                            }
                            if (vrmModel.poses.isNotEmpty()) {
                                selectPose(vrmModel.poses.first(), smoothTransitions = currentState.smoothTransitions)
                            }
                        }
                    },
                    onFailure = { error ->
                        Log.e("CameraViewModel", "Failed to load avatar", error)
                        updateUiState {
                            copy(
                                avatarState = currentState.avatarState.copy(
                                    isLoading = false,
                                    loadingProgress = 0.0f,
                                ),
                                arError = ARError.AvatarError("Failed to load avatar: ${error.message}"),
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Error loading avatar", e)
                updateUiState {
                    copy(
                        avatarState = avatarState.copy(
                            isLoading = false,
                            loadingProgress = 0.0f,
                        ),
                        arError = ARError.AvatarError("Error loading avatar: ${e.message}"),
                    )
                }
            }
        }
    }

    fun updateAvatarTransform(transform: Transform) {
        avatarController.setTransform(transform)
        Log.d("CameraViewModel", "Avatar transform updated: $transform")
    }

    fun toggleAvatarVisibility(uiStateProvider: UiStateProvider) {
        val currentState = uiStateProvider()
        val newVisibility = !currentState.avatarState.isVisible
        avatarController.setVisible(newVisibility)
        Log.d("CameraViewModel", "Avatar visibility toggled: $newVisibility")
    }

    fun resetAvatarTransform() {
        val defaultTransform = Transform.identity()
        avatarController.setTransform(defaultTransform)
        Log.d("CameraViewModel", "Avatar transform reset to default")
    }

    fun selectExpression(expression: Expression?, smoothTransitions: Boolean) {
        if (smoothTransitions && expression != null) {
            expressionController.transitionToExpression(expression)
        } else {
            expressionController.applyExpression(expression)
        }
        Log.d("CameraViewModel", "Selected expression: ${expression?.name ?: "none"}")
    }

    fun setBlendShapeWeight(shapeName: String, weight: Float) {
        expressionController.setBlendShapeWeight(shapeName, weight)
    }

    fun clearExpression() {
        expressionController.clearExpression()
        Log.d("CameraViewModel", "Cleared expression")
    }

    fun setExpressionTransitionDuration(duration: Float) {
        expressionController.setTransitionDuration(duration)
    }

    fun blendExpressions(expressionWeights: Map<Expression, Float>) {
        expressionController.blendExpressions(expressionWeights)
    }

    fun selectPose(pose: Pose?, smoothTransitions: Boolean) {
        if (smoothTransitions && pose != null) {
            poseController.transitionToPose(pose)
        } else {
            poseController.applyPose(pose)
        }
        Log.d("CameraViewModel", "Selected pose: ${pose?.name ?: "none"}")
    }

    fun setBoneTransform(boneName: String, transform: Transform) {
        poseController.setBoneTransform(boneName, transform)
    }

    fun toggleBoneLock(boneName: String) {
        if (poseController.isBoneLocked(boneName)) {
            poseController.unlockBone(boneName)
        } else {
            poseController.lockBone(boneName)
        }
        Log.d("CameraViewModel", "Toggled bone lock for: $boneName")
    }

    fun clearPose() {
        poseController.clearPose()
        Log.d("CameraViewModel", "Cleared pose")
    }

    fun resetToDefaultPose() {
        poseController.resetToDefaultPose()
        Log.d("CameraViewModel", "Reset to default pose")
    }

    fun setPoseTransitionDuration(duration: Float) {
        poseController.setTransitionDuration(duration)
    }

    fun blendPoses(poseWeights: Map<Pose, Float>) {
        poseController.blendPoses(poseWeights)
    }

    fun updateAvatarTransitions(deltaTime: Float) {
        expressionController.updateTransition(deltaTime)
        poseController.updateTransition(deltaTime)
    }

    fun initializeAvatarLibrary(
        scope: CoroutineScope,
        updateUiState: UiStateUpdater,
        uiStateProvider: UiStateProvider,
    ) {
        loadAvatarsFromLibrary(
            scope = scope,
            updateUiState = updateUiState,
            uiStateProvider = uiStateProvider,
        )
        loadAvatarLibraryStats(
            scope = scope,
            updateUiState = updateUiState,
        )
    }

    fun refreshAvatarLibrary(
        scope: CoroutineScope,
        updateUiState: UiStateUpdater,
        uiStateProvider: UiStateProvider,
    ) {
        initializeAvatarLibrary(
            scope = scope,
            updateUiState = updateUiState,
            uiStateProvider = uiStateProvider,
        )
    }

    fun loadAvatarsFromLibrary(
        scope: CoroutineScope,
        updateUiState: UiStateUpdater,
        uiStateProvider: UiStateProvider,
    ) {
        scope.launch {
            try {
                updateUiState {
                    copy(
                        isLoadingAvatarLibrary = true,
                        avatarLibraryError = null,
                    )
                }

                val sortBy: AvatarSortBy = uiStateProvider().avatarSortBy

                avatarLibraryManager.getAvatarsSortedBy(sortBy)
                    .catch { error: Throwable ->
                        updateUiState {
                            copy(
                                avatarLibraryError = error.message ?: "Failed to load avatars",
                                isLoadingAvatarLibrary = false,
                            )
                        }
                    }
                    .collect { avatars: List<AvatarInfo> ->
                        updateUiState {
                            copy(
                                avatarLibrary = avatars,
                                isLoadingAvatarLibrary = false,
                                avatarLibraryError = null,
                            )
                        }
                    }
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Error in loadAvatarsFromLibrary", e)
                updateUiState {
                    copy(
                        avatarLibraryError = e.message ?: "Unknown error occurred",
                        isLoadingAvatarLibrary = false,
                    )
                }
            }
        }
    }

    fun loadAvatarLibraryStats(
        scope: CoroutineScope,
        updateUiState: UiStateUpdater,
    ) {
        scope.launch {
            try {
                val stats = vrmRepository.getLibraryStatistics()
                updateUiState { copy(avatarLibraryStats = stats) }
            } catch (e: Exception) {
                Log.w("CameraViewModel", "Failed to load avatar library statistics: ${e.message}")
            }
        }
    }

    fun selectAvatarFromLibrary(
        avatarId: String,
        scope: CoroutineScope,
        updateUiState: UiStateUpdater,
        uiStateProvider: UiStateProvider,
    ) {
        scope.launch {
            try {
                vrmRepository.recordAvatarUsage(avatarId)

                updateUiState { copy(selectedAvatarId = avatarId) }

                val avatarInfo = uiStateProvider().avatarLibrary.find { it.id == avatarId }
                if (avatarInfo != null) {
                    loadAvatar(
                        uri = Uri.fromFile(File(avatarInfo.filePath)),
                        scope = scope,
                        updateUiState = updateUiState,
                        uiStateProvider = uiStateProvider,
                    )
                    Log.d("CameraViewModel", "Selected and loading avatar: ${avatarInfo.name}")
                } else {
                    updateUiState { copy(avatarLibraryError = "Avatar not found in library") }
                }

                refreshAvatarLibrary(
                    scope = scope,
                    updateUiState = updateUiState,
                    uiStateProvider = uiStateProvider,
                )
            } catch (e: Exception) {
                updateUiState { copy(avatarLibraryError = "Failed to select avatar: ${e.message}") }
                Log.e("CameraViewModel", "Failed to select avatar", e)
            }
        }
    }

    fun toggleAvatarFavorite(
        avatarId: String,
        scope: CoroutineScope,
        updateUiState: UiStateUpdater,
        uiStateProvider: UiStateProvider,
    ) {
        scope.launch {
            try {
                val avatar = uiStateProvider().avatarLibrary.find { it.id == avatarId }
                if (avatar != null) {
                    val result = vrmRepository.setAvatarFavorite(avatarId, !avatar.isFavorite)
                    if (result.isFailure) {
                        updateUiState {
                            copy(avatarLibraryError = "Failed to update favorite: ${result.exceptionOrNull()?.message}")
                        }
                    } else {
                        refreshAvatarLibrary(
                            scope = scope,
                            updateUiState = updateUiState,
                            uiStateProvider = uiStateProvider,
                        )
                        Log.d("CameraViewModel", "Toggled favorite for avatar: ${avatar.name}")
                    }
                }
            } catch (e: Exception) {
                updateUiState { copy(avatarLibraryError = "Failed to toggle favorite: ${e.message}") }
                Log.e("CameraViewModel", "Failed to toggle favorite", e)
            }
        }
    }

    fun deleteAvatarFromLibrary(
        avatarId: String,
        scope: CoroutineScope,
        updateUiState: UiStateUpdater,
        uiStateProvider: UiStateProvider,
    ) {
        scope.launch {
            try {
                updateUiState { copy(isLoadingAvatarLibrary = true) }

                val result = vrmRepository.deleteAvatar(avatarId)
                if (result.isFailure) {
                    updateUiState {
                        copy(
                            avatarLibraryError = "Failed to delete avatar: ${result.exceptionOrNull()?.message}",
                            isLoadingAvatarLibrary = false,
                        )
                    }
                } else {
                    if (uiStateProvider().selectedAvatarId == avatarId) {
                        updateUiState {
                            copy(
                                selectedAvatarId = null,
                                currentAvatar = null,
                                avatarState = AvatarState(),
                            )
                        }
                    }

                    refreshAvatarLibrary(
                        scope = scope,
                        updateUiState = updateUiState,
                        uiStateProvider = uiStateProvider,
                    )
                    Log.d("CameraViewModel", "Deleted avatar: $avatarId")
                }
            } catch (e: Exception) {
                updateUiState {
                    copy(
                        avatarLibraryError = "Failed to delete avatar: ${e.message}",
                        isLoadingAvatarLibrary = false,
                    )
                }
                Log.e("CameraViewModel", "Failed to delete avatar", e)
            }
        }
    }

    fun renameAvatarInLibrary(
        avatarId: String,
        newName: String,
        scope: CoroutineScope,
        updateUiState: UiStateUpdater,
        uiStateProvider: UiStateProvider,
    ) {
        scope.launch {
            try {
                val result = vrmRepository.renameAvatar(avatarId, newName)
                if (result.isFailure) {
                    updateUiState {
                        copy(avatarLibraryError = "Failed to rename avatar: ${result.exceptionOrNull()?.message}")
                    }
                } else {
                    updateUiState {
                        copy(
                            showRenameDialog = false,
                            renameAvatarId = null,
                            renameCurrentName = "",
                        )
                    }

                    refreshAvatarLibrary(
                        scope = scope,
                        updateUiState = updateUiState,
                        uiStateProvider = uiStateProvider,
                    )
                    Log.d("CameraViewModel", "Renamed avatar $avatarId to: $newName")
                }
            } catch (e: Exception) {
                updateUiState { copy(avatarLibraryError = "Failed to rename avatar: ${e.message}") }
                Log.e("CameraViewModel", "Failed to rename avatar", e)
            }
        }
    }

    fun cleanupAvatarLibrary(
        scope: CoroutineScope,
        updateUiState: UiStateUpdater,
        uiStateProvider: UiStateProvider,
    ) {
        scope.launch {
            try {
                updateUiState { copy(isLoadingAvatarLibrary = true) }

                val result = vrmRepository.cleanupLibrary()

                updateUiState { copy(isLoadingAvatarLibrary = false) }

                if (result.success) {
                    refreshAvatarLibrary(
                        scope = scope,
                        updateUiState = updateUiState,
                        uiStateProvider = uiStateProvider,
                    )
                    Log.d("CameraViewModel", "Avatar library cleanup completed")
                } else {
                    updateUiState { copy(avatarLibraryError = result.error) }
                }
            } catch (e: Exception) {
                updateUiState {
                    copy(
                        avatarLibraryError = "Failed to cleanup library: ${e.message}",
                        isLoadingAvatarLibrary = false,
                    )
                }
                Log.e("CameraViewModel", "Failed to cleanup avatar library", e)
            }
        }
    }

    fun clearAvatarLibraryError(updateUiState: UiStateUpdater) {
        updateUiState { copy(avatarLibraryError = null) }
    }
}
