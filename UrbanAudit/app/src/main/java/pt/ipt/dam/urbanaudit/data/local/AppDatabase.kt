package pt.ipt.dam.urbanaudit.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

// 1. A Tabela (Entity) - Representa como os dados são gravados no telemóvel
@Entity(tableName = "ocorrencias")
data class OcorrenciaEntity(
    @PrimaryKey val id: Int,
    val titulo: String,
    val descricao: String,
    val latitude: Double,
    val longitude: Double,
    val fotoBase64: String,
    val estado: String,
    val owner_id: Int
)

// 2. DAO (Data Access Object) - As tuas "queries" SQL
@Dao
interface OcorrenciaDao {
    // Buscar todas as ocorrências guardadas para quando não há net
    @Query("SELECT * FROM ocorrencias")
    fun getAll(): List<OcorrenciaEntity>
    // Guardar uma lista nova inteira (substitui se já existir o mesmo ID)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(ocorrencias: List<OcorrenciaEntity>)

    // Apagar tudo (útil para limpar antes de inserir dados novos vindos da API)
    @Query("DELETE FROM ocorrencias")
    fun deleteAll()
}
// 3. A Base de Dados - Singleton para não abrir várias vezes ao mesmo tempo
@Database(entities = [OcorrenciaEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun ocorrenciaDao(): OcorrenciaDao
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "urban_audit_local_db" // Nome do ficheiro da BD no Android
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

