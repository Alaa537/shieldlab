package com.alaa.shieldlab

object EnforcementConfig {
    // DIAGNOSE = never kill (safe) | WARN = toast but continue | KILL = terminate
    enum class Mode { DIAGNOSE, WARN, KILL }

    // ═══════════════════════════════════════════
    // افتراضي: DIAGNOSE — لا يقتل أبداً
    // ═══════════════════════════════════════════
    val MODE: Mode = Mode.DIAGNOSE

    // Individual check toggles
    const val CHECK_CERT     = true
    const val CHECK_DEBUGGER = false
    const val CHECK_ROOT     = false
    const val CHECK_FRIDA    = true
    const val CHECK_EMULATOR = false
}
