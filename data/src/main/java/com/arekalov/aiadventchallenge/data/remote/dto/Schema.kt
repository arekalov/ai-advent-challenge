package com.arekalov.aiadventchallenge.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class Schema(
    val type: String = "object",
    val properties: Map<String, Property>,
    val required: List<String>
)

