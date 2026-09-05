package pt.ipt.dam.urbanaudit.ui.ocorrencia

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import pt.ipt.dam.urbanaudit.R
import pt.ipt.dam.urbanaudit.data.local.TokenManager
import pt.ipt.dam.urbanaudit.data.model.OcorrenciaCreate
import pt.ipt.dam.urbanaudit.data.remote.ApiClient
import pt.ipt.dam.urbanaudit.data.remote.ApiService
import pt.ipt.dam.urbanaudit.utils.ImageUtils

class CriarOcorrenciaActivity : AppCompatActivity() {

    private lateinit var ivFoto: ImageView
    private lateinit var tvCoordenadas: TextView
    private var fotoBase64: String? = null
    private var lat: Double? = null
    private var lng: Double? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // LAUNCHER 1: CÂMARA
    private val tirarFotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            ivFoto.setImageBitmap(bitmap)
            fotoBase64 = ImageUtils.converterBitmapParaBase64(bitmap)
        } else {
            Toast.makeText(this, "Foto cancelada", Toast.LENGTH_SHORT).show()
        }
    }

    // LAUNCHER 2: GALERIA
    private val escolherGaleriaLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            ivFoto.setImageBitmap(bitmap)
            fotoBase64 = ImageUtils.converterBitmapParaBase64(bitmap)
        } else {
            Toast.makeText(this, "Nenhuma imagem selecionada", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_criar_ocorrencia) // Cria o XML

        ivFoto = findViewById(R.id.ivFoto)
        tvCoordenadas = findViewById(R.id.tvCoordenadas)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val etTitulo = findViewById<EditText>(R.id.etTitulo)
        val etDescricao = findViewById<EditText>(R.id.etDescricao)

        // Botão Câmara
        findViewById<Button>(R.id.btnCamera).setOnClickListener {
            tirarFotoLauncher.launch(null)
        }

        // Botão Galeria
        findViewById<Button>(R.id.btnGaleria).setOnClickListener {
            escolherGaleriaLauncher.launch("image/*") // Filtra só por imagens
        }

        obterLocalizacaoGPS()

        findViewById<Button>(R.id.btnSubmeter).setOnClickListener {
            val titulo = etTitulo.text.toString().trim()
            val descricao = etDescricao.text.toString().trim()
            // REQUISITO: Validações Rigorosas da Inserção
            if (titulo.isEmpty()) {
                etTitulo.error = "O título é obrigatório"
                return@setOnClickListener
            }
            if (descricao.isEmpty()) {
                etDescricao.error = "A descrição é obrigatória"
                return@setOnClickListener
            }
            if (fotoBase64 == null) {
                Toast.makeText(this, "Por favor, tira uma foto ou escolhe da galeria", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (lat == null || lng == null) {
                Toast.makeText(this, "A aguardar sinal GPS... Certifica-te que tens a localização ligada.", Toast.LENGTH_LONG).show()
                obterLocalizacaoGPS() // Tenta forçar novamente
                return@setOnClickListener
            }
            val request = OcorrenciaCreate(titulo, descricao, lat!!, lng!!, fotoBase64!!)
            val tokenManager = TokenManager(this)
            // REQUISITO: Tratamento de Erros no envio
            lifecycleScope.launch {
                try {
                    val api = ApiClient.getClient(tokenManager).create(ApiService::class.java)
                    val response = api.createOcorrencia(request)
                    if (response.isSuccessful) {
                        Toast.makeText(this@CriarOcorrenciaActivity, "Ocorrência enviada com sucesso!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@CriarOcorrenciaActivity, "Erro ao enviar para o servidor. Tenta novamente.", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@CriarOcorrenciaActivity, "Falha na rede. Ficheiro muito grande ou sem internet.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun obterLocalizacaoGPS() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                lat = location.latitude
                lng = location.longitude
                // Mostrar a coordenada na interface
                tvCoordenadas.text = "GPS: Lat ${lat.toString().take(7)}, Lng ${lng.toString().take(7)}"
                tvCoordenadas.setTextColor(android.graphics.Color.parseColor("#388E3C")) // Verde para OK
            } else {
                tvCoordenadas.text = "Sinal GPS não encontrado. Liga a localização."
            }
        }
    }
}