package com.ananas.pinelauncher.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ananas.pinelauncher.R

// Подключаем твой шрифт
val PineFont = FontFamily(
    Font(R.font.pine_font, FontWeight.Normal)
)

// Переопределяем ВСЮ типографику
val AppTypography = Typography().copy(

    displayLarge = Typography().displayLarge.copy(fontFamily = PineFont),
    displayMedium = Typography().displayMedium.copy(fontFamily = PineFont),
    displaySmall = Typography().displaySmall.copy(fontFamily = PineFont),

    headlineLarge = Typography().headlineLarge.copy(fontFamily = PineFont),
    headlineMedium = Typography().headlineMedium.copy(fontFamily = PineFont),
    headlineSmall = Typography().headlineSmall.copy(fontFamily = PineFont),

    titleLarge = Typography().titleLarge.copy(fontFamily = PineFont),
    titleMedium = Typography().titleMedium.copy(fontFamily = PineFont),
    titleSmall = Typography().titleSmall.copy(fontFamily = PineFont),

    bodyLarge = Typography().bodyLarge.copy(fontFamily = PineFont),
    bodyMedium = Typography().bodyMedium.copy(fontFamily = PineFont),
    bodySmall = Typography().bodySmall.copy(fontFamily = PineFont),

    labelLarge = Typography().labelLarge.copy(fontFamily = PineFont),
    labelMedium = Typography().labelMedium.copy(fontFamily = PineFont),
    labelSmall = Typography().labelSmall.copy(fontFamily = PineFont),
)