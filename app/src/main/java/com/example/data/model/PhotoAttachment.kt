package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PhotoAttachment(
    val id: String,
    val imagePath: String,
    val location: String,
    val dateIso: String,
    val timeFormatted: String,
    val timestamp: Long = System.currentTimeMillis()
)
