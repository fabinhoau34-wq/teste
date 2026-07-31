package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

enum class CalcMode {
    SCIENTIFIC,
    CONCRETE_VOLUME,
    BRICKS_WALL,
    MORTAR_RENDER,
    TILES_FLOORING,
    EXCAVATION,
    LABOR_PRODUCTIVITY
}

class CalculatorViewModel : ViewModel() {

    private val _currentMode = MutableStateFlow(CalcMode.SCIENTIFIC)
    val currentMode = _currentMode.asStateFlow()

    // Scientific Calculator State
    private val _expression = MutableStateFlow("")
    val expression = _expression.asStateFlow()

    private val _resultDisplay = MutableStateFlow("0")
    val resultDisplay = _resultDisplay.asStateFlow()

    private val _isRadMode = MutableStateFlow(true)
    val isRadMode = _isRadMode.asStateFlow()

    private val _memoryValue = MutableStateFlow(0.0)
    val memoryValue = _memoryValue.asStateFlow()

    // Civil Engineering Formula States
    // Concrete
    val concreteLength = MutableStateFlow("5.0")
    val concreteWidth = MutableStateFlow("0.20")
    val concreteHeight = MutableStateFlow("0.40")
    val concreteWastePercent = MutableStateFlow("10")
    val concreteResult = MutableStateFlow("")

    // Bricks
    val wallLength = MutableStateFlow("10.0")
    val wallHeight = MutableStateFlow("2.80")
    val brickLengthCm = MutableStateFlow("19")
    val brickHeightCm = MutableStateFlow("19")
    val mortarJointCm = MutableStateFlow("1.5")
    val brickWastePercent = MutableStateFlow("10")
    val brickResult = MutableStateFlow("")

    // Mortar
    val mortarArea = MutableStateFlow("25.0")
    val mortarThicknessMm = MutableStateFlow("20")
    val mortarResult = MutableStateFlow("")

    // Tiles
    val roomLength = MutableStateFlow("4.0")
    val roomWidth = MutableStateFlow("5.0")
    val tileLengthCm = MutableStateFlow("60")
    val tileWidthCm = MutableStateFlow("60")
    val tileWastePercent = MutableStateFlow("10")
    val tileResult = MutableStateFlow("")

    // Excavation
    val excLength = MutableStateFlow("6.0")
    val excWidth = MutableStateFlow("3.0")
    val excDepth = MutableStateFlow("1.5")
    val swellPercent = MutableStateFlow("30") // Empolamento %
    val excResult = MutableStateFlow("")

    // Labor Productivity
    val quantityDone = MutableStateFlow("100") // e.g. 100 m²
    val hoursWorked = MutableStateFlow("8")
    val workersCount = MutableStateFlow("2")
    val laborResult = MutableStateFlow("")

    fun setMode(mode: CalcMode) {
        _currentMode.value = mode
    }

    // Scientific Calculator Button Clicks
    fun onButtonClick(btn: String) {
        when (btn) {
            "C" -> {
                _expression.value = ""
                _resultDisplay.value = "0"
            }
            "DEL" -> {
                if (_expression.value.isNotEmpty()) {
                    _expression.value = _expression.value.dropLast(1)
                }
            }
            "=" -> {
                evaluateExpression()
            }
            "DEG/RAD" -> {
                _isRadMode.value = !_isRadMode.value
            }
            "MC" -> _memoryValue.value = 0.0
            "MR" -> _expression.value += _memoryValue.value.toString()
            "M+" -> {
                val currentNum = _resultDisplay.value.toDoubleOrNull() ?: 0.0
                _memoryValue.value += currentNum
            }
            "M-" -> {
                val currentNum = _resultDisplay.value.toDoubleOrNull() ?: 0.0
                _memoryValue.value -= currentNum
            }
            "π" -> _expression.value += "π"
            "e" -> _expression.value += "e"
            "x²" -> _expression.value += "^2"
            "x^y" -> _expression.value += "^"
            "√" -> _expression.value += "sqrt("
            "sin" -> _expression.value += "sin("
            "cos" -> _expression.value += "cos("
            "tan" -> _expression.value += "tan("
            "log" -> _expression.value += "log("
            "ln" -> _expression.value += "ln("
            else -> {
                _expression.value += btn
            }
        }
    }

