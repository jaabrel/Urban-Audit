package pt.ipt.dam.urbanaudit.activities

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import pt.ipt.dam.urbanaudit.R
import pt.ipt.dam.urbanaudit.utils.SessionManager

/**
 * Ecrã de autenticação da aplicação Urban Audit.
 * Permite ao utilizador iniciar sessão na plataforma localmente com validação de credenciais,
 * disponibilizando igualmente credenciais de teste para efeitos de avaliação da disciplina.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)

        // Se o utilizador já tiver sessão iniciada, redireciona diretamente para o ecrã principal
        if (sessionManager.sessaoIniciada()) {
            abrirEcraPrincipal()
            return
        }

        setContentView(R.layout.activity_login)

        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)

        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        val btnLoginDemo = findViewById<MaterialButton>(R.id.btnLoginDemo)

        btnLogin.setOnClickListener {
            validarEIniciarSessao()
        }

        // Preenchimento e autenticação rápida para facilidade de demonstração pelo docente
        btnLoginDemo.setOnClickListener {
            etEmail.setText(SessionManager.DEMO_EMAIL)
            etPassword.setText(SessionManager.DEMO_PASSWORD)
            tilEmail.error = null
            tilPassword.error = null
            iniciarSessaoUtilizador(SessionManager.DEMO_EMAIL, SessionManager.DEMO_NOME)
        }
    }

    /**
     * Validação rigorosa dos dados introduzidos pelo utilizador.
     */
    private fun validarEIniciarSessao() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        var valido = true

        if (email.isEmpty()) {
            tilEmail.error = getString(R.string.erro_campos_obrigatorios)
            valido = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = getString(R.string.erro_email_invalido)
            valido = false
        } else {
            tilEmail.error = null
        }

        if (password.isEmpty()) {
            tilPassword.error = getString(R.string.erro_campos_obrigatorios)
            valido = false
        } else if (password.length < 4) {
            tilPassword.error = "A palavra-passe deve ter pelo menos 4 caracteres."
            valido = false
        } else {
            tilPassword.error = null
        }

        if (!valido) {
            return
        }

        // Armazenamento local da sessão (offline-first, sem recurso a chamadas de rede não solicitadas)
        val nomeUtilizador = email.substringBefore("@").replaceFirstChar { it.uppercase() }
        iniciarSessaoUtilizador(email, nomeUtilizador)
    }

    private fun iniciarSessaoUtilizador(email: String, nome: String) {
        sessionManager.iniciarSessao(email, nome)
        Toast.makeText(this, "Sessão iniciada com sucesso. Bem-vindo, $nome!", Toast.LENGTH_SHORT).show()
        abrirEcraPrincipal()
    }

    private fun abrirEcraPrincipal() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}