package com.alaa.shieldlab

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val CHANNEL = "shieldlab/diagnostic"
    private val handler = Handler(Looper.getMainLooper())

    private val periodicCheck = object : Runnable {
        override fun run() {
            val app = application as? ShieldApplication ?: return
            val failure = try { app.runAllChecks() } catch (t: Throwable) {
                "periodic_exception: ${t.message}"
            }
            if (failure != null) {
                app.log("Periodic check failed: $failure")
                app.handleFailure(failure)
                return
            }
            handler.postDelayed(this, 5000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // فحص ثاني (لو Application اتخطى)
        val app = application as? ShieldApplication
        if (app != null) {
            val failure = try { app.runAllChecks() } catch (t: Throwable) {
                "activity_onCreate_exception: ${t.message}"
            }
            if (failure != null) {
                app.handleFailure(failure)
                return
            }
        }

        handler.postDelayed(periodicCheck, 5000L)
    }

    override fun onResume() {
        super.onResume()
        // فحص عند كل resume (لو المستخدم رجع من الخلفية)
        val app = application as? ShieldApplication
        if (app != null && EnforcementConfig.MODE == EnforcementConfig.Mode.KILL) {
            val failure = try { app.runAllChecks() } catch (_: Throwable) { null }
            if (failure != null) {
                app.handleFailure(failure)
            }
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(periodicCheck)
        super.onDestroy()
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "getDiagnostics" -> {
                        // في KILL mode: مفيش تفاصيل
                        if (EnforcementConfig.MODE == EnforcementConfig.Mode.KILL) {
                            result.success(mapOf(
                                "mode" to "KILL",
                                "status" to "protected",
                            ))
                        } else {
                            result.success(getDiagnostics())
                        }
                    }
                    else -> result.notImplemented()
                }
            }
    }

    private fun getDiagnostics(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        try {
            map["mode"] = EnforcementConfig.MODE.name
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
            map["envSafe"] = !(env["frida"] == true || env["debugger"] == true)
        } catch (t: Throwable) {
            map["error"] = "${t::class.java.simpleName}: ${t.message}"
        }
        return map
    }
}
