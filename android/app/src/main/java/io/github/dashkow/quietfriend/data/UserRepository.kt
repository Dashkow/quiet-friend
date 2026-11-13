package io.github.dashkow.quietfriend.data

import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import io.github.dashkow.quietfriend.auth.GoogleAuth
import io.github.dashkow.quietfriend.data.api.ApiClient
import io.github.dashkow.quietfriend.data.api.CreateUserRequest
import io.github.dashkow.quietfriend.data.api.CreateUserResponse
import io.github.dashkow.quietfriend.data.api.UsersApi
import retrofit2.Response
import java.io.IOException
import java.time.ZoneId

sealed interface CreateUserResult {
    data class Success(val documentsAcceptanceRequired: Boolean) : CreateUserResult
    data object NoNetwork : CreateUserResult
    /** 401 (кроме истёкшего токена) и 403: выйти из Firebase и вернуться на экран входа. */
    data object SignInAgain : CreateUserResult
    /** 426: версия приложения ниже минимальной. */
    data object UpgradeRequired : CreateUserResult
    /** 400, 429, 500, 503 и всё неожиданное. */
    data object Failed : CreateUserResult
}

/** Шаги 8–14 сценария UC-01: POST /api/v1/users и обработка ошибок по таблице из анализа. */
class UserRepository(
    private val googleAuth: GoogleAuth,
    private val api: UsersApi = ApiClient.users,
) {

    suspend fun createUser(): CreateUserResult = try {
        doCreateUser()
    } catch (e: IOException) {
        CreateUserResult.NoNetwork
    } catch (e: FirebaseNetworkException) {
        CreateUserResult.NoNetwork
    } catch (e: FirebaseAuthException) {
        // Не удалось получить токен: пользователь отключён или сессия отозвана
        CreateUserResult.SignInAgain
    }

    private suspend fun doCreateUser(): CreateUserResult {
        val body = CreateUserRequest(timezone = ZoneId.systemDefault().id)

        var response = send(body, forceRefresh = false) ?: return CreateUserResult.SignInAgain
        var error = if (response.isSuccessful) null else ApiClient.parseError(response.errorBody()?.string())

        // TOKEN_EXPIRED: обновить токен и один раз повторить запрос
        if (response.code() == 401 && error?.reason == "TOKEN_EXPIRED") {
            response = send(body, forceRefresh = true) ?: return CreateUserResult.SignInAgain
            error = if (response.isSuccessful) null else ApiClient.parseError(response.errorBody()?.string())
        }

        if (response.isSuccessful) {
            val result = response.body() ?: return CreateUserResult.Failed
            return CreateUserResult.Success(result.documentsAcceptanceRequired)
        }

        // request_id — чтобы найти запрос в логах бэкенда; персональных данных здесь нет
        Log.w(TAG, "POST /api/v1/users: ${response.code()} ${error?.code}/${error?.reason} request_id=${error?.requestId}")
        return when (response.code()) {
            401, 403 -> CreateUserResult.SignInAgain
            426 -> CreateUserResult.UpgradeRequired
            else -> CreateUserResult.Failed
        }
    }

    private suspend fun send(body: CreateUserRequest, forceRefresh: Boolean): Response<CreateUserResponse>? {
        val token = googleAuth.idToken(forceRefresh) ?: return null
        return api.createUser(authorization = "Bearer $token", body = body)
    }

    private companion object {
        const val TAG = "UserRepository"
    }
}
