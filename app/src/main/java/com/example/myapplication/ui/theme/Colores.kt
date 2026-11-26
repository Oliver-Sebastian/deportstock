package com.example.myapplication.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.TemaApp

/**
 * Colores de la app con nombre de USO (texto, tarjeta, error...), no de tono.
 * Cada tema define los mismos nombres, así las pantallas no cambian al cambiar de tema.
 * Los tres últimos (relleno neutro, bordes y estados sólidos) tienen un valor por defecto:
 * solo los cambian los temas que lo necesitan (Retro, Varsity y Solar).
 */
data class PaletaApp(
    /** true si el fondo es oscuro (define el color de los íconos de la barra de navegación del sistema). */
    val esOscura: Boolean,
    /** true si la barra superior es oscura (define el color de los íconos de la barra de estado). */
    val barraOscura: Boolean,
    /** Botones principales, interruptores y elementos activos. */
    val primario: Color,
    /** Texto e íconos sobre el color primario. */
    val sobrePrimario: Color,
    /** Enlaces secundarios (pantalla de ingreso). */
    val secundario: Color,
    /** Acento de apoyo. */
    val terciario: Color,
    /** Fondo de las pantallas. */
    val fondo: Color,
    /** Barra superior. */
    val barra: Color,
    /** Título e íconos de la barra superior. */
    val sobreBarra: Color,
    /** Subtítulo de la barra superior. */
    val sobreBarraSecundario: Color,
    /** Íconos de la barra superior (atrás, ajustes). */
    val iconoBarra: Color,
    /** Barra de pestañas del estudiante. */
    val barraInferior: Color,
    /** Tarjetas. */
    val superficie: Color,
    /** Tarjetas dentro de tarjetas, filas y campos. */
    val superficieAlta: Color,
    /** Fondo de los diálogos. */
    val dialogo: Color,
    /** Bordes y separadores. */
    val borde: Color,
    /** Borde de los campos de texto sin foco. */
    val bordeCampo: Color,
    /** Controles apagados o deshabilitados. */
    val deshabilitado: Color,
    /** Texto principal. */
    val texto: Color,
    /** Texto de apoyo. */
    val textoSecundario: Color,
    /** Texto menor: fechas, ayudas, conteos. */
    val textoTenue: Color,
    /** Texto destacado en verde (correos, subtítulos). */
    val acentoTexto: Color,
    /** Mensajes de error y bloqueos. */
    val error: Color,
    /** Íconos y marcas de daño. */
    val errorFuerte: Color,
    /** Fondo de avisos de error. */
    val errorFondo: Color,
    /** Botones de acciones destructivas. */
    val errorBoton: Color,
    /** Texto sobre botones destructivos. */
    val sobreError: Color,
    /** Préstamos pendientes y avisos. */
    val advertencia: Color,
    /** Novedades por resolver y reparación. */
    val naranja: Color,
    /** Detalle de daños. */
    val salmon: Color,
    /** Fondo de avisos y recordatorios. */
    val advertenciaFondo: Color,
    /** Disponible. */
    val exito: Color,
    /** Fondo de estados correctos. */
    val exitoFondo: Color,
    /** Prestado. */
    val info: Color,
    /** Fondo informativo. */
    val infoFondo: Color,
    /** Novedad: incompleto. */
    val incompleto: Color,
    /** Novedad: extraviado. */
    val extraviado: Color,
    /**
     * Relleno de los botones secundarios ("Cambiar") y del filtro elegido. Por defecto, el color
     * de los bordes; los temas de bordes oscuros (Retro, Solar) ponen uno claro, o el texto no se leería.
     */
    val rellenoNeutro: Color = borde,
    /** Borde que el tema pone a todas las tarjetas, en vez de la sombra. 0 dp: tarjetas de Material, sin borde. */
    val grosorBorde: Dp = 0.dp,
    /** true: etiquetas de estado con fondo de color sólido y texto blanco, para leerlas al sol (Solar). */
    val estadosSolidos: Boolean = false
)

