package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.CalcMode
import com.example.ui.viewmodel.CalculatorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Tab2CalculatorScreen(
    viewModel: CalculatorViewModel
) {
    val currentMode by viewModel.currentMode.collectAsState()
    val expression by viewModel.expression.collectAsState()
    val resultDisplay by viewModel.resultDisplay.collectAsState()
    val isRadMode by viewModel.isRadMode.collectAsState()

    val modesList = listOf(
        Pair(CalcMode.SCIENTIFIC, "Científica"),
        Pair(CalcMode.CONCRETE_VOLUME, "Concreto"),
        Pair(CalcMode.BRICKS_WALL, "Tijolos/Blocos"),
        Pair(CalcMode.MORTAR_RENDER, "Argamassa"),
        Pair(CalcMode.TILES_FLOORING, "Pisos"),
        Pair(CalcMode.EXCAVATION, "Escavação"),
        Pair(CalcMode.LABOR_PRODUCTIVITY, "Produtividade")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 12.dp)
    ) {
        // Mode Selector Bar
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(modesList) { (mode, label) ->
                val isSelected = currentMode == mode
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) OrangePrimary else DarkSurface)
                        .border(1.dp, if (isSelected) OrangeSecondary else DarkBorder, RoundedCornerShape(8.dp))
                        .clickable { viewModel.setMode(mode) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // Mode Screen
        when (currentMode) {
            CalcMode.SCIENTIFIC -> ScientificKeypadView(
                expression = expression,
                resultDisplay = resultDisplay,
                isRadMode = isRadMode,
                onButtonClick = { btn -> viewModel.onButtonClick(btn) }
            )
            CalcMode.CONCRETE_VOLUME -> ConcreteCalculatorView(viewModel)
            CalcMode.BRICKS_WALL -> BricksCalculatorView(viewModel)
            CalcMode.MORTAR_RENDER -> MortarCalculatorView(viewModel)
            CalcMode.TILES_FLOORING -> TilesCalculatorView(viewModel)
            CalcMode.EXCAVATION -> ExcavationCalculatorView(viewModel)
            CalcMode.LABOR_PRODUCTIVITY -> LaborProductivityCalculatorView(viewModel)
        }
    }
}

