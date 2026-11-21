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

    // Core
    external fun nativeCertHash(apkPath: String): String
    external fun nativeEnforceCert(apkPath: String): Int
    external fun nativeExpectedHash(): String
    external fun nativeDiagnostic(apkPath: String): String
    external fun nativeApkHash(apkPath: String): String
    external fun nativeEnvFlags(): Int

    // Extra (Vigil-style)
    external fun nativeExtraFlags(pkgName: String): Int

    // NEW: Deep emulator
    external fun nativeDeepEmulatorFlags(): Int

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

    fun deepEmulatorReport(): Map<String, Boolean> {
        if (!available()) return emptyMap()
        val f = try { nativeDeepEmulatorFlags() } catch (_: Throwable) { 0 }
        return mapOf(
            "qemu_artifacts"    to ((f and 1) != 0),
            "arm_translation"   to ((f and 2) != 0),
            "native_bridge"     to ((f and 4) != 0),
            "build_props"       to ((f and 8) != 0),
            "shared_folder"     to ((f and 16) != 0),
            "qemu_kernel"       to ((f and 32) != 0),
            "task_leak"         to ((f and 64) != 0),
        )
    }

    fun stackReport(): Map<String, Boolean> {
        val r = StackTraceProbe.check()
        return mapOf(
            "substrate"      to r.substrate,
            "xposed_stack"   to r.xposed,
            "zygote_double"  to r.zygoteDouble,
        )
    }
}
