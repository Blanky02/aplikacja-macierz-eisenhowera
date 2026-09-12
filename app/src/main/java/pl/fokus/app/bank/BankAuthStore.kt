package pl.fokus.app.bank

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class BankAuthStore(context: Context) {
    private val preferences = EncryptedSharedPreferences.create(
        context,
        "fokus_bank_auth",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    var accessToken: String?
        get() = preferences.getString(KEY_ACCESS_TOKEN, null)
        private set(value) {
            preferences.edit().putString(KEY_ACCESS_TOKEN, value).apply()
        }

    var userId: String?
        get() = preferences.getString(KEY_USER_ID, null)
        private set(value) {
            preferences.edit().putString(KEY_USER_ID, value).apply()
        }

    var userEmail: String?
        get() = preferences.getString(KEY_USER_EMAIL, null)
        private set(value) {
            preferences.edit().putString(KEY_USER_EMAIL, value).apply()
        }

    fun saveSession(user: BankUser, token: String) {
        accessToken = token
        userId = user.id
        userEmail = user.email
    }

    fun clear() {
        preferences.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_EMAIL)
            .apply()
    }

    private companion object {
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_USER_ID = "user_id"
        const val KEY_USER_EMAIL = "user_email"
    }
}
