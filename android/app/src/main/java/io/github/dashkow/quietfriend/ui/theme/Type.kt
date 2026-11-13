package io.github.dashkow.quietfriend.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import io.github.dashkow.quietfriend.R

// Nunito из макетов: 400, 600, 700, 800. Файлы в res/font, лицензия OFL
val Nunito = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
)

private val Default = Typography()

private fun TextStyle.nunito() = copy(fontFamily = Nunito)

// Стандартные размеры Material 3, шрифт — Nunito во всех стилях
val Typography = Typography(
    displayLarge = Default.displayLarge.nunito(),
    displayMedium = Default.displayMedium.nunito(),
    displaySmall = Default.displaySmall.nunito(),
    headlineLarge = Default.headlineLarge.nunito(),
    headlineMedium = Default.headlineMedium.nunito(),
    headlineSmall = Default.headlineSmall.nunito(),
    titleLarge = Default.titleLarge.nunito(),
    titleMedium = Default.titleMedium.nunito(),
    titleSmall = Default.titleSmall.nunito(),
    bodyLarge = Default.bodyLarge.nunito(),
    bodyMedium = Default.bodyMedium.nunito(),
    bodySmall = Default.bodySmall.nunito(),
    labelLarge = Default.labelLarge.nunito(),
    labelMedium = Default.labelMedium.nunito(),
    labelSmall = Default.labelSmall.nunito(),
)
