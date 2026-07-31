package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CustomFieldItem(
    val id: String,
    val title: String,
    val content: String = "",
    val includeInReceipt: Boolean = true,
    val orderIndex: Int = 0
)
