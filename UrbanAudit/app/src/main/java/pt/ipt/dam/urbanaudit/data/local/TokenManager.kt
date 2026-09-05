package pt.ipt.dam.urbanaudit.data.local

import android.content.Context
import android.content.SharedPreferences

class TokenManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("urban_audit_prefs", Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        prefs.edit().putString("JWT_TOKEN", token).apply()
    }
    fun getToken(): String? = prefs.getString("JWT_TOKEN", null)
    fun saveUserId(userId: Int) {
        prefs.edit().putInt("USER_ID", userId).apply()
    }
    fun getUserId(): Int = prefs.getInt("USER_ID", -1)
    fun clear() {
        prefs.edit().clear().apply()
    }
    fun saveRole(role: String) = prefs.edit().putString("ROLE", role).apply()
    fun getRole(): String? = prefs.getString("ROLE", "user")

    fun saveEmail(email: String) = prefs.edit().putString("EMAIL", email).apply()
    fun getEmail(): String? = prefs.getString("EMAIL", "Utilizador")
}