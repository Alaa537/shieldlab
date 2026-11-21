package com.alaa.shieldlab

import android.util.Log

object Enforcement {
    val loadError: String? = try {
        System.loadLibrary("shieldlab")
        null
    } catch (t: Throwable) {
        Log.e("ShieldLab", "native load failed", t)
        "${t::class.java.simpleName}: ${t.message}"
    }

    fun available(): Boolean = loadError == null

    external fun nativeCertHash(apkPath: String): String
    external fun nativeEnforceCert(apkPath: String): Int
    external fun nativeExpectedHash(): String
    external fun nativeDiagnostic(apkPath: String): String
    external fun nativeApkHash(apkPath: String): String
    external fun nativeEnvFlags(): Int

    fun envReport(): Map<String, Boolean> {
        if (!available()) return mapOf(
            "debugger" to false, "frida" to false,
            "root" to false, "emulator" to false
        )
        val f = try { nativeEnvFlags() } catch (_: Throwable) { 0 }
        return mapOf(
            "debugger" to ((f and 1) != 0),
            "frida"    to ((f and 2) != 0),
            "root"     to ((f and 4) != 0),
            "emulator" to ((f and 8) != 0),
        )
    }
}
