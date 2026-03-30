package app.cryptoseal

import android.app.Application
import app.cryptoseal.data.api.ApiService

/**
 * Base Application class for the CryptoSeal project.
 * 
 * This class is the first thing to run when the app's process is created.
 * It is registered in the AndroidManifest.xml and serves as the global 
 * configuration point for the application.
 */
class CryptoSealApplication : Application() {

    /**
     * Called when the application is starting, before any activity, service, 
     * or receiver objects have been created.
     * 
     * We use this opportunity to initialize our singleton [ApiService] 
     * with the application context, ensuring that networking and session 
     * management are ready before the first screen (Login) appears.
     */
    override fun onCreate() {
        super.onCreate()

        // Initialize the ApiService with 'this' (the application context).
        // This allows the ApiService to set up SessionManager for secure storage.
        ApiService.initialize(this)
    }
}
