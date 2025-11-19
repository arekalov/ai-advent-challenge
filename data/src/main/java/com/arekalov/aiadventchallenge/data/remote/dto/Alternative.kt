package com.arekalov.aiadventchallenge.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class Alternative(
    val message: MessageDto,
    val status: AlternativeStatus
    // toolCalls убираем, они теперь внутри message.toolCallList
)

