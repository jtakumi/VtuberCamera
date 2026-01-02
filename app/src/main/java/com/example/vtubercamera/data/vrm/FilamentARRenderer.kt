package com.example.vtubercamera.data.vrm

import android.graphics.Bitmap
import android.util.Log
import android.view.Surface
import android.opengl.Matrix
import com.google.android.filament.Camera
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.IndexBuffer
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.SwapChain
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.VertexBuffer
import com.example.vtubercamera.data.vrm.math.Transform
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.gltfio.MaterialProvider
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.gltfio.UbershaderLoader
import com.google.android.filament.RenderableManager
import com.google.android.filament.utils.Utils
import com.google.ar.core.Frame
import com.google.ar.core.LightEstimate
import com.google.ar.core.Session
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.graphics.createBitmap
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Filament-based AR renderer implementation for VRM avatar rendering.
 *
 * This class integrates Google's Filament 3D rendering engine with ARCore
 * to provide high-quality VRM avatar rendering in augmented reality environments.
 *
 * Key features:
 * - VRM model loading and conversion to Filament format
 * - Real-time AR tracking and rendering
 * - Dynamic lighting based on environment estimation
 * - Shadow casting and receiving
 * - Material and texture management
 * - Performance optimization with LOD and culling
 * - Memory management and resource cleanup
 *
 * The renderer follows a component-based architecture with separate managers
 * for different aspects of rendering (materials, textures, lighting, shadows).
 *
 * @param vrmConverter Service for converting VRM models to Filament format
 * @param materialManager Service for managing Filament materials
 * @param textureManager Service for managing Filament textures
 * @param lightingSystem Service for managing lighting calculations
 * @param shadowSystem Service for managing shadow rendering
 *
 * @author VTuber Camera Team
 * @since 1.0.0
 *
 * @see ARRenderer
 * @see VRMFilamentConverter
 * @see FilamentMaterialManager
 * @see LightingSystem
 * @see ShadowSystem
 */
