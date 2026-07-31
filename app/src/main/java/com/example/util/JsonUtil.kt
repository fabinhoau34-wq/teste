package com.example.util

import com.example.data.model.CustomFieldItem
import com.example.data.model.PhotoAttachment
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object JsonUtil {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val customFieldListType = Types.newParameterizedType(List::class.java, CustomFieldItem::class.java)
    private val photoListType = Types.newParameterizedType(List::class.java, PhotoAttachment::class.java)

    private val customFieldAdapter = moshi.adapter<List<CustomFieldItem>>(customFieldListType)
    private val photoAdapter = moshi.adapter<List<PhotoAttachment>>(photoListType)

    fun fieldsToJson(fields: List<CustomFieldItem>): String {
        return try {
            customFieldAdapter.toJson(fields)
        } catch (e: Exception) {
            "[]"
        }
    }

    fun jsonToFields(json: String): List<CustomFieldItem> {
        return try {
            if (json.isBlank()) emptyList() else customFieldAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun photosToJson(photos: List<PhotoAttachment>): String {
        return try {
            photoAdapter.toJson(photos)
        } catch (e: Exception) {
            "[]"
        }
    }

    fun jsonToPhotos(json: String): List<PhotoAttachment> {
        return try {
            if (json.isBlank()) emptyList() else photoAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
