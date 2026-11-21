package com.alaa.shieldlab

import android.content.Context
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

    // Existing
    external fun nativeCertHash(apkPath: String): String
    external fun nativeEnforceCert(apkPath: String): Int
    external fun nativeExpectedHash(): String
    external fun nativeDiagnostic(apkPath: String): String
    external fun nativeApkHash(apkPath: String): String
    external fun nativeEnvFlags(): Int

    // NEW — from Vigil-style checks
    external fun nativeExtraFlags(pkgName: String): Int

    fun envReport(): Map<String, Boolean> {
        if (!available()) return mapOf(
            "debugger" to false, "frida" to false,
            "root" to false, "emulator" to false,
        )
        val f = try { nativeEnvFlags() } catch (_: Throwable) { 0 }
        return mapOf(
            "debugger" to ((f and 1) != 0),
            "frida"    to ((f and 2) != 0),
            "root"     to ((f and 4) != 0),
            "emulator" to ((f and 8) != 0),
        )
    }

    fun extraReport(ctx: Context): Map<String, Boolean> {
        if (!available()) return emptyMap()
        val pkg = ctx.packageName
        val f = try { nativeExtraFlags(pkg) } catch (_: Throwable) { 0 }
        val xposed = XposedProbe.check()
        return mapOf(
            "rwxp"          to ((f and 1) != 0),
            "clone"         to ((f and 2) != 0),
            "emulator_hard" to ((f and 4) != 0),
            "debug_port"    to ((f and 8) != 0),
            "xposed_bridge" to xposed.xposedBridge,
            "xposed_helpers" to xposed.xposedHelpers,
            "lsposed"       to xposed.lsposed,
            "substrate"     to xposed.substrate,
        )
    }
}
