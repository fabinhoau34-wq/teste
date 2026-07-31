package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.entity.MeetingReminderEntity
import com.example.data.repository.MeetingRepository
import com.example.util.NotificationHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MeetingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MeetingRepository

    private val _selectedDate = MutableStateFlow(getTodayIso())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _viewMode = MutableStateFlow("Dia") // Dia, Agenda, Mês
    val viewMode: StateFlow<String> = _viewMode.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val meetingsForSelectedDate: StateFlow<List<MeetingReminderEntity>>

    val allMeetings: StateFlow<List<MeetingReminderEntity>>

    // Editing State
    val editingMeetingId = MutableStateFlow<Long?>(null)
    val titleState = MutableStateFlow("")
    val dateState = MutableStateFlow(getTodayIso())
    val timeState = MutableStateFlow("09:00")
    val locationState = MutableStateFlow("")
    val notesState = MutableStateFlow("")
    val priorityState = MutableStateFlow("Média")
    val reminderMinutesBefore = MutableStateFlow("15") // 15 min antes
    val showMeetingModal = MutableStateFlow(false)

    val confirmDeleteMeeting = MutableStateFlow<MeetingReminderEntity?>(null)
    val toastMessage = MutableSharedFlow<String>()

    init {
        val dao = AppDatabase.getDatabase(application).meetingDao()
        repository = MeetingRepository(dao)

        allMeetings = repository.allMeetings
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        meetingsForSelectedDate = _selectedDate.flatMapLatest { date ->
            repository.getMeetingsByDate(date)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun selectDate(dateIso: String) {
        _selectedDate.value = dateIso
        dateState.value = dateIso
    }

    fun setViewMode(mode: String) {
        _viewMode.value = mode
    }

    fun openNewMeetingModal() {
        editingMeetingId.value = null
        titleState.value = ""
        dateState.value = _selectedDate.value
        timeState.value = "09:00"
        locationState.value = ""
        notesState.value = ""
        priorityState.value = "Média"
        reminderMinutesBefore.value = "15"
        showMeetingModal.value = true
    }

    fun openEditMeetingModal(meeting: MeetingReminderEntity) {
        editingMeetingId.value = meeting.id
        titleState.value = meeting.title
        dateState.value = meeting.dateIso
        timeState.value = meeting.timeIso
        locationState.value = meeting.location
        notesState.value = meeting.notes
        priorityState.value = meeting.priority
        showMeetingModal.value = true
    }

    fun saveMeeting() {
        if (titleState.value.isBlank()) {
            viewModelScope.launch { toastMessage.emit("Informe o título da reunião.") }
            return
        }

        val dateStr = dateState.value
        val timeStr = timeState.value

        // Calculate alarm timestamp
        val reminderTimeMs = calculateAlarmTimestamp(dateStr, timeStr, reminderMinutesBefore.value)

        val entity = MeetingReminderEntity(
            id = editingMeetingId.value ?: 0L,
            title = titleState.value.trim(),
            dateIso = dateStr,
            timeIso = timeStr,
            location = locationState.value.trim(),
            notes = notesState.value.trim(),
            priority = priorityState.value,
            reminderTimestamp = reminderTimeMs
        )

        viewModelScope.launch {
            val savedId = repository.saveMeeting(entity)
            val finalId = if (entity.id == 0L) savedId else entity.id

            if (reminderTimeMs > System.currentTimeMillis()) {
                NotificationHelper.scheduleAlarm(
                    context = getApplication(),
                    meetingId = finalId,
                    title = "Reunião: ${entity.title}",
                    notes = "Às ${entity.timeIso} em ${entity.location.ifBlank { "Local não informado" }}",
                    triggerAtMillis = reminderTimeMs
                )
            }

            toastMessage.emit("Compromisso salvo na agenda com lembrete!")
            showMeetingModal.value = false
        }
    }

    fun toggleComplete(meeting: MeetingReminderEntity) {
        viewModelScope.launch {
            repository.updateMeeting(meeting.copy(isCompleted = !meeting.isCompleted))
        }
    }

    fun requestDeleteMeeting(meeting: MeetingReminderEntity) {
        confirmDeleteMeeting.value = meeting
    }

    fun executeDeleteMeeting(meeting: MeetingReminderEntity) {
        viewModelScope.launch {
            repository.deleteMeeting(meeting.id)
            NotificationHelper.cancelAlarm(getApplication(), meeting.id)
            toastMessage.emit("Compromisso excluído!")
            confirmDeleteMeeting.value = null
        }
    }

    private fun calculateAlarmTimestamp(
        dateIso: String,
        timeIso: String,
        minsBeforeStr: String
    ): Long {
        return try {
            val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val meetingDate = dateTimeFormat.parse("$dateIso $timeIso")
            if (meetingDate != null) {
                val minsBefore = minsBeforeStr.toLongOrNull() ?: 15L
                meetingDate.time - (minsBefore * 60 * 1000)
            } else 0L
        } catch (e: Exception) {
            0L
        }
    }

    companion object {
        fun getTodayIso(): String {
            return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        }
    }
}
