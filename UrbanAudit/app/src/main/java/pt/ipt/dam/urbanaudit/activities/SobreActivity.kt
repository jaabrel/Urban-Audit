package pt.ipt.dam.urbanaudit.activities

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import pt.ipt.dam.urbanaudit.R

class SobreActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sobre)
        // Identificação obrigatória do curso, disciplina e bibliotecas
        val info = """
            Curso: Licenciatura em Eng. Informática
            Disciplina: Desenvolvimento de Aplicações Móveis (DAM)
            Ano Letivo: 2025/26
            
            Autores:
            - Aluno 1 (Nº 24844)
            - Aluno 2 (Nº 26402)
            
            Bibliotecas de Terceiros Usadas:
            1. Retrofit2 (com.squareup.retrofit2) - API REST.
            2. Room (androidx.room) - Base de dados local SQlite.
            3. Google Play Services Location -  GPS.
            4. Gson - JSON.
        """.trimIndent()

        // findViewById<TextView>(R.id.tvInfo).text = info
    }
}