/** Oscuro: los mismos colores que la app tenía antes de los temas. */
val PaletaOscura = PaletaApp(
    esOscura = true,
    barraOscura = true,
    primario = Pink80,
    sobrePrimario = Color(0xFF00390A),
    secundario = PurpleGrey80,
    terciario = Purple80,
    fondo = Color(0xFF0F170F),
    barra = Color(0xFF142416),
    sobreBarra = Color.White,
    sobreBarraSecundario = Color(0xFFA5D6A7),
    iconoBarra = Color.White,
    barraInferior = Color(0xFF142416),
    superficie = Color(0xFF162118),
    superficieAlta = Color(0xFF1B261D),
    dialogo = Color(0xFF1E261F),
    borde = Color(0xFF2E4030),
    bordeCampo = Color(0xFF2E3D2F),
    deshabilitado = Color(0xFF253126),
    texto = Color.White,
    textoSecundario = Color.LightGray,
    textoTenue = Color.Gray,
    acentoTexto = Color(0xFFA5D6A7),
    error = Color(0xFFFF8A80),
    errorFuerte = Color(0xFFFF5252),
    errorFondo = Color(0xFF421C1C),
    errorBoton = Color(0xFFB3261E),
    sobreError = Color.White,
    advertencia = Color(0xFFFFD54F),
    naranja = Color(0xFFFFB74D),
    salmon = Color(0xFFFFAB91),
    advertenciaFondo = Color(0xFF332A15),
    exito = Color(0xFF4CAF50),
    exitoFondo = Color(0xFF1B331E),
    info = Color(0xFF42A5F5),
    infoFondo = Color(0xFF102636),
    incompleto = Color(0xFFBA68C8),
    extraviado = Color(0xFF90A4AE)
)

/** Claro: fondo blanco, texto casi negro y verdes oscuros (todo el texto supera 4,5:1). */
val PaletaClara = PaletaApp(
    esOscura = false,
    barraOscura = false,
    primario = Color(0xFF2E7D32),
    sobrePrimario = Color(0xFFFFFFFF),
    secundario = Color(0xFF1B5E20),
    terciario = Color(0xFF00695C),
    fondo = Color(0xFFF4F7F4),
    barra = Color(0xFFFFFFFF),
    sobreBarra = Color(0xFF1B1F1B),
    sobreBarraSecundario = Color(0xFF2E7D32),
    iconoBarra = Color(0xFF1B1F1B),
    barraInferior = Color(0xFFFFFFFF),
    superficie = Color(0xFFFFFFFF),
    superficieAlta = Color(0xFFEDF3EE),
    dialogo = Color(0xFFFFFFFF),
    borde = Color(0xFFD0DBD1),
    bordeCampo = Color(0xFF7D8A7E),
    deshabilitado = Color(0xFFE1E7E1),
    texto = Color(0xFF1B1F1B),
    textoSecundario = Color(0xFF424A43),
    textoTenue = Color(0xFF5F6B60),
    acentoTexto = Color(0xFF2E7D32),
    error = Color(0xFFC62828),
    errorFuerte = Color(0xFFD32F2F),
    errorFondo = Color(0xFFFDECEA),
    errorBoton = Color(0xFFC62828),
    sobreError = Color(0xFFFFFFFF),
    advertencia = Color(0xFF7A5C00),
    naranja = Color(0xFFB54708),
    salmon = Color(0xFFBF360C),
    advertenciaFondo = Color(0xFFFFF4D6),
    exito = Color(0xFF2E7D32),
    exitoFondo = Color(0xFFE3F2E5),
    info = Color(0xFF1565C0),
    infoFondo = Color(0xFFE3F0FB),
    incompleto = Color(0xFF8E24AA),
    extraviado = Color(0xFF546E7A)
)

/**
 * Institucional: identidad de la Universidad de Nariño (CSS de SAPIENS y app institucional).
 * Barra verde oficial con texto blanco, fondo verde muy claro y tarjetas blancas con borde gris.
 * Los rojos, naranjas y azules del CSS se oscurecieron lo justo para leerse sobre blanco.
 */
