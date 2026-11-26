package com.example.myapplication.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Colores de adorno del estilo neón, medidos en los diseños "cyberpunk" del equipo.
 * Los colores de texto, fondos y estados están en [PaletaCyberpunk]; estos son los de los
 * bordes brillantes, marcos de íconos y detalles HUD.
 */
object Neon {
    val cian = Color(0xFF22E5FF)          // bordes principales
    val magenta = Color(0xFFE255F2)       // esquinas y lado derecho de los paneles
    val lima = Color(0xFFADFE57)          // íconos, interruptor, elegido
    val limaTexto = Color(0xFFB8EE86)     // títulos de sección y etiquetas
    val amarillo = Color(0xFFE5FC5A)      // marco del candado
    val rojo = Color(0xFFF0707F)          // cerrar sesión
    val tenue = Color(0xFF4C6B60)         // marco apagado (Apariencia, Notificaciones)
    val apagado = Color(0xFF38505C)       // borde de las miniaturas de tema no elegidas
    val tituloCian = Color(0xFF9DE4F6)    // "Bienvenido, ..."
    val chevron = Color(0xFF5FB2BE)       // flechas ">" de las filas
    val panel = Color(0xFF111C28)         // relleno de paneles
    val insignia = Color(0xFF0E2230)      // fondo del lápiz de la foto
}

/**
 * Reloj compartido por todas las animaciones neón. Se lee solo al dibujar, así cada cuadro
 * vuelve a pintar los bordes sin recomponer la pantalla.
 */
class RelojNeon(
    /** Brillo que "respira": va de 0,55 a 1 y vuelve, en 2,4 s. */
    val pulso: State<Float>,
    /** De 0 a 1 en 6 s, en bucle: la luz que recorre los bordes y los anillos que giran. */
    val ciclo: State<Float>
)

/** null fuera de los temas neón: los adornos se dibujan quietos. */
val LocalRelojNeon = staticCompositionLocalOf<RelojNeon?> { null }

/**
 * Crea el reloj. Si el usuario quitó las animaciones del sistema (Opciones de desarrollador
 * o Accesibilidad), el brillo queda fijo y nada se mueve.
 */
@Composable
fun rememberRelojNeon(): RelojNeon {
    val contexto = LocalContext.current
    val animar = remember {
        Settings.Global.getFloat(contexto.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
    if (!animar) {
        return remember { RelojNeon(mutableFloatStateOf(0.85f), mutableFloatStateOf(0.15f)) }
    }
    val transicion = rememberInfiniteTransition(label = "neon")
    val pulso = transicion.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulso"
    )
    val ciclo = transicion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "ciclo"
    )
    return remember(pulso, ciclo) { RelojNeon(pulso, ciclo) }
}

/** Esquinas cortadas en diagonal de los paneles HUD. */
data class CortesNeon(val arribaIzq: Dp, val arribaDer: Dp, val abajoDer: Dp, val abajoIzq: Dp) {
    companion object {
        /** Tarjetas grandes (perfil, cuenta). */
        val Tarjeta = CortesNeon(14.dp, 16.dp, 18.dp, 14.dp)
        /** Grupos de opciones. */
        val Grupo = CortesNeon(10.dp, 16.dp, 12.dp, 10.dp)
        /** Botones del menú y de "Ver más". */
        val Boton = CortesNeon(10.dp, 10.dp, 10.dp, 10.dp)
        /** Elementos de listas (artículos, préstamos) y tarjetas chicas. */
        val Lista = CortesNeon(8.dp, 12.dp, 8.dp, 8.dp)
        /** Diálogos. */
        val Dialogo = CortesNeon(12.dp, 18.dp, 12.dp, 12.dp)
    }
}

/**
 * Contorno con las esquinas cortadas. [margen] lo mete hacia adentro (para la línea interior):
 * los cortes se achican lo justo para que las diagonales queden paralelas.
 */
