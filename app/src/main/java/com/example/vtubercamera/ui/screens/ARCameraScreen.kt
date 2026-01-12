package com.example.vtubercamera.ui.screens

import android.Manifest
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vtubercamera.ui.components.AvatarTransformPanel
import com.example.vtubercamera.ui.components.ExpressionControlPanel
import com.example.vtubercamera.ui.components.ExpressionSelectionMenu
import com.example.vtubercamera.ui.components.LightingControlPanel
import com.example.vtubercamera.ui.components.PermissionRequestComponent
import com.example.vtubercamera.ui.components.PoseControlPanel
import com.example.vtubercamera.ui.components.PoseSelectionMenu
import com.example.vtubercamera.ui.modifiers.modernCameraGestures
import com.example.vtubercamera.ui.viewmodels.CameraViewModel
import com.example.vtubercamera.utils.PermissionUtils
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.vtubercamera.R
import com.example.vtubercamera.ui.ar.ARRenderOverlay

/**
 * AR Camera Screen for VTuber avatar recording and interaction
 * Provides real-time AR avatar rendering with expression and pose controls
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ARCameraScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToAvatarLibrary: () -> Unit = {},
    viewModel: CameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    LocalConfiguration.current

    // Collect state from ViewModel
    // For now, use a local state for recording since the method might not exist yet
    var isRecording by remember { mutableStateOf(false) }
    val flashMode by viewModel.flashMode.collectAsStateWithLifecycle()
    val cameraSelector by viewModel.cameraSelector.collectAsStateWithLifecycle()
    val zoomRatio by viewModel.zoomRatio.collectAsStateWithLifecycle()
    val needsCameraRebind by viewModel.needsCameraRebind.collectAsStateWithLifecycle()

    val isARMode by viewModel.isARMode.collectAsStateWithLifecycle()
    val arError by viewModel.arError.collectAsStateWithLifecycle()

    // Snackbar for AR errors with simple duplicate suppression
    val snackbarHostState = remember { SnackbarHostState() }
    var lastShownError by remember { mutableStateOf<com.example.vtubercamera.data.vrm.ARError?>(null) }
    val currentAvatar by viewModel.currentAvatar.collectAsStateWithLifecycle()
    val avatarState by viewModel.avatarState.collectAsStateWithLifecycle()
    val currentExpression by viewModel.currentExpression.collectAsStateWithLifecycle()
    val currentPose by viewModel.currentPose.collectAsStateWithLifecycle()
    val activeBlendShapes by viewModel.activeBlendShapes.collectAsStateWithLifecycle()
    val isExpressionTransitioning by viewModel.isExpressionTransitioning.collectAsStateWithLifecycle()
    val expressionTransitionProgress by viewModel.expressionTransitionProgress.collectAsStateWithLifecycle()
    val activeBoneTransforms by viewModel.activeBoneTransforms.collectAsStateWithLifecycle()
    val isPoseTransitioning by viewModel.isPoseTransitioning.collectAsStateWithLifecycle()
    val poseTransitionProgress by viewModel.poseTransitionProgress.collectAsStateWithLifecycle()

    // UI State
    var showExpressionControls by remember { mutableStateOf(false) }
    var showPoseControls by remember { mutableStateOf(false) }
    var showLightingControls by remember { mutableStateOf(false) }
    var showTransformControls by remember { mutableStateOf(false) }
    var showAvatarInfo by remember { mutableStateOf(false) }

    // Permission handling
    var hasCameraPermission by remember {
        mutableStateOf(PermissionUtils.hasCameraPermission(context))
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Camera permission required for AR mode", Toast.LENGTH_LONG)
                .show()
        }
    }

    val avatarImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.loadAvatar(uri)
        }
    }


    LaunchedEffect(Unit) {
        // Initialize AR mode
        viewModel.enableARMode(context, lifecycleOwner)

        // Request camera permission if not granted
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Handle permission request
    if (!hasCameraPermission) {
        PermissionRequestComponent(
            title = "Camera Permission Required",
            buttonText = "Grant Permission",
            onButtonClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (currentAvatar != null) {
                            "AR Camera - ${currentAvatar!!.name}"
                        } else {
                            "AR Camera"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    var showExpressionMenu by remember { mutableStateOf(false) }
                    var showPoseMenu by remember { mutableStateOf(false) }

                    if (currentAvatar != null) {
                        // Expression quick menu
                        Box {
                            IconButton(onClick = { showExpressionMenu = !showExpressionMenu }) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEmotions,
                                    contentDescription = "Select Expression"
                                )
                            }
                            ExpressionSelectionMenu(
                                expressionData = currentAvatar!!.getExpressionData(),
                                currentExpression = currentExpression,
                                expanded = showExpressionMenu,
                                onDismissRequest = { showExpressionMenu = false },
                                onExpressionSelected = { expr ->
                                    showExpressionMenu = false
                                    if (expr == null) viewModel.clearExpression() else viewModel.selectExpression(
                                        expr
                                    )
                                }
                            )
                        }

                        // Pose quick menu
                        Box {
                            IconButton(onClick = { showPoseMenu = !showPoseMenu }) {
                                Icon(
                                    imageVector = Icons.Default.Accessibility,
                                    contentDescription = "Select Pose"
                                )
                            }
                            PoseSelectionMenu(
                                poseData = currentAvatar!!.getPoseData(),
                                currentPose = currentPose,
                                expanded = showPoseMenu,
                                onDismissRequest = { showPoseMenu = false },
                                onPoseSelected = { pose ->
                                    showPoseMenu = false
                                    if (pose == null) viewModel.clearPose() else viewModel.selectPose(
                                        pose
                                    )
                                }
                            )
                        }
                    }

                    // Avatar library access
                    IconButton(onClick = onNavigateToAvatarLibrary) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Avatar Library"
                        )
                    }

                    // Avatar info toggle
                    IconButton(onClick = { showAvatarInfo = !showAvatarInfo }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Avatar Info"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Snackbar host for errors
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
            )

            // AR Camera Preview using robust CameraX host (avoids frequent unbind/rebind)
            com.example.vtubercamera.ui.camerax.CameraXPreviewHost(
                context = context,
                lifecycleOwner = lifecycleOwner,
                cameraSelector = cameraSelector,
                flashMode = flashMode,
                zoomRatio = zoomRatio,
                needsCameraRebind = needsCameraRebind,
                modifier = Modifier
                    .fillMaxSize()
                    .modernCameraGestures(
                        onScale = { newZoom ->
                            viewModel.smoothZoomTo(targetZoom = newZoom, duration = 0)
                        },
                        onDoubleTap = { viewModel.resetZoom() },
                        currentZoom = zoomRatio
                    ),
                onCameraReboundHandled = { viewModel.onCameraRebound() },
                onImageCaptureCreated = { /* not used here */ },
                onCameraCreated = { cam -> viewModel.setCamera(cam) },
                onCameraProviderChanged = { /* no-op */ },
                onPreviewViewChanged = { /* no-op */ },
                onPreviewChanged = { /* no-op */ }
            )

            // AR Rendering overlay (draws on top of camera preview)
            if (isARMode) {
                ARRenderOverlay(
                    isARMode = true,
                    modifier = Modifier.fillMaxSize(),
                    viewModel = viewModel
                )
            }

            // Show AR error in a Snackbar once, avoid rapid repeats
            LaunchedEffect(arError) {
                val error = arError ?: return@LaunchedEffect
                if (error != lastShownError) {
                    val msg = error.message ?: "AR error"
                    snackbarHostState.showSnackbar(
                        message = msg,
                        withDismissAction = true,
                        duration = SnackbarDuration.Short
                    )
                    lastShownError = error
                }
                // Clear after handling to prevent persistent UI state
                viewModel.clearARError()
            }

            // Avatar status overlay
            if (showAvatarInfo && currentAvatar != null) {
                AvatarStatusOverlay(
                    avatar = currentAvatar!!,
                    avatarState = avatarState,
                    currentExpression = currentExpression,
                    currentPose = currentPose,
                    onDismiss = { showAvatarInfo = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                )
            }

            // Main control buttons
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Expression controls
                FloatingActionButton(
                    onClick = { showExpressionControls = !showExpressionControls },
                    containerColor = if (showExpressionControls) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = "Expression Controls"
                    )
                }

                // Transform controls
                FloatingActionButton(
                    onClick = { showTransformControls = !showTransformControls },
                    containerColor = if (showTransformControls) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenWith,
                        contentDescription = "Transform Controls"
                    )
                }

                // Capture/Record button
                FloatingActionButton(
                    onClick = {
                        // Toggle recording state (placeholder implementation)
                        isRecording = !isRecording
                    },
                    containerColor = if (isRecording) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = if (isRecording) {
                            Icons.Default.Stop
                        } else {
                            Icons.Default.Videocam
                        },
                        contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Pose controls
                FloatingActionButton(
                    onClick = { showPoseControls = !showPoseControls },
                    containerColor = if (showPoseControls) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Accessibility,
                        contentDescription = "Pose Controls"
                    )
                }
            }

            // Camera controls
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Camera switch
                FloatingActionButton(
                    onClick = { viewModel.switchCamera() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera"
                    )
                }

                // Flash control
                FloatingActionButton(
                    onClick = { viewModel.toggleFlash() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = when (flashMode) {
                            ImageCapture.FLASH_MODE_OFF -> Icons.Default.FlashOff
                            ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                            ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                            else -> Icons.Default.FlashOff
                        },
                        contentDescription = "Flash Control"
                    )
                }

                // Lighting controls
                FloatingActionButton(
                    onClick = { showLightingControls = !showLightingControls },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "Lighting Controls"
                    )
                }
            }

            // Control panels
            AnimatedVisibility(
                visible = showExpressionControls && currentAvatar != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
                currentAvatar?.let { avatar ->
                    ExpressionControlPanel(
                        expressionData = avatar.getExpressionData(),
                        currentExpression = currentExpression,
                        activeBlendShapes = activeBlendShapes,
                        isTransitioning = isExpressionTransitioning,
                        transitionProgress = expressionTransitionProgress,
                        onExpressionSelected = viewModel::selectExpression,
                        onBlendShapeChanged = viewModel::setBlendShapeWeight,
                        onTransitionDurationChanged = viewModel::setExpressionTransitionDuration,
                        onClearExpression = viewModel::clearExpression,
                        modifier = Modifier
                            .width(320.dp)
                            .padding(16.dp)
                    )
                }
            }

            // Transform panel
            AnimatedVisibility(
                visible = showTransformControls && currentAvatar != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                val avatarTransform by viewModel.avatarTransform.collectAsStateWithLifecycle()
                AvatarTransformPanel(
                    transform = avatarTransform,
                    onTransformChange = viewModel::updateAvatarTransform,
                    onReset = viewModel::resetAvatarTransform,
                    modifier = Modifier
                        .width(380.dp)
                        .padding(16.dp)
                )
            }

            AnimatedVisibility(
                visible = showPoseControls && currentAvatar != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                currentAvatar?.let { avatar ->
                    PoseControlPanel(
                        poseData = avatar.getPoseData(),
                        currentPose = currentPose,
                        activeBoneTransforms = activeBoneTransforms,
                        isTransitioning = isPoseTransitioning,
                        transitionProgress = poseTransitionProgress,
                        onPoseSelected = viewModel::selectPose,
                        onBoneTransformChanged = viewModel::setBoneTransform,
                        onTransitionDurationChanged = viewModel::setPoseTransitionDuration,
                        onClearPose = viewModel::clearPose,
                        boneLocks = emptySet(),
                        onBoneLockToggled = { boneName -> viewModel.toggleBoneLock(boneName) },
                        onResetToDefaultPose = viewModel::resetToDefaultPose,
                        modifier = Modifier
                            .width(320.dp)
                            .padding(16.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = showLightingControls,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                LightingControlPanel(
                    lightingSettings = viewModel.lightingSettings.collectAsStateWithLifecycle().value,
                    lightingPresets = viewModel.lightingPresets.collectAsStateWithLifecycle().value,
                    isExpanded = true,
                    onExpandedChange = { },
                    onLightingSettingsChange = viewModel::updateLightingSettings,
                    onPresetSelected = viewModel::selectLightingPreset,
                    modifier = Modifier
                        .width(400.dp)
                        .padding(16.dp)
                )
            }

            // No avatar message
            if (currentAvatar == null) {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Avatar Loaded",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap the button below or the person icon to select an avatar",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                avatarImportLauncher.launch(
                                    arrayOf(
                                        "model/gltf-binary",
                                        "application/octet-stream",
                                        "application/vrm",
                                        "application/*"
                                    )
                                )
                            }
                        ) {
                            Text(stringResource(R.string.ar_select_vrm_file))
                        }
                    }
                }
            }
        }
    }
}

// Camera binding is provided by CameraXPreviewHost; no local binding here.

@Composable
private fun AvatarStatusOverlay(
    avatar: com.example.vtubercamera.data.vrm.VRMModel,
    avatarState: com.example.vtubercamera.data.vrm.AvatarState,
    currentExpression: com.example.vtubercamera.data.vrm.Expression?,
    currentPose: com.example.vtubercamera.data.vrm.Pose?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(280.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Avatar Status",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Name: ${avatar.name}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Visible: ${if (avatarState.isVisible) "Yes" else "No"}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Loading: ${if (avatarState.isLoading) "Yes" else "No"}",
                style = MaterialTheme.typography.bodySmall
            )

            currentExpression?.let {
                Text(
                    text = "Expression: ${it.name}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            currentPose?.let {
                Text(
                    text = "Pose: ${it.name}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text = "Expressions: ${avatar.expressions.size}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Poses: ${avatar.poses.size}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
