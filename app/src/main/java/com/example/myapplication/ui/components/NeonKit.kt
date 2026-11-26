package com.example.myapplication.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.Colores
import com.example.myapplication.ui.theme.CortesNeon
import com.example.myapplication.ui.theme.LocalRelojNeon
import com.example.myapplication.ui.theme.Neon
import com.example.myapplication.ui.theme.RelojNeon
import com.example.myapplication.ui.theme.caminoCortado
import com.example.myapplication.ui.theme.lineaNeon
import com.example.myapplication.ui.theme.luzRecorriendo
import com.example.myapplication.ui.theme.trazoNeon

// =============================================================================
// Piezas del estilo neón (tema Cyberpunk). Fuera de ese tema no se usan.
// =============================================================================

/**
 * Panel HUD: esquinas cortadas en diagonal, relleno azul noche, borde neón que pasa de cian a
 * magenta, línea interior tenue, acentos magenta en dos esquinas y marcas tipo HUD.
 *
 * Animación: el brillo "respira" y una luz recorre el borde ([luz]). Todo se dibuja detrás
 * del contenido; el contenido se recorta a la forma del panel.
 *
 * Para versiones más livianas (elementos de listas, diálogos) se apagan [detalles], [luz],
 * [acentos], [lineaInterior] y [animado]; [barraLateral] pinta una barra de color a la
 * izquierda (por ejemplo, el color del estado de un artículo).
 */
@Composable
fun PanelNeon(
    modifier: Modifier = Modifier,
    cortes: CortesNeon = CortesNeon.Tarjeta,
    colorBorde: Color = Neon.cian,
    colorAcento: Color = Neon.magenta,
    detalles: Boolean = true,
    luz: Boolean = true,
    desfase: Float? = null,
    onClick: (() -> Unit)? = null,
    rol: Role? = null,
    relleno: Color = Neon.panel,
    acentos: Boolean = true,
    lineaInterior: Boolean = true,
    animado: Boolean = true,
    barraLateral: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    // Sin animación no se lee el reloj: el panel se dibuja una vez y no se repinta en cada cuadro
    val reloj = if (animado) LocalRelojNeon.current else null
    val forma = CutCornerShape(
        topStart = cortes.arribaIzq,
        topEnd = cortes.arribaDer,
        bottomEnd = cortes.abajoDer,
        bottomStart = cortes.abajoIzq
    )
    Box(
        modifier = modifier
            // Capa propia: cada cuadro de la animación repinta solo este panel, no la pantalla.
            .graphicsLayer()
            .drawWithCache {
                val c = floatArrayOf(
                    cortes.arribaIzq.toPx(), cortes.arribaDer.toPx(),
                    cortes.abajoDer.toPx(), cortes.abajoIzq.toPx()
                )
                val w = size.width
                val h = size.height
                val exterior = caminoCortado(w, h, c)
                val interior = caminoCortado(w, h, c, margen = 5.dp.toPx())
                // El borde es cian y se vuelve magenta cerca de dos esquinas opuestas
                // (arriba a la derecha y abajo a la izquierda), como en los diseños.
                val radioAcento = maxOf(w, h) * 0.5f
                val tinteArriba = Brush.radialGradient(
                    0f to colorAcento,
                    0.35f to colorAcento.copy(alpha = 0.8f),
                    1f to colorAcento.copy(alpha = 0f),
                    center = Offset(w, 0f),
                    radius = radioAcento
                )
                val tinteAbajo = Brush.radialGradient(
                    0f to colorAcento,
                    0.35f to colorAcento.copy(alpha = 0.8f),
                    1f to colorAcento.copy(alpha = 0f),
                    center = Offset(0f, h),
                    radius = radioAcento
                )
                // Relleno propio (diálogos, tarjetas de alerta): tal cual, para que no se vea lo de atrás
                val fondo = if (relleno == Neon.panel) {
                    Brush.verticalGradient(listOf(Neon.panel.copy(alpha = 0.94f), Color(0xF00C1622)))
                } else {
                    SolidColor(relleno)
                }
                val medida = PathMeasure().apply { setPath(exterior, true) }
                val tramo = Path()
                val grosor = 1.6.dp.toPx()
                // Cada panel arranca la luz en un punto distinto, para que no vayan sincronizadas.
                val inicioLuz = desfase ?: (((w * 7f + h * 3f) % 1000f) / 1000f)
                onDrawBehind {
                    val intensidad = reloj?.pulso?.value ?: 0.85f
                    drawPath(exterior, fondo)
                    if (lineaInterior) drawPath(interior, colorBorde, alpha = 0.18f, style = Stroke(1.dp.toPx()))
                    trazoNeon(exterior, colorBorde, grosor, intensidad)
                    if (colorAcento != colorBorde) {
                        trazoNeon(exterior, tinteArriba, grosor, intensidad, nucleo = false)
                        trazoNeon(exterior, tinteAbajo, grosor, intensidad, nucleo = false)
                        if (acentos) acentosDeEsquina(c, colorAcento, intensidad)
                    }
                    if (barraLateral != null) {
                        lineaNeon(Offset(1.5.dp.toPx(), h * 0.22f), Offset(1.5.dp.toPx(), h * 0.78f), barraLateral, 3.dp.toPx(), intensidad)
                    }
                    if (detalles) detallesHud(c, colorBorde, colorAcento, intensidad)
                    if (luz && reloj != null) {
                        luzRecorriendo(medida, tramo, (reloj.ciclo.value + inicioLuz) % 1f, colorBorde, grosor)
                    }
                }
            }
            .clip(forma)
            .then(if (onClick != null) Modifier.clickable(role = rol, onClick = onClick) else Modifier),
        content = content
    )
}

