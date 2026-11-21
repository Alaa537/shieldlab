package com.alaa.shieldlab

import android.app.Application
import android.os.Build
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ShieldApplication : Application() {
    private val TAG = "ShieldLab"

    val logFile: File by lazy {
        val dir = File(filesDir, ".shield").apply { mkdirs() }
        File(dir, ".diag").also {
            try { if (it.exists()) it.delete() } catch (_: Throwable) {}
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            log("═══ Startup ═══")
            log("Mode=${EnforcementConfig.MODE}")
            log("Device=${Build.MANUFACTURER} ${Build.MODEL} / Android ${Build.VERSION.RELEASE}")
            log("Native=${Enforcement.loadError ?: "OK"}")

            val failure = runAllChecks()
            if (failure != null) {
                log("✗ FAILED: $failure")
                handleFailure(failure)
                return
            }
            log("✓ All checks passed")
        } catch (t: Throwable) {
            log("✗ EXCEPTION: ${t::class.java.simpleName}: ${t.message}")
            log(t.stackTraceToString())
            handleFailure("onCreate_exception")
        }
    }

    fun runAllChecks(): String? {
        if (!Enforcement.available()) return "native_not_loaded"

        val apk = try { SignatureBridge.getApkPath(this) } catch (t: Throwable) {
            return "apk_path: ${t.message}"
        }

        // 1) Cert
        if (EnforcementConfig.CHECK_CERT) {
            val result = try { Enforcement.nativeEnforceCert(apk) } catch (t: Throwable) {
                return "cert_exception: ${t.message}"
            }
            if (result != 0) {
                val actual = try { Enforcement.nativeCertHash(apk) } catch (_: Throwable) { "?" }
                val expected = try { Enforcement.nativeExpectedHash() } catch (_: Throwable) { "?" }
                log("cert_mismatch result=$result actual=$actual expected=$expected")
                return "cert_result_$result"
            }
        }

        // 2) Core env
        val env = try { Enforcement.envReport() } catch (t: Throwable) {
            return "env_exception: ${t.message}"
        }
        if (EnforcementConfig.CHECK_DEBUGGER && env["debugger"] == true) return "debugger"
        if (EnforcementConfig.CHECK_FRIDA    && env["frida"]    == true) return "frida"
        if (EnforcementConfig.CHECK_ROOT     && env["root"]     == true) return "root"
        if (EnforcementConfig.CHECK_EMULATOR && env["emulator"] == true) return "emulator"

        // 3) Extra (Vigil)
        val extra = try { Enforcement.extraReport(this) } catch (t: Throwable) {
            return "extra_exception: ${t.message}"
        }
        if (EnforcementConfig.CHECK_RWXP          && extra["rwxp"]          == true) return "rwxp_segment"
        if (EnforcementConfig.CHECK_CLONE         && extra["clone"]         == true) return "clone_detected"
        if (EnforcementConfig.CHECK_EMULATOR_HARD && extra["emulator_hard"] == true) return "emulator_hard"
        if (EnforcementConfig.CHECK_DEBUG_PORT    && extra["debug_port"]    == true) return "debug_port"
        if (EnforcementConfig.CHECK_XPOSED) {
            if (extra["xposed_bridge"]  == true) return "xposed_bridge"
            if (extra["xposed_helpers"] == true) return "xposed_helpers"
            if (extra["lsposed"]        == true) return "lsposed"
            if (extra["substrate"]      == true) return "substrate"
        }

        // 4) Stack trace hook
        if (EnforcementConfig.CHECK_STACK_HOOK) {
            val stack = try { Enforcement.stackReport() } catch (_: Throwable) { emptyMap() }
            if (stack["xposed_stack"]  == true) return "stack_xposed"
            if (stack["substrate"]     == true) return "stack_substrate"
            if (stack["zygote_double"] == true) return "stack_zygote_double"
        }

        // 5) Deep emulator
        val emu = try { Enforcement.deepEmulatorReport() } catch (t: Throwable) {
            return "emu_exception: ${t.message}"
        }
        if (EnforcementConfig.CHECK_QEMU_ARTIFACTS  && emu["qemu_artifacts"]  == true) return "qemu_artifacts"
        if (EnforcementConfig.CHECK_ARM_TRANSLATION && emu["arm_translation"] == true) return "arm_translation"
        if (EnforcementConfig.CHECK_NATIVE_BRIDGE   && emu["native_bridge"]   == true) return "native_bridge"
        if (EnforcementConfig.CHECK_QEMU_KERNEL     && emu["qemu_kernel"]     == true) return "qemu_kernel"
        if (EnforcementConfig.CHECK_SHARED_FOLDER   && emu["shared_folder"]   == true) return "shared_folder"
        if (EnforcementConfig.CHECK_BUILD_PROPS     && emu["build_props"]     == true) return "build_props"
        if (EnforcementConfig.CHECK_TASK_LEAK       && emu["task_leak"]       == true) return "task_leak"

        return null
    }

    fun handleFailure(reason: String) {
        if (EnforcementConfig.MODE == EnforcementConfig.Mode.KILL) {
            log("KILL: $reason")
            killImmediately()
        } else {
            log("DIAGNOSE: $reason (not killing)")
        }
    }

    private fun killImmediately() {
        try { android.os.Process.killProcess(android.os.Process.myPid()) } catch (_: Throwable) {}
        try { System.exit(1) } catch (_: Throwable) {}
        try { Runtime.getRuntime().halt(1) } catch (_: Throwable) {}
    }

    fun log(msg: String) {
        Log.d(TAG, msg)
        try { logFile.appendText("[${stamp()}] $msg\n") } catch (_: Throwable) {}
    }

    private fun stamp() = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
}
