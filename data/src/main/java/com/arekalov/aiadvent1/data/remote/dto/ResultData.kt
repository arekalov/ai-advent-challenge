package com.arekalov.aiadvent1.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ResultData(
    val alternatives: List<Alternative>,
    val usage: Usage,
    val modelVersion: String
)

