package com.manichord.mgit.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import java.io.File

// Courtesy of VLC
// ref: https://github.com/videolan/vlc-android/commit/62897067a0fcaf02140deeafb1f93cb7c90c9fc8#diff-8c75f7d01dd35f6b14dee21e963e99911b5c45c5c5629f74dced6e16336689f1
class PermissionsHelper {

    companion object {
        fun isExternalStorageManager(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                Environment.isExternalStorageManager()

        fun canReadStorage(context: Context): Boolean {
            return Build.VERSION.SDK_INT <= Build.VERSION_CODES.M ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED || isExternalStorageManager()
        }

        /**
         * Returns true if writing to [path] on this device requires MANAGE_EXTERNAL_STORAGE.
         * Paths inside the app-specific external or internal directories are always writable
         * without any special permission.
         */
        fun requiresFullStoragePermission(path: String, context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
            return try {
                val target = File(path).canonicalPath
                val appExternal = context.getExternalFilesDir(null)?.canonicalPath
                val appInternal = context.filesDir.canonicalPath
                fun isInside(parent: String) = target == parent || target.startsWith(parent + "/")
                if (appExternal != null && isInside(appExternal)) return false
                if (isInside(appInternal)) return false
                true
            } catch (e: java.io.IOException) {
                true
            }
        }
    }
}




