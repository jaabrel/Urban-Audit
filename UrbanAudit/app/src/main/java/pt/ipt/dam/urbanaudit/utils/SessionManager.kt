package pt.ipt.dam.urbanaudit.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Gestor de sessão local baseado em SharedPreferences.
 * Permite autenticação e persistência de sessão de utilizador sem necessidade de ligação a serviços externos.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "UrbanAudit_Session"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NOME = "user_nome"
    }

    /**
     * Guarda a sessão iniciada pelo utilizador.
     */
    fun iniciarSessao(email: String, nome: String) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_NOME, nome)
            apply()
        }
    }

    /**
     * Verifica se existe um utilizador autenticado.
     */
    fun sessaoIniciada(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    /**
     * Devolve o email do utilizador em sessão.
     */
    fun getEmail(): String {
        return prefs.getString(KEY_USER_EMAIL, "") ?: ""
    }

    /**
     * Devolve o nome do utilizador em sessão.
     */
    fun getNome(): String {
        return prefs.getString(KEY_USER_NOME, "Utilizador") ?: "Utilizador"
    }

    /**
     * Termina a sessão atual.
     */
    fun terminarSessao() {
        prefs.edit().apply {
            clear()
            apply()
        }
    }
}
