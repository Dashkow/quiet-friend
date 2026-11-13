package io.github.dashkow.quietfriend.ui.login

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.dashkow.quietfriend.R
import io.github.dashkow.quietfriend.auth.GoogleAuth
import io.github.dashkow.quietfriend.auth.GoogleSignInResult
import io.github.dashkow.quietfriend.data.CreateUserResult
import io.github.dashkow.quietfriend.data.UserRepository
import io.github.dashkow.quietfriend.util.isOnline
import kotlinx.coroutines.launch

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Error(@StringRes val message: Int) : LoginUiState
    data object UpgradeRequired : LoginUiState
    data class SignedIn(val documentsAcceptanceRequired: Boolean) : LoginUiState
}

class LoginViewModel : ViewModel() {

    private val googleAuth = GoogleAuth()
    private val users = UserRepository(googleAuth)

    var state by mutableStateOf<LoginUiState>(LoginUiState.Idle)
        private set

    /** Кнопка «Войти через Google» и «Повторить». [activityContext] не сохраняется. */
    fun onSignInClick(activityContext: Context) {
        if (state == LoginUiState.Loading) return
        state = LoginUiState.Loading
        viewModelScope.launch {
            if (!activityContext.isOnline()) {
                state = LoginUiState.Error(R.string.error_no_internet)
                return@launch
            }
            // Уже вошли в Firebase (например, бэкенд был недоступен) — окно выбора аккаунта не нужно
            if (!googleAuth.isSignedIn) {
                when (googleAuth.signIn(activityContext)) {
                    GoogleSignInResult.Success -> Unit
                    GoogleSignInResult.Cancelled -> { state = LoginUiState.Idle; return@launch }
                    GoogleSignInResult.NoNetwork -> { state = LoginUiState.Error(R.string.error_no_internet); return@launch }
                    GoogleSignInResult.Failed -> { state = LoginUiState.Error(R.string.error_sign_in_failed); return@launch }
                }
            }
            registerOnBackend(activityContext)
        }
    }

    private suspend fun registerOnBackend(context: Context) {
        state = when (val result = users.createUser()) {
            is CreateUserResult.Success -> LoginUiState.SignedIn(result.documentsAcceptanceRequired)
            CreateUserResult.NoNetwork -> LoginUiState.Error(R.string.error_no_internet)
            CreateUserResult.UpgradeRequired -> LoginUiState.UpgradeRequired
            CreateUserResult.Failed -> LoginUiState.Error(R.string.error_generic)
            CreateUserResult.SignInAgain -> {
                googleAuth.signOut(context)
                LoginUiState.Idle
            }
        }
    }
}
