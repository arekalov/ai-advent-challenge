package com.arekalov.aiadventchallenge.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class JsonResponse(
    val response: String,
    val category: String
)

