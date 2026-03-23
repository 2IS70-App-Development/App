package app.cryptoseal

import android.app.Application
import app.cryptoseal.data.api.ApiService

class CryptoSealApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiService.initialize(this)
    }
}