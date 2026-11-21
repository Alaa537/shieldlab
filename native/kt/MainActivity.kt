package com.alaa.shieldlab

import android.os.Build
import android.os.Bundle
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val CHANNEL = "shieldlab/diagnostic"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "getDiagnostics" -> {
                        result.success(getDiagnostics())
                    }
                    else -> result.notImplemented()
                }
            }
    }

    private fun getDiagnostics(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        try {
            map["package"] = packageName
            map["device"] = "${Build.MANUFACTURER} ${Build.MODEL}"
            map["android"] = "${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
            map["abi"] = Build.SUPPORTED_ABIS.firstOrNull() ?: "?"
            map["installer"] = SignatureBridge.getInstaller(this)

            val app = application as? ShieldApplication
            map["logPath"] = app?.logFile?.absolutePath ?: ""

            if (!Enforcement.available()) {
                map["nativeLoad"] = "FAILED: ${Enforcement.loadError}"
                map["certMatch"] = false
                map["envSafe"] = false
                return map
            }
            map["nativeLoad"] = "OK"

            val apk = SignatureBridge.getApkPath(this)
            map["apkPath"] = apk

            val actual = try { Enforcement.nativeCertHash(apk) } catch (t: Throwable) { "ERR:${t.message}" }
            val expected = try { Enforcement.nativeExpectedHash() } catch (t: Throwable) { "ERR:${t.message}" }
            val result = try { Enforcement.nativeEnforceCert(apk) } catch (_: Throwable) { -1 }
            val diag = try { Enforcement.nativeDiagnostic(apk) } catch (t: Throwable) { "ERR:${t.message}" }
            val apkHash = try { Enforcement.nativeApkHash(apk) } catch (t: Throwable) { "ERR:${t.message}" }

            map["apkHash"] = apkHash
            map["certActual"] = actual
            map["certExpected"] = expected
            map["certResult"] = result.toString()
            map["certDiag"] = diag
            map["certMatch"] = (result == 0)

            val env = Enforcement.envReport()
            map["debugger"] = env["debugger"] ?: false
            map["frida"] = env["frida"] ?: false
            map["root"] = env["root"] ?: false
            map["emulator"] = env["emulator"] ?: false
            map["envSafe"] = !(env["frida"] == true)
        } catch (t: Throwable) {
            map["error"] = "${t::class.java.simpleName}: ${t.message}"
        }
        return map
    }
}
