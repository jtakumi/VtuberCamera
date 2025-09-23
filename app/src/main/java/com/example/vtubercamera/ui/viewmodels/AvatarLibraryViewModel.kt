package com.example.vtubercamera.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vtubercamera.data.vrm.AvatarInfo
import com.example.vtubercamera.data.vrm.AvatarLibraryManager
import com.example.vtubercamera.data.vrm.AvatarLibraryStats
import com.example.vtubercamera.data.vrm.AvatarSortBy
import com.example.vtubercamera.data.VRMRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Avatar Library screen
 */
@HiltViewModel
class AvatarLibraryViewModel @Inject constructor(
    private val avatarLibraryManager: AvatarLibraryManager,
    private val vrmRepository: VRMRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AvatarLibraryUiState())
    val uiState: StateFlow<AvatarLibraryUiState> = _uiState.asStateFlow()

    private val _sortBy = MutableStateFlow(AvatarSortBy.DATE_ADDED_DESC)
    
    init {
        loadAvatars()
        loadLibraryStats()
    }

    /**
     * Load avatars from the library
     */
    private fun loadAvatars() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                avatarLibraryManager.getAvatarsSortedBy(_sortBy.value)
                    .catch { error ->
                        _uiState.update { 
                            it.copy(
                                isLoading = false, 
                                error = error.message ?: "Failed to load avatars"
                            ) 
                        }
                    }
                    .collect { avatars ->
                        _uiState.update { 
                            it.copy(
                                avatars = avatars,
                                isLoading = false,
                                error = null
                            ) 
                        }
                    }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = e.message ?: "Unknown error occurred"
                    ) 
                }
            }
        }
    }

    /**
     * Load library statistics
     */
    private fun loadLibraryStats() {
        viewModelScope.launch {
            try {
                val stats = vrmRepository.getLibraryStatistics()
                _uiState.update { it.copy(stats = stats) }
            } catch (e: Exception) {
                // Stats loading failure is not critical, just log it
                _uiState.update { 
                    it.copy(error = "Failed to load library statistics: ${e.message}")
                }
            }
        }
    }

    /**
     * Refresh the avatar library
     */
    fun refreshLibrary() {
        loadAvatars()
        loadLibraryStats()
    }

    /**
     * Select an avatar
     */
    fun selectAvatar(avatarId: String) {
        viewModelScope.launch {
            try {
                // Record usage
                vrmRepository.recordAvatarUsage(avatarId)
                
                // Update selected avatar
                _uiState.update { it.copy(selectedAvatarId = avatarId) }
                
                // Refresh to show updated usage
                refreshLibrary()
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = "Failed to select avatar: ${e.message}")
                }
            }
        }
    }

    /**
     * Toggle favorite status of an avatar
     */
    fun toggleFavorite(avatarId: String) {
        viewModelScope.launch {
            try {
                val avatar = _uiState.value.avatars.find { it.id == avatarId }
                if (avatar != null) {
                    val result = vrmRepository.setAvatarFavorite(avatarId, !avatar.isFavorite)
                    if (result.isFailure) {
                        _uiState.update { 
                            it.copy(error = "Failed to update favorite: ${result.exceptionOrNull()?.message}")
                        }
                    } else {
                        refreshLibrary()
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = "Failed to toggle favorite: ${e.message}")
                }
            }
        }
    }

    /**
     * Delete an avatar
     */
    fun deleteAvatar(avatarId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                val result = vrmRepository.deleteAvatar(avatarId)
                if (result.isFailure) {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = "Failed to delete avatar: ${result.exceptionOrNull()?.message}"
                        )
                    }
                } else {
                    refreshLibrary()
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to delete avatar: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Show import dialog
     */
    fun showImportDialog() {
        _uiState.update { it.copy(showImportDialog = true) }
    }

    /**
     * Hide import dialog
     */
    fun hideImportDialog() {
        _uiState.update { it.copy(showImportDialog = false) }
    }

    /**
     * Show rename dialog
     */
    fun showRenameDialog(avatarId: String) {
        val avatar = _uiState.value.avatars.find { it.id == avatarId }
        if (avatar != null) {
            _uiState.update { 
                it.copy(
                    showRenameDialog = true,
                    renameAvatarId = avatarId,
                    renameCurrentName = avatar.name
                )
            }
        }
    }

    /**
     * Hide rename dialog
     */
    fun hideRenameDialog() {
        _uiState.update { 
            it.copy(
                showRenameDialog = false,
                renameAvatarId = null,
                renameCurrentName = ""
            )
        }
    }

    /**
     * Rename an avatar
     */
    fun renameAvatar(avatarId: String, newName: String) {
        viewModelScope.launch {
            try {
                val result = vrmRepository.renameAvatar(avatarId, newName)
                if (result.isFailure) {
                    _uiState.update { 
                        it.copy(error = "Failed to rename avatar: ${result.exceptionOrNull()?.message}")
                    }
                } else {
                    hideRenameDialog()
                    refreshLibrary()
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = "Failed to rename avatar: ${e.message}")
                }
            }
        }
    }

    /**
     * Change sort order
     */
    fun setSortBy(sortBy: AvatarSortBy) {
        _sortBy.value = sortBy
        loadAvatars()
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Cleanup library (remove orphaned files, etc.)
     */
    fun cleanupLibrary() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                val result = vrmRepository.cleanupLibrary()
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = if (result.success) null else result.error
                    )
                }
                
                if (result.success) {
                    refreshLibrary()
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to cleanup library: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Get library health report
     */
    fun getHealthReport() {
        viewModelScope.launch {
            try {
                val report = avatarLibraryManager.validateLibraryHealth()
                _uiState.update { it.copy(healthReport = report) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = "Failed to generate health report: ${e.message}")
                }
            }
        }
    }
}

/**
 * UI state for the Avatar Library screen
 */
data class AvatarLibraryUiState(
    val avatars: List<AvatarInfo> = emptyList(),
    val stats: AvatarLibraryStats? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedAvatarId: String? = null,
    val showImportDialog: Boolean = false,
    val showRenameDialog: Boolean = false,
    val renameAvatarId: String? = null,
    val renameCurrentName: String = "",
    val healthReport: com.example.vtubercamera.data.vrm.LibraryHealthReport? = null
)