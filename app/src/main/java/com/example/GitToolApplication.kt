package com.example

import android.app.Application
import android.os.Process
import android.util.Log
import com.example.di.AppContainer
import com.example.di.DefaultAppContainer
import com.msi.gittool.security.IntegrityChecker
import com.msi.gittool.security.NativeSecurity
import com.msi.gittool.security.PlayIntegrityHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GitToolApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()

        // 1. Signature tamper check (disabled in DEBUG mode so testing on AI Studio Emulator isn't blocked)
        if (!BuildConfig.DEBUG && !IntegrityChecker.isSignatureValid(this)) {
            Log.e("GitToolSec", "Signature invalid - tampered!")
            Process.killProcess(Process.myPid())
            return
        }

        // 2. Debugger check (disabled in DEBUG mode to allow standard developer session runs)
        if (!BuildConfig.DEBUG && NativeSecurity.isDebuggerAttached()) {
            Log.e("GitToolSec", "Debugger detected")
            Process.killProcess(Process.myPid())
            return
        }

        // 3. Frida / hooking framework check (disabled in DEBUG mode)
        if (!BuildConfig.DEBUG && NativeSecurity.isFridaRunning()) {
            Log.e("GitToolSec", "Frida/Xposed detected")
            Process.killProcess(Process.myPid())
            return
        }

        // 4. Root & emulator check (logged for reference)
        Log.d("GitToolSec", "Rooted: ${NativeSecurity.isRooted()}, Emulator: ${NativeSecurity.isEmulator()}")

        // 5. Play Integrity (async validation check)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = PlayIntegrityHelper.getToken(this@GitToolApplication)
                Log.d("GitToolSec", "Integrity token retrieved: ${token?.take(30)}...")
            } catch (e: Exception) {
                Log.e("GitToolSec", "Play Integrity retrieval failed", e)
            }
        }

        container = DefaultAppContainer(this)
    }
}

