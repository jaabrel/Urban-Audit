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

/**
 * Ecrã Principal (Dashboard / Feed) da aplicação Urban Audit.
 * 
 * Funcionalidades principais:
 * - Apresentação da lista de ocorrências urbanas georreferenciadas.
 * - Suporte a Modo Offline com persistência local em base de dados SQLite via AndroidX Room.
 * - Sincronização automática com a API REST remota sempre que há conectividade.
 * - Filtragem dinâmica por categorias (Vias, Iluminação, Limpeza, Espaços Verdes, etc.).
 * - Controlo de acesso: verificação de sessão ativa no ciclo de vida (onResume) e permissões de eliminação.
 * - Navegação rápida para os ecrãs Sobre, Perfil de Utilizador e Criar Nova Ocorrência (FAB).
 */
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

        // Inicialização do gestor de sessão e cliente HTTP Retrofit
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

        // Navegação para a Página de Perfil (com Logoff e Os Meus Posts)
        val abrirPerfil: (View) -> Unit = {
            startActivity(Intent(this, pt.ipt.dam.urbanaudit.ui.profile.PerfilActivity::class.java))
        }
        findViewById<Button?>(R.id.btnPerfil)?.setOnClickListener(abrirPerfil)
        findViewById<View?>(R.id.layoutBadgeUtilizador)?.setOnClickListener(abrirPerfil)

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

    /**
     * Verificação de segurança no regresso ao ecrã:
     * Garante que se a sessão for terminada noutra atividade (ex: Perfil),
     * o utilizador é imediatamente reencaminhado para o Login.
     */
    override fun onResume() {
        super.onResume()
        if (tokenManager.getToken() == null) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }
        carregarOcorrencias()
    }

    /**
     * Estratégia de Carregamento de Dados (Offline-First / Resiliente):
     * 1. Contacta a API REST para obter os registos mais atualizados da cloud.
     * 2. Em caso de sucesso, atualiza a base de dados local Room (cache) para permitir acesso futuro sem net.
     * 3. Em caso de falha de rede (ex: sem Wi-Fi/dados), recorre automaticamente à base de dados Room.
     */
    private fun carregarOcorrencias() {
        val dao = AppDatabase.getDatabase(this).ocorrenciaDao()

        lifecycleScope.launch {
            try {
                // 1. Tentar obter da API REST remota (requer conectividade)
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

    /**
     * Aplica o filtro selecionado (Vias, Iluminação, Limpeza, etc.) à lista global de ocorrências,
     * atualiza o adaptador da RecyclerView e gere a visibilidade do estado vazio.
     */
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

    /**
     * Elimina uma ocorrência remotamente através da API REST.
     * Operação protegida pelo servidor: apenas o próprio autor ou administradores têm permissão.
     */
    private fun apagarOcorrencia(idOcorrencia: Int) {
        lifecycleScope.launch {
            try {
                val response = apiService.deleteOcorrencia(idOcorrencia)
                if (response.isSuccessful) {
                    Toast.makeText(this@MainActivity, "Ocorrência apagada com sucesso!", Toast.LENGTH_SHORT).show()
                    carregarOcorrencias()
                } else {
                    Toast.makeText(this@MainActivity, "Não tem permissão para apagar esta ocorrência.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Erro de rede ao apagar.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}