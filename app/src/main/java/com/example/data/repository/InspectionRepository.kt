package com.example.data.repository

import com.example.data.dao.InspectionDao
import com.example.data.entity.CustomFieldConfigEntity
import com.example.data.entity.InspectionRecordEntity
import kotlinx.coroutines.flow.Flow

class InspectionRepository(private val inspectionDao: InspectionDao) {

    val customFieldConfigs: Flow<List<CustomFieldConfigEntity>> =
        inspectionDao.getAllCustomFieldConfigs()

    suspend fun saveCustomFieldConfig(config: CustomFieldConfigEntity) {
        inspectionDao.insertCustomFieldConfig(config)
    }

    suspend fun saveCustomFieldConfigs(configs: List<CustomFieldConfigEntity>) {
        inspectionDao.insertCustomFieldConfigs(configs)
    }

    suspend fun deleteCustomFieldConfig(id: String) {
        inspectionDao.deleteCustomFieldConfigById(id)
    }

    fun getRecordsByDate(dateIso: String): Flow<List<InspectionRecordEntity>> {
        return inspectionDao.getRecordsByDate(dateIso)
    }

    fun getRecordsByMultipleDates(datesIso: List<String>): Flow<List<InspectionRecordEntity>> {
        return inspectionDao.getRecordsByMultipleDates(datesIso)
    }

    suspend fun getRecordById(id: Long): InspectionRecordEntity? {
        return inspectionDao.getRecordById(id)
    }

    suspend fun saveRecord(record: InspectionRecordEntity): Long {
        return inspectionDao.insertRecord(record)
    }

    suspend fun updateRecord(record: InspectionRecordEntity) {
        inspectionDao.updateRecord(record)
    }

    suspend fun deleteRecord(id: Long) {
        inspectionDao.deleteRecordById(id)
    }
}
