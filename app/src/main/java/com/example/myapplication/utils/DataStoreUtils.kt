package com.example.myapplication.utils

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

// DataStore instance for the app
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
val FAVORITE_PALETTES_KEY = stringPreferencesKey("favorite_palettes")
val THEME_KEY = booleanPreferencesKey("theme_preference")
