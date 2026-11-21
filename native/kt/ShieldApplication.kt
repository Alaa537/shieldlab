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

    // Log مخفي — في filesDir الخاص بالتطبيق (غير مرئي بدون root)
    val logFile: File by lazy {
        val dir = File(filesDir, ".shield").apply { mkdirs() }
        File(dir, ".diag").also {
            try { if (it.exists()) it.delete() } catch (_: Throwable) {}
        }
    }

    // also keep external copy in DIAGNOSE mode only
    private val externalLog: File? by lazy {
        if (EnforcementConfig.MODE == EnforcementConfig.Mode.DIAGNOSE) {
            val dir = getExternalFilesDir(null) ?: return@lazy null
            File(dir, "shieldlab_diag.log").also {
                try { if (it.exists()) it.delete() } catch (_: Throwable) {}
            }
        } else null
    }

    override fun onCreate() {
        super.onCreate()
        try {
            log("═══ Startup ═══")
            log("Mode=${EnforcementConfig.MODE}")
            log("Device=${Build.MANUFACTURER} ${Build.MODEL} / Android ${Build.VERSION.RELEASE}")
            log("ABIs=${Build.SUPPORTED_ABIS.joinToString()}")
            log("Native=${Enforcement.loadError ?: "OK"}")

            val failure = runAllChecks()
            if (failure != null) {
                log("✗ FAILED: $failure")
                handleFailure(failure)
                return
            }
            log("✓ All checks passed")
        } catch (t: Throwable) {
            // Fail-closed: exception = محاولة تخطي
            log("✗ EXCEPTION in onCreate: ${t::class.java.simpleName}: ${t.message}")
            log(t.stackTraceToString())
            handleFailure("onCreate exception")
        }
    }

    fun runAllChecks(): String? {
        if (!Enforcement.available()) return "native_not_loaded"

        val apk = try { SignatureBridge.getApkPath(this) } catch (t: Throwable) {
            return "apk_path: ${t.message}"
        }

        // 1) Cert check
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

        // 2) Environment
        val env = try { Enforcement.envReport() } catch (t: Throwable) {
            return "env_exception: ${t.message}"
        }
        if (EnforcementConfig.CHECK_DEBUGGER && env["debugger"] == true) return "debugger"
        if (EnforcementConfig.CHECK_FRIDA    && env["frida"]    == true) return "frida"
        if (EnforcementConfig.CHECK_ROOT     && env["root"]     == true) return "root"
        if (EnforcementConfig.CHECK_EMULATOR && env["emulator"] == true) return "emulator"

        return null
    }

    fun handleFailure(reason: String) {
        when (EnforcementConfig.MODE) {
            EnforcementConfig.Mode.KILL -> {
                log("KILL: $reason")
                killImmediately()
            }
            EnforcementConfig.Mode.DIAGNOSE -> {
                log("DIAGNOSE: $reason (not killing)")
            }
        }
    }

    /**
     * Kill بـ 4 طرق متتالية — عشان لو حد hook واحدة منهم
     */
    private fun killImmediately() {
        // 1) اقفل الأنشطة
        try {
            val ctx = applicationContext
            ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.let {
                it.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } catch (_: Throwable) {}

        // 2) Kill process
        try {
            android.os.Process.killProcess(android.os.Process.myPid())
        } catch (_: Throwable) {}

        // 3) System.exit
        try {
            System.exit(1)
        } catch (_: Throwable) {}

        // 4) halt — يتخطى shutdown hooks
        try {
            Runtime.getRuntime().halt(1)
        } catch (_: Throwable) {}
    }

    fun log(msg: String) {
        Log.d(TAG, msg)
        try { logFile.appendText("[${stamp()}] $msg\n") } catch (_: Throwable) {}
        try { externalLog?.appendText("[${stamp()}] $msg\n") } catch (_: Throwable) {}
    }

    fun readLog(): String = try {
        if (logFile.exists()) logFile.readText() else "(no log)"
    } catch (t: Throwable) { "read error: ${t.message}" }

    private fun stamp() = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
}
