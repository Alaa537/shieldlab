package com.alaa.shieldlab

object EnforcementConfig {
    enum class Mode { DIAGNOSE, KILL }

    // ═══════════════════════════════════════════
    // PRODUCTION: KILL
    // ═══════════════════════════════════════════
    val MODE: Mode = Mode.KILL

    // ═══════════════════════════════════════════
    // Individual checks (production config)
    // ═══════════════════════════════════════════
    const val CHECK_CERT     = true    // أساسي — يكشف أي تعديل
    const val CHECK_DEBUGGER = true    // أساسي — يكشف adb/Frida
    const val CHECK_FRIDA    = true    // أساسي
    const val CHECK_ROOT     = false   // ← اختياري (المختبِر ممکن یکون rooted)
    const val CHECK_EMULATOR = false   // ← اختياري (ممکن یختبر على emulator)
}
