package pt.ipt.dam.urbanaudit.bd

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import pt.ipt.dam.urbanaudit.models.Ocorrencia

@Dao
interface OcorrenciaDao {
    @Insert
    fun inserir(ocorrencia: Ocorrencia)

    @Query("Select * FROM ocorrencias_locais WHERE sincronizado = 0")
    fun obterNaoSincronizadas(): List<Ocorrencia>

    @Query("UPDATE ocorrencias_locais SET sincronizado = 1, idServidor = :idServidor WHERE idLocal = :idLocal")
    fun marcarComoSincronizada(idLocal: Int, idServidor: Int)
}