@Singleton
class FilamentARRenderer @Inject constructor(
    private val vrmConverter: VRMFilamentConverter,
    private val materialManager: FilamentMaterialManager,
    private val textureManager: FilamentTextureManager,
    private val lightingSystem: LightingSystem,
    private val shadowSystem: ShadowSystem
) : ARRenderer {

    companion object {
        private const val TAG = "FilamentARRenderer"
    }

    // Filament engine components
    private var engine: Engine? = null
    private var scene: Scene? = null
    private var camera: Camera? = null
    private var cameraEntity: Int = 0
    private var renderer: Renderer? = null
    private var view: View? = null
    private var swapChain: SwapChain? = null
    private var assetLoader: AssetLoader? = null
    private var resourceLoader: ResourceLoader? = null
    private var filamentAsset: FilamentAsset? = null
    private var materialProvider: MaterialProvider? = null

    // AR and rendering state
    private var isInitialized = false
    private var surface: Surface? = null
    private var arSession: Session? = null
    private var viewportWidth = 0
    private var viewportHeight = 0
    private var avatarRenderingEnabled = true
    private var currentLightEstimate: LightEstimate? = null

    // Avatar rendering state
    private var currentAvatarState: AvatarState? = null
    private var loadedVRMModel: VRMModel? = null
    private var filamentMeshData: FilamentMeshData? = null
    private var materialInstances = mutableMapOf<String, FilamentMaterialInstance>()
    private var textureInstances = mutableMapOf<String, FilamentTextureInstance>()
    private var renderableEntities = mutableListOf<FilamentRenderable>()

    /**
     * Initializes the Filament AR renderer with the provided surface and AR session.
     *
     * This method sets up the complete rendering pipeline including:
     * - Filament engine initialization
     * - Scene and camera setup
     * - Lighting and shadow system initialization
     * - AR session integration
     *
     * Must be called before any rendering operations can be performed.
     *
     * @param surface The rendering surface (typically from a SurfaceView or TextureView)
     * @param arSession The ARCore session for AR tracking and environment data
     *
     * @throws ARError.RenderingError if initialization fails
     */
    override fun initialize(surface: Surface, arSession: Session) {
        Log.d(TAG, "Initializing FilamentARRenderer")

        try {
            val previousSurface = this.surface
            this.surface = surface
            this.arSession = arSession

            // Initialize lighting and shadow systems
            lightingSystem.resetToDefaults()
            shadowSystem.initialize()

            val filamentReady = initializeOrUpdateFilament(surface, previousSurface)
            isInitialized = filamentReady

            if (filamentReady) {
                Log.d(TAG, "FilamentARRenderer initialized successfully")
            } else {
                Log.w(
                    TAG,
                    "Filament not initialized (invalid Surface or Filament unavailable); renderer will remain uninitialized"
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize FilamentARRenderer", e)
            isInitialized = false
            throw ARError.RenderingError("Failed to initialize Filament renderer: ${e.message}")
        }
    }

    /**
     * Updates the rendering frame with new AR data and avatar state.
     *
     * This method is called for each frame and performs:
     * - AR camera pose updates from the frame
     * - Avatar model loading if changed
     * - Transform application (position, rotation, scale)
     * - Expression and pose updates
     * - Lighting updates based on environment
     * - Scene rendering
     *
     * Should be called from the main rendering loop at the target frame rate.
     *
     * @param frame The current AR frame containing camera and tracking data
     * @param avatarState The current state of the avatar including transform and animations
     */
    override fun updateFrame(frame: Frame, avatarState: AvatarState) {
        if (!isInitialized) {
            Log.w(TAG, "Renderer not initialized, skipping frame update")
            return
        }

        try {
            currentAvatarState = avatarState

            // TODO: Update AR camera with frame data
            // updateARCamera(frame)

            // Update avatar rendering if state changed
            if (avatarState.shouldRender && avatarState.model != loadedVRMModel) {
                avatarState.model?.let { model ->
                    loadVRMModelToScene(model)
                }
            }

            // Apply avatar transform
            applyAvatarTransform(avatarState.transform)

            // Apply expression and pose
            applyExpression(avatarState.currentExpression)
            applyPose(avatarState.currentPose)

            // Update material lighting
            updateMaterialLighting()

            // Minimal render path (black screen OK)
            renderScene(frame.timestamp)

        } catch (e: Exception) {
            Log.e(TAG, "Error updating frame", e)
        }
    }

    override fun renderAvatar(vrmModel: VRMModel, transform: Transform) {
        if (!isInitialized) {
            Log.w(TAG, "Renderer not initialized, skipping avatar render")
            return
        }

        try {
            Log.d(TAG, "Rendering avatar: ${vrmModel.name}")

            // Load VRM model into Filament scene if not already loaded
            if (loadedVRMModel?.id != vrmModel.id) {
                loadVRMModelToScene(vrmModel)
                loadedVRMModel = vrmModel
            }

            // Apply transform to avatar
            applyAvatarTransform(transform)

            // Update materials with current lighting
            updateMaterialLighting()

        } catch (e: Exception) {
            Log.e(TAG, "Error rendering avatar", e)
            throw ARError.RenderingError("Failed to render avatar: ${e.message}")
        }
    }

    override fun setLighting(lightEstimate: LightEstimate) {
        currentLightEstimate = lightEstimate

        if (!isInitialized) return

        try {
            // Update lighting system with environment lighting
            lightingSystem.updateEnvironmentLighting(lightEstimate)

            // Update shadow system with new lighting
            val cameraPosition = floatArrayOf(0f, 0f, 5f)
            val cameraTarget = floatArrayOf(0f, 0f, 0f)
            shadowSystem.updateShadows(
                lightingSystem.finalLightingParameters.value,
                cameraPosition,
                cameraTarget
            )

            // TODO: Apply lighting to Filament scene
            // updateSceneLighting(lightEstimate)

            Log.d(TAG, "Updated lighting - Intensity: ${lightEstimate.pixelIntensity}")

        } catch (e: Exception) {
            Log.e(TAG, "Error setting lighting", e)
        }
    }

    override fun captureFrame(): Bitmap {
        if (!isInitialized) {
            throw ARError.RenderingError("Renderer not initialized")
        }
        if (viewportWidth <= 0 || viewportHeight <= 0) {
            throw ARError.RenderingError(
                "Viewport not set (width=$viewportWidth, height=$viewportHeight)"
            )
        }

        try {
            // TODO: Capture frame from Filament renderer
            // return captureFilamentFrame()

            // Placeholder implementation
            Log.d(TAG, "Capturing frame (placeholder)")
            return createBitmap(viewportWidth, viewportHeight)

        } catch (e: Exception) {
            Log.e(TAG, "Error capturing frame", e)
            throw ARError.RenderingError("Failed to capture frame: ${e.message}")
        }
    }

    override fun cleanup() {
        Log.d(TAG, "Cleaning up FilamentARRenderer")

        try {
            // Clear current model resources
            clearCurrentModel()

            // Cleanup lighting and shadow systems
            shadowSystem.cleanup()

            // Clear texture cache
            textureManager.clearCache()

            // Clear material cache
            materialManager.clearCache()

            cleanupFilamentEngine()
            assetLoader = null
            resourceLoader = null
            materialProvider = null

            surface = null
            arSession = null
            currentAvatarState = null
            loadedVRMModel = null
            currentLightEstimate = null
            filamentMeshData = null
            textureInstances.clear()
            isInitialized = false

            Log.d(TAG, "FilamentARRenderer cleanup completed")

        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }

    override fun isInitialized(): Boolean = isInitialized

    override fun setViewport(width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height

        if (!isInitialized) return

        try {
            updateFilamentViewport(width, height)

            Log.d(TAG, "Viewport updated: ${width}x${height}")

        } catch (e: Exception) {
            Log.e(TAG, "Error setting viewport", e)
        }
    }

    private fun initializeOrUpdateFilament(surface: Surface, previousSurface: Surface?): Boolean {
        // In JVM unit tests (and other headless scenarios), `Surface` is commonly mocked and
        // `surface.isValid` may be false. Also, Filament may not be available (native libs).
        // We treat these cases as "not initialized" so callers won't assume render backend exists.
        if (!surface.isValid) {
            Log.w(TAG, "Surface is invalid; skipping Filament initialization")
            return false
        }

        return try {
            val engine = engine ?: Engine.create().also { created ->
                Utils.init()
                this.engine = created
                this.renderer = created.createRenderer()
                this.scene = created.createScene()
                this.view = created.createView()
                this.cameraEntity = EntityManager.get().create()
                this.camera = created.createCamera(cameraEntity)

                // Initialize glTF I/O helpers
                ensureGltfioLoaders(created)

                // Wire up the view
                this.view?.scene = this.scene
                this.view?.camera = this.camera
            }

            // SwapChain is tied to Surface; recreate if surface changes (rotation / resume)
            if (swapChain == null || previousSurface !== surface) {
                swapChain?.let {
                    try {
                        engine.destroySwapChain(it)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to destroy old SwapChain", e)
                    }
                }
                swapChain = engine.createSwapChain(surface)
            }

            // Apply current viewport if already known
            if (viewportWidth > 0 && viewportHeight > 0) {
                updateFilamentViewport(viewportWidth, viewportHeight)
            }

            // Consider backend ready only if core components exist
            engine.let { ensureGltfioLoaders(it) }

            this.engine != null && this.renderer != null && this.view != null && this.swapChain != null
        } catch (t: Throwable) {
            Log.w(TAG, "Filament not available; running in headless mode", t)
            false
        }
    }

    private fun ensureGltfioLoaders(engine: Engine) {
        if (materialProvider == null) {
            materialProvider = UbershaderLoader(engine)
        }
        if (assetLoader == null) {
            assetLoader = AssetLoader(engine, materialProvider!!, EntityManager.get())
        }
        if (resourceLoader == null) {
            resourceLoader = ResourceLoader(engine)
        }
    }

    private fun updateFilamentViewport(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        view?.viewport = Viewport(0, 0, width, height)
    }

    private fun renderScene(frameTimeNanos: Long) {
        val renderer = renderer ?: return
        val swapChain = swapChain ?: return
        val view = view ?: return

        try {
            val timeNanos = if (frameTimeNanos > 0L) frameTimeNanos else System.nanoTime()
            if (renderer.beginFrame(swapChain, timeNanos)) {
                renderer.render(view)
                renderer.endFrame()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Render failed", e)
        }
    }

    private fun cleanupFilamentEngine() {
        val engine = engine
        if (engine == null) {
            swapChain = null
            view = null
            scene = null
            camera = null
            renderer = null
            cameraEntity = 0
            return
        }

        // Destroy order: SwapChain -> View/Scene/Camera -> Renderer -> Engine
        swapChain?.let {
            try {
                engine.destroySwapChain(it)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to destroy SwapChain", e)
            }
        }
        swapChain = null

        view?.let {
            try {
                engine.destroyView(it)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to destroy View", e)
            }
        }
        view = null

        scene?.let {
            try {
                engine.destroyScene(it)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to destroy Scene", e)
            }
        }
        scene = null

        if (cameraEntity != 0) {
            try {
                engine.destroyCameraComponent(cameraEntity)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to destroy Camera component", e)
            }
            try {
                EntityManager.get().destroy(cameraEntity)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to destroy Camera entity", e)
            }
        }
        cameraEntity = 0
        camera = null

        renderer?.let {
            try {
                engine.destroyRenderer(it)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to destroy Renderer", e)
            }
        }
        renderer = null

        try {
            engine.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to destroy Engine", e)
        }
        this.engine = null
    }

    override fun setAvatarRenderingEnabled(enabled: Boolean) {
        avatarRenderingEnabled = enabled
        Log.d(TAG, "Avatar rendering enabled: $enabled")
    }

    // Private helper methods for VRM Filament integration

    /**
     * Load VRM model into Filament scene
     */
    private fun loadVRMModelToScene(vrmModel: VRMModel) {
        Log.d(TAG, "Loading VRM model to Filament scene: ${vrmModel.name}")

        try {
            // Clear previous model
            clearCurrentModel()

            val gltfLoaded = loadAssetWithGltfio(vrmModel)

            if (!gltfLoaded) {
                // Convert VRM to Filament format
                filamentMeshData = vrmConverter.convertVRMToFilamentMesh(vrmModel)

                // Load textures
                loadTextures(filamentMeshData!!.textures)

                // Create materials
                createMaterials(filamentMeshData!!.materials)

                // Create renderables
                createRenderables(filamentMeshData!!.meshes)
            }

            loadedVRMModel = vrmModel

            Log.d(TAG, "Successfully loaded VRM model: ${vrmModel.name}")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to load VRM model to scene", e)
            throw ARError.RenderingError("Failed to load VRM model: ${e.message}")
        }
    }

    private fun loadAssetWithGltfio(vrmModel: VRMModel): Boolean {
        val engine = engine ?: return false
        val scene = scene ?: return false

        if (vrmModel.meshData.isEmpty()) {
            Log.w(TAG, "VRM mesh data is empty; skipping glTF loading")
            return false
        }

        return try {
            ensureGltfioLoaders(engine)

            val buffer = ByteBuffer.allocateDirect(vrmModel.meshData.size)
                .order(ByteOrder.nativeOrder())
            buffer.put(vrmModel.meshData)
            buffer.flip()

            val asset = assetLoader?.createAssetFromBinary(buffer)
                ?: return false

            resourceLoader?.loadResources(asset)
            engine.flushAndWait()
            resourceLoader?.destroyResourceData()

            scene.addEntities(asset.entities)
            filamentAsset = asset

            val transform = currentAvatarState?.transform ?: Transform.identity()
            applyAvatarTransform(transform)

            renderableEntities.clear()
            asset.entities.forEachIndexed { index, entity ->
                renderableEntities.add(
                    FilamentRenderable(
                        name = "${vrmModel.name}_$index",
                        mesh = FilamentMesh(
                            name = "${vrmModel.name}_mesh_$index",
                            vertexBuffer = ByteBuffer.allocateDirect(0),
                            indexBuffer = ByteBuffer.allocateDirect(0),
                            vertexCount = 0,
                            indexCount = 0,
                            attributes = VertexAttributes(
                                hasPositions = true,
                                hasNormals = false,
                                hasUVs = false,
                                hasColors = false,
                                hasBoneWeights = false,
                                hasBoneIndices = false
                            ),
                            materials = emptyList(),
                            boundingBox = vrmModel.boundingBox
                        ),
                        materials = emptyList(),
                        transform = transform,
                        visible = true,
                        entity = entity
                    )
                )
            }

            renderableEntities.forEach { renderable ->
                shadowSystem.addShadowCaster(renderable)
                shadowSystem.addShadowReceiver(renderable)
            }

            true
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to load VRM with gltfio, falling back", t)
            false
        }
    }

    /**
     * Load textures for the VRM model
     */
    private fun loadTextures(textures: List<FilamentTexture>) {
        Log.d(TAG, "Loading ${textures.size} textures")

        textures.forEach { texture ->
            try {
                val textureInstance = textureManager.loadTexture(texture)
                textureInstances[texture.name] = textureInstance
                Log.d(TAG, "Loaded texture: ${texture.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load texture: ${texture.name}", e)
            }
        }
    }

    /**
     * Create materials for the VRM model
     */
    private fun createMaterials(materials: List<FilamentMaterial>) {
        Log.d(TAG, "Creating ${materials.size} materials")

        materials.forEach { material ->
            try {
                // Convert FilamentTextureInstance map to FilamentTexture map
                val textureMap = textureInstances.mapValues { (_, instance) ->
                    instance.originalTexture ?: FilamentTexture(
                        name = instance.name,
                        data = ByteArray(0),
                        format = TextureFormat.UNKNOWN,
                        width = instance.width,
                        height = instance.height,
                        mipLevels = instance.mipLevels,
                        sRGB = instance.sRGB
                    )
                }
                val materialInstance = materialManager.createMaterial(material, textureMap)
                materialInstances[material.name] = materialInstance
                Log.d(TAG, "Created material: ${material.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create material: ${material.name}", e)
            }
        }
    }

    /**
     * Create renderables from meshes
     */
    private fun createRenderables(meshes: List<FilamentMesh>) {
        Log.d(TAG, "Creating ${meshes.size} renderables")

        meshes.forEach { mesh ->
            try {
                val renderable = createRenderable(mesh)
                renderableEntities.add(renderable)

                // Add to shadow system
                shadowSystem.addShadowCaster(renderable)
                shadowSystem.addShadowReceiver(renderable)

                Log.d(TAG, "Created renderable: ${mesh.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create renderable: ${mesh.name}", e)
            }
        }
    }

    /**
     * Create a single renderable from mesh
     */
    private fun createRenderable(mesh: FilamentMesh): FilamentRenderable {
        // TODO: Create actual Filament renderable when dependencies are available
        // This would involve:
        // 1. Creating vertex buffer from mesh.vertexBuffer
        // 2. Creating index buffer from mesh.indexBuffer
        // 3. Setting up vertex attributes
        // 4. Assigning materials
        // 5. Creating entity and adding to scene

        return FilamentRenderable(
            name = mesh.name,
            mesh = mesh,
            materials = mesh.materials.mapNotNull { materialName ->
                materialInstances[materialName]
            },
            transform = Transform.identity(),
            visible = true
        )
    }

    /**
     * Apply transform to avatar
     */
    private fun applyAvatarTransform(transform: Transform) {
        filamentAsset?.let { asset ->
            applyTransformToEntity(asset.root, transform)
        }

        if (renderableEntities.isEmpty()) return

        Log.d(TAG, "Applying transform to ${renderableEntities.size} renderables")

        renderableEntities.forEach { renderable ->
            renderable.transform = transform
            renderable.entity?.let { entity ->
                applyTransformToEntity(entity, transform)
            }
        }
    }

    private fun applyTransformToEntity(entity: Int, transform: Transform) {
        val engine = engine ?: return
        val transformManager = engine.transformManager
        val instance = transformManager.getInstance(entity)
        if (instance == 0) return

        val matrix = createTransformMatrix(transform)
        transformManager.setTransform(instance, matrix)
    }

    private fun createTransformMatrix(transform: Transform): FloatArray {
        val matrix = FloatArray(16)
        Matrix.setIdentityM(matrix, 0)

        val rotationMatrix = quaternionToMatrix(transform.rotation)
        Matrix.multiplyMM(matrix, 0, matrix, 0, rotationMatrix, 0)
        Matrix.scaleM(matrix, 0, transform.scale.x, transform.scale.y, transform.scale.z)

        matrix[12] = transform.position.x
        matrix[13] = transform.position.y
        matrix[14] = transform.position.z

        return matrix
    }

    private fun quaternionToMatrix(rotation: com.example.vtubercamera.data.vrm.math.Quaternion): FloatArray {
        val matrix = FloatArray(16)
        val x = rotation.x
        val y = rotation.y
        val z = rotation.z
        val w = rotation.w

        val xx = x * x
        val yy = y * y
        val zz = z * z
        val xy = x * y
        val xz = x * z
        val yz = y * z
        val wx = w * x
        val wy = w * y
        val wz = w * z

        matrix[0] = 1f - 2f * (yy + zz)
        matrix[1] = 2f * (xy + wz)
        matrix[2] = 2f * (xz - wy)
        matrix[3] = 0f

        matrix[4] = 2f * (xy - wz)
        matrix[5] = 1f - 2f * (xx + zz)
        matrix[6] = 2f * (yz + wx)
        matrix[7] = 0f

        matrix[8] = 2f * (xz + wy)
        matrix[9] = 2f * (yz - wx)
        matrix[10] = 1f - 2f * (xx + yy)
        matrix[11] = 0f

        matrix[12] = 0f
        matrix[13] = 0f
        matrix[14] = 0f
        matrix[15] = 1f

        return matrix
    }

    /**
     * Update material lighting parameters
     */
    private fun updateMaterialLighting() {
        // Use lighting system's final parameters instead of creating our own
        val lightingParams = lightingSystem.finalLightingParameters.value

        materialInstances.values.forEach { materialInstance ->
            materialManager.updateLighting(materialInstance, lightingParams)
        }
    }

    /**
     * Create lighting parameters from AR light estimate
     */
    private fun createLightingParameters(lightEstimate: LightEstimate): LightingParameters {
        val intensity = lightEstimate.pixelIntensity

        return LightingParameters(
            lightDirection = floatArrayOf(0f, -1f, 0f),
            lightColor = floatArrayOf(intensity, intensity, intensity),
            lightIntensity = intensity,
            ambientColor = floatArrayOf(intensity * 0.2f, intensity * 0.2f, intensity * 0.2f),
            cameraPosition = floatArrayOf(0f, 0f, 5f)
        )
    }

    /**
     * Clear current model resources
     */
    private fun clearCurrentModel() {
        Log.d(TAG, "Clearing current model resources")

        // Remove from shadow system
        renderableEntities.forEach { renderable ->
            shadowSystem.removeShadowCaster(renderable)
            shadowSystem.removeShadowReceiver(renderable)
            destroyRenderable(renderable)
        }

        filamentAsset?.let { asset ->
            scene?.removeEntities(asset.entities)
            assetLoader?.destroyAsset(asset)
            resourceLoader?.destroyResourceData()
        }
        filamentAsset = null

        renderableEntities.clear()
        materialInstances.clear()
        textureInstances.clear()
        textureManager.clearCache()

        filamentMeshData = null
    }

    private fun destroyRenderable(renderable: FilamentRenderable) {
        val engine = engine ?: return
        renderable.entity?.let { entity ->
            scene?.removeEntity(entity)
            try {
                EntityManager.get().destroy(entity)
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to destroy entity ${renderable.name}", t)
            }
        }
        renderable.vertexBuffer?.let { buffer ->
            try {
                engine.destroyVertexBuffer(buffer)
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to destroy vertex buffer for ${renderable.name}", t)
            }
        }
        renderable.indexBuffer?.let { buffer ->
            try {
                engine.destroyIndexBuffer(buffer)
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to destroy index buffer for ${renderable.name}", t)
            }
        }
    }

    /**
     * Apply expression to avatar
     */
    private fun applyExpression(expression: Expression?) {
        if (expression == null) return

        Log.d(TAG, "Applying expression: ${expression.name}")

        // TODO: Apply blend shapes to mesh
        // This would involve updating vertex positions based on blend shape weights
        expression.blendShapeKeys.forEach { (_, _) ->
            // applyBlendShape(shapeName, weight)
        }
    }

    /**
     * Apply pose to avatar
     */
    private fun applyPose(pose: Pose?) {
        if (pose == null) return

        Log.d(TAG, "Applying pose: ${pose.name}")

        // TODO: Apply bone transforms
        // This would involve updating bone matrices for skeletal animation
        pose.boneTransforms.forEach { (_, _) ->
            // applyBoneTransform(boneName, boneTransform)
        }
    }

    /**
     * Get rendering statistics
     */
    fun getRenderingStatistics(): RenderingStatistics {
        val meshData = filamentMeshData
        return RenderingStatistics(
            loadedModel = loadedVRMModel?.name,
            meshCount = meshData?.meshes?.size ?: 0,
            materialCount = materialInstances.size,
            textureCount = textureInstances.size,
            renderableCount = renderableEntities.size,
            totalVertices = meshData?.totalVertices ?: 0,
            totalTriangles = meshData?.totalTriangles ?: 0,
            textureMemoryUsage = textureInstances.values.sumOf { it.getMemoryUsage() }
        )
    }

    /**
     * Get lighting system for external access
     */
    fun getLightingSystem(): LightingSystem = lightingSystem

    /**
     * Get shadow system for external access
     */
    fun getShadowSystem(): ShadowSystem = shadowSystem

    /**
     * Update lighting settings
     */
    fun updateLightingSettings(settings: LightingSettings) {
        lightingSystem.updateLightingSettings(settings)
        updateMaterialLighting()
    }

    /**
     * Set shadow quality
     */
    fun setShadowQuality(quality: ShadowQuality) {
        shadowSystem.setShadowQuality(quality)
    }

}

/**

 * Filament renderable entity
 */
data class FilamentRenderable(
    val name: String,
    val mesh: FilamentMesh,
    val materials: List<FilamentMaterialInstance>,
    var transform: Transform,
    var visible: Boolean,
    val entity: Int? = null,
    val vertexBuffer: VertexBuffer? = null,
    val indexBuffer: IndexBuffer? = null
) {
    fun isValid(): Boolean = materials.isNotEmpty()
}

/**
 * Rendering statistics
 */
data class RenderingStatistics(
    val loadedModel: String?,
    val meshCount: Int,
    val materialCount: Int,
    val textureCount: Int,
    val renderableCount: Int,
    val totalVertices: Int,
    val totalTriangles: Int,
    val textureMemoryUsage: Long
) {
    fun getFormattedMemoryUsage(): String {
        return when {
            textureMemoryUsage < 1024 -> "${textureMemoryUsage}B"
            textureMemoryUsage < 1024 * 1024 -> "${textureMemoryUsage / 1024}KB"
            textureMemoryUsage < 1024 * 1024 * 1024 -> "${textureMemoryUsage / (1024 * 1024)}MB"
            else -> "${textureMemoryUsage / (1024 * 1024 * 1024)}GB"
        }
    }

    fun getSummary(): String {
        return "Model: $loadedModel, Meshes: $meshCount, Materials: $materialCount, " +
                "Textures: $textureCount, Vertices: $totalVertices, Memory: ${getFormattedMemoryUsage()}"
    }
}