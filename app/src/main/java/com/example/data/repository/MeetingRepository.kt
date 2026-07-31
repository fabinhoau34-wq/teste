package com.example.data.repository

import com.example.data.dao.MeetingDao
import com.example.data.entity.MeetingReminderEntity
import kotlinx.coroutines.flow.Flow

class MeetingRepository(private val meetingDao: MeetingDao) {

    val allMeetings: Flow<List<MeetingReminderEntity>> = meetingDao.getAllMeetings()

    fun getMeetingsByDate(dateIso: String): Flow<List<MeetingReminderEntity>> {
        return meetingDao.getMeetingsByDate(dateIso)
    }

    suspend fun saveMeeting(meeting: MeetingReminderEntity): Long {
        return meetingDao.insertMeeting(meeting)
    }

    suspend fun updateMeeting(meeting: MeetingReminderEntity) {
        meetingDao.updateMeeting(meeting)
    }

    suspend fun deleteMeeting(id: Long) {
        meetingDao.deleteMeetingById(id)
    }
}
