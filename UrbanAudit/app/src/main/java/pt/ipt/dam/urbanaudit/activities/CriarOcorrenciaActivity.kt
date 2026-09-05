package pt.ipt.dam.urbanaudit.activities

import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.ipt.dam.urbanaudit.R
import pt.ipt.dam.urbanaudit.bd.AppDatabase
import pt.ipt.dam.urbanaudit.models.Ocorrencia
import pt.ipt.dam.urbanaudit.utils.ImageUtils
import pt.ipt.dam.urbanaudit.utils.SessionManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Ecrã de registo de nova ocorrência na via pública.
 * Integra os componentes de hardware obrigatórios do guião da UC:
 * 1. Câmara fotográfica nativa (via FileProvider para captura em alta resolução);
 * 2. Módulo de geolocalização GPS (via FusedLocationProviderClient e Geocoder).
 */
class CriarOcorrenciaActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Vistas do formulário
    private lateinit var ivPreviewFoto: ImageView
    private lateinit var tvFotoAviso: TextView
    private lateinit var btnTirarFoto: MaterialButton
    private lateinit var tilTitulo: TextInputLayout
    private lateinit var etTitulo: TextInputEditText
    private lateinit var tilCategoria: TextInputLayout
    private lateinit var actvCategoria: AutoCompleteTextView
    private lateinit var tilDescricao: TextInputLayout
    private lateinit var etDescricao: TextInputEditText
    private lateinit var etLatitude: TextInputEditText
    private lateinit var etLongitude: TextInputEditText
    private lateinit var etEndereco: TextInputEditText
    private lateinit var tvStatusGps: TextView
    private lateinit var btnAtualizarGps: MaterialButton
    private lateinit var btnPublicar: MaterialButton

    // Ficheiro e URI para a fotografia
    private var ficheiroFotoTemp: File? = null
    private var uriFotoTemp: Uri? = null
    private var caminhoFotoFinal: String = ""

    // Contratos de atividade para permissões e câmara
    private val tirarFotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { sucesso ->
        if (sucesso && ficheiroFotoTemp != null && ficheiroFotoTemp!!.exists()) {
            caminhoFotoFinal = ficheiroFotoTemp!!.absolutePath
            val bitmap = ImageUtils.carregarBitmapRedimensionado(caminhoFotoFinal, 600, 600)
            if (bitmap != null) {
                ivPreviewFoto.setImageBitmap(bitmap)
                ivPreviewFoto.imageTintList = null
                tvFotoAviso.text = "Fotografia capturada com sucesso."
                tvFotoAviso.setTextColor(ContextCompat.getColor(this, R.color.secondary))
                btnTirarFoto.text = getString(R.string.btn_alterar_foto)
            }
        }
    }

    private val pedirPermissaoCameraLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedida ->
        if (concedida) {
            iniciarCapturaFotografia()
        } else {
            Toast.makeText(this, "Permissão de câmara necessária para fotografar a ocorrência.", Toast.LENGTH_SHORT).show()
        }
    }

    private val pedirPermissoesLocalizacaoLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissoes ->
        val fineGranted = permissoes[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissoes[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineGranted || coarseGranted) {
            obterLocalizacaoAtual()
        } else {
            tvStatusGps.text = getString(R.string.status_gps_sem_permissao)
            // Coordenadas por defeito centradas em Tomar (IPT) caso a permissão seja recusada
            definirCoordenadasPadraoTomar()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_criar_ocorrencia)

        sessionManager = SessionManager(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        inicializarVistas()
        configurarCategorias()
        configurarBotoes()

        // Solicita automaticamente as coordenadas ao entrar no ecrã
        verificarPermissoesEObterLocalizacao()
    }

    private fun inicializarVistas() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarCriar)
        toolbar.setNavigationOnClickListener { finish() }

        ivPreviewFoto = findViewById(R.id.ivPreviewFoto)
        tvFotoAviso = findViewById(R.id.tvFotoAviso)
        btnTirarFoto = findViewById(R.id.btnTirarFoto)
        tilTitulo = findViewById(R.id.tilTitulo)
        etTitulo = findViewById(R.id.etTitulo)
        tilCategoria = findViewById(R.id.tilCategoria)
        actvCategoria = findViewById(R.id.actvCategoria)
        tilDescricao = findViewById(R.id.tilDescricao)
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
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categorias)
        actvCategoria.setAdapter(adapter)
        actvCategoria.setText(categorias[0], false)
    }

    private fun configurarBotoes() {
        btnTirarFoto.setOnClickListener {
            verificarPermissoesEIniciarCamera()
        }

        btnAtualizarGps.setOnClickListener {
            verificarPermissoesEObterLocalizacao()
        }

        btnPublicar.setOnClickListener {
            validarEPublicarOcorrencia()
        }
    }

    /**
     * Gestão do componente de hardware: Câmara Fotográfica.
     */
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
            Toast.makeText(this, "Erro ao preparar câmara: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Gestão do componente de hardware: Sensor de Geolocalização (GPS).
     */
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
        tvStatusGps.text = getString(R.string.status_gps_a_obter)

        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        preencherCoordenadas(location.latitude, location.longitude)
                    } else {
                        // Tenta obter a última localização conhecida em cache do dispositivo
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                            if (lastLoc != null) {
                                preencherCoordenadas(lastLoc.latitude, lastLoc.longitude)
                            } else {
                                definirCoordenadasPadraoTomar()
                            }
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

    private fun preencherCoordenadas(lat: Double, lng: Double) {
        etLatitude.setText(String.format(Locale.US, "%.6f", lat))
        etLongitude.setText(String.format(Locale.US, "%.6f", lng))
        tvStatusGps.text = getString(R.string.status_gps_obtido)
        tvStatusGps.setTextColor(ContextCompat.getColor(this, R.color.secondary))

        // Geocodificação reversa para obter a morada em Portugal
        obterMoradaReversa(lat, lng)
    }

    private fun definirCoordenadasPadraoTomar() {
        // Coordenadas centrais de Tomar (IPT - Campus da Quinta do Contador)
        val latPadrao = 39.6035
        val lngPadrao = -8.4078
        etLatitude.setText(String.format(Locale.US, "%.6f", latPadrao))
        etLongitude.setText(String.format(Locale.US, "%.6f", lngPadrao))
        etEndereco.setText("Quinta do Contador, Estrada da Serra, Tomar")
        tvStatusGps.text = "Coordenadas preenchidas (Tomar)"
    }

    /**
     * Converte as coordenadas geográficas numa morada legível através do Geocoder.
     */
    private fun obterMoradaReversa(lat: Double, lng: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val geocoder = Geocoder(this@CriarOcorrenciaActivity, Locale.forLanguageTag("pt-PT"))
                @Suppress("DEPRECATION")
                val resultados: List<Address>? = geocoder.getFromLocation(lat, lng, 1)

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
                // Em caso de falha de ligação ao serviço de geocodificação, mantém o texto existente
            }
        }
    }

    /**
     * Valida os campos obrigatórios e publica a ocorrência localmente no SQLite / Room.
     */
    private fun validarEPublicarOcorrencia() {
        val titulo = etTitulo.text.toString().trim()
        val descricao = etDescricao.text.toString().trim()
        val categoria = actvCategoria.text.toString().trim().ifBlank { "Geral" }
        val endereco = etEndereco.text.toString().trim()

        val latStr = etLatitude.text.toString().trim()
        val lngStr = etLongitude.text.toString().trim()

        var formularioValido = true

        if (titulo.isEmpty()) {
            tilTitulo.error = getString(R.string.erro_titulo_obrigatorio)
            formularioValido = false
        } else {
            tilTitulo.error = null
        }

        if (descricao.isEmpty()) {
            tilDescricao.error = getString(R.string.erro_descricao_obrigatoria)
            formularioValido = false
        } else {
            tilDescricao.error = null
        }

        if (caminhoFotoFinal.isBlank() || !File(caminhoFotoFinal).exists()) {
            Toast.makeText(this, getString(R.string.erro_foto_obrigatoria), Toast.LENGTH_LONG).show()
            formularioValido = false
        }

        val latitude = latStr.toDoubleOrNull() ?: 39.6035
        val longitude = lngStr.toDoubleOrNull() ?: -8.4078

        if (!formularioValido) {
            return
        }

        val dataHoraAtual = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

        val novaOcorrencia = Ocorrencia(
            titulo,
            descricao,
            categoria,
            latitude,
            longitude,
            if (endereco.isBlank()) "Tomar, Portugal" else endereco,
            caminhoFotoFinal,
            "",
            dataHoraAtual,
            sessionManager.getEmail(),
            "Pendente",
            false
        )

        // Gravação assíncrona na base de dados Room (sem chamadas à API REST conforme exigido)
        CoroutineScope(Dispatchers.IO).launch {
            AppDatabase.getInstance(this@CriarOcorrenciaActivity).ocorrenciaDao().inserir(novaOcorrencia)

            withContext(Dispatchers.Main) {
                Toast.makeText(this@CriarOcorrenciaActivity, getString(R.string.sucesso_publicacao), Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}