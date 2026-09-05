package pt.ipt.dam.urbanaudit.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * Gestor de Sessão e Tokens (SharedPreferences).
 * 
 * Responsabilidades:
 * - Persistência local do token de autenticação JWT retornado pela API.
 * - Armazenamento de metadados da sessão: ID do utilizador, perfil de acesso (role) e e-mail.
 * - Limpeza total dos dados em caso de término de sessão (Logoff).
 */
class TokenManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("urban_audit_prefs", Context.MODE_PRIVATE)

    /** Guarda o token JWT nas preferências */
    fun saveToken(token: String) {
        prefs.edit().putString("JWT_TOKEN", token).apply()
    }

    /** Retorna o token JWT atual, ou null se não autenticado */
    fun getToken(): String? = prefs.getString("JWT_TOKEN", null)

    /** Guarda o identificador numérico do utilizador */
    fun saveUserId(userId: Int) {
        prefs.edit().putInt("USER_ID", userId).apply()
    }

    /** Retorna o ID do utilizador autenticado (-1 se inexistente) */
    fun getUserId(): Int = prefs.getInt("USER_ID", -1)

    /** Elimina todas as chaves da sessão (Logoff) */
    fun clear() {
        prefs.edit().clear().apply()
    }

    /** Guarda o papel/perfil do utilizador (ex: 'admin' ou 'user') */
    fun saveRole(role: String) = prefs.edit().putString("ROLE", role).apply()

    /** Retorna o papel/perfil do utilizador (defeito: 'user') */
    fun getRole(): String? = prefs.getString("ROLE", "user")

    /** Guarda o endereço de e-mail do utilizador autenticado */
    fun saveEmail(email: String) = prefs.edit().putString("EMAIL", email).apply()

    /** Retorna o e-mail do utilizador autenticado */
    fun getEmail(): String? = prefs.getString("EMAIL", "Utilizador")
}