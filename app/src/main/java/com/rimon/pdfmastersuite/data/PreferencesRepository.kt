package com.rimon.pdfmastersuite.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

class PreferencesRepository(private val context: Context) {

    companion object {
        val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val KEY_HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
    }

    val isSoundEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_SOUND_ENABLED] ?: true
    }

    val isHapticsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_HAPTICS_ENABLED] ?: true
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SOUND_ENABLED] = enabled
        }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_HAPTICS_ENABLED] = enabled
        }
    }
}
