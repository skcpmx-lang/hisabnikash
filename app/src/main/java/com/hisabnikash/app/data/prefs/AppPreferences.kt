package com.hisabnikash.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "hisabnikash_prefs")

/**
 * App-level preferences. All monetary/business data lives in Room; this only
 * holds UI/session state and the optional local PIN.
 */
class AppPreferences(context: Context) : ActiveBusinessStore {

    private val store = context.applicationContext.dataStore

    companion object {
        val KEY_ACTIVE_BUSINESS = longPreferencesKey("active_business_id")
        val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val KEY_PIN_SALT = stringPreferencesKey("pin_salt")
        val KEY_PIN_HASH = stringPreferencesKey("pin_hash")
        val KEY_BIOMETRIC = booleanPreferencesKey("biometric_enabled")
        val KEY_LAST_BACKUP_AT = longPreferencesKey("last_backup_at")
    }

    val activeBusinessId: Flow<Long?> =
        store.data.map { it[KEY_ACTIVE_BUSINESS] ?: -1L }.map { it.takeIf { v -> v > 0 } }

    val onboardingDone: Flow<Boolean> =
        store.data.map { it[KEY_ONBOARDING_DONE] ?: false }

    val pinHash: Flow<String?> = store.data.map { it[KEY_PIN_HASH] }

    val pinSalt: Flow<String?> = store.data.map { it[KEY_PIN_SALT] }

    val biometricEnabled: Flow<Boolean> =
        store.data.map { it[KEY_BIOMETRIC] ?: false }

    val lastBackupAt: Flow<Long?> = store.data.map { it[KEY_LAST_BACKUP_AT] }

    suspend fun setActiveBusiness(id: Long) {
        store.edit { it[KEY_ACTIVE_BUSINESS] = id }
    }

    suspend fun clearActiveBusiness() {
        store.edit { it.remove(KEY_ACTIVE_BUSINESS) }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        store.edit { it[KEY_ONBOARDING_DONE] = done }
    }

    suspend fun setPin(salt: String, hash: String) {
        store.edit {
            it[KEY_PIN_SALT] = salt
            it[KEY_PIN_HASH] = hash
        }
    }

    suspend fun clearPin() {
        store.edit {
            it.remove(KEY_PIN_SALT)
            it.remove(KEY_PIN_HASH)
            it[KEY_BIOMETRIC] = false
        }
    }

    suspend fun setBiometric(enabled: Boolean) {
        store.edit { it[KEY_BIOMETRIC] = enabled }
    }

    suspend fun setLastBackup(at: Long) {
        store.edit { it[KEY_LAST_BACKUP_AT] = at }
    }
}
