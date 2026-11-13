package io.github.dashkow.quietfriend.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dashkow.quietfriend.R
import io.github.dashkow.quietfriend.ui.theme.Ink
import io.github.dashkow.quietfriend.ui.theme.InkMuted
import io.github.dashkow.quietfriend.ui.theme.MintBorder
import io.github.dashkow.quietfriend.ui.theme.MintDark
import io.github.dashkow.quietfriend.ui.theme.Peach
import io.github.dashkow.quietfriend.ui.theme.QuietFriendTheme
import io.github.dashkow.quietfriend.ui.theme.White
import android.content.Context

/** Экран 01. Вход (макет Main). */
@Composable
fun LoginScreen(
    state: LoginUiState,
    onSignInClick: (Context) -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Spacer(Modifier.height(32.dp))
        Box(
            modifier = Modifier
                .size(148.dp)
                .clip(CircleShape)
                .background(Peach),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_quiet_friend),
                contentDescription = null,
                modifier = Modifier.size(112.dp),
            )
        }
        Text(
            text = stringResource(R.string.app_name),
            fontSize = 34.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Ink,
        )
        Text(
            text = stringResource(R.string.login_tagline),
            fontSize = 17.sp,
            lineHeight = 26.sp,
            textAlign = TextAlign.Center,
            color = InkMuted,
            modifier = Modifier.widthIn(max = 280.dp),
        )

        Spacer(Modifier.weight(1f))

        if (state is LoginUiState.Error) {
            Text(
                text = stringResource(state.message),
                fontSize = 15.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
                color = Ink,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Peach)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }

        OutlinedButton(
            onClick = { onSignInClick(context) },
            enabled = state != LoginUiState.Loading,
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, MintBorder),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = White, contentColor = Ink),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            if (state == LoginUiState.Loading) {
                CircularProgressIndicator(
                    color = MintDark,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp),
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_person),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = stringResource(
                            if (state is LoginUiState.Error) R.string.action_retry else R.string.login_button
                        ),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.login_other_methods),
            fontSize = 13.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            color = InkMuted,
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LoginScreenPreview() {
    QuietFriendTheme { LoginScreen(state = LoginUiState.Idle, onSignInClick = {}) }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LoginScreenErrorPreview() {
    QuietFriendTheme { LoginScreen(state = LoginUiState.Error(R.string.error_no_internet), onSignInClick = {}) }
}
