package app.cryptoseal.data.api

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import app.cryptoseal.data.model.User
import com.google.gson.Gson

/**
 * Manages the user's session by securely storing authentication tokens and user profiles.
 * It uses [EncryptedSharedPreferences] to ensure that sensitive data is stored encrypted at rest.
 *
 * @param context The application context required to initialize encrypted storage.
 */
class SessionManager(context: Context) {
    private val gson = Gson()

    // Create a MasterKey for encrypting the SharedPreferences
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    // Encrypted storage instance for sensitive user data
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

    /**
     * The JWT authentication token for the current session.
     * Persisted securely in [EncryptedSharedPreferences].
     */
    var authToken: String?
        get() = sharedPreferences.getString(KEY_TOKEN, null)
        set(value) {
            sharedPreferences.edit { putString(KEY_TOKEN, value) }
        }

    /**
     * The profile information of the currently logged-in user.
     * Stored as a JSON string and automatically deserialized.
     */
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

    /**
     * Clears all session data from secure storage.
     * Typically called during logout.
     */
    fun clear() {
        sharedPreferences.edit { clear() }
    }
}