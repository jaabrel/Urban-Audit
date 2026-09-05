package pt.ipt.dam.urbanaudit.ui.ocorrencia

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.ipt.dam.urbanaudit.R
import pt.ipt.dam.urbanaudit.data.local.TokenManager
import pt.ipt.dam.urbanaudit.data.model.OcorrenciaCreate
import pt.ipt.dam.urbanaudit.data.remote.ApiClient
import pt.ipt.dam.urbanaudit.data.remote.ApiService
import pt.ipt.dam.urbanaudit.utils.ImageUtils
import java.io.File
import java.util.Locale

/**
 * Ecrã de Registo e Publicação de Ocorrências Municipais (Urban Audit).
 * 
 * Integrações de Hardware e Sensores:
 * 1. Câmara Fotográfica: Captura de alta resolução com [FileProvider] e [ActivityResultContracts.TakePicture].
 * 2. Galeria do Dispositivo: Seleção alternativa de imagens através de [ActivityResultContracts.GetContent].
 * 3. Sensor GPS (Geolocalização): Deteção de coordenadas precisas via [FusedLocationProviderClient].
 * 4. Geocodificação Inversa (Geocoder): Conversão de coordenadas (Lat/Lng) em endereço legível (rua, localidade).
 * 
 * Regras de Negócio e Validações:
 * - Título com comprimento mínimo de 3 caracteres.
 * - Descrição com comprimento mínimo de 10 caracteres.
 * - Fotografia obrigatória convertida para Base64 com compressão JPEG.
 * - Tratamento de erros detalhados da API REST (incluindo parsing de validação HTTP 422).
 */
class CriarOcorrenciaActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Componentes de interface gráfica (Vistas)
    private lateinit var ivPreviewFoto: ImageView
    private lateinit var tvFotoAviso: TextView
    private lateinit var btnTirarFoto: MaterialButton
    private lateinit var etTitulo: TextInputEditText
    private lateinit var actvCategoria: AutoCompleteTextView
    private lateinit var etDescricao: TextInputEditText
    private lateinit var etLatitude: TextInputEditText
    private lateinit var etLongitude: TextInputEditText
    private lateinit var etEndereco: TextInputEditText
    private lateinit var tvStatusGps: TextView
    private lateinit var btnAtualizarGps: MaterialButton
    private lateinit var btnPublicar: MaterialButton

    // Ficheiro e dados da fotografia em memória
    private var ficheiroFotoTemp: File? = null
    private var uriFotoTemp: Uri? = null
    private var fotoBase64: String = ""

    // Coordenadas
    private var lat: Double = 39.6035
    private var lng: Double = -8.4078

    // Launcher de captura fotográfica em alta resolução via FileProvider
    private val tirarFotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { sucesso ->
        if (sucesso && ficheiroFotoTemp != null && ficheiroFotoTemp!!.exists()) {
            val bitmap = ImageUtils.carregarBitmapRedimensionado(ficheiroFotoTemp!!.absolutePath, 800, 800)
            if (bitmap != null) {
                ivPreviewFoto.setImageBitmap(bitmap)
                ivPreviewFoto.imageTintList = null
                fotoBase64 = ImageUtils.converterBitmapParaBase64(bitmap)
                tvFotoAviso.text = "Fotografia capturada com sucesso."
                tvFotoAviso.setTextColor(ContextCompat.getColor(this, R.color.secondary))
                btnTirarFoto.text = getString(R.string.btn_alterar_foto)
            }
        }
    }

    // Launcher de seleção de fotografia a partir da galeria do dispositivo
    private val escolherGaleriaLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    if (bitmap != null) {
                        ivPreviewFoto.setImageBitmap(bitmap)
                        ivPreviewFoto.imageTintList = null
                        fotoBase64 = ImageUtils.converterBitmapParaBase64(bitmap, 75)
                        tvFotoAviso.text = "Fotografia selecionada da galeria."
                        tvFotoAviso.setTextColor(ContextCompat.getColor(this, R.color.secondary))
                        btnTirarFoto.text = getString(R.string.btn_alterar_foto)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Erro ao carregar imagem da galeria", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Launcher de pedido de permissão de câmara
    private val pedirPermissaoCameraLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedida ->
        if (concedida) {
            iniciarCapturaFotografia()
        } else {
            Toast.makeText(this, "Permissão de câmara necessária para fotografar.", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher de pedido de permissões de localização (GPS)
    private val pedirPermissoesLocalizacaoLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissoes ->
        val fineGranted = permissoes[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissoes[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineGranted || coarseGranted) {
            obterLocalizacaoAtual()
        } else {
            tvStatusGps.text = "Sem permissão de GPS. Coordenadas preenchidas por defeito (Tomar)."
            definirCoordenadasPadraoTomar()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_criar_ocorrencia)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        inicializarVistas()
        configurarCategorias()
        configurarBotoes()

        // Obter automaticamente localização GPS de hardware ao entrar
        verificarPermissoesEObterLocalizacao()
    }

    private fun inicializarVistas() {
        val toolbar = findViewById<MaterialToolbar?>(R.id.toolbarCriar)
        toolbar?.setNavigationOnClickListener { finish() }

        ivPreviewFoto = findViewById(R.id.ivPreviewFoto)
        tvFotoAviso = findViewById(R.id.tvFotoAviso)
        btnTirarFoto = findViewById(R.id.btnTirarFoto)
        etTitulo = findViewById(R.id.etTitulo)
        actvCategoria = findViewById(R.id.actvCategoria)
        etDescricao = findViewById(R.id.etDescricao)
        etLatitude = findViewById(R.id.etLatitude)
        etLongitude = findViewById(R.id.etLongitude)
        etEndereco = findViewById(R.id.etEndereco)
        tvStatusGps = findViewById(R.id.tvStatusGps)
        btnAtualizarGps = findViewById(R.id.btnAtualizarGps)
        btnPublicar = findViewById(R.id.btnPublicar)
    }

    private fun configurarCategorias() {
        val categorias = arrayOf(
            "Vias e Pavimento",
            "Iluminação Pública",
            "Resíduos e Limpeza",
            "Espaços Verdes",
            "Sinalização e Trânsito",
            "Mobiliário Urbano",
            "Outro"
        )
        val adapterCategorias = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categorias)
        actvCategoria.setAdapter(adapterCategorias)
        actvCategoria.setText(categorias[0], false)
    }

    private fun configurarBotoes() {
        btnTirarFoto.setOnClickListener {
            verificarPermissoesEIniciarCamera()
        }

        findViewById<MaterialButton?>(R.id.btnGaleria)?.setOnClickListener {
            escolherGaleriaLauncher.launch("image/*")
        }

        btnAtualizarGps.setOnClickListener {
            verificarPermissoesEObterLocalizacao()
        }

        btnPublicar.setOnClickListener {
            validarEPublicar()
        }
    }

    // --- GESTÃO DA CÂMARA ---

    private fun verificarPermissoesEIniciarCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            iniciarCapturaFotografia()
        } else {
            pedirPermissaoCameraLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun iniciarCapturaFotografia() {
        try {
            ficheiroFotoTemp = ImageUtils.criarFicheiroImagem(this)
            uriFotoTemp = FileProvider.getUriForFile(
                this,
                "pt.ipt.dam.urbanaudit.fileprovider",
                ficheiroFotoTemp!!
            )
            tirarFotoLauncher.launch(uriFotoTemp!!)
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao abrir câmara: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    // --- GESTÃO DO GPS ---

    private fun verificarPermissoesEObterLocalizacao() {
        val finePerm = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarsePerm = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (finePerm == PackageManager.PERMISSION_GRANTED || coarsePerm == PackageManager.PERMISSION_GRANTED) {
            obterLocalizacaoAtual()
        } else {
            pedirPermissoesLocalizacaoLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun obterLocalizacaoAtual() {
        tvStatusGps.text = "A obter coordenadas GPS precisas..."

        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        preencherCoordenadas(location.latitude, location.longitude)
                    } else {
                        // Tentar obter a última localização conhecida em cache
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                            if (lastLoc != null) {
                                preencherCoordenadas(lastLoc.latitude, lastLoc.longitude)
                            } else {
                                definirCoordenadasPadraoTomar()
                            }
                        }.addOnFailureListener {
                            definirCoordenadasPadraoTomar()
                        }
                    }
                }
                .addOnFailureListener {
                    definirCoordenadasPadraoTomar()
                }
        } catch (e: SecurityException) {
            definirCoordenadasPadraoTomar()
        }
    }

    private fun preencherCoordenadas(latitude: Double, longitude: Double) {
        this.lat = latitude
        this.lng = longitude
        etLatitude.setText(String.format(Locale.US, "%.6f", latitude))
        etLongitude.setText(String.format(Locale.US, "%.6f", longitude))
        tvStatusGps.text = "Localização obtida com sucesso."
        tvStatusGps.setTextColor(ContextCompat.getColor(this, R.color.secondary))

        obterMoradaReversa(latitude, longitude)
    }

    private fun definirCoordenadasPadraoTomar() {
        this.lat = 39.6035
        this.lng = -8.4078
        etLatitude.setText(String.format(Locale.US, "%.6f", lat))
        etLongitude.setText(String.format(Locale.US, "%.6f", lng))
        etEndereco.setText("Quinta do Contador, Estrada da Serra, Tomar")
        tvStatusGps.text = "Coordenadas preenchidas (Tomar)"
    }

    private fun obterMoradaReversa(latitude: Double, longitude: Double) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(this@CriarOcorrenciaActivity, Locale.forLanguageTag("pt-PT"))
                @Suppress("DEPRECATION")
                val resultados: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)

                if (!resultados.isNullOrEmpty()) {
                    val endereco = resultados[0]
                    val rua = endereco.thoroughfare ?: ""
                    val localidade = endereco.locality ?: endereco.subAdminArea ?: "Tomar"
                    val textoMorada = if (rua.isNotBlank()) "$rua, $localidade" else localidade

                    withContext(Dispatchers.Main) {
                        etEndereco.setText(textoMorada)
                    }
                }
            } catch (e: Exception) {
                // Se a geocodificação inversa não responder, mantém apenas as coordenadas
            }
        }
    }

    // --- PUBLICAÇÃO ---

    private fun validarEPublicar() {
        val titulo = etTitulo.text?.toString()?.trim() ?: ""
        if (titulo.length < 3) {
            etTitulo.error = "O título deve ter no mínimo 3 caracteres."
            etTitulo.requestFocus()
            return
        }

        val categoria = actvCategoria.text?.toString()?.trim()?.ifBlank { "Outro" } ?: "Outro"
        val descUsuario = etDescricao.text?.toString()?.trim() ?: ""
        val endereco = etEndereco.text?.toString()?.trim() ?: ""

        // Se o utilizador editou manualmente as coordenadas (suporta ponto e vírgula)
        val latTexto = etLatitude.text?.toString()?.replace(',', '.')?.trim()
        val lngTexto = etLongitude.text?.toString()?.replace(',', '.')?.trim()
        val latFinal = (latTexto?.toDoubleOrNull() ?: lat).coerceIn(-90.0, 90.0)
        val lngFinal = (lngTexto?.toDoubleOrNull() ?: lng).coerceIn(-180.0, 180.0)

        // Formatação inteligente da descrição garantindo sempre no mínimo 10 caracteres
        val descricaoCompleta = buildString {
            append("[$categoria] ")
            if (descUsuario.isNotBlank()) {
                append(descUsuario)
            } else {
                append("Ocorrência registada na via pública.")
            }
            if (endereco.isNotBlank() && !descUsuario.contains(endereco)) {
                append(" (Local: $endereco)")
            }
        }.trim()

        if (descricaoCompleta.length < 10) {
            etDescricao.error = "A descrição deve ter no mínimo 10 caracteres."
            etDescricao.requestFocus()
            return
        }

        btnPublicar.isEnabled = false
        btnPublicar.text = "A publicar ocorrência..."

        lifecycleScope.launch {
            try {
                val api = ApiClient.getClient(TokenManager(this@CriarOcorrenciaActivity)).create(ApiService::class.java)
                val novaOcorrencia = OcorrenciaCreate(
                    titulo = titulo,
                    descricao = descricaoCompleta,
                    latitude = latFinal,
                    longitude = lngFinal,
                    fotoBase64 = fotoBase64,
                    categoria = categoria
                )
                val response = api.createOcorrencia(novaOcorrencia)

                if (response.isSuccessful) {
                    Toast.makeText(this@CriarOcorrenciaActivity, "Ocorrência publicada com sucesso!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    val erroCorpo = response.errorBody()?.string() ?: ""
                    Log.e("API_ERRO", "Falha HTTP ${response.code()}: $erroCorpo")
                    var detalhe = "Erro no servidor (${response.code()})"
                    try {
                        if (erroCorpo.isNotBlank()) {
                            val json = org.json.JSONObject(erroCorpo)
                            val detail = json.opt("detail")
                            if (detail is org.json.JSONArray) {
                                val listaErros = mutableListOf<String>()
                                for (i in 0 until detail.length()) {
                                    val errItem = detail.getJSONObject(i)
                                    val loc = errItem.optJSONArray("loc")
                                    val campo = if (loc != null && loc.length() > 1) loc.getString(loc.length() - 1) else "Campo"
                                    val msg = errItem.optString("msg", "Inválido")
                                    val campoNome = when (campo) {
                                        "titulo" -> "Título"
                                        "descricao" -> "Descrição"
                                        "latitude" -> "Latitude"
                                        "longitude" -> "Longitude"
                                        "fotoBase64" -> "Fotografia"
                                        else -> campo
                                    }
                                    listaErros.add("$campoNome: $msg")
                                }
                                detalhe = listaErros.joinToString("\n")
                            } else if (detail != null) {
                                detalhe = detail.toString()
                            }
                        }
                    } catch (e: Exception) {
                        detalhe = "Código de erro: ${response.code()}"
                    }

                    AlertDialog.Builder(this@CriarOcorrenciaActivity)
                        .setTitle("Erro na Publicação (${response.code()})")
                        .setMessage(detalhe)
                        .setPositiveButton("OK", null)
                        .show()

                    btnPublicar.isEnabled = true
                    btnPublicar.text = getString(R.string.btn_publicar)
                }
            } catch (e: Exception) {
                Toast.makeText(this@CriarOcorrenciaActivity, "Erro de rede: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                btnPublicar.isEnabled = true
                btnPublicar.text = getString(R.string.btn_publicar)
            }
        }
    }
}