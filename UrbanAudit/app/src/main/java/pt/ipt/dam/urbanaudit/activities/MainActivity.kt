package pt.ipt.dam.urbanaudit.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.ipt.dam.urbanaudit.R
import pt.ipt.dam.urbanaudit.bd.AppDatabase
import pt.ipt.dam.urbanaudit.models.Ocorrencia
import pt.ipt.dam.urbanaudit.utils.SessionManager

/**
 * Ecrã Principal da aplicação Urban Audit.
 * Apresenta a listagem de ocorrências municipais registadas localmente,
 * permitindo filtragem por categoria, navegação para a secção "Sobre",
 * visualização detalhada e abertura do fluxo de registo de nova ocorrência.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: OcorrenciaAdapter
    private lateinit var layoutEstadoVazio: LinearLayout
    private lateinit var tvTotalRegistos: TextView
    private lateinit var tvIdentificacaoUtilizador: TextView
    private lateinit var chipGroupFiltros: ChipGroup

    private var todasAsOcorrencias: List<Ocorrencia> = emptyList()
    private var filtroSelecionado: String = "Todos"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)

        // Verificação de controlo de acesso: utilizador tem de estar autenticado
        if (!sessionManager.sessaoIniciada()) {
            redirecionarParaLogin()
            return
        }

        setContentView(R.layout.activity_main)

        // Inicialização de vistas
        recyclerView = findViewById(R.id.rvOcorrencias)
        layoutEstadoVazio = findViewById(R.id.layoutEstadoVazio)
        tvTotalRegistos = findViewById(R.id.tvTotalRegistos)
        tvIdentificacaoUtilizador = findViewById(R.id.tvIdentificacaoUtilizador)
        chipGroupFiltros = findViewById(R.id.chipGroupFiltros)

        tvIdentificacaoUtilizador.text = sessionManager.getEmail()

        // Configuração da lista
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = OcorrenciaAdapter(emptyList()) { ocorrencia ->
            abrirDetalheOcorrencia(ocorrencia)
        }
        recyclerView.adapter = adapter

        // Botão Sobre obrigatório pelo guião da UC DAM (acessível no ecrã inicial)
        findViewById<MaterialButton>(R.id.btnSobre).setOnClickListener {
            startActivity(Intent(this, SobreActivity::class.java))
        }

        // Botão para aceder ao perfil ou terminar sessão
        findViewById<View?>(R.id.btnPerfil)?.setOnClickListener {
            confirmarTerminarSessao()
        }

        // Botão flutuante para registar nova ocorrência com foto e GPS
        findViewById<ExtendedFloatingActionButton>(R.id.fabNovaOcorrencia).setOnClickListener {
            startActivity(Intent(this, CriarOcorrenciaActivity::class.java))
        }

        // Configuração dos filtros por categoria
        configurarFiltros()

        carregarOcorrencias()
    }

    override fun onResume() {
        super.onResume()
        // Atualiza os dados sempre que o utilizador regressa a este ecrã
        carregarOcorrencias()
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
     * Carrega as ocorrências guardadas na base de dados local SQLite via Room.
     */
    private fun carregarOcorrencias() {
        CoroutineScope(Dispatchers.IO).launch {
            val lista = AppDatabase.getInstance(this@MainActivity).ocorrenciaDao().obterTodas()

            withContext(Dispatchers.Main) {
                todasAsOcorrencias = lista
                aplicarFiltro()
            }
        }
    }

    /**
     * Aplica o filtro selecionado à lista em memória.
     */
    private fun aplicarFiltro() {
        val listaFiltrada = if (filtroSelecionado == "Todos") {
            todasAsOcorrencias
        } else {
            todasAsOcorrencias.filter { it.categoria.equals(filtroSelecionado, ignoreCase = true) }
        }

        adapter.atualizarLista(listaFiltrada)
        tvTotalRegistos.text = "Ocorrências Registadas (${listaFiltrada.size})"

        if (listaFiltrada.isEmpty()) {
            layoutEstadoVazio.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            layoutEstadoVazio.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun abrirDetalheOcorrencia(ocorrencia: Ocorrencia) {
        val intent = Intent(this, DetalheOcorrenciaActivity::class.java)
        intent.putExtra("ID_OCORRENCIA", ocorrencia.idLocal)
        startActivity(intent)
    }

    private fun confirmarTerminarSessao() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Terminar Sessão")
            .setMessage("Deseja sair da sua conta?")
            .setPositiveButton("Sair") { _, _ ->
                sessionManager.terminarSessao()
                redirecionarParaLogin()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun redirecionarParaLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}