val PaletaInstitucional = PaletaApp(
    esOscura = false,
    barraOscura = true,
    primario = UdenarVerde,
    sobrePrimario = Color(0xFFFFFFFF),
    secundario = UdenarVerdeOscuro,
    terciario = UdenarAzulPetroleo,
    fondo = UdenarFondo,
    barra = UdenarVerde,
    sobreBarra = Color(0xFFFFFFFF),
    sobreBarraSecundario = Color(0xFFFFFFFF),
    iconoBarra = Color(0xFFFFFFFF),
    barraInferior = Color(0xFFFFFFFF),
    superficie = Color(0xFFFFFFFF),
    superficieAlta = UdenarFondoTarjeta,
    dialogo = Color(0xFFFFFFFF),
    borde = UdenarBorde,
    bordeCampo = Color(0xFF8F8F8F),
    deshabilitado = UdenarGrisClaro,
    texto = UdenarTexto,
    textoSecundario = UdenarTextoSecundario,
    textoTenue = Color(0xFF6B6B6B),
    acentoTexto = UdenarVerdeOscuro,
    error = Color(0xFFC0392B),
    errorFuerte = Color(0xFFEC2327),
    errorFondo = Color(0xFFFDECEC),
    errorBoton = Color(0xFFC0392B),
    sobreError = Color(0xFFFFFFFF),
    advertencia = Color(0xFF8A5A00),
    naranja = Color(0xFFB84A1B),
    salmon = Color(0xFFB23C17),
    advertenciaFondo = Color(0xFFFFF3DC),
    exito = Color(0xFF007A34),
    exitoFondo = Color(0xFFE6F4EC),
    info = Color(0xFF2E6F96),
    infoFondo = Color(0xFFE8F2F8),
    incompleto = Color(0xFF8E24AA),
    extraviado = Color(0xFF546E7A)
)

/**
 * Cyberpunk: fondo azul noche con cuadrícula, paneles HUD con borde neón cian y magenta,
 * acentos lima (botones, interruptores, elegido) y texto blanco. Colores medidos en los
 * diseños del equipo. La barra superior es transparente para que se vea el fondo.
 */
val PaletaCyberpunk = PaletaApp(
    esOscura = true,
    barraOscura = true,
    primario = Color(0xFFADFE57),
    sobrePrimario = Color(0xFF135206),
    secundario = Color(0xFF22E5FF),
    terciario = Color(0xFFE255F2),
    fondo = Color(0xFF0A1520),
    barra = Color.Transparent,
    sobreBarra = Color(0xFFFFFFFF),
    sobreBarraSecundario = Color(0xFF96D6E0),
    iconoBarra = Color(0xFF45D9EE),
    barraInferior = Color(0xFF0D1A25),
    superficie = Color(0xFF111C28),
    superficieAlta = Color(0xFF15273A),
    dialogo = Color(0xFF0F1B27),
    borde = Color(0xFF2B5B6B),
    bordeCampo = Color(0xFF3E7F92),
    deshabilitado = Color(0xFF1C2C3A),
    texto = Color(0xFFFFFFFF),
    textoSecundario = Color(0xFFC4DCE2),
    textoTenue = Color(0xFF9CC6CF),
    acentoTexto = Color(0xFF22E5FF),
    error = Color(0xFFF0707F),
    errorFuerte = Color(0xFFFF4D6D),
    errorFondo = Color(0xFF2A1420),
    errorBoton = Color(0xFFC2374D),
    sobreError = Color(0xFFFFFFFF),
    advertencia = Color(0xFFE5FC5A),
    naranja = Color(0xFFFFB547),
    salmon = Color(0xFFFF9A8B),
    advertenciaFondo = Color(0xFF262A12),
    exito = Color(0xFFADFE57),
    exitoFondo = Color(0xFF17290F),
    info = Color(0xFF22E5FF),
    infoFondo = Color(0xFF0E2633),
    incompleto = Color(0xFFE255F2),
    extraviado = Color(0xFF9FB3C8)
)

/**
 * Retro: ropa deportiva vintage de los 80 y 90 (rompevientos y pistas clásicas). Papel arena,
 * tarjetas marfil con borde carbón definido y barra verde petróleo con íconos mostaza.
 * El coral (#E05A47) y la mostaza (#E9A825) puros no llegan a 4,5:1 como texto sobre el papel
 * (3,7:1 y 2,0:1): en textos y etiquetas van del mismo tono, más oscuros.
 */
