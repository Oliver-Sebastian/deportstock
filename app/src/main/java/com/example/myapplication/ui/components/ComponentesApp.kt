package com.example.myapplication.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.myapplication.ui.theme.Colores
import com.example.myapplication.ui.theme.CortesNeon
import com.example.myapplication.ui.theme.LocalRelojNeon
import com.example.myapplication.ui.theme.Neon
import com.example.myapplication.ui.theme.lineaNeon
import com.example.myapplication.ui.theme.trazoNeon

// =============================================================================
// Piezas que cambian de aspecto según el tema. Con los temas normales dibujan exactamente lo
// mismo que la pieza de Material que reemplazan (Card, ElevatedCard, AlertDialog...); con los
// temas neón (Cyberpunk) se dibujan con el estilo HUD.
// =============================================================================

/** Cómo se ve una tarjeta con los temas neón. */
enum class EstiloNeon {
    /** Elementos de una lista: borde fino con brillo, sin adornos ni animación (son muchos). */
    LISTA,
    /** Tarjetas de una sección: borde cian que se vuelve magenta en dos esquinas y brillo que respira. */
    TARJETA,
    /** La tarjeta principal de la pantalla: además, marcas HUD y una luz que recorre el borde. */
    DESTACADA
}

/**
 * Reemplazo de [Card]: los mismos parámetros, y con los temas neón un panel HUD según
 * [estiloNeon]. [colorNeon] es el color del borde neón y [acentoNeon] una barra de color a la
 * izquierda (por ejemplo, el estado del artículo). El relleno sale de `colors.containerColor`.
 * Con los temas de bordes marcados (Retro, Varsity, Solar) la tarjeta es plana y lleva el borde del tema.
 */
@Composable
fun TarjetaApp(
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    estiloNeon: EstiloNeon = EstiloNeon.LISTA,
    colorNeon: Color = Neon.cian,
    acentoNeon: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (Colores.esNeon) {
        PanelSegunEstilo(
            modifier, estiloNeon, colorNeonSegunBorde(colorNeon, border), acentoNeon, colors.containerColor, onClick, content
        )
        return
    }
    val conBordeDelTema = Colores.grosorBorde > 0.dp
    val borde = if (conBordeDelTema) bordeDelTema(border) else border
    val sombra = if (conBordeDelTema) CardDefaults.cardElevation() else elevation
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = shape, colors = colors, elevation = sombra, border = borde, content = content)
    } else {
        Card(modifier = modifier, shape = shape, colors = colors, elevation = sombra, border = borde, content = content)
    }
}

/**
 * Borde de una tarjeta con los temas de bordes marcados: el del tema si no tenía, o el suyo
 * (por ejemplo, el rojo de un préstamo vencido) con al menos el grosor del tema.
 */
private fun bordeDelTema(borde: BorderStroke?): BorderStroke =
    if (borde == null) BorderStroke(Colores.grosorBorde, Colores.borde)
    else BorderStroke(Colores.anchoBorde(borde.width), borde.brush)

/** Reemplazo de [ElevatedCard], igual que [TarjetaApp]. */
@Composable
fun TarjetaElevadaApp(
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.elevatedShape,
    colors: CardColors = CardDefaults.elevatedCardColors(),
    elevation: CardElevation = CardDefaults.elevatedCardElevation(),
    onClick: (() -> Unit)? = null,
    estiloNeon: EstiloNeon = EstiloNeon.LISTA,
    colorNeon: Color = Neon.cian,
    acentoNeon: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    when {
        Colores.esNeon -> PanelSegunEstilo(modifier, estiloNeon, colorNeon, acentoNeon, colors.containerColor, onClick, content)
        // Retro, Varsity y Solar: tarjeta plana con el borde del tema en lugar de la sombra
        Colores.grosorBorde > 0.dp -> TarjetaApp(
            modifier = modifier, shape = shape, colors = colors, onClick = onClick, content = content
        )
        onClick != null -> ElevatedCard(
            onClick = onClick, modifier = modifier, shape = shape, colors = colors,
            elevation = elevation, content = content
        )
        else -> ElevatedCard(modifier = modifier, shape = shape, colors = colors, elevation = elevation, content = content)
    }
}

