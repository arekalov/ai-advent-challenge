package com.arekalov.aiadvent1.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class JsonSchema(
    val schema: Schema
)

