package pt.ipt.dam.urbanaudit.activities

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import pt.ipt.dam.urbanaudit.R
import pt.ipt.dam.urbanaudit.models.Ocorrencia
import pt.ipt.dam.urbanaudit.utils.ImageUtils
import java.io.File

/**
 * Adaptador do RecyclerView para a lista de ocorrências da Urban Audit.
 * Permite apresentar os detalhes de cada auditoria e escutar o clique para abrir o ecrã de detalhe.
 */
class OcorrenciaAdapter(
    private var ocorrencias: List<Ocorrencia>,
    private val aoClicar: (Ocorrencia) -> Unit
) : RecyclerView.Adapter<OcorrenciaAdapter.OcorrenciaViewHolder>() {

    class OcorrenciaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivFoto: ImageView = view.findViewById(R.id.ivFotoOcorrencia)
        val tvTitulo: TextView = view.findViewById(R.id.tvTituloOcorrencia)
        val tvCategoria: TextView = view.findViewById(R.id.tvCategoriaOcorrencia)
        val tvEstado: TextView = view.findViewById(R.id.tvEstadoOcorrencia)
        val tvLocalizacao: TextView = view.findViewById(R.id.tvLocalizacaoOcorrencia)
        val tvData: TextView = view.findViewById(R.id.tvDataOcorrencia)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OcorrenciaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ocorrencia, parent, false)
        return OcorrenciaViewHolder(view)
    }

    override fun onBindViewHolder(holder: OcorrenciaViewHolder, position: Int) {
        val ocorrencia = ocorrencias[position]

        holder.tvTitulo.text = ocorrencia.titulo
        holder.tvCategoria.text = ocorrencia.categoria.ifBlank { "Geral" }
        holder.tvEstado.text = ocorrencia.estado

        // Aplicação de cores dinâmicas no badge de estado
        val context = holder.itemView.context
        when (ocorrencia.estado) {
            "Resolvido" -> {
                holder.tvEstado.setTextColor(ContextCompat.getColor(context, R.color.status_resolvido_text))
                holder.tvEstado.backgroundTintList = ContextCompat.getColorStateList(context, R.color.status_resolvido_bg)
            }
            "Em Análise" -> {
                holder.tvEstado.setTextColor(ContextCompat.getColor(context, R.color.status_analise_text))
                holder.tvEstado.backgroundTintList = ContextCompat.getColorStateList(context, R.color.status_analise_bg)
            }
            else -> {
                holder.tvEstado.setTextColor(ContextCompat.getColor(context, R.color.status_pendente_text))
                holder.tvEstado.backgroundTintList = ContextCompat.getColorStateList(context, R.color.status_pendente_bg)
            }
        }

        // Apresentação de morada amigável ou coordenadas
        if (ocorrencia.endereco.isNotBlank()) {
            holder.tvLocalizacao.text = ocorrencia.endereco
        } else {
            holder.tvLocalizacao.text = "GPS: %.4f, %.4f".format(ocorrencia.latitude, ocorrencia.longitude)
        }

        // Data de registo
        holder.tvData.text = ocorrencia.dataHora.ifBlank { "Registo recente" }

        // Carregamento da fotografia tirada pela câmara
        if (ocorrencia.caminhoFoto.isNotBlank() && File(ocorrencia.caminhoFoto).exists()) {
            val bitmap = ImageUtils.carregarBitmapRedimensionado(ocorrencia.caminhoFoto, 200, 200)
            if (bitmap != null) {
                holder.ivFoto.setImageBitmap(bitmap)
                holder.ivFoto.imageTintList = null
            } else {
                holder.ivFoto.setImageResource(R.drawable.ic_camera)
            }
        } else {
            holder.ivFoto.setImageResource(R.drawable.ic_camera)
        }

        // Clique para abrir detalhe da ocorrência
        holder.itemView.setOnClickListener {
            aoClicar(ocorrencia)
        }
    }

    override fun getItemCount(): Int = ocorrencias.size

    /**
     * Atualiza a lista de ocorrências apresentadas.
     */
    fun atualizarLista(novaLista: List<Ocorrencia>) {
        this.ocorrencias = novaLista
        notifyDataSetChanged()
    }
}