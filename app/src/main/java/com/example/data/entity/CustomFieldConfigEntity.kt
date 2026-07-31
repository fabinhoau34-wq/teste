package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_field_configs")
data class CustomFieldConfigEntity(
    @PrimaryKey val id: String,
    val title: String,
    val defaultIncludeInReceipt: Boolean = true,
    val orderIndex: Int = 0
)
