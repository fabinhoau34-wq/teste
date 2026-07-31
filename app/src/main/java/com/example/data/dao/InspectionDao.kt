package com.example.data.dao

import androidx.room.*
import com.example.data.entity.CustomFieldConfigEntity
import com.example.data.entity.InspectionRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InspectionDao {

    // Custom Fields
    @Query("SELECT * FROM custom_field_configs ORDER BY orderIndex ASC")
    fun getAllCustomFieldConfigs(): Flow<List<CustomFieldConfigEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomFieldConfig(config: CustomFieldConfigEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomFieldConfigs(configs: List<CustomFieldConfigEntity>)

    @Delete
    suspend fun deleteCustomFieldConfig(config: CustomFieldConfigEntity)

    @Query("DELETE FROM custom_field_configs WHERE id = :id")
    suspend fun deleteCustomFieldConfigById(id: String)

    // Inspection Records
    @Query("SELECT * FROM inspection_records WHERE dateIso = :dateIso ORDER BY createdAt ASC")
    fun getRecordsByDate(dateIso: String): Flow<List<InspectionRecordEntity>>

    @Query("SELECT * FROM inspection_records WHERE dateIso IN (:datesIso) ORDER BY dateIso ASC, createdAt ASC")
    fun getRecordsByMultipleDates(datesIso: List<String>): Flow<List<InspectionRecordEntity>>

    @Query("SELECT * FROM inspection_records WHERE id = :id")
    suspend fun getRecordById(id: Long): InspectionRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: InspectionRecordEntity): Long

    @Update
    suspend fun updateRecord(record: InspectionRecordEntity)

    @Delete
    suspend fun deleteRecord(record: InspectionRecordEntity)

    @Query("DELETE FROM inspection_records WHERE id = :id")
    suspend fun deleteRecordById(id: Long)
}
