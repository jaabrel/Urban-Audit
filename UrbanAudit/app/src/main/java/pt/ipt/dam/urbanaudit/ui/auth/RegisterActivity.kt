package pt.ipt.dam.urbanaudit.ui.auth

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import pt.ipt.dam.urbanaudit.R
import pt.ipt.dam.urbanaudit.data.local.TokenManager
import pt.ipt.dam.urbanaudit.data.model.RegisterRequest
import pt.ipt.dam.urbanaudit.data.remote.ApiClient
import pt.ipt.dam.urbanaudit.data.remote.ApiService

/**
 * Ecrã de Registo de Novos Utilizadores da aplicação Urban Audit.
 * 
 * Responsabilidades:
 * - Validação de integridade dos campos de entrada (e-mail e palavra-passe obrigatórios).
 * - Envio assíncrono do pedido de registo via API REST com Coroutines.
 * - Tratamento de respostas de sucesso (HTTP 201/200) e erros (e.g. e-mail já existente).
 */
class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etEmail = findViewById<EditText>(R.id.etRegEmail)
        val etPassword = findViewById<EditText>(R.id.etRegPassword)
        val btnRegistar = findViewById<Button>(R.id.btnRegistar)

        btnRegistar.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Validação de Dados na Inserção: campos não podem estar em branco
            if (email.isEmpty()) {
                etEmail.error = "O e-mail é obrigatório!"
                etEmail.requestFocus()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                etPassword.error = "A palavra-passe é obrigatória!"
                etPassword.requestFocus()
                return@setOnClickListener
            }

            // Tratamento de erros de execução e chamada assíncrona à API
            lifecycleScope.launch {
                try {
                    val api = ApiClient.getClient(TokenManager(this@RegisterActivity)).create(ApiService::class.java)
                    val response = api.register(RegisterRequest(email, password))

                    if (response.isSuccessful) {
                        Toast.makeText(this@RegisterActivity, "Conta criada com sucesso! Já pode iniciar sessão.", Toast.LENGTH_LONG).show()
                        finish() // Encerra o ecrã atual e regressa ao ecrã de Login
                    } else {
                        Toast.makeText(this@RegisterActivity, "Erro ao criar conta: E-mail já existente ou formato inválido.", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@RegisterActivity, "Erro de rede. Verifique a sua ligação à internet.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}