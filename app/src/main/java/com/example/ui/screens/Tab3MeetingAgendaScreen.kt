package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.MeetingReminderEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.MeetingViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Tab3MeetingAgendaScreen(
    viewModel: MeetingViewModel
) {
    val context = LocalContext.current

    val selectedDate by viewModel.selectedDate.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val meetingsForDate by viewModel.meetingsForSelectedDate.collectAsState()
    val allMeetings by viewModel.allMeetings.collectAsState()

    val showMeetingModal by viewModel.showMeetingModal.collectAsState()
    val confirmDeleteMeeting by viewModel.confirmDeleteMeeting.collectAsState()

    val title by viewModel.titleState.collectAsState()
    val date by viewModel.dateState.collectAsState()
    val time by viewModel.timeState.collectAsState()
    val location by viewModel.locationState.collectAsState()
    val notes by viewModel.notesState.collectAsState()
    val priority by viewModel.priorityState.collectAsState()
    val minsBefore by viewModel.reminderMinutesBefore.collectAsState()

    // Listen for toasts
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openNewMeetingModal() },
                containerColor = OrangePrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nova Reunião")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // View Mode Selector Bar (Outlook Style: Dia, Agenda, Mês)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AGENDA OUTLOOK",
                    color = OrangePrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Dia", "Agenda", "Mês").forEach { mode ->
                        val isSelected = viewMode == mode
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) OrangePrimary else DarkSurface)
                                .clickable { viewModel.setViewMode(mode) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = mode,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Quick Date Calendar Strip
            DateCalendarStrip(
                selectedDate = selectedDate,
                onDateSelected = { viewModel.selectDate(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Meetings List
            val displayList = when (viewMode) {
                "Agenda" -> allMeetings
                else -> meetingsForDate
            }

            if (displayList.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.EventBusy,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (viewMode == "Agenda") "Nenhum compromisso futuro agendado." else "Sem reuniões para esta data.",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.openNewMeetingModal() },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OrangePrimary)
                        ) {
                            Text("Agendar Reunião", color = OrangePrimary, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(displayList, key = { it.id }) { meeting ->
                        MeetingCard(
                            meeting = meeting,
                            onToggleComplete = { viewModel.toggleComplete(meeting) },
                            onEditClick = { viewModel.openEditMeetingModal(meeting) },
                            onDeleteClick = { viewModel.requestDeleteMeeting(meeting) }
                        )
                    }
                }
            }
        }
    }

    // MODAL DIALOGS

    // Confirm Delete Dialog
    confirmDeleteMeeting?.let { m ->
        AlertDialog(
            onDismissRequest = { viewModel.confirmDeleteMeeting.value = null },
            title = { Text("Excluir Reunião", color = Color.Red) },
            text = { Text("Deseja excluir a reunião \"${m.title}\"? O lembrete será cancelado.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { viewModel.executeDeleteMeeting(m) }) {
                    Text("Excluir", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.confirmDeleteMeeting.value = null }) {
                    Text("Cancelar", color = TextMuted)
                }
            },
            containerColor = DarkSurface
        )
    }

    // Add / Edit Meeting Dialog Modal
    if (showMeetingModal) {
        AlertDialog(
            onDismissRequest = { viewModel.showMeetingModal.value = false },
            title = {
                Text(
                    text = if (viewModel.editingMeetingId.value == null) "NOVA REUNIÃO / LEMBRETE" else "EDITAR REUNIÃO",
                    color = OrangePrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { viewModel.titleState.value = it },
                        label = { Text("Título do Compromisso *") },
                        placeholder = { Text("Ex: Reunião de Alinhamento com Empreiteiro") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangePrimary,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = date,
                            onValueChange = { viewModel.dateState.value = it },
                            label = { Text("Data (YYYY-MM-DD)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangePrimary,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        OutlinedTextField(
                            value = time,
                            onValueChange = { viewModel.timeState.value = it },
                            label = { Text("Hora (HH:mm)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangePrimary,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }

                    OutlinedTextField(
                        value = location,
                        onValueChange = { viewModel.locationState.value = it },
                        label = { Text("Local / Sala / Link") },
                        placeholder = { Text("Ex: Canteiro de Obras ou Teams") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangePrimary,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { viewModel.notesState.value = it },
                        label = { Text("Pauta / Descrição") },
                        singleLine = false,
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangePrimary,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    // Priority Selector
                    Text("Prioridade:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Alta", "Média", "Baixa").forEach { p ->
                            val isSel = priority == p
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isSel) {
                                            when (p) {
                                                "Alta" -> Color(0xFFD32F2F)
                                                "Média" -> OrangePrimary
                                                else -> Color(0xFF388E3C)
                                            }
                                        } else DarkSurfaceVariant
                                    )
                                    .clickable { viewModel.priorityState.value = p }
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = p,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Notification Advance Selector
                    Text("Notificar Lembrete:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("5", "15", "30", "60").forEach { m ->
                            val isSel = minsBefore == m
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) OrangePrimary else DarkSurfaceVariant)
                                    .clickable { viewModel.reminderMinutesBefore.value = m }
                                    .padding(vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${m}m antes",
                                    color = if (isSel) Color.White else TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.saveMeeting() },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                ) {
                    Text("Salvar na Agenda", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showMeetingModal.value = false }) {
                    Text("Cancelar", color = TextMuted)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
fun DateCalendarStrip(
    selectedDate: String,
    onDateSelected: (String) -> Unit
) {
    val daysList = remember(selectedDate) {
        val list = mutableListOf<String>()
        val cal = Calendar.getInstance()
        try {
            val p = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val d = p.parse(selectedDate)
            if (d != null) cal.time = d
        } catch (e: Exception) {}

        cal.add(Calendar.DAY_OF_YEAR, -3)
        for (i in 0..6) {
            val f = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            list.add(f.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(daysList) { dateIso ->
            val isSel = dateIso == selectedDate
            val dayNum = dateIso.takeLast(2)
            val dayOfWeekStr = try {
                val p = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val d = p.parse(dateIso)
                if (d != null) SimpleDateFormat("EEE", Locale("pt", "BR")).format(d).uppercase() else ""
            } catch (e: Exception) { "" }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSel) OrangePrimary else DarkSurface)
                    .border(1.dp, if (isSel) OrangeSecondary else DarkBorder, RoundedCornerShape(8.dp))
                    .clickable { onDateSelected(dateIso) }
                    .padding(vertical = 8.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = dayOfWeekStr,
                        color = if (isSel) Color.White else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dayNum,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun MeetingCard(
    meeting: MeetingReminderEntity,
    onToggleComplete: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val priorityColor = when (meeting.priority) {
        "Alta" -> Color(0xFFE53935)
        "Média" -> OrangePrimary
        else -> Color(0xFF43A047)
    }

    val formattedDateStr = try {
        val p = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val f = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val d = p.parse(meeting.dateIso)
        if (d != null) f.format(d) else meeting.dateIso
    } catch (e: Exception) { meeting.dateIso }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(10.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (meeting.isCompleted) DarkBorder else priorityColor
            )
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Checkbox(
                    checked = meeting.isCompleted,
                    onCheckedChange = { onToggleComplete() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = OrangePrimary,
                        uncheckedColor = DarkBorder,
                        checkmarkColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(priorityColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = meeting.title,
                            color = if (meeting.isCompleted) TextMuted else Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textDecoration = if (meeting.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = OrangeSecondary, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$formattedDateStr às ${meeting.timeIso}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )

                        if (meeting.location.isNotBlank()) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Icon(Icons.Default.Place, contentDescription = null, tint = OrangeSecondary, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = meeting.location,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (meeting.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = meeting.notes,
                            color = TextMuted,
                            fontSize = 11.sp,
                            maxLines = 2
                        )
                    }
                }
            }

            Row {
                IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = OrangePrimary, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color.Red, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
