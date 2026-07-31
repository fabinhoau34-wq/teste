package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meeting_reminders")
data class MeetingReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dateIso: String, // YYYY-MM-DD
    val timeIso: String, // HH:mm
    val location: String = "",
    val notes: String = "",
    val priority: String = "Média", // Alta, Média, Baixa
    val isCompleted: Boolean = false,
    val reminderTimestamp: Long = 0L
)
