package app.cryptoseal

import android.app.Application
import app.cryptoseal.data.api.ApiService

/**
 * Base Application class for CryptoSeal.
 * Responsible for global initialization, such as the [ApiService].
 */
class CryptoSealApplication : Application() {
    /**
     * Called when the application is starting, before any activity, service,
     * or receiver objects (excluding content providers) have been created.
     * Initializes the [ApiService] with the application context.
     */
    override fun onCreate() {
        super.onCreate()
        ApiService.initialize(this)
    }
}