/**
 * Si la tarjeta ya tenía un borde de color (el rojo de un préstamo vencido, el color de una
 * novedad), el borde neón toma ese color; si el borde era el gris de siempre, es cian.
 */
private fun colorNeonSegunBorde(colorNeon: Color, borde: BorderStroke?): Color {
    if (colorNeon != Neon.cian) return colorNeon
    val colorDelBorde = (borde?.brush as? SolidColor)?.value ?: return Neon.cian
    return if (colorDelBorde == Colores.borde || colorDelBorde.alpha == 0f) Neon.cian else colorDelBorde.copy(alpha = 1f)
}

@Composable
private fun PanelSegunEstilo(
    modifier: Modifier,
    estilo: EstiloNeon,
    color: Color,
    acento: Color?,
    relleno: Color,
    onClick: (() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit
) {
    val lista = estilo == EstiloNeon.LISTA
    PanelNeon(
        modifier = modifier,
        cortes = when (estilo) {
            EstiloNeon.LISTA -> CortesNeon.Lista
            EstiloNeon.TARJETA -> CortesNeon.Grupo
            EstiloNeon.DESTACADA -> CortesNeon.Tarjeta
        },
        colorBorde = color,
        // En las listas el borde es de un solo color: sin magenta ni acentos de esquina
        colorAcento = if (lista) color else Neon.magenta,
        detalles = estilo == EstiloNeon.DESTACADA,
        luz = estilo == EstiloNeon.DESTACADA,
        onClick = onClick,
        relleno = if (relleno.alpha < 0.5f) Neon.panel else relleno,
        acentos = !lista,
        lineaInterior = !lista,
        animado = !lista,
        barraLateral = acento
    ) {
        Column(content = content)
    }
}

/**
 * Reemplazo de [AlertDialog] con los mismos parámetros. Con los temas neón es un panel HUD
 * liviano (sin marcas ni luz que recorre): el ícono va en un marco del color [tonoNeon] (null
 * para dejarlo sin marco, como el logo de "Acerca de") y el título lleva una línea neón debajo.
 *
 * Con poca altura (teléfono girado) el ícono no se muestra y los márgenes se achican, para que
 * el contenido (que debe ser desplazable) y los botones quepan en la pantalla.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DialogoApp(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = AlertDialogDefaults.containerColor,
    tonoNeon: TonoNeon? = TonoNeon.CIAN,
    properties: DialogProperties = DialogProperties()
) {
    val compacta = alturaCompacta()
    if (!Colores.esNeon) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = confirmButton,
            modifier = modifier,
            dismissButton = dismissButton,
            icon = if (compacta) null else icon,
            title = title,
            text = text,
            shape = shape,
            containerColor = containerColor,
            properties = properties
        )
        return
    }
    val reloj = LocalRelojNeon.current
    BasicAlertDialog(onDismissRequest = onDismissRequest, modifier = modifier, properties = properties) {
        // El margen deja lugar al brillo del borde dentro de la ventana del diálogo
        PanelNeon(
            modifier = Modifier.padding(10.dp),
            cortes = CortesNeon.Dialogo,
            detalles = false,
            luz = false,
            acentos = false,
            relleno = containerColor
        ) {
            Column(modifier = Modifier.padding(if (compacta) 16.dp else 24.dp)) {
                val mostrarIcono = icon != null && !compacta
                val alineacion = if (mostrarIcono) Alignment.CenterHorizontally else Alignment.Start
                if (icon != null && mostrarIcono) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .then(if (tonoNeon != null) Modifier.marcoNeon(tonoNeon, reloj).padding(10.dp) else Modifier)
                    ) {
                        icon()
                    }
                    Spacer(Modifier.height(16.dp))
                }
                if (title != null) {
                    Box(modifier = Modifier.align(alineacion)) {
                        ProvideTextStyle(MaterialTheme.typography.headlineSmall) {
                            CompositionLocalProvider(LocalContentColor provides Colores.texto) { title() }
                        }
                    }
                    LineaTituloNeon(
                        modifier = Modifier
                            .align(alineacion)
                            .padding(top = if (compacta) 6.dp else 10.dp)
                    )
                    Spacer(Modifier.height(if (compacta) 10.dp else 16.dp))
                }
                if (text != null) {
                    // Ocupa lo que quede: si el contenido es largo, se desplaza y los botones siguen a la vista
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .align(Alignment.Start)
                    ) {
                        ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                            CompositionLocalProvider(LocalContentColor provides Colores.textoSecundario) { text() }
                        }
                    }
                    Spacer(Modifier.height(if (compacta) 14.dp else 24.dp))
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    dismissButton?.invoke()
                    confirmButton()
                }
            }
        }
    }
}

/** Línea corta cian a magenta con brillo, debajo del título de los diálogos neón. */
@Composable
private fun LineaTituloNeon(modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier
            .size(width = 56.dp, height = 3.dp)
            .drawWithCache {
                val pincel = Brush.horizontalGradient(listOf(Neon.cian, Neon.magenta))
                val linea = Path().apply {
                    moveTo(0f, size.height / 2f)
                    lineTo(size.width, size.height / 2f)
                }
                onDrawBehind { trazoNeon(linea, pincel, 2.dp.toPx()) }
            }
    )
}