/** Barras magenta gruesas sobre los cortes de arriba a la derecha y abajo a la izquierda. */
private fun DrawScope.acentosDeEsquina(c: FloatArray, color: Color, intensidad: Float) {
    val w = size.width
    val h = size.height
    val g = 3.dp.toPx()
    val o = 2.dp.toPx()          // hacia afuera, pegada al borde
    val recorte = 0.15f           // la barra no llega a los extremos de la diagonal
    if (c[1] > 0f) {
        val a = Offset(w - c[1], 0f)
        val b = Offset(w, c[1])
        lineaNeon(
            Offset(a.x + (b.x - a.x) * recorte + o, a.y + (b.y - a.y) * recorte - o),
            Offset(b.x - (b.x - a.x) * recorte + o, b.y - (b.y - a.y) * recorte - o),
            color, g, intensidad
        )
    }
    if (c[3] > 0f) {
        val a = Offset(0f, h - c[3])
        val b = Offset(c[3], h)
        lineaNeon(
            Offset(a.x + (b.x - a.x) * recorte - o, a.y + (b.y - a.y) * recorte + o),
            Offset(b.x - (b.x - a.x) * recorte - o, b.y - (b.y - a.y) * recorte + o),
            color, g, intensidad
        )
    }
}

/** Marcas HUD: rayitas arriba a la derecha, barras laterales y guiones abajo a la izquierda. */
private fun DrawScope.detallesHud(c: FloatArray, principal: Color, acento: Color, intensidad: Float) {
    val w = size.width
    val h = size.height
    val fino = 1.dp.toPx()
    // Barras laterales más brillantes, sobre el borde
    if (h > 60.dp.toPx()) {
        lineaNeon(Offset(1.5.dp.toPx(), h * 0.30f), Offset(1.5.dp.toPx(), h * 0.62f), principal, 2.6.dp.toPx(), intensidad)
        lineaNeon(Offset(w - 1.5.dp.toPx(), h * 0.40f), Offset(w - 1.5.dp.toPx(), h * 0.70f), principal, 2.6.dp.toPx(), intensidad)
    }
    if (w < 170.dp.toPx()) return
    // Rayitas arriba a la derecha
    val y = 8.dp.toPx()
    val x0 = w - c[1] - 86.dp.toPx()
    for (k in 0 until 4) {
        val x = x0 + k * 5.dp.toPx()
        drawLine(acento, Offset(x, y), Offset(x, y + 5.dp.toPx()), strokeWidth = fino, alpha = 0.65f * intensidad)
    }
    drawLine(principal, Offset(x0 + 26.dp.toPx(), y + 2.5.dp.toPx()), Offset(x0 + 52.dp.toPx(), y + 2.5.dp.toPx()), strokeWidth = fino * 1.5f, alpha = 0.6f * intensidad)
    drawLine(acento, Offset(x0 + 56.dp.toPx(), y + 2.5.dp.toPx()), Offset(x0 + 66.dp.toPx(), y + 2.5.dp.toPx()), strokeWidth = fino * 1.5f, alpha = 0.6f * intensidad)
    // Guiones abajo a la izquierda
    if (h > 60.dp.toPx()) {
        val yb = h - 8.dp.toPx()
        val xb = c[3] + 12.dp.toPx()
        for (k in 0 until 3) {
            val x = xb + k * 14.dp.toPx()
            drawLine(principal, Offset(x, yb), Offset(x + 10.dp.toPx(), yb), strokeWidth = fino * 1.5f, alpha = 0.5f * intensidad)
        }
        for (k in 0 until 3) {
            val x = xb + 48.dp.toPx() + k * 4.dp.toPx()
            drawLine(acento, Offset(x, yb - 3.dp.toPx()), Offset(x, yb + 1.dp.toPx()), strokeWidth = fino, alpha = 0.6f * intensidad)
        }
    }
}

