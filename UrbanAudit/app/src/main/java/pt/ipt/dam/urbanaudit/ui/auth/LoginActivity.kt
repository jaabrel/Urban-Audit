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

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val tokenManager = TokenManager(this)
        if (tokenManager.getToken() != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        btnLogin.setOnClickListener {
            val email = findViewById<EditText>(R.id.etEmail).text.toString()
            val password = findViewById<EditText>(R.id.etPassword).text.toString()

            lifecycleScope.launch {
                try {
                    val api = ApiClient.getClient(tokenManager).create(ApiService::class.java)
                    val response = api.login(LoginRequest(email, password))

                    if (response.isSuccessful && response.body() != null) {
                        tokenManager.saveToken(response.body()!!.access_token)
                        tokenManager.saveUserId(response.body()!!.userId)
                        tokenManager.saveRole(response.body()!!.role)
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "Erro no Login", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("API_ERRO", "Falha na rede: ${e.message}")
                    Toast.makeText(this@LoginActivity, "Erro de rede", Toast.LENGTH_SHORT).show()
                }
            }
        }
        val btnIrParaRegisto = findViewById<Button?>(R.id.btnIrParaRegisto)
        btnIrParaRegisto?.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        val btnLoginDemo = findViewById<Button?>(R.id.btnLoginDemo)
        btnLoginDemo?.setOnClickListener {
            findViewById<EditText>(R.id.etEmail)?.setText("estudante@ipt.pt")
            findViewById<EditText>(R.id.etPassword)?.setText("dam2026")
        }
    }
}