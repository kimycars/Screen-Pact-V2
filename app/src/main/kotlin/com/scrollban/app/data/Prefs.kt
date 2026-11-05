package com.scrollban.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "screenpact_prefs")

object Prefs {
    private val OWN_NAME   = stringPreferencesKey("own_name")
    private val SETUP_DONE = booleanPreferencesKey("setup_done")
    private val INSTALL_ID = stringPreferencesKey("install_id")

    /**
     * Identificador aleatorio de esta instalación. Va dentro del QR de emparejamiento para que
     * la app reconozca su propio QR y no deje emparejarse consigo misma.
     */
    suspend fun getInstallId(context: Context): String {
        context.dataStore.data.map { it[INSTALL_ID] }.first()?.let { return it }
        var id = ""
        context.dataStore.edit { prefs ->
            id = prefs[INSTALL_ID] ?: UUID.randomUUID().toString().also { prefs[INSTALL_ID] = it }
        }
        return id
    }

    suspend fun setOwnName(context: Context, name: String) {
        context.dataStore.edit { it[OWN_NAME] = name }
    }

    suspend fun getOwnName(context: Context): String =
        context.dataStore.data.map { it[OWN_NAME] ?: "" }.first()

    /** Returns true once the user has completed onboarding at least once. */
    suspend fun isSetupDone(context: Context): Boolean =
        context.dataStore.data.map { it[SETUP_DONE] ?: false }.first()

    suspend fun setSetupDone(context: Context) {
        context.dataStore.edit { it[SETUP_DONE] = true }
    }
}
