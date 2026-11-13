package io.github.dashkow.quietfriend.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface UsersApi {

    @POST("api/v1/users")
    suspend fun createUser(
        @Header("Authorization") authorization: String,
        @Body body: CreateUserRequest,
    ): Response<CreateUserResponse>
}