fun caminoCortado(ancho: Float, alto: Float, cortes: FloatArray, margen: Float = 0f): Path {
    val ajuste = margen * 0.414f
    val (ai, ad, abd, abi) = cortes.map { (it - ajuste).coerceAtLeast(0f) }
    val l = margen
    val t = margen
    val r = ancho - margen
    val b = alto - margen
    return Path().apply {
        moveTo(l + ai, t)
        lineTo(r - ad, t)
        lineTo(r, t + ad)
        lineTo(r, b - abd)
        lineTo(r - abd, b)
        lineTo(l + abi, b)
        lineTo(l, b - abi)
        lineTo(l, t + ai)
        close()
    }
}

/**
 * Trazo de neón: el mismo camino varias veces, cada vez más ancho y más transparente (halo),
 * y una línea fina casi blanca en el centro (el "tubo" encendido). [intensidad] viene del pulso.
 * Sin [nucleo] no se dibuja la línea blanca (para teñir encima de otro trazo).
 */
fun DrawScope.trazoNeon(
    camino: Path,
    pincel: Brush,
    grosor: Float,
    intensidad: Float = 0.85f,
    nucleo: Boolean = true
) {
    // El halo lleva puntas y uniones redondeadas para que se vea como un resplandor y no
    // como un rectángulo; el tubo del centro conserva las esquinas en punta.
    drawPath(camino, pincel, alpha = 0.05f * intensidad, style = Stroke(grosor * 8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(camino, pincel, alpha = 0.10f * intensidad, style = Stroke(grosor * 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(camino, pincel, alpha = 0.28f * intensidad, style = Stroke(grosor * 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(camino, pincel, alpha = 1f, style = Stroke(grosor))
    if (nucleo) {
        drawPath(camino, SolidColor(Color.White), alpha = 0.30f * intensidad, style = Stroke(grosor * 0.35f))
    }
}

/** Versión de un solo color. */
fun DrawScope.trazoNeon(camino: Path, color: Color, grosor: Float, intensidad: Float = 0.85f, nucleo: Boolean = true) =
    trazoNeon(camino, SolidColor(color), grosor, intensidad, nucleo)

/**
 * Línea recta con brillo (barras laterales, acentos de esquina). Igual que [trazoNeon] pero con
 * drawLine, que no reserva memoria: se dibuja en cada cuadro de la animación.
 */
fun DrawScope.lineaNeon(desde: Offset, hasta: Offset, color: Color, grosor: Float, intensidad: Float = 0.85f) {
    drawLine(color, desde, hasta, strokeWidth = grosor * 8f, cap = StrokeCap.Round, alpha = 0.05f * intensidad)
    drawLine(color, desde, hasta, strokeWidth = grosor * 4.5f, cap = StrokeCap.Round, alpha = 0.10f * intensidad)
    drawLine(color, desde, hasta, strokeWidth = grosor * 2.2f, cap = StrokeCap.Round, alpha = 0.28f * intensidad)
    drawLine(color, desde, hasta, strokeWidth = grosor)
    drawLine(Color.White, desde, hasta, strokeWidth = grosor * 0.35f, alpha = 0.30f * intensidad)
}

/**
 * Luz que recorre el borde: un tramo brillante que avanza según [progreso] (0..1).
 * [medida] y [tramo] se crean una vez (en drawWithCache) para no reservar memoria en cada cuadro.
 */
fun DrawScope.luzRecorriendo(
    medida: PathMeasure,
    tramo: Path,
    progreso: Float,
    color: Color,
    grosor: Float,
    fraccion: Float = 0.12f
) {
    val largo = medida.length
    if (largo <= 0f) return
    val inicio = progreso.coerceIn(0f, 1f) * largo
    val fin = inicio + largo * fraccion
    tramo.reset()
    medida.getSegment(inicio, minOf(fin, largo), tramo, true)
    if (fin > largo) medida.getSegment(0f, fin - largo, tramo, true)
    drawPath(tramo, color, alpha = 0.22f, style = Stroke(grosor * 5f, cap = StrokeCap.Round))
    drawPath(tramo, color, alpha = 0.95f, style = Stroke(grosor * 1.4f, cap = StrokeCap.Round))
    drawPath(tramo, Color.White, alpha = 0.7f, style = Stroke(grosor * 0.5f, cap = StrokeCap.Round))
}
