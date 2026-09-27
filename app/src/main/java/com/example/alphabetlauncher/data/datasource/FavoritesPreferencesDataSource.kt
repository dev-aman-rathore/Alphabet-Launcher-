package com.example.alphabetlauncher.data.datasource

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher_favorites_prefs")

/**
 * DataStore-backed preference data source for persisting user-curated favorite apps.
 * Preserves the exact user ordering of favorites using a delimited package identifier string.
 */
class FavoritesPreferencesDataSource(
    private val context: Context
) {
    companion object {
        private val KEY_FAVORITES = stringPreferencesKey("custom_favorite_packages")
        private const val DELIMITER = "|"

        // Default popular packages seeded on first launch
        val DEFAULT_FAVORITES = listOf(
            "com.whatsapp",
            "com.android.chrome",
            "com.google.android.youtube",
            "com.google.android.gm",
            "com.google.android.apps.photos",
            "com.google.android.calculator",
            "com.android.camera",
            "com.google.android.GoogleCamera"
        )
    }

    val favoritePackagesFlow: Flow<List<String>> = context.dataStore.data.map { preferences ->
        val raw = preferences[KEY_FAVORITES]
        if (raw == null) {
            // First run: return default popular list
            DEFAULT_FAVORITES
        } else if (raw.isBlank()) {
            emptyList()
        } else {
            raw.split(DELIMITER).filter { it.isNotBlank() }
        }
    }

    suspend fun addFavorite(packageName: String) {
        context.dataStore.edit { preferences ->
            val raw = preferences[KEY_FAVORITES]
            val currentList = if (raw == null) {
                DEFAULT_FAVORITES.toMutableList()
            } else if (raw.isBlank()) {
                mutableListOf()
            } else {
                raw.split(DELIMITER).filter { it.isNotBlank() }.toMutableList()
            }

            if (!currentList.contains(packageName)) {
                currentList.add(packageName)
                preferences[KEY_FAVORITES] = currentList.joinToString(DELIMITER)
            }
        }
    }

    suspend fun removeFavorite(packageName: String) {
        context.dataStore.edit { preferences ->
            val raw = preferences[KEY_FAVORITES]
            val currentList = if (raw == null) {
                DEFAULT_FAVORITES.toMutableList()
            } else if (raw.isBlank()) {
                mutableListOf()
            } else {
                raw.split(DELIMITER).filter { it.isNotBlank() }.toMutableList()
            }

            if (currentList.remove(packageName)) {
                preferences[KEY_FAVORITES] = currentList.joinToString(DELIMITER)
            }
        }
    }
}
