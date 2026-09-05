package pt.ipt.dam.urbanaudit.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch
import pt.ipt.dam.urbanaudit.R
import pt.ipt.dam.urbanaudit.data.local.AppDatabase
import pt.ipt.dam.urbanaudit.data.local.OcorrenciaEntity
import pt.ipt.dam.urbanaudit.data.local.TokenManager
import pt.ipt.dam.urbanaudit.data.model.Ocorrencia
import pt.ipt.dam.urbanaudit.data.remote.ApiClient
import pt.ipt.dam.urbanaudit.data.remote.ApiService
import pt.ipt.dam.urbanaudit.ui.about.SobreActivity
import pt.ipt.dam.urbanaudit.ui.auth.LoginActivity
import pt.ipt.dam.urbanaudit.ui.ocorrencia.CriarOcorrenciaActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager
    private lateinit var apiService: ApiService
    private lateinit var rvOcorrencias: RecyclerView
    private lateinit var layoutEstadoVazio: LinearLayout
    private lateinit var tvTotalRegistos: TextView
    private lateinit var tvIdentificacaoUtilizador: TextView
    private lateinit var chipGroupFiltros: ChipGroup

    private var adapter: OcorrenciaAdapter? = null
    private var todasAsOcorrencias: List<Ocorrencia> = emptyList()
    private var filtroSelecionado: String = "Todos"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tokenManager = TokenManager(this)

        val retrofit = ApiClient.getClient(tokenManager)
        apiService = retrofit.create(ApiService::class.java)

        inicializarVistas()
        configurarFiltros()
    }

    private fun inicializarVistas() {
        rvOcorrencias = findViewById(R.id.rvOcorrencias)
        rvOcorrencias.layoutManager = LinearLayoutManager(this)

        layoutEstadoVazio = findViewById(R.id.layoutEstadoVazio)
        tvTotalRegistos = findViewById(R.id.tvTotalRegistos)
        tvIdentificacaoUtilizador = findViewById(R.id.tvIdentificacaoUtilizador)
        chipGroupFiltros = findViewById(R.id.chipGroupFiltros)

        // Exibir e-mail e perfil do utilizador
        val email = tokenManager.getEmail() ?: "Utilizador"
        val role = tokenManager.getRole() ?: "user"
        tvIdentificacaoUtilizador.text = if (role == "admin") "$email (Admin)" else email

        // Botão para o ecrã SOBRE (Obrigatório)
        findViewById<Button?>(R.id.btnSobre)?.setOnClickListener {
            startActivity(Intent(this, SobreActivity::class.java))
        }

        // Botão Terminar Sessão
        findViewById<Button?>(R.id.btnSair)?.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Terminar Sessão")
                .setMessage("Deseja sair da sua conta?")
                .setPositiveButton("Sair") { _, _ ->
                    tokenManager.clear()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        // Botão para REPORTAR NOVA OCORRÊNCIA (FAB)
        findViewById<View?>(R.id.fabNovaOcorrencia)?.setOnClickListener {
            startActivity(Intent(this, CriarOcorrenciaActivity::class.java))
        }
    }

    private fun configurarFiltros() {
        chipGroupFiltros.setOnCheckedStateChangeListener { _, checkedIds ->
            filtroSelecionado = when {
                checkedIds.contains(R.id.chipVias) -> "Vias e Pavimento"
                checkedIds.contains(R.id.chipIluminacao) -> "Iluminação Pública"
                checkedIds.contains(R.id.chipResiduos) -> "Resíduos e Limpeza"
                checkedIds.contains(R.id.chipEspacosVerdes) -> "Espaços Verdes"
                checkedIds.contains(R.id.chipOutro) -> "Outro"
                else -> "Todos"
            }
            aplicarFiltro()
        }
    }

    override fun onResume() {
        super.onResume()
        carregarOcorrencias()
    }

    private fun carregarOcorrencias() {
        val dao = AppDatabase.getDatabase(this).ocorrenciaDao()

        lifecycleScope.launch {
            try {
                // 1. Tentar buscar à API (Requer Internet)
                val response = apiService.getOcorrencias()
                if (response.isSuccessful && response.body() != null) {
                    todasAsOcorrencias = response.body()!!

                    // 2. Guardar Localmente no Room para cache offline segura
                    try {
                        dao.deleteAll()
                        val listaParaGravar = todasAsOcorrencias.map {
                            OcorrenciaEntity(
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
                        dao.insertAll(listaParaGravar)
                    } catch (dbError: Exception) {
                        // Ignora erro se Room estiver em conflito de versão
                    }

                    // 3. Aplicar filtro e atualizar ecrã
                    aplicarFiltro()
                } else {
                    Toast.makeText(this@MainActivity, "Erro na API (${response.code()})", Toast.LENGTH_SHORT).show()
                }

            } catch (networkError: Exception) {
                // 4. FALHA DE REDE (Sem Internet): Tentar entrar no Modo Offline via Room
                try {
                    val listaOffline = dao.getAll()
                    if (listaOffline.isNotEmpty()) {
                        todasAsOcorrencias = listaOffline.map {
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
                        aplicarFiltro()
                        Toast.makeText(this@MainActivity, "Modo Offline (dados locais em cache)", Toast.LENGTH_LONG).show()
                    } else {
                        todasAsOcorrencias = emptyList()
                        aplicarFiltro()
                        Toast.makeText(this@MainActivity, "Sem internet e sem dados guardados.", Toast.LENGTH_LONG).show()
                    }
                } catch (dbOfflineError: Exception) {
                    Toast.makeText(this@MainActivity, "Sem internet e base de dados inacessível.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun aplicarFiltro() {
        val listaFiltrada = if (filtroSelecionado == "Todos") {
            todasAsOcorrencias
        } else {
            todasAsOcorrencias.filter { ocorrencia ->
                ocorrencia.obterCategoria().equals(filtroSelecionado, ignoreCase = true)
            }
        }

        if (adapter == null) {
            adapter = OcorrenciaAdapter(
                lista = listaFiltrada,
                meuUserId = tokenManager.getUserId(),
                meuRole = tokenManager.getRole() ?: "user",
                aoClicarApagar = { idOcorrencia -> apagarOcorrencia(idOcorrencia) }
            )
            rvOcorrencias.adapter = adapter
        } else {
            adapter?.atualizarLista(listaFiltrada)
        }

        tvTotalRegistos.text = "Ocorrências Registadas (${listaFiltrada.size})"

        if (listaFiltrada.isEmpty()) {
            layoutEstadoVazio.visibility = View.VISIBLE
            rvOcorrencias.visibility = View.GONE
        } else {
            layoutEstadoVazio.visibility = View.GONE
            rvOcorrencias.visibility = View.VISIBLE
        }
    }

    // Função que é chamada pelo botão de apagar dentro da RecyclerView
    private fun apagarOcorrencia(idOcorrencia: Int) {
        lifecycleScope.launch {
            try {
                val response = apiService.deleteOcorrencia(idOcorrencia)
                if (response.isSuccessful) {
                    Toast.makeText(this@MainActivity, "Ocorrência apagada com sucesso!", Toast.LENGTH_SHORT).show()
                    carregarOcorrencias()
                } else {
                    Toast.makeText(this@MainActivity, "Não tens permissão para apagar esta ocorrência.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Erro de rede ao apagar.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}