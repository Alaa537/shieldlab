package com.alaa.shieldlab

object EnforcementConfig {
    enum class Mode { DIAGNOSE, KILL }

    val MODE: Mode = Mode.KILL

    // ─── Core (recommended) ───
    const val CHECK_CERT          = true
    const val CHECK_DEBUGGER      = true
    const val CHECK_FRIDA         = true
    const val CHECK_RWXP          = true
    const val CHECK_XPOSED        = true
    const val CHECK_STACK_HOOK    = true   // NEW: stack trace probe

    // ─── Emulator: high confidence only ───
    const val CHECK_QEMU_ARTIFACTS  = true   // NEW
    const val CHECK_ARM_TRANSLATION = true   // NEW
    const val CHECK_NATIVE_BRIDGE   = true   // NEW
    const val CHECK_QEMU_KERNEL     = true   // NEW
    const val CHECK_SHARED_FOLDER   = true   // NEW (BlueStacks)
    const val CHECK_BUILD_PROPS     = true   // NEW (rating > 3)
    const val CHECK_EMULATOR_HARD   = false  // ← battery/GL (false pos)

    // ─── Optional / risky ───
    const val CHECK_CLONE         = false    // ← false pos in some launchers
    const val CHECK_ROOT          = false    // ← disable for rooted tester
    const val CHECK_EMULATOR      = false    // ← old cpuinfo check
    const val CHECK_DEBUG_PORT    = false    // ← disable if adb wireless
    const val CHECK_TASK_LEAK     = false    // ← soft signal
}
