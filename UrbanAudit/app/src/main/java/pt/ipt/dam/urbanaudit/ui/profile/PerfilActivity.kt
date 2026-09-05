package pt.ipt.dam.urbanaudit.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import pt.ipt.dam.urbanaudit.R
import pt.ipt.dam.urbanaudit.data.local.AppDatabase
import pt.ipt.dam.urbanaudit.data.local.TokenManager
import pt.ipt.dam.urbanaudit.data.model.Ocorrencia
import pt.ipt.dam.urbanaudit.data.remote.ApiClient
import pt.ipt.dam.urbanaudit.data.remote.ApiService
import pt.ipt.dam.urbanaudit.ui.auth.LoginActivity
import pt.ipt.dam.urbanaudit.ui.main.OcorrenciaAdapter

class PerfilActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager
    private lateinit var apiService: ApiService

    private lateinit var tvPerfilEmail: TextView
    private lateinit var tvPerfilRole: TextView
    private lateinit var tvTotalMinhasOcorrencias: TextView
    private lateinit var tvSubtituloMinhas: TextView
    private lateinit var btnLogoff: MaterialButton
    private lateinit var rvMinhasOcorrencias: RecyclerView
    private lateinit var layoutMinhasVazio: LinearLayout

    private var adapter: OcorrenciaAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        tokenManager = TokenManager(this)
        val retrofit = ApiClient.getClient(tokenManager)
        apiService = retrofit.create(ApiService::class.java)

        inicializarVistas()
        apresentarDadosUtilizador()
        carregarMinhasOcorrencias()
    }

    private fun inicializarVistas() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarPerfil)
        toolbar.setNavigationOnClickListener { finish() }

        tvPerfilEmail = findViewById(R.id.tvPerfilEmail)
        tvPerfilRole = findViewById(R.id.tvPerfilRole)
        tvTotalMinhasOcorrencias = findViewById(R.id.tvTotalMinhasOcorrencias)
        tvSubtituloMinhas = findViewById(R.id.tvSubtituloMinhas)
        btnLogoff = findViewById(R.id.btnLogoff)
        rvMinhasOcorrencias = findViewById(R.id.rvMinhasOcorrencias)
        layoutMinhasVazio = findViewById(R.id.layoutMinhasVazio)

        rvMinhasOcorrencias.layoutManager = LinearLayoutManager(this)

        btnLogoff.setOnClickListener {
            confirmarLogoff()
        }
    }

    private fun apresentarDadosUtilizador() {
        val email = tokenManager.getEmail() ?: "Utilizador"
        val role = tokenManager.getRole() ?: "user"

        tvPerfilEmail.text = email
        tvPerfilRole.text = if (role.equals("admin", ignoreCase = true)) "Administrador" else "Cidadão / Utilizador"
    }

    override fun onResume() {
        super.onResume()
        carregarMinhasOcorrencias()
    }

    private fun carregarMinhasOcorrencias() {
        val meuUserId = tokenManager.getUserId()
        val meuRole = tokenManager.getRole() ?: "user"

        lifecycleScope.launch {
            try {
                val response = apiService.getOcorrencias()
                if (response.isSuccessful && response.body() != null) {
                    val todas = response.body()!!
                    // Filtra apenas as ocorrências cujo autor é o utilizador autenticado
                    val minhas = todas.filter { it.owner_id == meuUserId }
                    atualizarInterfaceMinhas(minhas, meuUserId, meuRole)
                } else {
                    // Fallback Room offline
                    carregarDoRoom(meuUserId, meuRole)
                }
            } catch (e: Exception) {
                // Fallback Room em caso de falha de rede
                carregarDoRoom(meuUserId, meuRole)
            }
        }
    }

    private fun carregarDoRoom(meuUserId: Int, meuRole: String) {
        val dao = AppDatabase.getDatabase(this).ocorrenciaDao()
        lifecycleScope.launch {
            try {
                val listaOffline = dao.getAll()
                val minhas = listaOffline
                    .filter { it.owner_id == meuUserId }
                    .map {
                        Ocorrencia(
                            id = it.id,
                            titulo = it.titulo,
                            descricao = it.descricao,
                            latitude = it.latitude,
                            longitude = it.longitude,
                            fotoBase64 = it.fotoBase64,
                            estado = it.estado,
                            owner_id = it.owner_id
                        )
                    }
                atualizarInterfaceMinhas(minhas, meuUserId, meuRole)
            } catch (dbError: Exception) {
                atualizarInterfaceMinhas(emptyList(), meuUserId, meuRole)
            }
        }
    }

    private fun atualizarInterfaceMinhas(minhas: List<Ocorrencia>, meuUserId: Int, meuRole: String) {
        tvTotalMinhasOcorrencias.text = "${minhas.size}"
        tvSubtituloMinhas.text = "${minhas.size} registo(s)"

        if (adapter == null) {
            adapter = OcorrenciaAdapter(
                lista = minhas,
                meuUserId = meuUserId,
                meuRole = meuRole,
                aoClicarApagar = { idOcorrencia -> apagarMinhaOcorrencia(idOcorrencia) }
            )
            rvMinhasOcorrencias.adapter = adapter
        } else {
            adapter?.atualizarLista(minhas)
        }

        if (minhas.isEmpty()) {
            layoutMinhasVazio.visibility = View.VISIBLE
            rvMinhasOcorrencias.visibility = View.GONE
        } else {
            layoutMinhasVazio.visibility = View.GONE
            rvMinhasOcorrencias.visibility = View.VISIBLE
        }
    }

    private fun apagarMinhaOcorrencia(idOcorrencia: Int) {
        lifecycleScope.launch {
            try {
                val response = apiService.deleteOcorrencia(idOcorrencia)
                if (response.isSuccessful) {
                    Toast.makeText(this@PerfilActivity, "Ocorrência eliminada com sucesso!", Toast.LENGTH_SHORT).show()
                    carregarMinhasOcorrencias()
                } else {
                    Toast.makeText(this@PerfilActivity, "Não foi possível eliminar a ocorrência (${response.code()})", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PerfilActivity, "Erro de rede ao eliminar: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmarLogoff() {
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Terminar Sessão")
            .setMessage("Deseja sair da sua conta e regressar ao ecrã de início de sessão?")
            .setPositiveButton("Sim, Sair") { _, _ ->
                tokenManager.clear()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finishAffinity()
            }
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_white)
        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.red_error))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
    }
}
