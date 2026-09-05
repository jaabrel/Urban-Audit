package pt.ipt.dam.urbanaudit.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import pt.ipt.dam.urbanaudit.R
import pt.ipt.dam.urbanaudit.data.local.TokenManager
import pt.ipt.dam.urbanaudit.data.remote.ApiClient
import pt.ipt.dam.urbanaudit.data.remote.ApiService
import pt.ipt.dam.urbanaudit.ui.about.SobreActivity
import pt.ipt.dam.urbanaudit.ui.ocorrencia.CriarOcorrenciaActivity
import pt.ipt.dam.urbanaudit.data.local.AppDatabase
import pt.ipt.dam.urbanaudit.data.local.OcorrenciaEntity
import pt.ipt.dam.urbanaudit.data.model.Ocorrencia

class MainActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager
    private lateinit var apiService: ApiService
    private lateinit var rvOcorrencias: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tokenManager = TokenManager(this)

        val retrofit = ApiClient.getClient(tokenManager)
        apiService = retrofit.create(ApiService::class.java)

        rvOcorrencias = findViewById(R.id.rvOcorrencias)
        rvOcorrencias.layoutManager = LinearLayoutManager(this)

        // Botão para o ecrã SOBRE (Obrigatório)
        findViewById<Button>(R.id.btnSobre).setOnClickListener {
            startActivity(Intent(this, SobreActivity::class.java))
        }
        // Botão para REPORTAR NOVA OCORRÊNCIA
        findViewById<Button>(R.id.btnNovaOcorrencia).setOnClickListener {
            startActivity(Intent(this, CriarOcorrenciaActivity::class.java))
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
                    val listaOcorrenciasApi = response.body()!!

                    // 2. Guardar Localmente no Room (Fazer "Cache")
                    dao.deleteAll() // Limpar as velhas

                    // Converter da classe 'Ocorrencia' (API) para 'OcorrenciaEntity' (Room)
                    val listaParaGravar = listaOcorrenciasApi.map {
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
                    dao.insertAll(listaParaGravar) // Guardar no telemóvel
                    // 3. Mostrar no ecrã
                    configurarAdapter(listaOcorrenciasApi)
                } else {
                    Toast.makeText(this@MainActivity, "Erro na API", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                // 4. FALHA DE REDE (Sem Internet): Entra o Modo Offline!
                val listaOffline = dao.getAll() // Ir buscar ao telemóvel

                if (listaOffline.isNotEmpty()) {
                    // Converter de volta de 'OcorrenciaEntity' para 'Ocorrencia'
                    val listaParaMostrar = listaOffline.map {
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
                    configurarAdapter(listaParaMostrar) // Mostrar os dados antigos
                    Toast.makeText(this@MainActivity, "Modo Offline (Sem Rede): A mostrar dados guardados", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@MainActivity, "Sem internet e sem dados guardados.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Função que é chamada pelo botão vermelho de apagar dentro da RecyclerView
    private fun apagarOcorrencia(idOcorrencia: Int) {
        lifecycleScope.launch {
            try {
                val response = apiService.deleteOcorrencia(idOcorrencia)
                if (response.isSuccessful) {
                    Toast.makeText(this@MainActivity, "Ocorrência apagada!", Toast.LENGTH_SHORT).show()
                    // Recarrega a lista para a ocorrência desaparecer da interface
                    carregarOcorrencias()
                } else {
                    Toast.makeText(this@MainActivity, "Não podes apagar esta ocorrência.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Erro de rede ao apagar.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun configurarAdapter(lista: List<Ocorrencia>) {
        val adapter = OcorrenciaAdapter(
            lista = lista,
            meuUserId = tokenManager.getUserId(),
            meuRole = tokenManager.getRole() ?: "user",
            aoClicarApagar = { idOcorrencia -> apagarOcorrencia(idOcorrencia) }
        )
        rvOcorrencias.adapter = adapter
    }
}