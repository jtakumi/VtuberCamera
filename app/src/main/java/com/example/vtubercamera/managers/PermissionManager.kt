package com.example.vtubercamera.managers

import android.content.Context
import com.example.vtubercamera.utils.PermissionUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun hasCameraPermission(): Boolean = PermissionUtils.hasCameraPermission(context)
    
    fun hasMediaPermissions(): Boolean = PermissionUtils.hasMediaPermissions(context)
    
    fun hasPartialMediaAccess(): Boolean = PermissionUtils.hasPartialMediaAccess(context)
    
    fun getRequiredMediaPermissions(): Array<String> = PermissionUtils.getRequiredMediaPermissions()
}