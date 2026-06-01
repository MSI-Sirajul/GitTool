package com.msi.gittool.security

import android.os.Debug
import java.io.File

object NativeSecurity {
    private const val isNativeLoaded = false

    fun isRooted(): Boolean {
        // Pure Kotlin fallback root checks
        val paths = arrayOf(
            "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su",
            "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
            "/system/bin/failsafe/su", "/data/local/su", "/su/bin/su", "/vendor/bin/su"
        )
        for (path in paths) {
            try {
                if (File(path).exists()) return true
            } catch (e: Throwable) {}
        }
        val tags = android.os.Build.TAGS
        if (tags != null && tags.contains("test-keys")) return true
        return false
    }

    fun isEmulator(): Boolean {
        // Pure Kotlin fallback emulator checks
        val fingerprint = android.os.Build.FINGERPRINT ?: ""
        val model = android.os.Build.MODEL ?: ""
        val hardware = android.os.Build.HARDWARE ?: ""
        return fingerprint.startsWith("generic") ||
                fingerprint.startsWith("unknown") ||
                model.contains("google_sdk") ||
                model.contains("Emulator") ||
                model.contains("Android SDK built for x86") ||
                hardware.contains("goldfish") ||
                hardware.contains("ranchu")
    }

    fun isDebuggerAttached(): Boolean {
        return Debug.isDebuggerConnected()
    }

    fun isFridaRunning(): Boolean {
        try {
            val mapsFile = File("/proc/self/maps")
            if (mapsFile.exists()) {
                var found = false
                mapsFile.forEachLine { line ->
                    if (line.contains("frida") || line.contains("gum-js-loop") || line.contains("linjector")) {
                        found = true
                    }
                }
                return found
            }
        } catch (e: Throwable) {}
        return false
    }
}