/**
 * Fondo de un ícono: el círculo de color de siempre (con [borde] si lo tenía) o, con los temas
 * neón, un marco HUD del [tono]: con esquinas en "L" si mide 40 dp o más, y solo el borde con
 * esquinas cortadas en los botones chicos.
 *
 * Se dibuja del tamaño que se le dio a la caja (por ejemplo, 36 dp). Sin [delTamanoDado], en un
 * IconButton el fondo salía de 48 dp (el área mínima de toque de Material) y se montaba sobre
 * los botones vecinos.
 */
fun Modifier.fondoIconoApp(color: Color, borde: BorderStroke? = null, tono: TonoNeon = TonoNeon.CIAN): Modifier =
    (
        if (Colores.esNeon) {
            marcoNeon(tono)
        } else {
            background(color, CircleShape).then(if (borde != null) Modifier.border(borde, CircleShape) else Modifier)
        }
    ).delTamanoDado()

/**
 * Hace que lo que se dibujó antes en la cadena de modificadores mida lo que permite el padre,
 * aunque el contenido pida más (los IconButton piden 48 dp de área de toque). El contenido queda
 * centrado y su área de toque no cambia.
 */
private fun Modifier.delTamanoDado(): Modifier = layout { medible, restricciones ->
    val ubicable = medible.measure(restricciones)
    layout(ubicable.width, ubicable.height) { ubicable.place(0, 0) }
}

/** Línea cian con brillo en el borde de arriba (para la barra de navegación inferior) con los temas neón. */
fun Modifier.lineaSuperiorNeon(): Modifier =
    if (!Colores.esNeon) this else drawWithContent {
        drawContent()
        lineaNeon(Offset(0f, 0f), Offset(size.width, 0f), Neon.cian, 1.2.dp.toPx(), 0.7f)
    }

/**
 * Línea tenue debajo de un dato, con un tramo cian brillante al comienzo (como una ficha HUD).
 * Solo con los temas neón; con los demás no dibuja nada.
 */
fun Modifier.lineaInferiorNeon(): Modifier =
    if (!Colores.esNeon) this else drawBehind {
        val y = size.height - 0.5.dp.toPx()
        drawLine(Colores.borde, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        lineaNeon(Offset(0f, y), Offset(minOf(18.dp.toPx(), size.width), y), Neon.cian, 1.2.dp.toPx(), 0.6f)
    }

/**
 * Filtro para la foto de fondo del ingreso y el registro: con los temas neón la foto pasa a
 * blanco y negro, y [VeloNeon] la tiñe de cian y magenta. Con los demás temas, sin filtro.
 */
fun filtroFotoNeon(): ColorFilter? =
    if (Colores.esNeon) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null

/** Velo cian y magenta sobre la foto de fondo, solo con los temas neón. */
@Composable
fun VeloNeon(modifier: Modifier = Modifier) {
    if (!Colores.esNeon) return
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Neon.cian.copy(alpha = 0.30f), Neon.magenta.copy(alpha = 0.22f))))
    )
}
