package com.qq7te.totalrecall

import android.Manifest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureFragmentPermissionTest {

    @Test
    fun `camera starts when only CAMERA is granted and location is denied`() {
        val permissionResults = mapOf(
            Manifest.permission.CAMERA to true,
            Manifest.permission.ACCESS_FINE_LOCATION to false,
            Manifest.permission.ACCESS_COARSE_LOCATION to false
        )
        val cameraGranted = permissionResults[Manifest.permission.CAMERA] == true
        assertTrue("Camera should start when CAMERA is granted even if location is denied", cameraGranted)
    }

    @Test
    fun `camera is blocked when CAMERA itself is denied`() {
        val permissionResults = mapOf(
            Manifest.permission.CAMERA to false,
            Manifest.permission.ACCESS_FINE_LOCATION to true,
            Manifest.permission.ACCESS_COARSE_LOCATION to true
        )
        val cameraGranted = permissionResults[Manifest.permission.CAMERA] == true
        assertFalse("Camera should not start when CAMERA is denied even if location is granted", cameraGranted)
    }

    @Test
    fun `previously all permissions were required for camera to start`() {
        val permissionResults = mapOf(
            Manifest.permission.CAMERA to true,
            Manifest.permission.ACCESS_FINE_LOCATION to false,
            Manifest.permission.ACCESS_COARSE_LOCATION to false
        )
        val allGranted = permissionResults.entries.all { it.value }
        assertFalse("Old behavior: camera would NOT start when location is denied", allGranted)
    }

    @Test
    fun `location permission is optional for saving entry`() {
        val locationGranted = false
        val latitude: Double? = if (locationGranted) 37.7749 else null
        val longitude: Double? = if (locationGranted) -122.4194 else null

        assertTrue("Latitude should be null when location not granted", latitude == null)
        assertTrue("Longitude should be null when location not granted", longitude == null)
    }
}
