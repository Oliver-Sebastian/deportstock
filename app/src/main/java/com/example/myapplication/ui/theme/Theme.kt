package com.example.myapplication.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.myapplication.data.TemaApp

private val DarkColorScheme = darkColorScheme(
    primary = Pink80,      // Usamos el verde más claro (8BC34A) para el modo oscuro
    secondary = PurpleGrey80,
    tertiary = Purple80,   // Usamos el verde oscuro como terciario
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF1C1B1F),
    onPrimary = Color(0xFF00390A), // Texto oscuro sobre fondo verde claro
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5),
    onSurfaceVariant = Color(0xFFCAC4D0),
    // Sin estos, Material usa sus grises morados por defecto en diálogos, menús y el
    // calendario (se veía en "Registrar Devolución"). Se toman de la misma paleta verde.
    surfaceContainerLowest = PaletaOscura.fondo,
    surfaceContainerLow = PaletaOscura.superficie,
    surfaceContainer = PaletaOscura.superficieAlta,
    surfaceContainerHigh = PaletaOscura.dialogo,
    surfaceContainerHighest = PaletaOscura.deshabilitado,
    secondaryContainer = PaletaOscura.exitoFondo,
    onSecondaryContainer = PaletaOscura.acentoTexto
)

/**
 * Esquema de Material 3 para los temas de fondo claro. Sale de la misma paleta que usan
 * las pantallas, así los componentes sin color propio (diálogos, campos, menús) combinan.
 */
private fun esquemaClaro(p: PaletaApp) = lightColorScheme(
    primary = p.primario,
    onPrimary = p.sobrePrimario,
    primaryContainer = p.exitoFondo,
    onPrimaryContainer = p.acentoTexto,
    secondary = p.secundario,
    // Blanco sobre los acentos oscuros; negro sobre los claros, como el dorado de Varsity
    onSecondary = Colores.contrasteSobre(p.secundario),
    tertiary = p.terciario,
    onTertiary = Colores.contrasteSobre(p.terciario),
    background = p.fondo,
    onBackground = p.texto,
    surface = p.superficie,
    onSurface = p.texto,
    surfaceVariant = p.superficieAlta,
    onSurfaceVariant = p.textoSecundario,
    surfaceContainerLowest = p.superficie,
    surfaceContainerLow = p.superficie,
    surfaceContainer = p.superficie,
    surfaceContainerHigh = p.dialogo,
    surfaceContainerHighest = p.superficieAlta,
    outline = p.bordeCampo,
    outlineVariant = p.borde,
    error = p.error,
    onError = p.sobreError,
    errorContainer = p.errorFondo,
    onErrorContainer = p.error
)

private val ClaroColorScheme = esquemaClaro(PaletaClara)
private val InstitucionalColorScheme = esquemaClaro(PaletaInstitucional)
private val RetroColorScheme = esquemaClaro(PaletaRetro)
private val VarsityColorScheme = esquemaClaro(PaletaVarsity)
private val SolarColorScheme = esquemaClaro(PaletaSolar)

/** Esquema de Material para los temas oscuros nuevos (Cyberpunk), a partir de su paleta. */
private fun esquemaOscuro(p: PaletaApp) = darkColorScheme(
    primary = p.primario,
    onPrimary = p.sobrePrimario,
    primaryContainer = p.exitoFondo,
    onPrimaryContainer = p.primario,
    secondary = p.secundario,
    onSecondary = p.fondo,
    tertiary = p.terciario,
    onTertiary = p.fondo,
    background = p.fondo,
    onBackground = p.texto,
    surface = p.superficie,
    onSurface = p.texto,
    surfaceVariant = p.superficieAlta,
    onSurfaceVariant = p.textoSecundario,
    surfaceContainerLowest = p.fondo,
    surfaceContainerLow = p.superficie,
    surfaceContainer = p.superficieAlta,
    surfaceContainerHigh = p.dialogo,
    surfaceContainerHighest = p.deshabilitado,
    secondaryContainer = p.exitoFondo,
    onSecondaryContainer = p.primario,
    outline = p.bordeCampo,
    outlineVariant = p.borde,
    error = p.error,
    onError = p.fondo,
    errorContainer = p.errorFondo,
    onErrorContainer = p.error
)

private val CyberpunkColorScheme = esquemaOscuro(PaletaCyberpunk)

/**
 * Tema de DEPORTSTOCK. Aplica el esquema de Material del [tema] elegido en Ajustes y
 * ajusta los íconos de las barras del sistema (hora, batería) para que se vean sobre él.
 */
@Composable
fun DeportStockTheme(
    tema: TemaApp = Colores.tema,
    content: @Composable () -> Unit
) {
    val paleta = Colores.paletaDe(tema)
    val colorScheme = when (tema) {
        TemaApp.OSCURO -> DarkColorScheme
        TemaApp.CLARO -> ClaroColorScheme
        TemaApp.INSTITUCIONAL -> InstitucionalColorScheme
        TemaApp.CYBERPUNK -> CyberpunkColorScheme
        TemaApp.RETRO -> RetroColorScheme
        TemaApp.VARSITY -> VarsityColorScheme
        TemaApp.SOLAR -> SolarColorScheme
    }
    // Reloj de las animaciones neón: solo existe con esos temas (con los demás no se anima nada).
    val reloj = if (tema.esNeon) rememberRelojNeon() else null

    val vista = LocalView.current
    if (!vista.isInEditMode) {
        SideEffect {
            val ventana = (vista.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(ventana, vista).apply {
                isAppearanceLightStatusBars = !paleta.barraOscura
                isAppearanceLightNavigationBars = !paleta.esOscura
            }
        }
    }

    CompositionLocalProvider(LocalRelojNeon provides reloj) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = if (tema.esNeon) TipografiaCyberpunk else Typography,
            // En neón, menús, calendario, avisos, chips y campos salen con esquinas cortadas
            shapes = if (tema.esNeon) FormasNeon else MaterialTheme.shapes,
            content = content
        )
    }
}

/** Se conserva para las vistas previas de Android Studio. */
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    DeportStockTheme(if (darkTheme) TemaApp.OSCURO else TemaApp.CLARO, content)
}