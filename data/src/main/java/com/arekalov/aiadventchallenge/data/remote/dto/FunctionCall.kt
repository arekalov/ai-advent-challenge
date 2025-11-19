package com.arekalov.aiadventchallenge.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class FunctionCall(
    val name: String,
    val arguments: String
)

