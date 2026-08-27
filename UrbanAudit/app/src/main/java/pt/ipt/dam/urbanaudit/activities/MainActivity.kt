package pt.ipt.dam.urbanaudit.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import pt.ipt.dam.urbanaudit.R
import pt.ipt.dam.urbanaudit.retrofit.RetrofitClient
import pt.ipt.dam.urbanaudit.bd.AppDatabase
import pt.ipt.dam.urbanaudit.models.Ocorrencia
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: OcorrenciaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Botão para aceder à secção/janela obrigatória (disponível a partir do ecrã inicial)
        findViewById<Button>(R.id.btnSobre).setOnClickListener {
            startActivity(Intent(this, SobreActivity::class.java))
        }

        // Botão para registar nova ocorrência
        findViewById<Button>(R.id.btnNovaOcorrencia).setOnClickListener {
            startActivity(Intent(this, CriarOcorrenciaActivity::class.java))
        }

        // Configurar a lista (RecyclerView) para mostrar as ocorrências
        recyclerView = findViewById(R.id.rvOcorrencias)
        recyclerView.layoutManager = LinearLayoutManager(this)

        carregarOcorrencias()
    }
    override fun onResume() {
        super.onResume()
        // Atualiza a lista sempre que o utilizador regressa do ecrã de criação
        carregarOcorrencias()
    }

    private fun carregarOcorrencias() {
        val token = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("TOKEN", "") ?: ""

        // 1. Interação com API em modo REST
        RetrofitClient.instance.getOcorrencias(token).enqueue(object : Callback<List<Ocorrencia>> {
            override fun onResponse(call: Call<List<Ocorrencia>>, response: Response<List<Ocorrencia>>) {
                if (response.isSuccessful) {
                    val ocorrencias = response.body() ?: emptyList()
                    adapter = OcorrenciaAdapter(ocorrencias)
                    recyclerView.adapter = adapter
                } else {
                    // Validação e mensagens de erro adequadas
                    Toast.makeText(this@MainActivity, "Erro ao carregar dados: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Ocorrencia>>, t: Throwable) {
                // 2. Se a API falhar (ex: sem internet), ler do armazenamento local
                Toast.makeText(this@MainActivity, "Sem ligação. A carregar dados locais.", Toast.LENGTH_LONG).show()

                CoroutineScope(Dispatchers.IO).launch {
                    val ocorrenciasLocais = AppDatabase(this@MainActivity).ocorrenciaDao().obterNaoSincronizadas()

                    withContext(Dispatchers.Main) {
                        adapter = OcorrenciaAdapter(ocorrenciasLocais)
                        recyclerView.adapter = adapter
                    }
                }
            }
        })
    }
}