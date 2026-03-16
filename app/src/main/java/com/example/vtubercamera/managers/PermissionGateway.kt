package com.example.vtubercamera.managers

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.vtubercamera.utils.PermissionUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionGateway @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun hasCameraPermission(): Boolean = PermissionUtils.hasCameraPermission(context)

    fun hasMediaPermissions(): Boolean = PermissionUtils.hasMediaPermissions(context)

    fun hasPartialMediaAccess(): Boolean = PermissionUtils.hasPartialMediaAccess(context)

    fun getRequiredMediaPermissions(): Array<String> = PermissionUtils.getRequiredMediaPermissions()

    fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}
