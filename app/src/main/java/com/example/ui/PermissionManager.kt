package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * PermissionManager helper for Camera and Gallery/Document media access.
 * Complies with modern Android privacy standards:
 * - Camera uses android.permission.CAMERA
 * - Media selection on modern Android uses the zero-permission Photo Picker
 *   (or READ_MEDIA_IMAGES / READ_EXTERNAL_STORAGE fallback on older APIs).
 */
object PermissionManager {

  const val CAMERA_PERMISSION = Manifest.permission.CAMERA

  /**
   * Checks if CAMERA permission is granted.
   */
  fun hasCameraPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
      context,
      CAMERA_PERMISSION
    ) == PackageManager.PERMISSION_GRANTED
  }

  /**
   * Checks if Media/Gallery permission is granted for reading local images.
   * On Android 13+ (API 33+), uses READ_MEDIA_IMAGES.
   * On Android 12 and below, uses READ_EXTERNAL_STORAGE.
   */
  fun hasGalleryPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_MEDIA_IMAGES
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_EXTERNAL_STORAGE
      ) == PackageManager.PERMISSION_GRANTED
    }
  }

  /**
   * Returns the appropriate gallery permission string based on API version.
   */
  fun getGalleryPermission(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      Manifest.permission.READ_MEDIA_IMAGES
    } else {
      Manifest.permission.READ_EXTERNAL_STORAGE
    }
  }
}
