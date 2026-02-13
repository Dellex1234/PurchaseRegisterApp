package com.example.purchaseregister.utils

import android.content.Context
import android.util.Base64

// --- PERSISTENCIA SUNAT (SOLO CREDENCIALES) ---
object SunatPrefs {
    private const val PREFS_NAME = "auth_prefs"
    private const val KEY_RUC = "sunat_ruc"
    private const val KEY_USER = "sunat_usuario"
    private const val KEY_CLAVE_SOL = "sunat_clave_sol"

    fun saveClaveSol(context: Context, claveSol: String) {
        try {
            val encrypted = Base64.encodeToString(claveSol.toByteArray(), Base64.NO_WRAP)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_CLAVE_SOL, encrypted).apply()
        } catch (e: Exception) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_CLAVE_SOL, claveSol).apply()
        }
    }

    fun getClaveSol(context: Context): String? {
        val encrypted = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CLAVE_SOL, null)

        return encrypted?.let {
            try {
                String(Base64.decode(it, Base64.NO_WRAP))
            } catch (e: Exception) {
                it
            }
        }
    }

    fun saveRuc(context: Context, ruc: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_RUC, ruc).apply()
    }

    fun saveUser(context: Context, usuario: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_USER, usuario).apply()
    }

    fun getRuc(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_RUC, null)
    }

    fun getUser(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USER, null)
    }

    fun clearCredentials(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}