package com.example.vtubercamera.data.vrm

import android.graphics.Bitmap
import android.view.Surface
import com.google.ar.core.Frame
import com.google.ar.core.LightEstimate
import com.google.ar.core.Session
import com.example.vtubercamera.data.vrm.math.Transform
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for FilamentARRenderer
 */
class FilamentARRendererTest {
    
    private lateinit var renderer: FilamentARRenderer
    private lateinit var mockSurface: Surface
    private lateinit var mockSession: Session
    private lateinit var mockFrame: Frame
    private lateinit var mockLightEstimate: LightEstimate
    
    @Before
    fun setup() {
        renderer = FilamentARRenderer()
        mockSurface = mockk(relaxed = true)
        mockSession = mockk(relaxed = true)
        mockFrame = mockk(relaxed = true)
        mockLightEstimate = mockk(relaxed = true)
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
        every { mockLightEstimate.pixelIntensity } returns 0.8f
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
            assertTrue("Error message should mention initialization", 
                e.message?.contains("not initialized") == true)
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
                licenseName = VRMMetadata.License.OTHER,
                otherLicenseUrl = ""
            )
        )
    }
}