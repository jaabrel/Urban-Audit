package pt.ipt.dam.urbanaudit.bd;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import pt.ipt.dam.urbanaudit.models.Ocorrencia;

/**
 * Base de dados Room SQLite da aplicação Urban Audit.
 */
@Database(entities = {Ocorrencia.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract OcorrenciaDao ocorrenciaDao();

    private static volatile AppDatabase instance;

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "urbanaudit_db"
                    )
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return instance;
    }
}
