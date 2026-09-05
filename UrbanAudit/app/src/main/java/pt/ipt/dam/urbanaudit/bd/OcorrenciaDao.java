package pt.ipt.dam.urbanaudit.bd;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;
import pt.ipt.dam.urbanaudit.models.Ocorrencia;

/**
 * Data Access Object (DAO) para operações CRUD na tabela de ocorrências.
 */
@Dao
public interface OcorrenciaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long inserir(Ocorrencia ocorrencia);

    @Update
    void atualizar(Ocorrencia ocorrencia);

    @Delete
    void eliminar(Ocorrencia ocorrencia);

    @Query("SELECT * FROM ocorrencias_locais ORDER BY idLocal DESC")
    List<Ocorrencia> obterTodas();

    @Query("SELECT * FROM ocorrencias_locais WHERE idLocal = :id LIMIT 1")
    Ocorrencia obterPorId(int id);

    @Query("SELECT * FROM ocorrencias_locais WHERE categoria = :categoria ORDER BY idLocal DESC")
    List<Ocorrencia> obterPorCategoria(String categoria);

    @Query("SELECT * FROM ocorrencias_locais WHERE sincronizado = 0 ORDER BY idLocal DESC")
    List<Ocorrencia> obterNaoSincronizadas();

    @Query("UPDATE ocorrencias_locais SET sincronizado = 1, idServidor = :idServidor WHERE idLocal = :idLocal")
    void marcarComoSincronizada(int idLocal, int idServidor);
}
