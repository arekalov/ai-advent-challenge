package com.arekalov.aiadventchallenge.data.local.converters

import androidx.room.TypeConverter
import com.arekalov.aiadventchallenge.domain.model.ModelMetrics
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * TypeConverters для Room database
 * Используется для сохранения сложных типов данных в SQLite
 */
class Converters {
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @TypeConverter
    fun fromModelMetrics(value: ModelMetrics?): String? {
        return value?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toModelMetrics(value: String?): ModelMetrics? {
        return value?.let { 
            try {
                json.decodeFromString<ModelMetrics>(it)
            } catch (e: Exception) {
                null
            }
        }
    }
}

