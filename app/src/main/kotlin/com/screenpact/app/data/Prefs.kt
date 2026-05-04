package com.screenpact.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "screenpact_prefs")

/**
 * Identidad propia: nombre que le mostramos al amigo al emparejar + secreto que generamos para CADA amigo.
 * En este diseño cada amigo lleva su propio secreto compartido, así que aquí guardamos solo el nombre que se incluye en QR.
 */
object Prefs {
    private val OWN_NAME = stringPreferencesKey("own_name")

    suspend fun setOwnName(context: Context, name: String) {
        context.dataStore.edit { it[OWN_NAME] = name }
    }

    suspend fun getOwnName(context: Context): String =
        context.dataStore.data.map { it[OWN_NAME] ?: "" }.first()
}
