package com.example.data.dao

import androidx.room.*
import com.example.data.entity.MeetingReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MeetingDao {
    @Query("SELECT * FROM meeting_reminders ORDER BY dateIso ASC, timeIso ASC")
    fun getAllMeetings(): Flow<List<MeetingReminderEntity>>

    @Query("SELECT * FROM meeting_reminders WHERE dateIso = :dateIso ORDER BY timeIso ASC")
    fun getMeetingsByDate(dateIso: String): Flow<List<MeetingReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeeting(meeting: MeetingReminderEntity): Long

    @Update
    suspend fun updateMeeting(meeting: MeetingReminderEntity)

    @Delete
    suspend fun deleteMeeting(meeting: MeetingReminderEntity)

    @Query("DELETE FROM meeting_reminders WHERE id = :id")
    suspend fun deleteMeetingById(id: Long)
}
