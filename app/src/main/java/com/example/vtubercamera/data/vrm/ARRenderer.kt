package com.example.vtubercamera.data.vrm

import android.graphics.Bitmap
import android.view.Surface
import com.google.ar.core.Frame
import com.google.ar.core.LightEstimate
import com.google.ar.core.Session
import com.example.vtubercamera.data.vrm.math.Transform

/**
 * Interface for AR rendering operations
 * Defines the contract for rendering VRM avatars in AR scenes
 */
interface ARRenderer {
    
    /**
     * Initialize the AR renderer with surface and AR session
     * @param surface The rendering surface
     * @param arSession The ARCore session
     */
    fun initialize(surface: Surface, arSession: Session)
    
    /**
     * Update the renderer with the current AR frame and avatar state
     * @param frame Current AR frame from ARCore
     * @param avatarState Current state of the avatar
     */
    fun updateFrame(frame: Frame, avatarState: AvatarState)
    
    /**
     * Render the VRM avatar with the given transform
     * @param vrmModel The VRM model to render
     * @param transform The transformation to apply to the avatar
     */
    fun renderAvatar(vrmModel: VRMModel, transform: Transform)
    
    /**
     * Set lighting information for realistic rendering
     * @param lightEstimate Light estimation from ARCore
     */
    fun setLighting(lightEstimate: LightEstimate)
    
    /**
     * Capture the current rendered frame as a bitmap
     * @return Bitmap of the current frame
     */
    fun captureFrame(): Bitmap
    
    /**
     * Clean up resources and shutdown the renderer
     */
    fun cleanup()
    
    /**
     * Check if the renderer is initialized and ready
     * @return true if initialized, false otherwise
     */
    fun isInitialized(): Boolean
    
    /**
     * Set the viewport size for rendering
     * @param width Viewport width
     * @param height Viewport height
     */
    fun setViewport(width: Int, height: Int)
    
    /**
     * Enable or disable avatar rendering
     * @param enabled true to enable avatar rendering
     */
    fun setAvatarRenderingEnabled(enabled: Boolean)
}