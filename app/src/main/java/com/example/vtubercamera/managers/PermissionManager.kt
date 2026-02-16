package com.example.vtubercamera.managers

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionManager @Inject constructor(
    private val permissionGateway: PermissionGateway,
) {
    fun hasCameraPermission(): Boolean = permissionGateway.hasCameraPermission()

    fun hasMediaPermissions(): Boolean = permissionGateway.hasMediaPermissions()

    fun hasPartialMediaAccess(): Boolean = permissionGateway.hasPartialMediaAccess()

    fun getRequiredMediaPermissions(): Array<String> = permissionGateway.getRequiredMediaPermissions()
}
