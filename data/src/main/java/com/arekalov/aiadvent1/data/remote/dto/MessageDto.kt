package com.arekalov.aiadvent1.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class MessageDto(
    val role: String,
    val text: String
)

