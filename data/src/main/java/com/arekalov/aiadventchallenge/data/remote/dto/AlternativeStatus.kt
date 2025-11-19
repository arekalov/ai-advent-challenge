package com.arekalov.aiadventchallenge.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AlternativeStatus {
    @SerialName("ALTERNATIVE_STATUS_UNSPECIFIED")
    UNSPECIFIED,
    
    @SerialName("ALTERNATIVE_STATUS_PARTIAL")
    PARTIAL,
    
    @SerialName("ALTERNATIVE_STATUS_TRUNCATED_FINAL")
    TRUNCATED_FINAL,
    
    @SerialName("ALTERNATIVE_STATUS_FINAL")
    FINAL,
    
    @SerialName("ALTERNATIVE_STATUS_CONTENT_FILTER")
    CONTENT_FILTER,
    
    @SerialName("ALTERNATIVE_STATUS_FINAL_CHOICE")
    FINAL_CHOICE,
    
    @SerialName("ALTERNATIVE_STATUS_TRUNCATED_FINAL_CHOICE")
    TRUNCATED_FINAL_CHOICE,
    
    @SerialName("ALTERNATIVE_STATUS_TOOL_CALLS")
    TOOL_CALLS;
    
    fun isSuccess(): Boolean {
        return this == FINAL_CHOICE || this == TRUNCATED_FINAL_CHOICE || this == FINAL
    }
    
    fun isContentFiltered(): Boolean {
        return this == CONTENT_FILTER
    }
    
    fun isToolCalls(): Boolean {
        return this == TOOL_CALLS
    }
}

