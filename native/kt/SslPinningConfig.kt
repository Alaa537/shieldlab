package com.alaa.shieldlab

object SslPinningConfig {
    // ═══════════════════════════════════════════════
    // SSL PINNING — NOT ACTIVE
    //
    // Reason: this demo app has no backend.
    // To activate:
    //   1. Deploy a server with TLS
    //   2. Extract pin (SHA-256 of public key, base64)
    //   3. Set DOMAINS below
    //   4. Uncomment `installPinning()` in ShieldApplication.onCreate
    //   5. Add OkHttp dependency to build.gradle.kts
    // ═══════════════════════════════════════════════

    const val ENABLED = false

    // domain → list of base64 SHA-256 public key pins
    // Last pin should be a BACKUP key (rotation safety)
    val DOMAINS: Map<String, List<String>> = mapOf(
        // "api.example.com" to listOf(
        //     "AAAA...primary_pin...=",
        //     "BBBB...backup_pin...=",
        // ),
    )

    fun isConfigured(): Boolean = ENABLED && DOMAINS.isNotEmpty()
}