/** Color del marco y del ícono de cada fila (como en los diseños: lima, cian, amarillo, rojo...). */
enum class TonoNeon(val marco: Color, val icono: Color) {
    LIMA(Neon.lima, Neon.lima),
    CIAN(Neon.cian, Neon.lima),
    AMARILLO(Neon.amarillo, Neon.amarillo),
    ROJO(Neon.rojo, Neon.rojo),
    TENUE(Neon.tenue, Neon.lima),
    TENUE_AMARILLO(Neon.tenue, Neon.amarillo)
}

/**
 * Cuadro de ícono HUD: relleno teñido, borde fino y esquinas en "L" brillantes que sobresalen
 * un poco; el ícono lleva un resplandor suave detrás.
 */
@Composable
fun MarcoIconoNeon(
    icono: Painter,
    tono: TonoNeon,
    modifier: Modifier = Modifier,
    tamano: Dp = 44.dp,
    tamanoIcono: Dp = 22.dp
) {
    val reloj = LocalRelojNeon.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(tamano)
            .marcoNeon(tono, reloj)
    ) {
        Icon(painter = icono, contentDescription = null, tint = tono.icono, modifier = Modifier.size(tamanoIcono))
    }
}

/**
 * Dibujo del marco de ícono HUD, como modificador para cualquier caja: relleno teñido, borde
 * fino y, si mide 40 dp o más, esquinas en "L" brillantes que sobresalen un poco. Las cajas
 * chicas (botones de acción) llevan solo el borde, con las esquinas cortadas.
 * Con [reloj] el brillo respira; sin él queda quieto (lo normal en listas).
 */
fun Modifier.marcoNeon(tono: TonoNeon, reloj: RelojNeon? = null): Modifier = drawWithCache {
    // Lo que no cambia entre cuadros se crea una sola vez
    val w = size.width
    val h = size.height
    val conEsquinas = size.minDimension >= 40.dp.toPx()
    val l = w * 0.30f
    val o = 2.dp.toPx()
    val esquinas = Path().apply {
        moveTo(-o, l); lineTo(-o, -o); lineTo(l, -o)
        moveTo(w - l, -o); lineTo(w + o, -o); lineTo(w + o, l)
        moveTo(w + o, h - l); lineTo(w + o, h + o); lineTo(w - l, h + o)
        moveTo(l, h + o); lineTo(-o, h + o); lineTo(-o, h - l)
    }
    val corte = 6.dp.toPx()
    val chico = caminoCortado(w, h, floatArrayOf(corte, corte, corte, corte))
    val brillo = Brush.radialGradient(listOf(tono.icono.copy(alpha = 0.28f), Color.Transparent))
    val borde = Stroke(1.dp.toPx())
    onDrawBehind {
        val intensidad = reloj?.pulso?.value ?: 0.85f
        if (conEsquinas) {
            drawRect(tono.marco.copy(alpha = 0.10f))
            drawRect(tono.marco, alpha = 0.40f, style = borde)
            drawCircle(brillo, radius = w * 0.45f)
            trazoNeon(esquinas, tono.marco, 2.dp.toPx(), intensidad)
        } else {
            drawPath(chico, tono.marco.copy(alpha = 0.14f))
            trazoNeon(chico, tono.marco.copy(alpha = 0.75f), 1.dp.toPx(), intensidad * 0.7f, nucleo = false)
        }
    }
}

