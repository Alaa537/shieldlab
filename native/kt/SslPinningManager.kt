package com.alaa.shieldlab

import android.content.Context
import com.wultra.android.sslpinning.CertStore
import com.wultra.android.sslpinning.CertStoreConfiguration
import java.net.URL

object SslPinningManager {
    private var certStore: CertStore? = null

    // هذه الدالة جاهزة للاستخدام المستقبلي عند وجود Backend
    fun initialize(context: Context, serviceUrl: String, publicKeyBase64: String) {
        if (certStore != null) return
        try {
            val publicKey = android.util.Base64.decode(publicKeyBase64, android.util.Base64.NO_WRAP)
            val config = CertStoreConfiguration.Builder(
                serviceUrl = URL(serviceUrl),
                publicKey = publicKey
            ).build()
            certStore = CertStore(config, context.applicationContext)
        } catch (e: Exception) {
            android.util.Log.e("SslPinning", "Failed to initialize", e)
        }
    }

    fun getCertStore(): CertStore? = certStore
}
