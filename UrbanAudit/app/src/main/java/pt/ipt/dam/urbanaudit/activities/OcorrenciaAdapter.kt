package pt.ipt.dam.urbanaudit.activities

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import pt.ipt.dam.urbanaudit.R
import pt.ipt.dam.urbanaudit.models.Ocorrencia

class OcorrenciaAdapter(private val ocorrencias: List<Ocorrencia>) : RecyclerView.Adapter<OcorrenciaAdapter.ViewHolder>() {

    // Mapeia os elementos gráficos (TextViews) criados num layout XML (ex: item_ocorrencia.xml)
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitulo: TextView = view.findViewById(R.id.tvTituloOcorrencia)
        val tvEstado: TextView = view.findViewById(R.id.tvEstadoOcorrencia)
        val tvLocalizacao: TextView = view.findViewById(R.id.tvLocalizacaoOcorrencia)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ocorrencia, parent, false)
        return ViewHolder(view)
    }

    // Preenche cada linha da lista com os dados guardados localmente ou vindos da API
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ocorrencia = ocorrencias[position]
        holder.tvTitulo.text = ocorrencia.titulo
        holder.tvEstado.text = "Estado: ${ocorrencia.estado}"
        holder.tvLocalizacao.text = "Lat: ${ocorrencia.latitude} | Lng: ${ocorrencia.longitude}"
    }

    override fun getItemCount() = ocorrencias.size
}