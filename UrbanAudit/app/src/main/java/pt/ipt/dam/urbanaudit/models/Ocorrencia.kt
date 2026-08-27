package pt.ipt.dam.urbanaudit.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ocorrencias_locais")
data class Ocorrencia (
    @PrimaryKey(autoGenerate = true) val idLocal: Int = 0,
    val idServidor: Int? = null,
    val titulo: String,
    val descricao: String,
    val latitude: Double,
    val longitude: Double,
    val fotoBase64: String,
    val estado: String = "Aberto",
    var sincronizado: Boolean = false
    )