val PaletaRetro = PaletaApp(
    esOscura = false,
    barraOscura = true,
    primario = Color(0xFF0F5C6E),             // verde petróleo
    sobrePrimario = Color(0xFFFFFFFF),
    secundario = Color(0xFFA63D2A),           // coral quemado oscuro: enlaces del ingreso
    terciario = Color(0xFFE05A47),            // coral quemado
    fondo = Color(0xFFF3EAD8),                // arena cálido
    barra = Color(0xFF0F5C6E),
    sobreBarra = Color(0xFFFFFFFF),
    sobreBarraSecundario = Color(0xFFF5D27A), // mostaza clara
    iconoBarra = Color(0xFFE9A825),           // mostaza vintage
    barraInferior = Color(0xFFFFFDF9),
    superficie = Color(0xFFFFFDF9),           // crema marfil
    superficieAlta = Color(0xFFF8F1E3),
    dialogo = Color(0xFFFFFDF9),
    borde = Color(0xFF231F20),                // borde definido, carbón
    bordeCampo = Color(0xFF5C554E),
    deshabilitado = Color(0xFFE6DCC8),
    texto = Color(0xFF231F20),                // carbón espresso
    textoSecundario = Color(0xFF5C554E),      // gris cálido
    textoTenue = Color(0xFF665E55),
    acentoTexto = Color(0xFF0F5C6E),
    error = Color(0xFFA8321F),
    errorFuerte = Color(0xFF9C2E1C),          // coral quemado, oscuro
    errorFondo = Color(0xFFFBE3DC),
    errorBoton = Color(0xFFA8321F),
    sobreError = Color(0xFFFFFFFF),
    advertencia = Color(0xFF7A5300),          // mostaza, oscura
    naranja = Color(0xFF844409),
    salmon = Color(0xFF9E3F26),
    advertenciaFondo = Color(0xFFFBEBC6),
    exito = Color(0xFF2A6130),
    exitoFondo = Color(0xFFDDEBEA),
    info = Color(0xFF285680),
    infoFondo = Color(0xFFE1EAF1),
    incompleto = Color(0xFF743A7A),
    extraviado = Color(0xFF4F585D),
    rellenoNeutro = Color(0xFFF5D79A),        // mostaza clara: botón "Cambiar" y filtro elegido
    grosorBorde = 1.5.dp
)

/**
 * Varsity: chaqueta universitaria (letterman). Fondo hueso, tarjetas blancas con borde suave,
 * barra borgoña con íconos dorados y texto azul marino. El dorado apagado (#D4A373) va en
 * íconos, insignias y fondos; como texto sería 2,3:1, así que ahí se usa uno más oscuro.
 */
val PaletaVarsity = PaletaApp(
    esOscura = false,
    barraOscura = true,
    primario = Color(0xFF8B1E3F),             // rojo borgoña
    sobrePrimario = Color(0xFFFFFFFF),
    secundario = Color(0xFF8B1E3F),
    terciario = Color(0xFFD4A373),            // dorado apagado
    fondo = Color(0xFFFAF6F0),                // crema hueso
    barra = Color(0xFF8B1E3F),
    sobreBarra = Color(0xFFFFFFFF),
    sobreBarraSecundario = Color(0xFFF1D9B5), // dorado claro
    iconoBarra = Color(0xFFD4A373),
    barraInferior = Color(0xFFFFFFFF),
    superficie = Color(0xFFFFFFFF),           // blanco cálido
    superficieAlta = Color(0xFFF6F0E6),
    dialogo = Color(0xFFFFFFFF),
    borde = Color(0xFFE5DEC9),                // bordes suaves
    bordeCampo = Color(0xFF857B66),
    deshabilitado = Color(0xFFECE5D8),
    texto = Color(0xFF14213D),                // azul marino profundo
    textoSecundario = Color(0xFF3C4760),
    textoTenue = Color(0xFF545E75),
    acentoTexto = Color(0xFF8B1E3F),
    error = Color(0xFFB02A1E),
    errorFuerte = Color(0xFFA3231B),
    errorFondo = Color(0xFFFBE9E6),
    errorBoton = Color(0xFFB02A1E),
    sobreError = Color(0xFFFFFFFF),
    advertencia = Color(0xFF7D5418),          // dorado, oscuro
    naranja = Color(0xFF93411A),
    salmon = Color(0xFF9F3B26),
    advertenciaFondo = Color(0xFFF7ECDA),
    exito = Color(0xFF2A6338),
    exitoFondo = Color(0xFFF5E9D6),           // dorado muy claro: insignias ("MONITOR EN TURNO")
    info = Color(0xFF1F4E8A),
    infoFondo = Color(0xFFE7EDF6),
    incompleto = Color(0xFF653A93),
    extraviado = Color(0xFF4F5A68),
    grosorBorde = 1.dp
)