@Composable
fun ScientificKeypadView(
    expression: String,
    resultDisplay: String,
    isRadMode: Boolean,
    onButtonClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Display Screen
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isRadMode) "RAD" else "DEG",
                        color = OrangePrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Calculadora Científica Ps",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = expression.ifEmpty { "0" },
                        color = TextSecondary,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.End,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = resultDisplay,
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.End
                    )
                }
            }
        }

        // Scientific Buttons Grid
        val rows = listOf(
            listOf("DEG/RAD", "MC", "MR", "M+", "M-"),
            listOf("sin", "cos", "tan", "(", ")"),
            listOf("√", "x²", "x^y", "π", "%"),
            listOf("C", "DEL", "÷", "×", "log"),
            listOf("7", "8", "9", "-", "ln"),
            listOf("4", "5", "6", "+", "e"),
            listOf("1", "2", "3", "=", "abs"),
            listOf("0", ".", "", "", "")
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            rows.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    row.forEach { btn ->
                        if (btn.isNotEmpty()) {
                            val isOp = btn in listOf("+", "-", "×", "÷", "=", "C", "DEL")
                            val isOrange = btn == "=" || btn == "C"

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            isOrange -> OrangePrimary
                                            isOp -> DarkSurfaceVariant
                                            else -> DarkSurface
                                        }
                                    )
                                    .border(1.dp, if (isOrange) OrangeSecondary else DarkBorder, RoundedCornerShape(8.dp))
                                    .clickable { onButtonClick(btn) }
                            ) {
                                Text(
                                    text = btn,
                                    color = if (isOrange) Color.White else if (isOp) OrangePrimary else Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

// Formula Views
@Composable
fun ConcreteCalculatorView(viewModel: CalculatorViewModel) {
    val length by viewModel.concreteLength.collectAsState()
    val width by viewModel.concreteWidth.collectAsState()
    val height by viewModel.concreteHeight.collectAsState()
    val waste by viewModel.concreteWastePercent.collectAsState()
    val result by viewModel.concreteResult.collectAsState()

    FormulaCardContainer(title = "CÁLCULO DE VOLUME DE CONCRETO & INSUMOS") {
        CalcInputField("Comprimento da Peça (m)", length) { viewModel.concreteLength.value = it }
        CalcInputField("Largura da Peça (m)", width) { viewModel.concreteWidth.value = it }
        CalcInputField("Altura / Espessura (m)", height) { viewModel.concreteHeight.value = it }
        CalcInputField("Margem de Perda (%)", waste) { viewModel.concreteWastePercent.value = it }

        Button(
            onClick = { viewModel.calculateConcreteVolume() },
            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calcular Volume e Insumos", fontWeight = FontWeight.Bold, color = Color.White)
        }

        FormulaResultBox(result)
    }
}

@Composable
fun BricksCalculatorView(viewModel: CalculatorViewModel) {
    val l by viewModel.wallLength.collectAsState()
    val h by viewModel.wallHeight.collectAsState()
    val bl by viewModel.brickLengthCm.collectAsState()
    val bh by viewModel.brickHeightCm.collectAsState()
    val joint by viewModel.mortarJointCm.collectAsState()
    val waste by viewModel.brickWastePercent.collectAsState()
    val result by viewModel.brickResult.collectAsState()

    FormulaCardContainer(title = "CÁLCULO DE TIJOLOS E BLOCOS DE ALVENARIA") {
        CalcInputField("Comprimento da Parede (m)", l) { viewModel.wallLength.value = it }
        CalcInputField("Altura da Parede (m)", h) { viewModel.wallHeight.value = it }
        CalcInputField("Comprimento do Tijolo (cm)", bl) { viewModel.brickLengthCm.value = it }
        CalcInputField("Altura do Tijolo (cm)", bh) { viewModel.brickHeightCm.value = it }
        CalcInputField("Espessura da Junta / Argamassa (cm)", joint) { viewModel.mortarJointCm.value = it }
        CalcInputField("Margem de Perda (%)", waste) { viewModel.brickWastePercent.value = it }

        Button(
            onClick = { viewModel.calculateBricks() },
            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calcular Tijolos Necessários", fontWeight = FontWeight.Bold, color = Color.White)
        }

        FormulaResultBox(result)
    }
}

@Composable
fun MortarCalculatorView(viewModel: CalculatorViewModel) {
    val area by viewModel.mortarArea.collectAsState()
    val thick by viewModel.mortarThicknessMm.collectAsState()
    val result by viewModel.mortarResult.collectAsState()

    FormulaCardContainer(title = "CÁLCULO DE ARGAMASSA / REVESTIMENTO") {
        CalcInputField("Área de Aplicação (m²)", area) { viewModel.mortarArea.value = it }
        CalcInputField("Espessura do Revestimento (mm)", thick) { viewModel.mortarThicknessMm.value = it }

        Button(
            onClick = { viewModel.calculateMortar() },
            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calcular Volume & Peso de Argamassa", fontWeight = FontWeight.Bold, color = Color.White)
        }

        FormulaResultBox(result)
    }
}

@Composable
fun TilesCalculatorView(viewModel: CalculatorViewModel) {
    val rl by viewModel.roomLength.collectAsState()
    val rw by viewModel.roomWidth.collectAsState()
    val tl by viewModel.tileLengthCm.collectAsState()
    val tw by viewModel.tileWidthCm.collectAsState()
    val waste by viewModel.tileWastePercent.collectAsState()
    val result by viewModel.tileResult.collectAsState()

    FormulaCardContainer(title = "CÁLCULO DE PISO E REVESTIMENTO CERÂMICO") {
        CalcInputField("Comprimento do Ambiente (m)", rl) { viewModel.roomLength.value = it }
        CalcInputField("Largura do Ambiente (m)", rw) { viewModel.roomWidth.value = it }
        CalcInputField("Comprimento da Peça (cm)", tl) { viewModel.tileLengthCm.value = it }
        CalcInputField("Largura da Peça (cm)", tw) { viewModel.tileWidthCm.value = it }
        CalcInputField("Margem de Recorte / Perda (%)", waste) { viewModel.tileWastePercent.value = it }

        Button(
            onClick = { viewModel.calculateTiles() },
            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calcular Quantidade de Piso", fontWeight = FontWeight.Bold, color = Color.White)
        }

        FormulaResultBox(result)
    }
}

@Composable
fun ExcavationCalculatorView(viewModel: CalculatorViewModel) {
    val l by viewModel.excLength.collectAsState()
    val w by viewModel.excWidth.collectAsState()
    val d by viewModel.excDepth.collectAsState()
    val swell by viewModel.swellPercent.collectAsState()
    val result by viewModel.excResult.collectAsState()

    FormulaCardContainer(title = "ESCAVAÇÃO E EMPOLAMENTO DE SOLO") {
        CalcInputField("Comprimento da Vala / Cava (m)", l) { viewModel.excLength.value = it }
        CalcInputField("Largura (m)", w) { viewModel.excWidth.value = it }
        CalcInputField("Profundidade (m)", d) { viewModel.excDepth.value = it }
        CalcInputField("Taxa de Empolamento do Solo (%)", swell) { viewModel.swellPercent.value = it }

        Button(
            onClick = { viewModel.calculateExcavation() },
            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calcular Volume Solto / Caçambas", fontWeight = FontWeight.Bold, color = Color.White)
        }

        FormulaResultBox(result)
    }
}

@Composable
fun LaborProductivityCalculatorView(viewModel: CalculatorViewModel) {
    val qty by viewModel.quantityDone.collectAsState()
    val hours by viewModel.hoursWorked.collectAsState()
    val workers by viewModel.workersCount.collectAsState()
    val result by viewModel.laborResult.collectAsState()

    FormulaCardContainer(title = "PRODUTIVIDADE DE MÃO DE OBRA (RUP)") {
        CalcInputField("Quantidade Executada (m² ou m³)", qty) { viewModel.quantityDone.value = it }
        CalcInputField("Horas Trabalhadas (h)", hours) { viewModel.hoursWorked.value = it }
        CalcInputField("Número de Operários da Equipe", workers) { viewModel.workersCount.value = it }

        Button(
            onClick = { viewModel.calculateLaborProductivity() },
            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calcular Índice RUP & Rendimento", fontWeight = FontWeight.Bold, color = Color.White)
        }

        FormulaResultBox(result)
    }
}

@Composable
fun FormulaCardContainer(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = title,
                        color = OrangePrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    content()
                }
            }
        }
    }
}

@Composable
fun CalcInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = OrangePrimary,
            unfocusedBorderColor = DarkBorder,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        )
    )
}

@Composable
fun FormulaResultBox(resultText: String) {
    if (resultText.isNotBlank()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(OrangePrimary)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "RESULTADO:",
                    color = OrangeSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = resultText,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
