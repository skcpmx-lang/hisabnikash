package com.hisabnikash.app.data.repo

import android.content.Context
import androidx.biometric.BiometricManager
import com.hisabnikash.app.data.prefs.AppPreferences
import kotlinx.coroutines.flow.first
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Optional local security: PIN (salted SHA-256) + biometric availability.
 * The app never asks for or stores banking credentials, OTPs or external
 * passwords.
 */
class SecurityRepository(private val context: Context, private val prefs: AppPreferences) {

    suspend fun isPinSet(): Boolean = prefs.pinHash.first() != null

    suspend fun setPin(pin: String) {
        val saltBytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val salt = saltBytes.joinToString("") { "%02x".format(it) }
        prefs.setPin(salt, hash(pin, salt))
    }

    suspend fun clearPin() = prefs.clearPin()

    suspend fun verifyPin(pin: String): Boolean {
        val salt = prefs.pinSalt.first() ?: return false
        val expected = prefs.pinHash.first() ?: return false
        return hash(pin, salt) == expected
    }

    suspend fun isBiometricEnabled(): Boolean = prefs.biometricEnabled.first()

    suspend fun setBiometric(enabled: Boolean) = prefs.setBiometric(enabled)

    fun canUseBiometric(): Boolean =
        BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS

    private fun hash(pin: String, salt: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest((salt + pin).toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