/**
 * Solar: alto contraste para usar al aire libre, bajo el sol, en las canchas. Fondo y tarjetas
 * blanco puro con borde negro de 2 dp, texto negro, azul cobalto en botones y barra, y etiquetas
 * de estado de color sólido con texto blanco. Ningún texto baja de 4,7:1.
 */
val PaletaSolar = PaletaApp(
    esOscura = false,
    barraOscura = true,
    primario = Color(0xFF0051FF),             // azul cobalto
    sobrePrimario = Color(0xFFFFFFFF),
    secundario = Color(0xFF0033CC),           // cobalto oscuro: enlaces
    terciario = Color(0xFF0033CC),
    fondo = Color(0xFFFFFFFF),
    barra = Color(0xFF0051FF),
    sobreBarra = Color(0xFFFFFFFF),
    sobreBarraSecundario = Color(0xFFFFFFFF),
    iconoBarra = Color(0xFFFFFFFF),
    barraInferior = Color(0xFFFFFFFF),
    superficie = Color(0xFFFFFFFF),
    superficieAlta = Color(0xFFF0F0F0),
    dialogo = Color(0xFFFFFFFF),
    borde = Color(0xFF000000),
    bordeCampo = Color(0xFF000000),
    deshabilitado = Color(0xFFD9D9D9),
    texto = Color(0xFF000000),
    textoSecundario = Color(0xFF1A1A1A),
    textoTenue = Color(0xFF333333),
    acentoTexto = Color(0xFF0033CC),
    error = Color(0xFFC00000),
    errorFuerte = Color(0xFFD50000),          // rojo
    errorFondo = Color(0xFFFFE5E5),
    errorBoton = Color(0xFFC00000),
    sobreError = Color(0xFFFFFFFF),
    advertencia = Color(0xFF8A5A00),          // ámbar
    naranja = Color(0xFFC43C00),              // naranja
    salmon = Color(0xFFB71C1C),
    advertenciaFondo = Color(0xFFFFF0B3),
    exito = Color(0xFF007A1F),                // verde
    exitoFondo = Color(0xFFE3F4E6),
    info = Color(0xFF0033CC),                 // azul
    infoFondo = Color(0xFFE5ECFF),
    incompleto = Color(0xFF8E00B3),           // morado
    extraviado = Color(0xFF3D3D3D),           // gris oscuro
    rellenoNeutro = Color(0xFFE0E9FF),        // cobalto muy claro: botón "Cambiar" y filtro elegido
    grosorBorde = 2.dp,
    estadosSolidos = true
)

/**
 * Colores del tema activo. Las pantallas usan, por ejemplo, `Colores.texto` en vez de `Color.White`.
 *
 * [tema] es un estado de Compose: al cambiarlo, toda pantalla que haya leído un color se vuelve
 * a dibujar con el nuevo tema, sin reiniciar la app. Como no es una función @Composable,
 * también se puede usar dentro de `remember`, de lambdas o de funciones normales.
 */
object Colores {
    var tema: TemaApp by mutableStateOf(TemaApp.OSCURO)

    val paleta: PaletaApp get() = paletaDe(tema)

    fun paletaDe(tema: TemaApp): PaletaApp = when (tema) {
        TemaApp.OSCURO -> PaletaOscura
        TemaApp.CLARO -> PaletaClara
        TemaApp.INSTITUCIONAL -> PaletaInstitucional
        TemaApp.CYBERPUNK -> PaletaCyberpunk
        TemaApp.RETRO -> PaletaRetro
        TemaApp.VARSITY -> PaletaVarsity
        TemaApp.SOLAR -> PaletaSolar
    }

