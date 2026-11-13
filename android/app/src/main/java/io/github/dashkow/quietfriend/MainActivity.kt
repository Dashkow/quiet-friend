package io.github.dashkow.quietfriend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.dashkow.quietfriend.ui.login.LoginScreen
import io.github.dashkow.quietfriend.ui.login.LoginUiState
import io.github.dashkow.quietfriend.ui.login.LoginViewModel
import io.github.dashkow.quietfriend.ui.login.SignedInStubScreen
import io.github.dashkow.quietfriend.ui.login.UpgradeRequiredScreen
import io.github.dashkow.quietfriend.ui.theme.QuietFriendTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuietFriendTheme {
                QuietFriendApp()
            }
        }
    }
}

@Composable
private fun QuietFriendApp(viewModel: LoginViewModel = viewModel()) {
    when (val state = viewModel.state) {
        is LoginUiState.SignedIn -> SignedInStubScreen(state.documentsAcceptanceRequired)
        LoginUiState.UpgradeRequired -> UpgradeRequiredScreen()
        else -> LoginScreen(state = state, onSignInClick = viewModel::onSignInClick)
    }
}
