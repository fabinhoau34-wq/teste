package com.example.util

import com.example.data.entity.InspectionRecordEntity
import java.text.SimpleDateFormat
import java.util.Locale

object ReceiptGenerator {

    fun generateDailyReceipt(
        dateIso: String,
        records: List<InspectionRecordEntity>
    ): String {
        val dateFormatted = try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = parser.parse(dateIso)
            if (date != null) formatter.format(date) else dateIso
        } catch (e: Exception) {
            dateIso
        }

        val sb = StringBuilder()
        sb.append("======== RECIBO DE VISTORIA ==========\n")
        sb.append("Data: ").append(dateFormatted).append("\n\n")

        if (records.isEmpty()) {
            sb.append("Nenhum registro cadastrado para esta data.\n")
            return sb.toString()
        }

        var totalPhotosCount = 0

        records.forEachIndexed { index, record ->
            val recordNum = index + 1
            sb.append("--- REGISTRO ").append(recordNum).append(" ---\n")

            // 1st position: Fixed field Localidade
            if (record.location.isNotBlank()) {
                sb.append("Localidade: ").append(record.location.trim()).append("\n")
            } else {
                sb.append("Localidade: Não informada\n")
            }

            // Dynamic custom fields in user configured order
            val customFields = JsonUtil.jsonToFields(record.fieldsJson)
                .filter { it.includeInReceipt }
                .sortedBy { it.orderIndex }

            for (field in customFields) {
                if (field.content.isNotBlank()) {
                    sb.append(field.title.trim()).append(": ").append(field.content.trim()).append("\n")
                }
            }

            // Last position: Fixed field Obs
            if (record.notes.isNotBlank()) {
                sb.append("Obs: ").append(record.notes.trim()).append("\n")
            } else {
                sb.append("Obs: Sem observações\n")
            }

            val photos = JsonUtil.jsonToPhotos(record.photosJson)
            totalPhotosCount += photos.size

            sb.append("\n")
        }

        if (totalPhotosCount > 0) {
            sb.append("Relatório Fotográfico\n")
            records.forEachIndexed { index, record ->
                val photos = JsonUtil.jsonToPhotos(record.photosJson)
                if (photos.isNotEmpty()) {
                    sb.append("- Registro ").append(index + 1).append(": ")
                        .append(photos.size).append(" foto(s) anexada(s)\n")
                }
            }
        }

        return sb.toString().trimEnd()
    }
}
