package pt.ipt.dam.urbanaudit.ui.main

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import pt.ipt.dam.urbanaudit.R
import pt.ipt.dam.urbanaudit.data.model.Ocorrencia
import pt.ipt.dam.urbanaudit.utils.ImageUtils
import java.util.Locale

class OcorrenciaAdapter(
    private var lista: List<Ocorrencia>,
    private val meuUserId: Int,
    private val meuRole: String,
    private val aoClicarApagar: (Int) -> Unit
) : RecyclerView.Adapter<OcorrenciaAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivFoto: ImageView = view.findViewById(R.id.ivFotoOcorrencia)
        val txtCategoria: TextView = view.findViewById(R.id.tvCategoriaOcorrencia)
        val txtEstado: TextView = view.findViewById(R.id.tvEstadoOcorrencia)
        val txtTitulo: TextView = view.findViewById(R.id.tvTituloOcorrencia)
        val txtLocalizacao: TextView = view.findViewById(R.id.tvLocalizacaoOcorrencia)
        val txtData: TextView = view.findViewById(R.id.tvDataOcorrencia)
        val btnApagar: View? = view.findViewById(R.id.btnApagar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ocorrencia, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ocorrencia = lista[position]
        val context = holder.itemView.context

        // 1. Título
        holder.txtTitulo.text = ocorrencia.titulo

        // 2. Categoria inteligente
        holder.txtCategoria.text = ocorrencia.obterCategoria()

        // 3. Estado da Ocorrência
        val estado = if (ocorrencia.estado.isNotBlank()) ocorrencia.estado else "Pendente"
        holder.txtEstado.text = estado
        when (estado.lowercase()) {
            "resolvido", "concluído", "concluido" -> {
                holder.txtEstado.setTextColor(ContextCompat.getColor(context, R.color.status_resolvido_text))
            }
            "em análise", "analise", "em analise" -> {
                holder.txtEstado.setTextColor(ContextCompat.getColor(context, R.color.status_analise_text))
            }
            else -> {
                holder.txtEstado.setTextColor(ContextCompat.getColor(context, R.color.status_pendente_text))
            }
        }

        // 4. Localização / Morada
        if (ocorrencia.descricao.contains("(Local: ")) {
            val local = ocorrencia.descricao.substringAfter("(Local: ").substringBefore(")")
            holder.txtLocalizacao.text = local
        } else if (ocorrencia.latitude != 0.0 || ocorrencia.longitude != 0.0) {
            holder.txtLocalizacao.text = String.format(Locale.US, "GPS: %.4f, %.4f", ocorrencia.latitude, ocorrencia.longitude)
        } else {
            holder.txtLocalizacao.text = "Tomar, Portugal"
        }

        // 5. Identificador / Autor
        holder.txtData.text = if (ocorrencia.owner_id > 0) "#${ocorrencia.id} • Utilizador ${ocorrencia.owner_id}" else "#${ocorrencia.id}"

        // 6. Fotografia (Descodificação de Base64 com suporte a cache visual)
        val bitmap = ImageUtils.converterBase64ParaBitmap(ocorrencia.fotoBase64)
        if (bitmap != null) {
            holder.ivFoto.setImageBitmap(bitmap)
            holder.ivFoto.imageTintList = null
            holder.ivFoto.scaleType = ImageView.ScaleType.CENTER_CROP
        } else {
            holder.ivFoto.setImageResource(R.drawable.ic_camera)
            holder.ivFoto.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.text_hint))
            holder.ivFoto.scaleType = ImageView.ScaleType.CENTER_INSIDE
        }

        // 7. Controlo de Acesso: apenas admin ou o próprio autor podem apagar
        if (meuRole == "admin" || ocorrencia.owner_id == meuUserId) {
            holder.btnApagar?.visibility = View.VISIBLE
        } else {
            holder.btnApagar?.visibility = View.GONE
        }

        holder.btnApagar?.setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle("Apagar Ocorrência")
                .setMessage("Deseja mesmo apagar esta ocorrência?")
                .setPositiveButton("Sim, Apagar") { _, _ -> aoClicarApagar(ocorrencia.id) }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    override fun getItemCount() = lista.size

    fun atualizarLista(novaLista: List<Ocorrencia>) {
        this.lista = novaLista
        notifyDataSetChanged()
    }
}