    /** true con los temas de estilo neón (paneles HUD, brillo y animaciones). */
    val esNeon: Boolean get() = tema.esNeon

    /**
     * Fondo de las pantallas (Scaffold). En los temas neón es transparente: debajo está el
     * fondo animado con la cuadrícula ([com.example.myapplication.ui.components.FondoApp]).
     */
    val fondoPantalla: Color get() = if (tema.esNeon) Color.Transparent else paleta.fondo

    val primario: Color get() = paleta.primario
    val sobrePrimario: Color get() = paleta.sobrePrimario
    val secundario: Color get() = paleta.secundario
    val terciario: Color get() = paleta.terciario
    val fondo: Color get() = paleta.fondo
    val barra: Color get() = paleta.barra
    val sobreBarra: Color get() = paleta.sobreBarra
    val sobreBarraSecundario: Color get() = paleta.sobreBarraSecundario
    val iconoBarra: Color get() = paleta.iconoBarra
    val barraInferior: Color get() = paleta.barraInferior
    val superficie: Color get() = paleta.superficie
    val superficieAlta: Color get() = paleta.superficieAlta
    val dialogo: Color get() = paleta.dialogo
    val borde: Color get() = paleta.borde
    val bordeCampo: Color get() = paleta.bordeCampo
    val deshabilitado: Color get() = paleta.deshabilitado
    val texto: Color get() = paleta.texto
    val textoSecundario: Color get() = paleta.textoSecundario
    val textoTenue: Color get() = paleta.textoTenue
    val acentoTexto: Color get() = paleta.acentoTexto
    val error: Color get() = paleta.error
    val errorFuerte: Color get() = paleta.errorFuerte
    val errorFondo: Color get() = paleta.errorFondo
    val errorBoton: Color get() = paleta.errorBoton
    val sobreError: Color get() = paleta.sobreError
    val advertencia: Color get() = paleta.advertencia
    val naranja: Color get() = paleta.naranja
    val salmon: Color get() = paleta.salmon
    val advertenciaFondo: Color get() = paleta.advertenciaFondo
    val exito: Color get() = paleta.exito
    val exitoFondo: Color get() = paleta.exitoFondo
    val info: Color get() = paleta.info
    val infoFondo: Color get() = paleta.infoFondo
    val incompleto: Color get() = paleta.incompleto
    val extraviado: Color get() = paleta.extraviado
    val rellenoNeutro: Color get() = paleta.rellenoNeutro

    /** Borde de las tarjetas en este tema; 0 dp si las tarjetas no llevan borde propio. */
    val grosorBorde: Dp get() = paleta.grosorBorde

    /** Para un borde que la pantalla ya ponía ([pedido]): ese grosor o el del tema, el mayor. */
    fun anchoBorde(pedido: Dp): Dp = if (paleta.grosorBorde > pedido) paleta.grosorBorde else pedido

    /**
     * Relleno (o borde) de una etiqueta de estado (DISPONIBLE, PRESTADO, el código...): [color] con
     * transparencia [alfa], como siempre, o, con los estados sólidos de Solar, el [color] lleno.
     */
    fun rellenoEtiqueta(color: Color, alfa: Float): Color =
        if (paleta.estadosSolidos) color else color.copy(alpha = alfa)

    /** Igual, para las etiquetas con un fondo propio ([fondo], por ejemplo errorFondo en "VENCIDO"). */
    fun rellenoEtiqueta(color: Color, fondo: Color): Color = if (paleta.estadosSolidos) color else fondo

    /** Texto e íconos de una etiqueta de [color]: ese color o, sobre el relleno sólido, blanco o negro. */
    fun textoEtiqueta(color: Color): Color = if (paleta.estadosSolidos) contrasteSobre(color) else color

    /** Blanco o negro, el que más contraste hace con [fondo] (con luminancia 0,179 empatan). */
    fun contrasteSobre(fondo: Color): Color = if (fondo.luminance() > 0.179f) Color.Black else Color.White
}
