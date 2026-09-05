package pt.ipt.dam.urbanaudit.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import pt.ipt.dam.urbanaudit.R
import pt.ipt.dam.urbanaudit.data.local.TokenManager
import pt.ipt.dam.urbanaudit.ui.auth.LoginActivity

class PerfilActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        val tokenManager = TokenManager(this)

        val tvUserId = findViewById<TextView>(R.id.tvUserId)
        val tvUserRole = findViewById<TextView>(R.id.tvUserRole)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        tvUserId.text = "ID de Utilizador: ${tokenManager.getUserId()}"
        tvUserRole.text = "Tipo de Conta: ${tokenManager.getRole()?.uppercase()}"

        btnLogout.setOnClickListener {
            // Limpa o Token e as SharedPreferences
            tokenManager.clear()

            // Volta para o Ecrã de Login e apaga o histórico das activities para não dar para "voltar atrás"
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}