    private fun evaluateExpression() {
        val expr = _expression.value.trim()
        if (expr.isEmpty()) return

        try {
            val result = parseAndEvaluate(expr, _isRadMode.value)
            val formatted = if (result % 1.0 == 0.0) {
                result.toLong().toString()
            } else {
                String.format(java.util.Locale.US, "%.6f", result).trimEnd('0').trimEnd('.')
            }
            _resultDisplay.value = formatted
        } catch (e: Exception) {
            _resultDisplay.value = "Erro"
        }
    }

    // Basic Math Expression Parser
    private fun parseAndEvaluate(input: String, isRad: Boolean): Double {
        var str = input.replace("×", "*")
            .replace("÷", "/")
            .replace("π", Math.PI.toString())
            .replace("e", Math.E.toString())

        return object {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < str.length) str[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < str.length) throw RuntimeException("Unexpected: " + ch.toChar())
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x += parseTerm()
                    else if (eat('-'.code)) x -= parseTerm()
                    else return x
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x *= parseFactor()
                    else if (eat('/'.code)) x /= parseFactor()
                    else if (eat('%'.code)) x %= parseFactor()
                    else return x
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()

                var x: Double
                val startPos = pos
                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code)
                } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                    while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                    x = str.substring(startPos, pos).toDouble()
                } else if (ch in 'a'.code..'z'.code) {
                    while (ch in 'a'.code..'z'.code) nextChar()
                    val func = str.substring(startPos, pos)
                    x = parseFactor()
                    x = when (func) {
                        "sqrt" -> sqrt(x)
                        "sin" -> if (isRad) sin(x) else sin(Math.toRadians(x))
                        "cos" -> if (isRad) cos(x) else cos(Math.toRadians(x))
                        "tan" -> if (isRad) tan(x) else tan(Math.toRadians(x))
                        "log" -> log10(x)
                        "ln" -> ln(x)
                        "abs" -> abs(x)
                        else -> throw RuntimeException("Unknown function: $func")
                    }
                } else {
                    throw RuntimeException("Unexpected: " + ch.toChar())
                }

                if (eat('^'.code)) x = x.pow(parseFactor())

                return x
            }
        }.parse()
    }

    // Engineering Calculations
    fun calculateConcreteVolume() {
        val l = concreteLength.value.toDoubleOrNull() ?: 0.0
        val w = concreteWidth.value.toDoubleOrNull() ?: 0.0
        val h = concreteHeight.value.toDoubleOrNull() ?: 0.0
        val waste = (concreteWastePercent.value.toDoubleOrNull() ?: 0.0) / 100.0

        val netVolume = l * w * h
        val totalVolume = netVolume * (1 + waste)

        // Standard 1:2:3 mix estimation (per m³: ~7 bags cement, 0.52 m³ sand, 0.78 m³ gravel)
        val cementBags = totalVolume * 7.0
        val sandM3 = totalVolume * 0.52
        val gravelM3 = totalVolume * 0.78

        concreteResult.value = """
            • Volume Líquido: ${String.format("%.2f", netVolume)} m³
            • Volume Total (+${concreteWastePercent.value}% perda): ${String.format("%.2f", totalVolume)} m³
            
            Estimativa de Insumos (Traço 1:2:3):
            - Cimento (50kg): ${ceil(cementBags).toInt()} sacos
            - Areia: ${String.format("%.2f", sandM3)} m³
            - Brita: ${String.format("%.2f", gravelM3)} m³
        """.trimIndent()
    }

    fun calculateBricks() {
        val l = wallLength.value.toDoubleOrNull() ?: 0.0
        val h = wallHeight.value.toDoubleOrNull() ?: 0.0
        val bl = (brickLengthCm.value.toDoubleOrNull() ?: 0.0) / 100.0
        val bh = (brickHeightCm.value.toDoubleOrNull() ?: 0.0) / 100.0
        val joint = (mortarJointCm.value.toDoubleOrNull() ?: 0.0) / 100.0
        val waste = (brickWastePercent.value.toDoubleOrNull() ?: 0.0) / 100.0

        val wallArea = l * h
        val unitArea = (bl + joint) * (bh + joint)

        if (unitArea <= 0) return

        val bricksPerM2 = 1.0 / unitArea
        val totalBricksNet = wallArea * bricksPerM2
        val totalBricksGross = totalBricksNet * (1 + waste)

        brickResult.value = """
            • Área da Parede: ${String.format("%.2f", wallArea)} m²
            • Consumo por m²: ${ceil(bricksPerM2).toInt()} tijolos/m²
            • Total Líquido: ${ceil(totalBricksNet).toInt()} unidades
            • Total Recomendado (+${brickWastePercent.value}% perda): ${ceil(totalBricksGross).toInt()} unidades
        """.trimIndent()
    }

    fun calculateMortar() {
        val area = mortarArea.value.toDoubleOrNull() ?: 0.0
        val thickMm = mortarThicknessMm.value.toDoubleOrNull() ?: 0.0

        val thicknessM = thickMm / 1000.0
        val volumeM3 = area * thicknessM
        val weightKg = volumeM3 * 1800.0 // ~1800 kg/m³ density

        mortarResult.value = """
            • Área de Aplicação: ${String.format("%.2f", area)} m²
            • Espessura da Camada: $thickMm mm
            • Volume de Argamassa: ${String.format("%.3f", volumeM3)} m³ (${String.format("%.1f", volumeM3 * 1000)} Litros)
            • Peso Estimado de Argamassa Seca: ${ceil(weightKg).toInt()} kg (~${ceil(weightKg / 20.0).toInt()} sacos de 20kg)
        """.trimIndent()
    }

    fun calculateTiles() {
        val l = roomLength.value.toDoubleOrNull() ?: 0.0
        val w = roomWidth.value.toDoubleOrNull() ?: 0.0
        val tl = (tileLengthCm.value.toDoubleOrNull() ?: 0.0) / 100.0
        val tw = (tileWidthCm.value.toDoubleOrNull() ?: 0.0) / 100.0
        val waste = (tileWastePercent.value.toDoubleOrNull() ?: 0.0) / 100.0

        val roomArea = l * w
        val tileArea = tl * tw

        if (tileArea <= 0) return

        val netTiles = roomArea / tileArea
        val grossTiles = netTiles * (1 + waste)
        val grossArea = roomArea * (1 + waste)

        tileResult.value = """
            • Área Útil do Ambiente: ${String.format("%.2f", roomArea)} m²
            • Peças Líquidas: ${ceil(netTiles).toInt()} peças
            • Área a Comprar (+${tileWastePercent.value}% perda): ${String.format("%.2f", grossArea)} m²
            • Total de Peças a Comprar: ${ceil(grossTiles).toInt()} peças
        """.trimIndent()
    }

    fun calculateExcavation() {
        val l = excLength.value.toDoubleOrNull() ?: 0.0
        val w = excWidth.value.toDoubleOrNull() ?: 0.0
        val d = excDepth.value.toDoubleOrNull() ?: 0.0
        val swell = (swellPercent.value.toDoubleOrNull() ?: 0.0) / 100.0

        val netVolume = l * w * d
        val looseVolume = netVolume * (1 + swell)

        excResult.value = """
            • Volume do In-situ (Corte): ${String.format("%.2f", netVolume)} m³
            • Taxa de Empolamento: ${swellPercent.value}%
            • Volume Solto / Transportado: ${String.format("%.2f", looseVolume)} m³
            • Viagens de Caminhão (Caçamba 6m³): ${ceil(looseVolume / 6.0).toInt()} caçambas
        """.trimIndent()
    }

    fun calculateLaborProductivity() {
        val qty = quantityDone.value.toDoubleOrNull() ?: 0.0
        val hours = hoursWorked.value.toDoubleOrNull() ?: 0.0
        val workers = workersCount.value.toDoubleOrNull() ?: 0.0

        val totalManHours = hours * workers
        if (totalManHours <= 0) return

        val productivity = qty / totalManHours
        val dailyPerWorker = (qty / workers) / (hours / 8.0)

        laborResult.value = """
            • Volume Total Executado: $qty m² (ou m³)
            • Total Homem-Hora (H.H): ${String.format("%.1f", totalManHours)} h.h
            • Índ. de Produtividade (RUP): ${String.format("%.2f", productivity)} m²/h.h
            • Roupagem Diária por Operário (jornada 8h): ${String.format("%.2f", dailyPerWorker)} m²/operário.dia
        """.trimIndent()
    }
}
