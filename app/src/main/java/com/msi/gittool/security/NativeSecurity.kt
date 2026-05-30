package com.msi.gittool.security

import android.os.Debug
import java.io.File

object NativeSecurity {
    private var isNativeLoaded = false

    init {
        try {
            System.loadLibrary("gittool_security")
            isNativeLoaded = true
        } catch (e: Throwable) {
            // Graceful fallback logger
        }
    }

    external fun isRootedNative(): Boolean
    external fun isEmulatorNative(): Boolean
    external fun isDebuggerAttachedNative(): Boolean
    external fun isFridaRunningNative(): Boolean

    fun isRooted(): Boolean {
        if (isNativeLoaded) {
            try { return isRootedNative() } catch (e: Throwable) {}
        }
        // Pure Kotlin fallback root checks
        val paths = arrayOf(
            "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su",
            "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
            "/system/bin/failsafe/su", "/data/local/su", "/su/bin/su", "/vendor/bin/su"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }
        val tags = android.os.Build.TAGS
        if (tags != null && tags.contains("test-keys")) return true
        return false
    }

    fun isEmulator(): Boolean {
        if (isNativeLoaded) {
            try { return isEmulatorNative() } catch (e: Throwable) {}
        }
        // Pure Kotlin fallback emulator checks
        val fingerprint = android.os.Build.FINGERPRINT
        val model = android.os.Build.MODEL
        val hardware = android.os.Build.HARDWARE
        return fingerprint.startsWith("generic") ||
                fingerprint.startsWith("unknown") ||
                model.contains("google_sdk") ||
                model.contains("Emulator") ||
                model.contains("Android SDK built for x86") ||
                hardware.contains("goldfish") ||
                hardware.contains("ranchu")
    }

    fun isDebuggerAttached(): Boolean {
        if (isNativeLoaded) {
            try { return isDebuggerAttachedNative() } catch (e: Throwable) {}
        }
        return Debug.isDebuggerConnected()
    }

    fun isFridaRunning(): Boolean {
        if (isNativeLoaded) {
            try { return isFridaRunningNative() } catch (e: Throwable) {}
        }
        // Check for Frida artifacts in process files / maps
        try {
            val mapsFile = File("/proc/self/maps")
            if (mapsFile.exists()) {
                mapsFile.forEachLine { line ->
                    if (line.contains("frida") || line.contains("gum-js-loop") || line.contains("linjector")) {
                        return@forEachLine
                    }
                }
            }
        } catch (e: Exception) {}
        return false
    }
}
