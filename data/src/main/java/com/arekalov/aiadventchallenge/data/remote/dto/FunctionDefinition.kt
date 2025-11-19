package com.arekalov.aiadventchallenge.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class FunctionDefinition(
    val name: String,
    val description: String,
    val parameters: JsonObject
)

