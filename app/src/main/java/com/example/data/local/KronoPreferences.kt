package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.util.KronoCrypto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "krono_prefs")

class KronoPreferences(private val context: Context) {
  companion object {
    private val KEY_USER_NAME = stringPreferencesKey("user_name")
    private val KEY_VAULT_PIN_HASH = stringPreferencesKey("vault_pin_hash")
    private val KEY_VAULT_PIN_SALT = stringPreferencesKey("vault_pin_salt")
    private val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
    private val KEY_EXIT_ON_AUTH_FAIL = booleanPreferencesKey("exit_on_auth_fail")
    private val KEY_SEEDED_VERSION = stringPreferencesKey("seeded_version")
    private val KEY_DRIVE_CONNECTED = booleanPreferencesKey("drive_connected")
    private val KEY_DRIVE_EMAIL = stringPreferencesKey("drive_email")
    private val KEY_DRIVE_LAST_SYNC_MS = longPreferencesKey("drive_last_sync_ms")
  }

  val userName: Flow<String> =
    context.dataStore.data.map { prefs -> prefs[KEY_USER_NAME] ?: "" }

  val vaultPinHash: Flow<String?> =
    context.dataStore.data.map { prefs -> prefs[KEY_VAULT_PIN_HASH] }

  val vaultPinSalt: Flow<String?> =
    context.dataStore.data.map { prefs -> prefs[KEY_VAULT_PIN_SALT] }

  val hasPinConfigured: Flow<Boolean> =
    context.dataStore.data.map { prefs -> !prefs[KEY_VAULT_PIN_HASH].isNullOrBlank() }

  val biometricEnabled: Flow<Boolean> =
    context.dataStore.data.map { prefs -> prefs[KEY_BIOMETRIC_ENABLED] ?: true }

  val exitOnAuthFail: Flow<Boolean> =
    context.dataStore.data.map { prefs -> prefs[KEY_EXIT_ON_AUTH_FAIL] ?: true }

  val seededVersion: Flow<String?> =
    context.dataStore.data.map { prefs -> prefs[KEY_SEEDED_VERSION] }

  val driveConnected: Flow<Boolean> =
    context.dataStore.data.map { prefs -> prefs[KEY_DRIVE_CONNECTED] ?: false }

  val driveAccountEmail: Flow<String?> =
    context.dataStore.data.map { prefs -> prefs[KEY_DRIVE_EMAIL] }

  val lastDriveSyncMs: Flow<Long> =
    context.dataStore.data.map { prefs -> prefs[KEY_DRIVE_LAST_SYNC_MS] ?: 0L }

  suspend fun setUserName(name: String) {
    context.dataStore.edit { it[KEY_USER_NAME] = name.trim() }
  }

  suspend fun setVaultPin(pin: String) {
    context.dataStore.edit { prefs ->
      val salt = KronoCrypto.generateSalt()
      prefs[KEY_VAULT_PIN_SALT] = salt
      prefs[KEY_VAULT_PIN_HASH] = KronoCrypto.hashPin(pin, salt)
    }
  }

  suspend fun setBiometricEnabled(enabled: Boolean) {
    context.dataStore.edit { it[KEY_BIOMETRIC_ENABLED] = enabled }
  }

  suspend fun setExitOnAuthFail(enabled: Boolean) {
    context.dataStore.edit { it[KEY_EXIT_ON_AUTH_FAIL] = enabled }
  }

  suspend fun setSeededVersion(version: String) {
    context.dataStore.edit { it[KEY_SEEDED_VERSION] = version }
  }

  suspend fun setDriveConnection(connected: Boolean, email: String?) {
    context.dataStore.edit { prefs ->
      prefs[KEY_DRIVE_CONNECTED] = connected
      if (email != null) {
        prefs[KEY_DRIVE_EMAIL] = email
      } else {
        prefs.remove(KEY_DRIVE_EMAIL)
      }
    }
  }

  suspend fun setLastDriveSyncMs(timestampMs: Long) {
    context.dataStore.edit { prefs ->
      prefs[KEY_DRIVE_LAST_SYNC_MS] = timestampMs
    }
  }
}
