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
            // REQUISITO: Validação de Dados na Inserção
            if (email.isEmpty()) {
                etEmail.error = "O email é obrigatório!"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                etPassword.error = "A password é obrigatória!"
                return@setOnClickListener
            }
            // REQUISITO: Tratamento de erros de execução
            lifecycleScope.launch {
                try {
                    val api = ApiClient.getClient(TokenManager(this@RegisterActivity)).create(ApiService::class.java)
                    val response = api.register(RegisterRequest(email, password))
                    if (response.isSuccessful) {
                        Toast.makeText(this@RegisterActivity, "Conta criada! Já podes fazer login.", Toast.LENGTH_LONG).show()
                        finish() // Fecha o ecrã e volta ao Login
                    } else {
                        Toast.makeText(this@RegisterActivity, "Erro: Email já existente ou inválido.", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@RegisterActivity, "Erro de rede. Verifica a internet.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}