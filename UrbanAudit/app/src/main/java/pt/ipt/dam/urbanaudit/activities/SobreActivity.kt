package pt.ipt.dam.urbanaudit.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import pt.ipt.dam.urbanaudit.R

/**
 * Ecrã "Sobre a Aplicação" exigido pelo guião de avaliação da disciplina de DAM (IPT):
 * - Indicação do nome do curso, disciplina e ano letivo;
 * - Nº, nome e fotografia dos autores do trabalho;
 * - Identificação das bibliotecas, frameworks e código de terceiros usadas, com respetiva justificação técnica.
 */
class SobreActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sobre)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarSobre)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }
}