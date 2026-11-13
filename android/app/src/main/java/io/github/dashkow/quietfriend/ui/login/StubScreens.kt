package io.github.dashkow.quietfriend.ui.login

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dashkow.quietfriend.R
import io.github.dashkow.quietfriend.ui.theme.InkMuted

/** Временный экран после входа: следующие экраны появятся в своих итерациях. */
@Composable
fun SignedInStubScreen(documentsAcceptanceRequired: Boolean) {
    CenteredColumn {
        Text(stringResource(R.string.signed_in_title), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            text = stringResource(
                if (documentsAcceptanceRequired) R.string.signed_in_next_documents else R.string.signed_in_next_home
            ),
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = InkMuted,
        )
    }
}

/** 426 UPGRADE_REQUIRED: «Пожалуйста, обновите приложение» со ссылкой на Google Play. */
@Composable
fun UpgradeRequiredScreen() {
    val context = LocalContext.current
    CenteredColumn {
        Text(
            text = stringResource(R.string.upgrade_title),
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = {
                val uri = Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            },
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text(stringResource(R.string.upgrade_button), fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CenteredColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) { content() }
}
