package io.github.dashkow.quietfriend.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Контракт POST /api/v1/users (анализ экрана 01, раздел 4)

@Serializable
data class CreateUserRequest(
    val timezone: String,
)

@Serializable
data class CreateUserResponse(
    val id: String,
    @SerialName("documents_acceptance_required") val documentsAcceptanceRequired: Boolean,
)

// Единый формат ошибок: {"error":{"code":...,"reason":...,"request_id":...}}

@Serializable
data class ErrorResponse(
    val error: ErrorBody,
)

@Serializable
data class ErrorBody(
    val code: String,
    val reason: String? = null,
    @SerialName("request_id") val requestId: String? = null,
)
