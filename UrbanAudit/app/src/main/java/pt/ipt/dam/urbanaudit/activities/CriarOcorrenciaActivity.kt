package pt.ipt.dam.urbanaudit.activities

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Base64
import java.io.ByteArrayOutputStream
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import pt.ipt.dam.urbanaudit.R
import pt.ipt.dam.urbanaudit.retrofit.RetrofitClient
import pt.ipt.dam.urbanaudit.bd.AppDatabase
import pt.ipt.dam.urbanaudit.models.Ocorrencia
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CriarOcorrenciaActivity : AppCompatActivity() {

    private var fotoCapturada: Bitmap? = null
    private var lat: Double = 0.0
    private var lng: Double = 0.0

    private val tirarFotoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            fotoCapturada = result.data?.extras?.getString("data") as Bitmap
            findViewById<ImageView>(R.id.ivPreviewFoto).setImageBitmap(fotoCapturada)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_criar_ocorrencia)

        obterLocalizacao()

        findViewById<Button>(R.id.btnFoto).setOnClickListener {
            tirarFotoLauncher.launch(android.content.Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE))
        }

        findViewById<Button>(R.id.btnSubmeter).setOnClickListener {
            submeterOcorrencia()
        }
    }

    private fun obterLocalizacao() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                lat = it.latitude
                lng = it.longitude
            }
        }
    }

    private fun submeterOcorrencia() {
        val titulo = findViewById<EditText>(R.id.etTitulo).text.toString()
        val descricao = findViewById<EditText>(R.id.etDescricao).text.toString()

        // Validação das regras de negócio
        if (titulo.isBlank() || descricao.isBlank() || fotoCapturada == null) {
            Toast.makeText(this, "Preencha todos os campos e tire uma foto.", Toast.LENGTH_SHORT).show()
            return
        }

        val ocorrencia = Ocorrencia(
            titulo = titulo,
            descricao = descricao,
            latitude = lat,
            longitude = lng,
            fotoBase64 = converterBitmapParaBase64(fotoCapturada!!) // Aqui converterias o Bitmap para Base64 String
        )

        val token = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("TOKEN", "") ?: ""

        RetrofitClient.instance.criarOcorrencia(token, ocorrencia).enqueue(object : Callback<Ocorrencia> {
            override fun onResponse(call: Call<Ocorrencia>, response: Response<Ocorrencia>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@CriarOcorrenciaActivity, "Sucesso!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@CriarOcorrenciaActivity, "Erro no servidor", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<Ocorrencia>, t: Throwable) {
                // Em caso de falha de rede, guardar localmente para enviar mais tarde
                CoroutineScope(Dispatchers.IO).launch {
                    AppDatabase(this@CriarOcorrenciaActivity).ocorrenciaDao().inserir(ocorrencia)
                }
                Toast.makeText(this@CriarOcorrenciaActivity, "Guardado offline.", Toast.LENGTH_SHORT).show()
                finish()
            }
        })
    }
    fun converterBitmapParaBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
}