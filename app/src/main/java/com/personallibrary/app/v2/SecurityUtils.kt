package com.personallibrary.app.v2

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecurityUtils {

    private const val ENCRYPTED_PREFS_NAME = "secure_app_prefs"

    fun getEncryptedSharedPreferences(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // Convenience methods for commonly secured data
    fun isShareDataEnabled(context: Context): Boolean {
        return getEncryptedSharedPreferences(context).getBoolean("share_data_enabled", false)
    }

    fun setShareDataEnabled(context: Context, enabled: Boolean) {
        getEncryptedSharedPreferences(context).edit()
            .putBoolean("share_data_enabled", enabled)
            .apply()
    }
}
