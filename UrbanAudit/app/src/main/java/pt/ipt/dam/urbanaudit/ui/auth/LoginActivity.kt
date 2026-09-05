package pt.ipt.dam.urbanaudit.ui.auth


import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import pt.ipt.dam.urbanaudit.R
import pt.ipt.dam.urbanaudit.data.local.TokenManager
import pt.ipt.dam.urbanaudit.data.model.*
import pt.ipt.dam.urbanaudit.data.remote.ApiClient
import pt.ipt.dam.urbanaudit.data.remote.ApiService
import pt.ipt.dam.urbanaudit.ui.main.MainActivity

/**
 * Ecrã de Autenticação (Login) da aplicação Urban Audit.
 * 
 * Responsabilidades:
 * - Validação de sessão existente através de TokenManager (auto-login).
 * - Recolha de credenciais (e-mail e palavra-passe).
 * - Comunicação assíncrona com a API REST remota usando Retrofit e Coroutines (lifecycleScope).
 * - Armazenamento seguro do token JWT, ID do utilizador e perfil de permissões (role).
 * - Navegação para o ecrã de registo ou ecrã principal.
 */
class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Gestor de preferências partilhadas para gestão do token JWT e dados de sessão
        val tokenManager = TokenManager(this)

        // Se o utilizador já tiver uma sessão ativa válida com token guardado, avança diretamente
        if (tokenManager.getToken() != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        btnLogin.setOnClickListener {
            val email = findViewById<EditText>(R.id.etEmail).text.toString().trim()
            val password = findViewById<EditText>(R.id.etPassword).text.toString().trim()

            // Validação simples de preenchimento antes do pedido de rede
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, preencha o e-mail e a palavra-passe.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Execução assíncrona em segundo plano para não bloquear a interface gráfica (UI Thread)
            lifecycleScope.launch {
                try {
                    val api = ApiClient.getClient(tokenManager).create(ApiService::class.java)
                    val response = api.login(LoginRequest(email, password))

                    // Sucesso: código HTTP 200 com token JWT retornado pelo servidor
                    if (response.isSuccessful && response.body() != null) {
                        val corpo = response.body()!!
                        // Gravação dos dados da sessão nas SharedPreferences
                        tokenManager.saveToken(corpo.access_token)
                        tokenManager.saveUserId(corpo.userId)
                        tokenManager.saveRole(corpo.role)
                        tokenManager.saveEmail(email)

                        // Redirecionamento para a página principal
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    } else {
                        // Credenciais incorretas ou utilizador não encontrado
                        Toast.makeText(this@LoginActivity, "Credenciais inválidas. Verifique o e-mail e a palavra-passe.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    // Tratamento de exceções de conectividade (sem ligação à internet ou servidor indisponível)
                    Log.e("API_ERRO", "Falha de comunicação na autenticação: ${e.message}")
                    Toast.makeText(this@LoginActivity, "Erro de rede ao contactar o servidor.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Navegação para o ecrã de registo de novos utilizadores
        val btnIrParaRegisto = findViewById<Button?>(R.id.btnIrParaRegisto)
        btnIrParaRegisto?.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}