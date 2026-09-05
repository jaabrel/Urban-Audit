package pt.ipt.dam.urbanaudit.ui.about

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import pt.ipt.dam.urbanaudit.R

/**
 * Ecrã "Sobre o Trabalho" (Identificação Académica Obrigatória).
 * 
 * Conteúdo apresentado:
 * - Identificação da Instituição (IPT), Escola (ESTT), Curso (LEI) e Disciplina (DAM 2025/2026).
 * - Autores do projeto: João Alexandre de Abreu (Nº 24844) e Rodrigo Mendes Pinheiro (Nº 26402).
 * - Citação e justificação técnica das bibliotecas de terceiros (Room, Retrofit, Fused Location, Material Design).
 * - Barra de ferramentas com seta de navegação para regresso ao ecrã anterior.
 */
class SobreActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sobre)

        // Configuração do botão de retrocesso da barra de ferramentas
        findViewById<MaterialToolbar>(R.id.toolbarSobre)?.setNavigationOnClickListener {
            finish()
        }
    }
}