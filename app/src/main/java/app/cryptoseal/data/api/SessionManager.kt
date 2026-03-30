package app.cryptoseal.data.api

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import app.cryptoseal.data.model.User
import com.google.gson.Gson

/**
 * SessionManager handles the secure, persistent storage of user session data.
 * 
 * It utilizes Android Jetpack's [EncryptedSharedPreferences] to ensure that 
 * sensitive information like JWT tokens are encrypted at rest on the device.
 * This prevents other apps or unauthorized access to the application's private data.
 *
 * @param context The application context required for initializing encrypted storage.
 */
class SessionManager(context: Context) {
    // Gson instance for converting the User data class to/from a JSON string.
    private val gson = Gson()

    /**
     * The MasterKey is used to encrypt the SharedPreferences file and its keys.
     * We use a hardware-backed key (if available) with AES256-GCM encryption.
     */
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    /**
     * Initialization of the EncryptedSharedPreferences instance.
     * 
     * Schemes used:
     * - AES256_SIV for key encryption (ensures deterministic but secure keys).
     * - AES256_GCM for value encryption (provides authenticated encryption for data).
     */
    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "crypto_seal_prefs", // The filename of the encrypted preferences file
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        // Preference key for the authentication token.
        private const val KEY_TOKEN = "auth_token"

        // Preference key for the serialized user profile.
        private const val KEY_USER = "current_user"
    }

    /**
     * The JWT (JSON Web Token) used for authenticating API requests.
     * 
     * Getter: Retrieves the token from the encrypted storage.
     * Setter: Saves the token to the encrypted storage immediately.
     */
    var authToken: String?
        get() = sharedPreferences.getString(KEY_TOKEN, null)
        set(value) {
            // Using the KTX extension .edit { ... } for concise SharedPreferences modification.
            sharedPreferences.edit { putString(KEY_TOKEN, value) }
        }

    /**
     * The profile information of the currently logged-in user.
     * 
     * Because SharedPreferences only supports primitives, we serialize the [User] 
     * object to a JSON string using Gson before saving, and deserialize it when reading.
     */
    var currentUser: User?
        get() {
            val userJson = sharedPreferences.getString(KEY_USER, null)
            return if (userJson != null) {
                try {
                    // Reconstruct the User object from the stored JSON string.
                    gson.fromJson(userJson, User::class.java)
                } catch (e: Exception) {
                    // Return null if the stored data is corrupted or incompatible.
                    null
                }
            } else null
        }
        set(value) {
            // Convert the User object to a JSON string for storage.
            sharedPreferences.edit { putString(KEY_USER, gson.toJson(value)) }
        }

    /**
     * Completely wipes all data stored in the encrypted preferences.
     * This is called during the Logout process to ensure no sensitive data remains on disk.
     */
    fun clear() {
        sharedPreferences.edit { clear() }
    }
}
