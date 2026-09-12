package com.talha.music.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val pureBlackKey = booleanPreferencesKey("pure_black_theme")

    val pureBlack: Flow<Boolean> = context.settingsDataStore.data
        .map { preferences -> preferences[pureBlackKey] ?: false }

    suspend fun setPureBlack(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[pureBlackKey] = enabled
        }
    }
}
