package com.example.myapplication.ui.theme

import android.graphics.Typeface
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
)

/**
 * Letra condensada del sistema (Roboto Condensed, viene en todos los Android) para el estilo
 * Cyberpunk, como en los diseños del equipo. No hay que agregar archivos de fuentes.
 */
private val FuenteCondensada = FontFamily(Typeface.create("sans-serif-condensed", Typeface.NORMAL))

/** Los mismos tamaños de [Typography], con la letra condensada. */
val TipografiaCyberpunk = Typography.run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = FuenteCondensada),
        displayMedium = displayMedium.copy(fontFamily = FuenteCondensada),
        displaySmall = displaySmall.copy(fontFamily = FuenteCondensada),
        headlineLarge = headlineLarge.copy(fontFamily = FuenteCondensada),
        headlineMedium = headlineMedium.copy(fontFamily = FuenteCondensada),
        headlineSmall = headlineSmall.copy(fontFamily = FuenteCondensada),
        titleLarge = titleLarge.copy(fontFamily = FuenteCondensada),
        titleMedium = titleMedium.copy(fontFamily = FuenteCondensada),
        titleSmall = titleSmall.copy(fontFamily = FuenteCondensada),
        bodyLarge = bodyLarge.copy(fontFamily = FuenteCondensada),
        bodyMedium = bodyMedium.copy(fontFamily = FuenteCondensada),
        bodySmall = bodySmall.copy(fontFamily = FuenteCondensada),
        labelLarge = labelLarge.copy(fontFamily = FuenteCondensada),
        labelMedium = labelMedium.copy(fontFamily = FuenteCondensada),
        labelSmall = labelSmall.copy(fontFamily = FuenteCondensada)
    )
}
