package com.example.vtubercamera.data.vrm

import com.example.vtubercamera.data.vrm.math.Transform
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for ShadowSystem
 */
class ShadowSystemTest {
    
    private lateinit var shadowSystem: ShadowSystem
    private lateinit var mockRenderable: FilamentRenderable
    
    @Before
    fun setup() {
        shadowSystem = ShadowSystem()
        
        // Create mock renderable
        val mockMesh = FilamentMesh(
            name = "test_mesh",
            vertexBuffer = ByteArray(0),
            indexBuffer = ByteArray(0),
            vertexCount = 0,
            indexCount = 0,
            materials = listOf("test_material")
        )
        
        mockRenderable = FilamentRenderable(
            name = "test_renderable",
            mesh = mockMesh,
            materials = emptyList(),
            transform = Transform.identity(),
            visible = true
        )
    }
    
    @Test
    fun `initialize should set up shadow system`() {
        shadowSystem.initialize()
        // Test passes if no exception is thrown
    }
    
    @Test
    fun `addShadowCaster should add renderable to shadow casters`() {
        shadowSystem.initialize()
        shadowSystem.addShadowCaster(mockRenderable)
        
        // Verify by trying to remove it (should not throw)
        shadowSystem.removeShadowCaster(mockRenderable)
    }
    
    @Test
    fun `addShadowReceiver should add renderable to shadow receivers`() {
        shadowSystem.initialize()
        shadowSystem.addShadowReceiver(mockRenderable)
        
        // Verify by trying to remove it (should not throw)
        shadowSystem.removeShadowReceiver(mockRenderable)
    }
    
    @Test
    fun `setShadowQuality should update shadow map size`() {
        shadowSystem.initialize()
        
        shadowSystem.setShadowQuality(ShadowQuality.HIGH)
        // Test passes if no exception is thrown
        
        shadowSystem.setShadowQuality(ShadowQuality.LOW)
        // Test passes if no exception is thrown
    }
    
    @Test
    fun `setSoftShadowsEnabled should update soft shadow setting`() {
        shadowSystem.initialize()
        
        shadowSystem.setSoftShadowsEnabled(true)
        shadowSystem.setSoftShadowsEnabled(false)
        // Test passes if no exception is thrown
    }
    
    @Test
    fun `getLightSpaceMatrix should return valid matrix`() {
        shadowSystem.initialize()
        
        val lightingParams = LightingParameters(
            lightDirection = floatArrayOf(0f, -1f, 0f),
            lightColor = floatArrayOf(1f, 1f, 1f),
            lightIntensity = 1f,
            ambientColor = floatArrayOf(0.2f, 0.2f, 0.2f),
            cameraPosition = floatArrayOf(0f, 0f, 5f)
        )
        
        val cameraPosition = floatArrayOf(0f, 0f, 5f)
        val cameraTarget = floatArrayOf(0f, 0f, 0f)
        
        shadowSystem.updateShadows(lightingParams, cameraPosition, cameraTarget)
        
        val lightSpaceMatrix = shadowSystem.getLightSpaceMatrix()
        assertNotNull(lightSpaceMatrix)
        assertEquals(16, lightSpaceMatrix.size)
    }
    
    @Test
    fun `getShadowShaderAdditions should return shader code`() {
        val shaderAdditions = shadowSystem.getShadowShaderAdditions()
        assertNotNull(shaderAdditions)
        assertTrue(shaderAdditions.contains("calculateShadow"))
        assertTrue(shaderAdditions.contains("shadowMap"))
    }
    
    @Test
    fun `clearShadowObjects should remove all shadow objects`() {
        shadowSystem.initialize()
        
        shadowSystem.addShadowCaster(mockRenderable)
        shadowSystem.addShadowReceiver(mockRenderable)
        
        shadowSystem.clearShadowObjects()
        
        // Test passes if no exception is thrown
    }
    
    @Test
    fun `cleanup should clean up resources`() {
        shadowSystem.initialize()
        shadowSystem.addShadowCaster(mockRenderable)
        
        shadowSystem.cleanup()
        
        // Test passes if no exception is thrown
    }
    
    @Test
    fun `updateShadows should handle lighting parameters`() {
        shadowSystem.initialize()
        
        val lightingParams = LightingParameters(
            lightDirection = floatArrayOf(1f, -1f, 0f),
            lightColor = floatArrayOf(0.8f, 0.8f, 1f),
            lightIntensity = 1.2f,
            ambientColor = floatArrayOf(0.3f, 0.3f, 0.3f),
            cameraPosition = floatArrayOf(2f, 2f, 5f)
        )
        
        val cameraPosition = floatArrayOf(2f, 2f, 5f)
        val cameraTarget = floatArrayOf(0f, 0f, 0f)
        
        shadowSystem.updateShadows(lightingParams, cameraPosition, cameraTarget)
        
        // Test passes if no exception is thrown
    }
}