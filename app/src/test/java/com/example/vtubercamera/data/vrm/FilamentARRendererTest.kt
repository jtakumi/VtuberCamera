package com.example.vtubercamera.data.vrm

import android.view.Surface
import com.google.ar.core.Frame
import com.google.ar.core.LightEstimate
import com.google.ar.core.Session
import com.example.vtubercamera.data.vrm.math.Transform
import java.nio.ByteBuffer
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Unit tests for FilamentARRenderer
 */
class FilamentARRendererTest {
    
    private lateinit var renderer: FilamentARRenderer
    private lateinit var vrmConverter: VRMFilamentConverter
    private lateinit var materialManager: FilamentMaterialManager
    private lateinit var textureManager: FilamentTextureManager
    private lateinit var lightingSystem: LightingSystem
    private lateinit var shadowSystem: ShadowSystem
    private lateinit var mockSurface: Surface
    private lateinit var mockSession: Session
    private lateinit var mockFrame: Frame
    private lateinit var mockLightEstimate: LightEstimate

    @Before
    fun setup() {
        vrmConverter = mock()
        materialManager = mock()
        textureManager = mock()
        lightingSystem = mock()
        shadowSystem = mock()

        whenever(lightingSystem.finalLightingParameters)
            .thenReturn(MutableStateFlow(LightingParameters()))
        doNothing().whenever(lightingSystem).resetToDefaults()
        doNothing().whenever(lightingSystem).updateEnvironmentLighting(any())
        doNothing().whenever(lightingSystem).updateLightingSettings(any())

        doNothing().whenever(shadowSystem).initialize()
        doNothing().whenever(shadowSystem).updateShadows(any(), any(), any())
        doNothing().whenever(shadowSystem).addShadowCaster(any())
        doNothing().whenever(shadowSystem).addShadowReceiver(any())
        doNothing().whenever(shadowSystem).removeShadowCaster(any())
        doNothing().whenever(shadowSystem).removeShadowReceiver(any())
        doNothing().whenever(shadowSystem).cleanup()

        whenever(vrmConverter.convertVRMToFilamentMesh(any()))
            .thenReturn(createTestFilamentMeshData())
        val testTextureInstance = createTestTextureInstance()
        whenever(textureManager.loadTexture(any())).thenReturn(testTextureInstance)
        whenever(materialManager.createMaterial(any(), any())).thenReturn(createTestMaterialInstance())
        doNothing().whenever(materialManager).updateLighting(any(), any())

        renderer = FilamentARRenderer(
            vrmConverter,
            materialManager,
            textureManager,
            lightingSystem,
            shadowSystem
        )

        mockSurface = mock()
        mockSession = mock()
        mockFrame = mock()
        mockLightEstimate = mock()
    }
    
    @Test
    fun `initialize should set renderer as initialized`() {
        // When
        renderer.initialize(mockSurface, mockSession)
        
        // Then
        assertTrue("Renderer should be initialized", renderer.isInitialized())
    }
    
    @Test
    fun `isInitialized should return false before initialization`() {
        // Then
        assertFalse("Renderer should not be initialized initially", renderer.isInitialized())
    }
    
    @Test
    fun `updateFrame should not crash when not initialized`() {
        // Given
        val avatarState = AvatarState()
        
        // When - should not throw exception
        renderer.updateFrame(mockFrame, avatarState)
        
        // Then - no exception thrown
        assertFalse("Renderer should still not be initialized", renderer.isInitialized())
    }
    
    @Test
    fun `renderAvatar should not crash when not initialized`() {
        // Given
        val vrmModel = createTestVRMModel()
        val transform = Transform.identity()
        
        // When - should not throw exception
        renderer.renderAvatar(vrmModel, transform)
        
        // Then - no exception thrown
        assertFalse("Renderer should still not be initialized", renderer.isInitialized())
    }
    
    @Test
    fun `setLighting should handle light estimate`() {
        // Given
        whenever(mockLightEstimate.pixelIntensity).thenReturn(0.8f)
        renderer.initialize(mockSurface, mockSession)
        
        // When - should not throw exception
        renderer.setLighting(mockLightEstimate)
        
        // Then - no exception thrown
        assertTrue("Renderer should remain initialized", renderer.isInitialized())
    }
    
    @Test
    fun `captureFrame should throw error when not initialized`() {
        // When & Then
        try {
            renderer.captureFrame()
            fail("Should throw ARError when not initialized")
        } catch (e: ARError.RenderingError) {
            // Expected
            assertTrue(
                "Error message should mention initialization",
                e.message.contains("not initialized")
            )
        }
    }
    
    @Test
    fun `captureFrame should return bitmap when initialized`() {
        // Given
        renderer.initialize(mockSurface, mockSession)
        renderer.setViewport(800, 600)
        
        // When
        val bitmap = renderer.captureFrame()
        
        // Then
        assertNotNull("Bitmap should not be null", bitmap)
        assertEquals("Bitmap width should match viewport", 800, bitmap.width)
        assertEquals("Bitmap height should match viewport", 600, bitmap.height)
    }
    
