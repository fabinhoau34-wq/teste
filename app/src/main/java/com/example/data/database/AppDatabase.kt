package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.InspectionDao
import com.example.data.dao.MeetingDao
import com.example.data.entity.CustomFieldConfigEntity
import com.example.data.entity.InspectionRecordEntity
import com.example.data.entity.MeetingReminderEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CustomFieldConfigEntity::class,
        InspectionRecordEntity::class,
        MeetingReminderEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun inspectionDao(): InspectionDao
    abstract fun meetingDao(): MeetingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vistoria_database"
                )
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Prepopulate default custom fields
                            CoroutineScope(Dispatchers.IO).launch {
                                val dao = getDatabase(context).inspectionDao()
                                dao.insertCustomFieldConfigs(
                                    listOf(
                                        CustomFieldConfigEntity(
                                            id = "inicio_field",
                                            title = "Início",
                                            defaultIncludeInReceipt = true,
                                            orderIndex = 0
                                        ),
                                        CustomFieldConfigEntity(
                                            id = "fim_field",
                                            title = "Fim Estimado",
                                            defaultIncludeInReceipt = true,
                                            orderIndex = 1
                                        ),
                                        CustomFieldConfigEntity(
                                            id = "servico_field",
                                            title = "Serviço / Atividade",
                                            defaultIncludeInReceipt = true,
                                            orderIndex = 2
                                        ),
                                        CustomFieldConfigEntity(
                                            id = "equipe_field",
                                            title = "Equipe / Pessoal",
                                            defaultIncludeInReceipt = true,
                                            orderIndex = 3
                                        )
                                    )
                                )
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