/** Brillo para texto neón (títulos, etiquetas). [radio] es el alcance del resplandor. */
@Composable
fun estiloBrillo(color: Color, radio: Dp = 5.dp): TextStyle {
    val desenfoque = with(LocalDensity.current) { radio.toPx() }
    return TextStyle(shadow = Shadow(color = color.copy(alpha = 0.65f), offset = Offset.Zero, blurRadius = desenfoque))
}

/** Título de sección entre corchetes: "[ OPCIONES DE USUARIO ]" en lima. */
@Composable
fun TituloSeccionNeon(texto: String, modifier: Modifier = Modifier) {
    val reloj = LocalRelojNeon.current
    Text(
        text = texto,
        style = MaterialTheme.typography.titleSmall.merge(estiloBrillo(Neon.lima)),
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.6.sp,
        color = Neon.limaTexto,
        modifier = modifier
            .padding(start = 4.dp, top = 16.dp, bottom = 6.dp)
            .graphicsLayer()
            .drawWithCache {
                val pie = 5.dp.toPx()
                val sobra = 3.dp.toPx()
                val w = size.width
                val h = size.height
                val izquierdo = Path().apply {
                    moveTo(pie, -sobra); lineTo(0f, -sobra); lineTo(0f, h + sobra); lineTo(pie, h + sobra)
                }
                val derecho = Path().apply {
                    moveTo(w - pie, -sobra); lineTo(w, -sobra); lineTo(w, h + sobra); lineTo(w - pie, h + sobra)
                }
                onDrawBehind {
                    val intensidad = (reloj?.pulso?.value ?: 0.85f) * 0.8f
                    trazoNeon(izquierdo, Neon.limaTexto, 1.4.dp.toPx(), intensidad)
                    trazoNeon(derecho, Neon.limaTexto, 1.4.dp.toPx(), intensidad)
                }
            }
            .padding(horizontal = 11.dp)
    )
}

/** Etiqueta neón ("MONITOR EN TURNO", "MONITOR"): borde lima con brillo y corchetes a los lados. */
@Composable
fun ChipNeon(texto: String, modifier: Modifier = Modifier, color: Color = Neon.lima, redondeo: Dp = 5.dp) {
    val reloj = LocalRelojNeon.current
    Text(
        text = texto,
        color = Neon.limaTexto,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        letterSpacing = 0.5.sp,
        // labelLarge trae la letra del tema (condensada en Cyberpunk); se le suma el brillo
        style = MaterialTheme.typography.labelLarge.merge(estiloBrillo(color, 4.dp)),
        modifier = modifier
            .padding(horizontal = 9.dp)
            .drawWithCache {
                val radio = CornerRadius(redondeo.toPx())
                val borde = Path().apply { addRoundRect(RoundRect(Rect(Offset.Zero, size), radio)) }
                val sep = 6.dp.toPx()
                val pie = 4.dp.toPx()
                val sobra = 3.dp.toPx()
                val w = size.width
                val h = size.height
                val izquierdo = Path().apply {
                    moveTo(-sep + pie, -sobra); lineTo(-sep, -sobra + pie)
                    lineTo(-sep, h + sobra - pie); lineTo(-sep + pie, h + sobra)
                }
                val derecho = Path().apply {
                    moveTo(w + sep - pie, -sobra); lineTo(w + sep, -sobra + pie)
                    lineTo(w + sep, h + sobra - pie); lineTo(w + sep - pie, h + sobra)
                }
                onDrawBehind {
                    val intensidad = reloj?.pulso?.value ?: 0.85f
                    drawPath(borde, color.copy(alpha = 0.10f))
                    trazoNeon(borde, color, 1.4.dp.toPx(), intensidad)
                    trazoNeon(izquierdo, color, 1.2.dp.toPx(), intensidad * 0.8f)
                    trazoNeon(derecho, color, 1.2.dp.toPx(), intensidad * 0.8f)
                }
            }
            .padding(horizontal = 12.dp, vertical = 4.dp)
    )
}

