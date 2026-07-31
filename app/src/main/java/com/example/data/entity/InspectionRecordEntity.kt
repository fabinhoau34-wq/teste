package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inspection_records")
data class InspectionRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateIso: String, // Format: YYYY-MM-DD
    val location: String, // Fixed field: Localidade
    val notes: String, // Fixed field: Obs
    val fieldsJson: String, // Dynamic custom fields content
    val photosJson: String, // Photo attachments
    val createdAt: Long = System.currentTimeMillis()
)
