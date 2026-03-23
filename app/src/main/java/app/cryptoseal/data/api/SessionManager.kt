package app.cryptoseal.data.api

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import app.cryptoseal.data.model.User
import com.google.gson.Gson

class SessionManager(context: Context) {
    private val gson = Gson()
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "crypto_seal_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER = "current_user"
    }

    var authToken: String?
        get() = sharedPreferences.getString(KEY_TOKEN, null)
        set(value) {
            sharedPreferences.edit { putString(KEY_TOKEN, value) }
        }

    var currentUser: User?
        get() {
            val userJson = sharedPreferences.getString(KEY_USER, null)
            return if (userJson != null) {
                try {
                    gson.fromJson(userJson, User::class.java)
                } catch (e: Exception) {
                    null
                }
            } else null
        }
        set(value) {
            sharedPreferences.edit { putString(KEY_USER, gson.toJson(value)) }
        }

    fun clear() {
        sharedPreferences.edit { clear() }
    }
}