/** Marco HUD del pie de Ajustes: esquinas de abajo con triángulos cian (izq.) y magenta (der.). */
@Composable
fun MarcoPieNeon(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val reloj = LocalRelojNeon.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer()
            .drawWithCache {
                val w = size.width
                val h = size.height
                val g = 1.3.dp.toPx()
                val t = 10.dp.toPx()
                val trianguloIzq = Path().apply { moveTo(0f, h - t); lineTo(t, h); lineTo(0f, h); close() }
                val trianguloDer = Path().apply { moveTo(w, h - t); lineTo(w - t, h); lineTo(w, h); close() }
                onDrawBehind {
                    val intensidad = reloj?.pulso?.value ?: 0.85f
                    lineaNeon(Offset(0f, h * 0.2f), Offset(0f, h - t), Neon.cian, g, intensidad)
                    lineaNeon(Offset(t, h), Offset(w * 0.24f, h), Neon.cian, g, intensidad)
                    drawPath(trianguloIzq, Neon.cian, alpha = 0.9f)
                    lineaNeon(Offset(w, h * 0.2f), Offset(w, h - t), Neon.magenta, g, intensidad)
                    lineaNeon(Offset(w - t, h), Offset(w * 0.76f, h), Neon.magenta, g, intensidad)
                    drawPath(trianguloDer, Neon.magenta, alpha = 0.9f)
                    for (k in 0 until 5) {
                        val x = w * 0.30f + k * 5.dp.toPx()
                        drawLine(Neon.magenta, Offset(x, h - 3.dp.toPx()), Offset(x, h), strokeWidth = 1.dp.toPx(), alpha = 0.6f)
                        val x2 = w * 0.62f + k * 5.dp.toPx()
                        drawLine(Neon.magenta, Offset(x2, h - 3.dp.toPx()), Offset(x2, h), strokeWidth = 1.dp.toPx(), alpha = 0.6f)
                    }
                }
            }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        content = content
    )
}

/** Brillo detrás de un control encendido (el interruptor de Notificaciones). */
fun Modifier.brilloNeon(reloj: RelojNeon?, color: Color, encendido: Boolean): Modifier =
    if (!encendido) this else drawWithCache {
        val ancho = 52.dp.toPx()
        val alto = 32.dp.toPx()
        val izquierda = (size.width - ancho) / 2f
        val arriba = (size.height - alto) / 2f
        val pista = Path().apply {
            addRoundRect(RoundRect(Rect(Offset(izquierda, arriba), Size(ancho, alto)), CornerRadius(alto / 2f)))
        }
        val halo = Stroke(14.dp.toPx())
        val cerca = Stroke(6.dp.toPx())
        onDrawBehind {
            val intensidad = reloj?.pulso?.value ?: 0.85f
            drawPath(pista, color, alpha = 0.10f * intensidad, style = halo)
            drawPath(pista, color, alpha = 0.22f * intensidad, style = cerca)
        }
    }

/**
 * Fondo de toda la app. Con los temas neón dibuja el fondo HUD: azul noche con resplandores,
 * cuadrícula, líneas de barrido, marcos tenues y una banda de luz que baja lentamente.
 * Con los demás temas es el color de fondo de siempre (cada pantalla pinta el suyo encima).
 */
