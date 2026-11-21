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
        val dir = getExternalFilesDir(null) ?: filesDir
        File(dir, "shieldlab_diag.log").also {
            try { if (it.exists()) it.delete() } catch (_: Throwable) {}
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            log("═══ ShieldLab startup ═══")
            log("Time: ${stamp()}")
            log("Package: $packageName")
            log("PID: ${android.os.Process.myPid()}")
            log("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            log("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            log("ABIs: ${Build.SUPPORTED_ABIS.joinToString()}")
            log("Native load: ${Enforcement.loadError ?: "OK"}")

            val apk = try { SignatureBridge.getApkPath(this) } catch (t: Throwable) { "ERR:${t.message}" }
            log("APK path: $apk")
            try {
                val f = File(apk)
                log("APK size: ${f.length()} bytes")
            } catch (_: Throwable) {}

            if (!Enforcement.available()) {
                log("✗ Native library not available")
                return
            }

            // Cert check
            try {
                val actual = Enforcement.nativeCertHash(apk)
                val expected = Enforcement.nativeExpectedHash()
                val result = Enforcement.nativeEnforceCert(apk)
                val diag = Enforcement.nativeDiagnostic(apk)
                log("Cert actual   : $actual")
                log("Cert expected : $expected")
                log("Result        : $result (0=match 1=mismatch 2=err 3=open)")
                log("Diagnostic    : $diag")
            } catch (t: Throwable) {
                log("✗ Cert check exception: ${t::class.java.simpleName}: ${t.message}")
            }

            // Env check
            try {
                val env = Enforcement.envReport()
                log("Env flags: $env")
            } catch (t: Throwable) {
                log("✗ Env check exception: ${t.message}")
            }

            log("═══ End startup log ═══")
        } catch (t: Throwable) {
            Log.e(TAG, "onCreate crashed", t)
        }
    }

    fun log(msg: String) {
        Log.d(TAG, msg)
        try { logFile.appendText("[${stamp()}] $msg\n") } catch (_: Throwable) {}
    }

    private fun stamp() = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
}
