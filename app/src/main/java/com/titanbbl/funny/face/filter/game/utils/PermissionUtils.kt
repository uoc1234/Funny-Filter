package com.titanbbl.funny.face.filter.game.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.titanbbl.funny.face.filter.game.R

object PermissionUtils {
    
    const val PERMISSIONS_REQUEST_CODE = 100
    
    // Required permissions for the app
    val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    
    /**
     * Check if all required permissions are granted
     */
    fun hasRequiredPermissions(context: Context): Boolean {
        return REQUIRED_PERMISSIONS.all {
            if (it == Manifest.permission.WRITE_EXTERNAL_STORAGE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10+ we don't need WRITE_EXTERNAL_STORAGE for app-specific storage
                true
            } else {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        }
    }
    
    /**
     * Request all required permissions
     */
    fun requestRequiredPermissions(fragment: Fragment) {
        val permissionsToRequest = REQUIRED_PERMISSIONS.filter {
            if (it == Manifest.permission.WRITE_EXTERNAL_STORAGE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10+ we don't need WRITE_EXTERNAL_STORAGE for app-specific storage
                false
            } else {
                ContextCompat.checkSelfPermission(fragment.requireContext(), it) != PackageManager.PERMISSION_GRANTED
            }
        }.toTypedArray()
        
        if (permissionsToRequest.isNotEmpty()) {
            fragment.requestPermissions(permissionsToRequest, PERMISSIONS_REQUEST_CODE)
        }
    }
    
    /**
     * Request all required permissions from activity
     */
    fun requestRequiredPermissions(activity: Activity) {
        val permissionsToRequest = REQUIRED_PERMISSIONS.filter {
            if (it == Manifest.permission.WRITE_EXTERNAL_STORAGE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10+ we don't need WRITE_EXTERNAL_STORAGE for app-specific storage
                false
            } else {
                ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
            }
        }.toTypedArray()
        
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, permissionsToRequest, PERMISSIONS_REQUEST_CODE)
        }
    }
    
    /**
     * Handle permission result
     */
    fun handlePermissionsResult(requestCode: Int, grantResults: IntArray): Boolean {
        return when (requestCode) {
            PERMISSIONS_REQUEST_CODE -> {
                grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            }
            else -> false
        }
    }
    
    /**
     * Show message when permissions are denied
     */
    fun showPermissionsDeniedMessage(context: Context) {
        Toast.makeText(
            context,
            context.getString(R.string.permissions_are_required_to_use_this_feature),
            Toast.LENGTH_LONG
        ).show()
    }
} 