@Composable
fun FondoApp(content: @Composable () -> Unit) {
    val neon = Colores.esNeon
    val reloj = LocalRelojNeon.current
    Box(modifier = Modifier.fillMaxSize()) {
        if (neon) {
            // Lo quieto se dibuja una vez; la banda que baja va en su propia capa para que
            // cada cuadro de la animación repinte solo la banda.
            Spacer(modifier = Modifier.fillMaxSize().fondoNeon())
            if (reloj != null) {
                Spacer(modifier = Modifier.fillMaxSize().graphicsLayer().bandaNeon(reloj))
            }
        }
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = if (neon) Color.Transparent else MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            content = content
        )
    }
}

/** Banda de luz cian que baja lentamente por la pantalla (un "escaneo"). */
private fun Modifier.bandaNeon(reloj: RelojNeon): Modifier = drawBehind {
    val banda = 160.dp.toPx()
    val y = reloj.ciclo.value * (size.height + banda) - banda
    drawRect(
        Brush.verticalGradient(
            listOf(Color.Transparent, Neon.cian.copy(alpha = 0.055f), Color.Transparent),
            startY = y,
            endY = y + banda
        ),
        topLeft = Offset(0f, y),
        size = Size(size.width, banda)
    )
}

private fun Modifier.fondoNeon(): Modifier = drawWithCache {
    val w = size.width
    val h = size.height
    val base = Brush.verticalGradient(listOf(Color(0xFF0B1A26), Color(0xFF08131E), Color(0xFF0A1824)))
    val brilloCian = Brush.radialGradient(
        listOf(Neon.cian.copy(alpha = 0.10f), Color.Transparent),
        center = Offset(w * 0.12f, h * 0.10f),
        radius = w * 0.95f
    )
    val brilloMagenta = Brush.radialGradient(
        listOf(Neon.magenta.copy(alpha = 0.07f), Color.Transparent),
        center = Offset(w * 0.92f, h * 0.88f),
        radius = w * 0.9f
    )
    val paso = 40.dp.toPx()
    val cuadricula = Path().apply {
        var x = 0f
        while (x <= w) { moveTo(x, 0f); lineTo(x, h); x += paso }
        var y = 0f
        while (y <= h) { moveTo(0f, y); lineTo(w, y); y += paso }
    }
    val barrido = Path().apply {
        val p = 3.dp.toPx()
        var y = 0f
        while (y <= h) { moveTo(0f, y); lineTo(w, y); y += p }
    }
    val nodo = 2.dp.toPx()
    val hud = Path().apply {
        // Marco arriba a la derecha
        moveTo(w * 0.60f, h * 0.115f); lineTo(w * 0.97f, h * 0.115f); lineTo(w * 0.97f, h * 0.17f)
        addOval(Rect(Offset(w * 0.60f, h * 0.115f), nodo))
        // Regla a la izquierda
        moveTo(w * 0.025f, h * 0.30f); lineTo(w * 0.025f, h * 0.46f)
        for (k in 0 until 6) {
            val y = h * 0.31f + k * 9.dp.toPx()
            moveTo(w * 0.025f, y); lineTo(w * 0.025f + 6.dp.toPx(), y)
        }
        // Línea a la derecha
        moveTo(w * 0.975f, h * 0.52f); lineTo(w * 0.975f, h * 0.70f)
        addOval(Rect(Offset(w * 0.975f, h * 0.70f), nodo))
        // Marco tenue detrás del contenido
        addRect(Rect(w * 0.05f, h * 0.60f, w * 0.95f, h * 0.80f))
        // Esquina abajo a la izquierda
        moveTo(w * 0.04f, h * 0.90f); lineTo(w * 0.04f, h * 0.955f); lineTo(w * 0.32f, h * 0.955f)
    }
    onDrawBehind {
        drawRect(base)
        drawRect(brilloCian)
        drawRect(brilloMagenta)
        drawPath(barrido, Neon.cian, alpha = 0.022f, style = Stroke(1f))
        drawPath(cuadricula, Neon.cian, alpha = 0.045f, style = Stroke(1f))
        drawPath(hud, Neon.cian, alpha = 0.14f, style = Stroke(1.dp.toPx()))
    }
}
