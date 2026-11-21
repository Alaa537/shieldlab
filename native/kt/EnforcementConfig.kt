package com.alaa.shieldlab

object EnforcementConfig {
    enum class Mode { DIAGNOSE, KILL }

    // ═══════════════════════════════════════════
    // PRODUCTION: KILL
    // ═══════════════════════════════════════════
    val MODE: Mode = Mode.KILL

    // ─── Core (always recommended) ───
    const val CHECK_CERT          = true
    const val CHECK_DEBUGGER      = true
    const val CHECK_FRIDA         = true
    const val CHECK_RWXP          = true   // NEW: rwxp memory segments

    // ─── Hooking frameworks ───
    const val CHECK_XPOSED        = true   // NEW: Xposed/LSPosed/Substrate
    const val CHECK_CLONE         = false  // ← can false-positive in some launchers

    // ─── Environment ───
    const val CHECK_ROOT          = false  // ← disable for rooted tester
    const val CHECK_EMULATOR      = false  // ← disable if testing on emulator
    const val CHECK_EMULATOR_HARD = false  // ← battery/GL based
    const val CHECK_DEBUG_PORT    = false  // ← disable if they use adb wireless
}
