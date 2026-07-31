package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.entity.CustomFieldConfigEntity
import com.example.data.entity.InspectionRecordEntity
import com.example.data.model.CustomFieldItem
import com.example.data.model.PhotoAttachment
import com.example.data.repository.InspectionRepository
import com.example.util.JsonUtil
import com.example.util.ReceiptGenerator
import com.example.util.WatermarkUtil
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class InspectionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: InspectionRepository

    // Date selection
    private val _selectedDate = MutableStateFlow(getTodayIso())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _selectedDates = MutableStateFlow<Set<String>>(setOf(getTodayIso()))
    val selectedDates: StateFlow<Set<String>> = _selectedDates.asStateFlow()

    private val _isMultiDateMode = MutableStateFlow(false)
    val isMultiDateMode: StateFlow<Boolean> = _isMultiDateMode.asStateFlow()

    // Configs
    val customFieldConfigs: StateFlow<List<CustomFieldConfigEntity>>

    // Records for selected date(s)
    @OptIn(ExperimentalCoroutinesApi::class)
    val records: StateFlow<List<InspectionRecordEntity>>

    // Editing State
    private val _editingRecordId = MutableStateFlow<Long?>(null)
    val editingRecordId: StateFlow<Long?> = _editingRecordId.asStateFlow()

    val locationState = MutableStateFlow("")
    val notesState = MutableStateFlow("")
    val customFieldsState = MutableStateFlow<List<CustomFieldItem>>(emptyList())
    val photosState = MutableStateFlow<List<PhotoAttachment>>(emptyList())

    // Dialogs & Modals
    val showFieldManager = MutableStateFlow(false)
    val showReceiptModal = MutableStateFlow(false)
    val generatedReceiptText = MutableStateFlow("")

    val confirmDeleteRecord = MutableStateFlow<InspectionRecordEntity?>(null)
    val confirmEditRecord = MutableStateFlow<InspectionRecordEntity?>(null)

    val toastMessage = MutableSharedFlow<String>()

    init {
        val dao = AppDatabase.getDatabase(application).inspectionDao()
        repository = InspectionRepository(dao)

        customFieldConfigs = repository.customFieldConfigs
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        records = combine(_isMultiDateMode, _selectedDate, _selectedDates) { isMulti, singleDate, multiSet ->
            Pair(isMulti, if (isMulti) multiSet.toList() else listOf(singleDate))
        }.flatMapLatest { (isMulti, dates) ->
            if (isMulti) {
                if (dates.isEmpty()) flowOf(emptyList()) else repository.getRecordsByMultipleDates(dates)
            } else {
                repository.getRecordsByDate(dates.firstOrNull() ?: getTodayIso())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Initialize custom fields from configs when creating a new record
        viewModelScope.launch {
            customFieldConfigs.collect { configs ->
                if (_editingRecordId.value == null && customFieldsState.value.isEmpty()) {
                    customFieldsState.value = configs.map { config ->
                        CustomFieldItem(
                            id = config.id,
                            title = config.title,
                            content = "",
                            includeInReceipt = config.defaultIncludeInReceipt,
                            orderIndex = config.orderIndex
                        )
                    }
                }
            }
        }
    }

    fun selectSingleDate(dateIso: String) {
        _selectedDate.value = dateIso
        _isMultiDateMode.value = false
        resetForm()
    }

    fun toggleMultiDate(dateIso: String) {
        _isMultiDateMode.value = true
        val current = _selectedDates.value.toMutableSet()
        if (current.contains(dateIso)) {
            if (current.size > 1) current.remove(dateIso)
        } else {
            current.add(dateIso)
        }
        _selectedDates.value = current
    }

    fun setMultiDateMode(enabled: Boolean) {
        _isMultiDateMode.value = enabled
        if (!enabled) {
            _selectedDates.value = setOf(_selectedDate.value)
        }
    }

    fun resetForm() {
        _editingRecordId.value = null
        locationState.value = ""
        notesState.value = ""
        photosState.value = emptyList()

        // Populate dynamic fields from configs
        val configs = customFieldConfigs.value
        customFieldsState.value = configs.map { config ->
            CustomFieldItem(
                id = config.id,
                title = config.title,
                content = "",
                includeInReceipt = config.defaultIncludeInReceipt,
                orderIndex = config.orderIndex
            )
        }
    }

    fun updateCustomFieldContent(fieldId: String, content: String) {
        customFieldsState.value = customFieldsState.value.map { field ->
            if (field.id == fieldId) field.copy(content = content) else field
        }
    }

    fun toggleFieldReceiptInForm(fieldId: String, include: Boolean) {
        customFieldsState.value = customFieldsState.value.map { field ->
            if (field.id == fieldId) field.copy(includeInReceipt = include) else field
        }
    }

    fun saveRecord() {
        if (locationState.value.isBlank() && customFieldsState.value.all { it.content.isBlank() }) {
            viewModelScope.launch { toastMessage.emit("Preencha ao menos a Localidade ou algum campo do registro.") }
            return
        }

        val record = InspectionRecordEntity(
            id = _editingRecordId.value ?: 0L,
            dateIso = _selectedDate.value,
            location = locationState.value,
            notes = notesState.value,
            fieldsJson = JsonUtil.fieldsToJson(customFieldsState.value),
            photosJson = JsonUtil.photosToJson(photosState.value)
        )

        viewModelScope.launch {
            if (_editingRecordId.value == null) {
                repository.saveRecord(record)
                toastMessage.emit("Registro salvo com sucesso!")
            } else {
                repository.updateRecord(record)
                toastMessage.emit("Registro atualizado com sucesso!")
            }
            resetForm()
        }
    }

    fun requestEditRecord(record: InspectionRecordEntity) {
        confirmEditRecord.value = record
    }

    fun executeEditRecord(record: InspectionRecordEntity) {
        _editingRecordId.value = record.id
        locationState.value = record.location
        notesState.value = record.notes

        val existingFields = JsonUtil.jsonToFields(record.fieldsJson)
        // Merge with existing field configs in case user added new config fields
        val configMap = customFieldConfigs.value.associateBy { it.id }
        val mergedFields = mutableListOf<CustomFieldItem>()

        existingFields.forEach { f -> mergedFields.add(f) }
        configMap.forEach { (id, cfg) ->
            if (mergedFields.none { it.id == id }) {
                mergedFields.add(
                    CustomFieldItem(
                        id = id,
                        title = cfg.title,
                        content = "",
                        includeInReceipt = cfg.defaultIncludeInReceipt,
                        orderIndex = cfg.orderIndex
                    )
                )
            }
        }

        customFieldsState.value = mergedFields.sortedBy { it.orderIndex }
        photosState.value = JsonUtil.jsonToPhotos(record.photosJson)
        confirmEditRecord.value = null
    }

    fun requestDeleteRecord(record: InspectionRecordEntity) {
        confirmDeleteRecord.value = record
    }

    fun executeDeleteRecord(record: InspectionRecordEntity) {
        viewModelScope.launch {
            repository.deleteRecord(record.id)
            if (_editingRecordId.value == record.id) {
                resetForm()
            }
            toastMessage.emit("Registro excluído!")
            confirmDeleteRecord.value = null
        }
    }

    fun addCustomFieldConfig(title: String) {
        if (title.isBlank()) return
        val id = "field_${System.currentTimeMillis()}"
        val maxOrder = customFieldConfigs.value.maxOfOrNull { it.orderIndex } ?: 0
        val newConfig = CustomFieldConfigEntity(
            id = id,
            title = title.trim(),
            defaultIncludeInReceipt = true,
            orderIndex = maxOrder + 1
        )
        viewModelScope.launch {
            repository.saveCustomFieldConfig(newConfig)
            // also update current editing state
            val current = customFieldsState.value.toMutableList()
            current.add(
                CustomFieldItem(
                    id = id,
                    title = title.trim(),
                    content = "",
                    includeInReceipt = true,
                    orderIndex = maxOrder + 1
                )
            )
            customFieldsState.value = current
        }
    }

    fun moveCustomFieldConfig(fromIndex: Int, toIndex: Int) {
        val configs = customFieldConfigs.value.toMutableList()
        if (fromIndex in configs.indices && toIndex in configs.indices) {
            val item = configs.removeAt(fromIndex)
            configs.add(toIndex, item)

            val reordered = configs.mapIndexed { idx, cfg ->
                cfg.copy(orderIndex = idx)
            }

            viewModelScope.launch {
                repository.saveCustomFieldConfigs(reordered)
            }

            // Sync with current form
            val formFields = customFieldsState.value.associateBy { it.id }
            val reorderedForm = reordered.mapNotNull { cfg ->
                formFields[cfg.id]?.copy(orderIndex = cfg.orderIndex, title = cfg.title)
                    ?: CustomFieldItem(cfg.id, cfg.title, "", cfg.defaultIncludeInReceipt, cfg.orderIndex)
            }
            customFieldsState.value = reorderedForm
        }
    }

    fun deleteCustomFieldConfig(id: String) {
        viewModelScope.launch {
            repository.deleteCustomFieldConfig(id)
            customFieldsState.value = customFieldsState.value.filter { it.id != id }
        }
    }

    fun attachPhoto(context: Context, uri: Uri) {
        viewModelScope.launch {
            val savedPath = WatermarkUtil.processAndSaveWatermarkedImage(
                context = context,
                sourceUri = uri,
                location = locationState.value
            )
            if (savedPath != null) {
                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                val photo = PhotoAttachment(
                    id = "photo_${System.currentTimeMillis()}",
                    imagePath = savedPath,
                    location = locationState.value,
                    dateIso = _selectedDate.value,
                    timeFormatted = timeStr
                )
                photosState.value = photosState.value + photo
                toastMessage.emit("Foto com marca D'água anexada!")
            } else {
                toastMessage.emit("Falha ao processar a imagem.")
            }
        }
    }

    fun removePhoto(photoId: String) {
        photosState.value = photosState.value.filter { it.id != photoId }
    }

    fun generateTotalDailyReceipt() {
        val currentRecords = records.value
        val text = ReceiptGenerator.generateDailyReceipt(_selectedDate.value, currentRecords)
        generatedReceiptText.value = text
        showReceiptModal.value = true
    }

    companion object {
        fun getTodayIso(): String {
            return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        }
    }
}
