package pt.ipt.dam.urbanaudit.ui.ocorrencia

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import pt.ipt.dam.urbanaudit.R
import pt.ipt.dam.urbanaudit.data.local.TokenManager
import pt.ipt.dam.urbanaudit.data.model.OcorrenciaCreate
import pt.ipt.dam.urbanaudit.data.remote.ApiClient
import pt.ipt.dam.urbanaudit.data.remote.ApiService
import java.io.ByteArrayOutputStream

class CriarOcorrenciaActivity : AppCompatActivity() {

    private var fotoBase64: String = ""
    private var lat: Double = 0.0
    private var lng: Double = 0.0

    // O objeto que lança a Câmara
    private val tirarFotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            findViewById<ImageView>(R.id.ivPreview).setImageBitmap(bitmap)
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream)
            fotoBase64 = Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_criar_ocorrencia) // Cria o XML
        // 1. Botão da Câmara
        findViewById<Button>(R.id.btnTirarFoto).setOnClickListener {
            tirarFotoLauncher.launch(null)
        }
        // 2. Obter GPS automaticamente ao abrir o ecrã
        obterLocalizacaoGPS()
        // 3. Botão Guardar e Enviar para API
        findViewById<Button>(R.id.btnSubmeter).setOnClickListener {
            val titulo = findViewById<EditText>(R.id.etTitulo).text.toString()
            val desc = findViewById<EditText>(R.id.etDescricao).text.toString()
            lifecycleScope.launch {
                val api = ApiClient.getClient(TokenManager(this@CriarOcorrenciaActivity)).create(ApiService::class.java)
                val novaOcorrencia = OcorrenciaCreate(titulo, desc, lat, lng, fotoBase64)
                val response = api.createOcorrencia(novaOcorrencia)

                if(response.isSuccessful) {
                    Toast.makeText(this@CriarOcorrenciaActivity, "Criado com sucesso!", Toast.LENGTH_SHORT).show()
                    finish() // Volta ao ecrã anterior
                }
            }
        }
    }

    private fun obterLocalizacaoGPS() {
        val client = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }
        client.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                lat = location.latitude
                lng = location.longitude
                Toast.makeText(this, "Localização obtida com sucesso", Toast.LENGTH_SHORT).show()
            }
        }
    }
}