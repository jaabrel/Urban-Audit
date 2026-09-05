package pt.ipt.dam.urbanaudit.ui.main

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import pt.ipt.dam.urbanaudit.data.model.Ocorrencia
import pt.ipt.dam.urbanaudit.R

class OcorrenciaAdapter(
    private val lista: List<Ocorrencia>,
    private val meuUserId: Int,
    private val meuRole: String,
    private val aoClicarApagar: (Int) -> Unit
) : RecyclerView.Adapter<OcorrenciaAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtTitulo = view.findViewById<TextView>(R.id.txtTitulo)
        val btnApagar = view.findViewById<Button>(R.id.btnApagar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ocorrencia, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ocorrencia = lista[position]
        holder.txtTitulo.text = ocorrencia.titulo
        // REGRA DE AVALIAÇÃO: Controlo de acesso
        if (meuRole == "admin" || ocorrencia.owner_id == meuUserId) {
            holder.btnApagar.visibility = View.VISIBLE
        } else {
            holder.btnApagar.visibility = View.GONE
        }
        holder.btnApagar.setOnClickListener {
            // REGRA DE AVALIAÇÃO: Confirmar antes de apagar
            AlertDialog.Builder(holder.itemView.context)
                .setTitle("Apagar")
                .setMessage("Deseja apagar esta ocorrência?")
                .setPositiveButton("Sim") { _, _ -> aoClicarApagar(ocorrencia.id) }
                .setNegativeButton("Não", null)
                .show()
        }
    }
    override fun getItemCount() = lista.size
}