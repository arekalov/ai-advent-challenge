package com.arekalov.aiadvent1.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class Alternative(
    val message: MessageDto,
    val status: AlternativeStatus
)