    @Test
    fun `setViewport should update dimensions`() {
        // Given
        renderer.initialize(mockSurface, mockSession)
        
        // When
        renderer.setViewport(1920, 1080)
        
        // Then - should not crash and should be able to capture with new dimensions
        val bitmap = renderer.captureFrame()
        assertEquals("Bitmap width should match new viewport", 1920, bitmap.width)
        assertEquals("Bitmap height should match new viewport", 1080, bitmap.height)
    }
    
    @Test
    fun `setAvatarRenderingEnabled should not crash`() {
        // When - should not throw exception
        renderer.setAvatarRenderingEnabled(true)
        renderer.setAvatarRenderingEnabled(false)
        
        // Then - no exception thrown
    }
    
    @Test
    fun `cleanup should reset initialization state`() {
        // Given
        renderer.initialize(mockSurface, mockSession)
        assertTrue("Renderer should be initialized", renderer.isInitialized())
        
        // When
        renderer.cleanup()
        
        // Then
        assertFalse("Renderer should not be initialized after cleanup", renderer.isInitialized())
    }
    
    @Test
    fun `cleanup should not crash when not initialized`() {
        // When - should not throw exception
        renderer.cleanup()

        // Then - no exception thrown
        assertFalse("Renderer should remain uninitialized", renderer.isInitialized())
    }

    private fun createTestFilamentMeshData(): FilamentMeshData {
        val mesh = FilamentMesh(
            name = "testMesh",
            vertexBuffer = ByteBuffer.allocate(12),
            indexBuffer = ByteBuffer.allocate(6),
            vertexCount = 3,
            indexCount = 3,
            attributes = VertexAttributes(
                hasPositions = true,
                hasNormals = true,
                hasUVs = true,
                hasColors = false,
                hasBoneWeights = false,
                hasBoneIndices = false
            ),
            materials = listOf("testMaterial"),
            boundingBox = null
        )

        val texture = FilamentTexture(
            name = "testTexture",
            data = ByteArray(4),
            format = TextureFormat.UNKNOWN,
            width = 1,
            height = 1,
            mipLevels = 1,
            sRGB = false
        )

        val material = FilamentMaterial(
            name = "testMaterial",
            index = 0,
            baseColorFactor = floatArrayOf(1f, 1f, 1f, 1f),
            metallicFactor = 0f,
            roughnessFactor = 1f,
            emissiveFactor = floatArrayOf(0f, 0f, 0f),
            baseColorTexture = texture.name,
            normalTexture = null,
            metallicRoughnessTexture = null,
            emissiveTexture = null,
            occlusionTexture = null,
            doubleSided = false,
            alphaMode = AlphaMode.OPAQUE,
            alphaCutoff = 0.5f
        )

        return FilamentMeshData(
            meshes = listOf(mesh),
            materials = listOf(material),
            textures = listOf(texture),
            boundingBox = null,
            totalVertices = 3,
            totalTriangles = 1
        )
    }

    private fun createTestTextureInstance(): FilamentTextureInstance {
        val texture = FilamentTexture(
            name = "testTexture",
            data = ByteArray(4),
            format = TextureFormat.UNKNOWN,
            width = 1,
            height = 1,
            mipLevels = 1,
            sRGB = false
        )

        return mock<FilamentTextureInstance>().apply {
            whenever(name).thenReturn(texture.name)
            whenever(width).thenReturn(texture.width)
            whenever(height).thenReturn(texture.height)
            whenever(mipLevels).thenReturn(texture.mipLevels)
            whenever(sRGB).thenReturn(texture.sRGB)
            whenever(originalTexture).thenReturn(texture)
        }
    }

    private fun createTestMaterialInstance(): FilamentMaterialInstance {
        val shader = FilamentShader(
            name = "testShader",
            vertexShader = "void main(){}",
            fragmentShader = "void main(){}",
            defines = emptyMap()
        )
        return FilamentMaterialInstance(
            name = "testMaterial",
            shader = shader,
            parameters = MaterialParameters()
        )
    }

    private fun createTestVRMModel(): VRMModel {
        return VRMModel(
            id = "test-avatar",
            name = "Test Avatar",
            meshData = byteArrayOf(),
            textureData = emptyMap(),
            expressions = emptyList(),
            poses = emptyList(),
            metadata = VRMMetadata(
                title = "Test Avatar",
                version = "1.0",
                author = "Test Author",
                contactInformation = "",
                reference = "",
                allowedUserName = VRMMetadata.AllowedUser.EVERYONE,
                violentUsage = VRMMetadata.Usage.DISALLOW,
                sexualUsage = VRMMetadata.Usage.DISALLOW,
                commercialUsage = VRMMetadata.Usage.ALLOW,
                otherPermissionUrl = "",
                licenseName = VRMMetadata.LicenseType.OTHER,
                otherLicenseUrl = ""
            )
        )
    }
}