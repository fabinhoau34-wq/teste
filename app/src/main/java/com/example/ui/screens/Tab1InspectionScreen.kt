package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.entity.InspectionRecordEntity
import com.example.data.model.CustomFieldItem
import com.example.ui.theme.*
import com.example.ui.viewmodel.InspectionViewModel
import com.example.util.JsonUtil
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Tab1InspectionScreen(
    viewModel: InspectionViewModel
) {
    val context = LocalContext.current

    val selectedDate by viewModel.selectedDate.collectAsState()
    val records by viewModel.records.collectAsState()
    val customFieldConfigs by viewModel.customFieldConfigs.collectAsState()

    val location by viewModel.locationState.collectAsState()
    val notes by viewModel.notesState.collectAsState()
    val customFields by viewModel.customFieldsState.collectAsState()
    val photos by viewModel.photosState.collectAsState()
    val editingRecordId by viewModel.editingRecordId.collectAsState()

    val showFieldManager by viewModel.showFieldManager.collectAsState()
    val showReceiptModal by viewModel.showReceiptModal.collectAsState()
    val generatedReceiptText by viewModel.generatedReceiptText.collectAsState()

    val confirmDeleteRecord by viewModel.confirmDeleteRecord.collectAsState()
    val confirmEditRecord by viewModel.confirmEditRecord.collectAsState()

    var isCalendarExpanded by remember { mutableStateOf(false) }

    // Camera launcher
    var photoUriTemp by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUriTemp != null) {
            viewModel.attachPhoto(context, photoUriTemp!!)
        }
    }

    val launchCameraWithUri: () -> Unit = {
        try {
            val tempFile = File.createTempFile("photo_", ".jpg", context.cacheDir)
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            photoUriTemp = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao abrir câmera: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCameraWithUri()
        } else {
            Toast.makeText(context, "Permissão de câmera necessária para tirar fotos.", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.attachPhoto(context, uri)
        }
    }

    // Listen for toast messages
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // 1. Navegação e Filtro por Data (DatePicker/Calendar reduzido & expandível)
        item {
            DateSelectorSection(
                selectedDate = selectedDate,
                isExpanded = isCalendarExpanded,
                onToggleExpand = { isCalendarExpanded = !isCalendarExpanded },
                onDateSelected = { date ->
                    viewModel.selectSingleDate(date)
                }
            )
        }

        // 2. Formulário do Registro Atual (Localidade fixo 1º, Campos Dinâmicos, Obs fixo último)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (editingRecordId == null) "NOVO LANÇAMENTO DIÁRIO" else "EDITANDO REGISTRO",
                            color = OrangePrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        TextButton(
                            onClick = { viewModel.showFieldManager.value = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = OrangeSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Config. Campos",
                                color = OrangeSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Campo Fixo 1: LOCALIDADE (Primeira Posição)
                    OutlinedTextField(
                        value = location,
                        onValueChange = { viewModel.locationState.value = it },
                        label = { Text("Localidade * (Fixo - 1ª Posição)") },
                        placeholder = { Text("Ex: Obra Bloco A - Pavimento 3") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangePrimary,
                            unfocusedBorderColor = DarkBorder,
                            focusedLabelColor = OrangePrimary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        leadingIcon = {
                            Icon(Icons.Default.Place, contentDescription = null, tint = OrangePrimary)
                        }
                    )

                    // Campos Dinâmicos Customizados
                    Text(
                        text = "Campos Customizados do Registro:",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    customFields.sortedBy { it.orderIndex }.forEach { field ->
                        CustomFieldInputRow(
                            field = field,
                            onContentChange = { newText ->
                                viewModel.updateCustomFieldContent(field.id, newText)
                            },
                            onReceiptToggle = { include ->
                                viewModel.toggleFieldReceiptInForm(field.id, include)
                            }
                        )
                    }

                    // Campo Fixo ÚLTIMO: Obs (Observações)
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { viewModel.notesState.value = it },
                        label = { Text("Obs (Observações - Fixo - Última Posição)") },
                        placeholder = { Text("Observações adicionais da vistoria...") },
                        singleLine = false,
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangePrimary,
                            unfocusedBorderColor = DarkBorder,
                            focusedLabelColor = OrangePrimary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        leadingIcon = {
                            Icon(Icons.Default.Note, contentDescription = null, tint = OrangePrimary)
                        }
                    )

                    // Seção Fotográfica com Marca D'água
                    Text(
                        text = "Relatório Fotográfico (Marca D'água Automática):",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                                if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                    launchCameraWithUri()
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = OrangePrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tirar Foto", color = Color.White, fontSize = 12.sp)
                        }

                        Button(
                            onClick = { galleryLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = OrangePrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Anexar", color = Color.White, fontSize = 12.sp)
                        }
                    }

                    // Previews de Fotos Anexadas
                    if (photos.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(photos, key = { it.id }) { photo ->
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, OrangePrimary, RoundedCornerShape(8.dp))
                                ) {
                                    AsyncImage(
                                        model = File(photo.imagePath),
                                        contentDescription = "Foto Anexada",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    IconButton(
                                        onClick = { viewModel.removePhoto(photo.id) },
                                        modifier = Modifier
                                            .size(22.dp)
                                            .align(Alignment.TopEnd)
                                            .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remover",
                                            tint = Color.Red,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // AÇÕES PRINCIPAIS: Botão "Salvar Registro" e Botão "Gerar Recibo Total do Dia"
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.saveRecord() },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (editingRecordId == null) "Salvar Registro" else "Atualizar",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (editingRecordId != null) {
                            OutlinedButton(
                                onClick = { viewModel.resetForm() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Cancelar", fontSize = 12.sp)
                            }
                        }
                    }

                    Button(
                        onClick = { viewModel.generateTotalDailyReceipt() },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OrangePrimary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = OrangePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gerar Recibo Total do Dia",
                            color = OrangePrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. Lista/Caixa de Registros Salvos
        item {
            Text(
                text = "REGISTROS SALVOS NO DIA (${records.size}):",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        if (records.isEmpty()) {
            item {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Nenhum registro salvo nesta data.",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            itemsIndexed(records, key = { _, r -> r.id }) { index, record ->
                SavedRecordCard(
                    sequenceNumber = index + 1, // Sequencial iniciando sempre no 1
                    record = record,
                    onEditClick = { viewModel.requestEditRecord(record) },
                    onDeleteClick = { viewModel.requestDeleteRecord(record) }
                )
            }
        }
    }

    // MODAL DIALOGS

    // Confirm Editing Modal Dialog
    confirmEditRecord?.let { rec ->
        AlertDialog(
            onDismissRequest = { viewModel.confirmEditRecord.value = null },
            title = { Text("Confirmar Edição", color = Color.White) },
            text = { Text("Deseja carregar os dados do Registro #${records.indexOf(rec) + 1} no formulário para edição?", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { viewModel.executeEditRecord(rec) }) {
                    Text("Editar", color = OrangePrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.confirmEditRecord.value = null }) {
                    Text("Cancelar", color = TextMuted)
                }
            },
            containerColor = DarkSurface
        )
    }

    // Confirm Deleting Modal Dialog
    confirmDeleteRecord?.let { rec ->
        AlertDialog(
            onDismissRequest = { viewModel.confirmDeleteRecord.value = null },
            title = { Text("Confirmar Exclusão", color = Color.Red) },
            text = { Text("Tem certeza que deseja excluir o Registro #${records.indexOf(rec) + 1}? Esta ação não pode ser desfeita.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { viewModel.executeDeleteRecord(rec) }) {
                    Text("Excluir", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.confirmDeleteRecord.value = null }) {
                    Text("Cancelar", color = TextMuted)
                }
            },
            containerColor = DarkSurface
        )
    }

    // Custom Fields Manager Dialog Modal
    if (showFieldManager) {
        CustomFieldManagerModal(
            customFieldConfigs = customFieldConfigs,
            onClose = { viewModel.showFieldManager.value = false },
            onAddField = { title -> viewModel.addCustomFieldConfig(title) },
            onMoveField = { from, to -> viewModel.moveCustomFieldConfig(from, to) },
            onDeleteField = { id -> viewModel.deleteCustomFieldConfig(id) }
        )
    }

    // Consolidated Receipt Modal Dialog (Tertiary White paper theme)
    if (showReceiptModal) {
        ReceiptPreviewModal(
            receiptText = generatedReceiptText,
            onClose = { viewModel.showReceiptModal.value = false },
            onCopy = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Recibo de Vistoria", generatedReceiptText)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Recibo copiado para a área de transferência!", Toast.LENGTH_SHORT).show()
            },
            onShare = {
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, generatedReceiptText)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, "Compartilhar Recibo de Vistoria")
                context.startActivity(shareIntent)
            }
        )
    }
}

@Composable
fun DateSelectorSection(
    selectedDate: String,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    val formattedDisplay = try {
        val p = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val f = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("pt", "BR"))
        val d = p.parse(selectedDate)
        if (d != null) f.format(d) else selectedDate
    } catch (e: Exception) {
        selectedDate
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "DATA SELECIONADA",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formattedDisplay.uppercase(),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expandir Calendário",
                        tint = OrangePrimary
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Divider(color = DarkBorder, modifier = Modifier.padding(bottom = 12.dp))

                    // Quick Date Strip (7 days around selected date)
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
                                    .background(if (isSel) OrangePrimary else DarkSurfaceVariant)
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

                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        TextButton(
                            onClick = {
                                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                onDateSelected(today)
                            }
                        ) {
                            Text("Hoje", color = OrangePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomFieldInputRow(
    field: CustomFieldItem,
    onContentChange: (String) -> Unit,
    onReceiptToggle: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = field.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "No Recibo",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Checkbox(
                    checked = field.includeInReceipt,
                    onCheckedChange = onReceiptToggle,
                    colors = CheckboxDefaults.colors(
                        checkedColor = OrangePrimary,
                        uncheckedColor = DarkBorder,
                        checkmarkColor = Color.White
                    ),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        OutlinedTextField(
            value = field.content,
            onValueChange = onContentChange,
            placeholder = { Text("Preencha ${field.title}...") },
            singleLine = false,
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OrangePrimary,
                unfocusedBorderColor = DarkBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
    }
}

@Composable
fun SavedRecordCard(
    sequenceNumber: Int,
    record: InspectionRecordEntity,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val customFields = remember(record.fieldsJson) { JsonUtil.jsonToFields(record.fieldsJson) }
    val photos = remember(record.photosJson) { JsonUtil.jsonToPhotos(record.photosJson) }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(10.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(OrangePrimary)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Registro #$sequenceNumber",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    if (photos.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = OrangeSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${photos.size} foto(s)",
                                color = OrangeSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Editar Registro",
                            tint = OrangePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Excluir Registro",
                            tint = Color.Red,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Divider(color = DarkBorder)

            // Localidade (1st)
            Row {
                Text("Localidade: ", color = OrangeSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = record.location.ifBlank { "Não informada" },
                    color = Color.White,
                    fontSize = 12.sp
                )
            }

            // Custom fields included
            customFields.filter { it.includeInReceipt && it.content.isNotBlank() }.forEach { f ->
                Row {
                    Text("${f.title}: ", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(f.content, color = Color.White, fontSize = 12.sp)
                }
            }

            // Obs (Last)
            if (record.notes.isNotBlank()) {
                Row {
                    Text("Obs: ", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(record.notes, color = Color.White, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun CustomFieldManagerModal(
    customFieldConfigs: List<com.example.data.entity.CustomFieldConfigEntity>,
    onClose: () -> Unit,
    onAddField: (String) -> Unit,
    onMoveField: (from: Int, to: Int) -> Unit,
    onDeleteField: (String) -> Unit
) {
    var newTitle by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Text("GERENCIADOR DE CAMPOS CUSTOMIZADOS", color = OrangePrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "A ordem configurada abaixo será exatamente a ordem impressa no recibo final.",
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                // Fixed header: Localidade (1st)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkSurfaceVariant)
                        .padding(8.dp)
                ) {
                    Text("1º [FIXO]: Localidade", color = OrangePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(customFieldConfigs) { idx, config ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(DarkSurfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${idx + 2}º ${config.title}",
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )

                            Row {
                                if (idx > 0) {
                                    IconButton(
                                        onClick = { onMoveField(idx, idx - 1) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = "Subir", tint = OrangePrimary, modifier = Modifier.size(16.dp))
                                    }
                                }
                                if (idx < customFieldConfigs.size - 1) {
                                    IconButton(
                                        onClick = { onMoveField(idx, idx + 1) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = "Descer", tint = OrangePrimary, modifier = Modifier.size(16.dp))
                                    }
                                }
                                IconButton(
                                    onClick = { onDeleteField(config.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color.Red, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // Fixed footer: Obs (Last)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkSurfaceVariant)
                        .padding(8.dp)
                ) {
                    Text("Último [FIXO]: Obs (Observações)", color = OrangePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Divider(color = DarkBorder)

                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    placeholder = { Text("Nome do novo campo (ex: Horário)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangePrimary,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            onAddField(newTitle)
                            newTitle = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Criar Novo Campo", color = Color.White)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) {
                Text("Concluir", color = OrangePrimary, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = DarkSurface
    )
}

@Composable
fun ReceiptPreviewModal(
    receiptText: String,
    onClose: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("RECIBO CONSOLIDADO DO DIA", color = OrangePrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar", tint = TextMuted)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tertiary White Paper Container
                Card(
                    colors = CardDefaults.cardColors(containerColor = WhiteReceiptBg),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                ) {
                    LazyColumn(modifier = Modifier.padding(14.dp)) {
                        item {
                            Text(
                                text = receiptText,
                                color = BlackReceiptText,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onCopy,
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copiar", color = Color.White, fontSize = 12.sp)
                    }

                    Button(
                        onClick = onShare,
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OrangePrimary),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Exportar", color = OrangePrimary, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {},
        containerColor = DarkSurface
    )
}
