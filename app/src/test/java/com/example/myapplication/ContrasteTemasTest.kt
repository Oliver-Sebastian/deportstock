package com.example.myapplication

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.PaletaApp
import com.example.myapplication.ui.theme.PaletaClara
import com.example.myapplication.ui.theme.PaletaCyberpunk
import com.example.myapplication.ui.theme.PaletaInstitucional
import com.example.myapplication.ui.theme.PaletaOscura
import com.example.myapplication.ui.theme.PaletaRetro
import com.example.myapplication.ui.theme.PaletaSolar
import com.example.myapplication.ui.theme.PaletaVarsity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Contraste de los temas según WCAG 2.1: al menos 4,5:1 para texto y 3:1 para íconos y bordes
 * de campos. Revisa los pares de colores que la app pone juntos (texto sobre fondos y tarjetas,
 * etiquetas de estado sobre su tinte, botones, barras), no colores sueltos.
 *
 * Retro, Varsity y Solar se revisan completos. De los temas anteriores, el texto principal: sus
 * etiquetas de estado se diseñaron antes de esta prueba y algunas quedan cerca de 4:1.
 */
class ContrasteTemasTest {

    private val nuevos = listOf("Retro" to PaletaRetro, "Varsity" to PaletaVarsity, "Solar" to PaletaSolar)
    private val anteriores = listOf(
        "Oscuro" to PaletaOscura,
        "Claro" to PaletaClara,
        "Udenar" to PaletaInstitucional,
        "Cyberpunk" to PaletaCyberpunk
    )

    @Test
    fun todosLosTemasTienenElTextoPrincipalLegible() {
        for ((nombre, p) in nuevos + anteriores) {
            for ((fondo, color) in listOf("el fondo" to p.fondo, "las tarjetas" to p.superficie, "los diálogos" to p.dialogo)) {
                exigir(nombre, Par("texto sobre $fondo", p.texto, color, TEXTO))
                exigir(nombre, Par("texto secundario sobre $fondo", p.textoSecundario, color, TEXTO))
            }
        }
    }

    @Test
    fun retroVarsityYSolarSeLeenEnTodaLaApp() {
        for ((nombre, p) in nuevos) pares(p).forEach { exigir(nombre, it) }
    }

    @Test
    fun solarEsBlancoYNegroConBordesDeDosDpYEstadosSolidos() {
        assertEquals(Color.White, PaletaSolar.fondo)
        assertEquals(Color.White, PaletaSolar.superficie)
        assertEquals(Color.Black, PaletaSolar.texto)
        assertEquals(Color.Black, PaletaSolar.borde)
        assertEquals(2.dp, PaletaSolar.grosorBorde)
        assertTrue(PaletaSolar.estadosSolidos)
    }

    @Test
    fun losTemasAnterioresQuedanIgual() {
        for ((nombre, p) in anteriores) {
            assertEquals("$nombre: sin borde propio en las tarjetas", 0.dp, p.grosorBorde)
            assertEquals("$nombre: el relleno neutro sigue siendo el color del borde", p.borde, p.rellenoNeutro)
            assertTrue("$nombre: etiquetas con tinte, no sólidas", !p.estadosSolidos)
        }
    }

    private data class Par(val que: String, val adelante: Color, val atras: Color, val minimo: Double)

    /** Los pares de colores que la app pone juntos con la paleta [p]. */
    private fun pares(p: PaletaApp): List<Par> {
        val lista = mutableListOf<Par>()
        val fondos = listOf(
            "fondo" to p.fondo,
            "superficie" to p.superficie,
            "superficieAlta" to p.superficieAlta,
            "dialogo" to p.dialogo
        )
        for ((fondo, atras) in fondos) {
            lista += Par("texto / $fondo", p.texto, atras, TEXTO)
            lista += Par("textoSecundario / $fondo", p.textoSecundario, atras, TEXTO)
            lista += Par("textoTenue / $fondo", p.textoTenue, atras, TEXTO)
            lista += Par("acentoTexto / $fondo", p.acentoTexto, atras, TEXTO)
        }
        lista += listOf(
            Par("primario como texto / superficie", p.primario, p.superficie, TEXTO),
            Par("primario como texto / fondo", p.primario, p.fondo, TEXTO),
            Par("pestaña elegida / barraInferior", p.primario, p.barraInferior, TEXTO),
            Par("pestañas / barraInferior", p.textoSecundario, p.barraInferior, TEXTO),
            Par("enlaces / fondo", p.secundario, p.fondo, TEXTO),
            Par("enlaces / superficie", p.secundario, p.superficie, TEXTO),
            Par("botones: sobrePrimario / primario", p.sobrePrimario, p.primario, TEXTO),
            Par("botón de borrar: sobreError / errorBoton", p.sobreError, p.errorBoton, TEXTO),
            Par("botón Cambiar: texto / rellenoNeutro", p.texto, p.rellenoNeutro, TEXTO),
            Par("filtro elegido: primario / rellenoNeutro", p.primario, p.rellenoNeutro, TEXTO),
            Par("barra superior: título", p.sobreBarra, p.barra, TEXTO),
            Par("barra superior: subtítulo", p.sobreBarraSecundario, p.barra, TEXTO),
            Par("error / superficie", p.error, p.superficie, TEXTO),
            Par("aviso de error", p.error, p.errorFondo, TEXTO),
            Par("advertencia / superficie", p.advertencia, p.superficie, TEXTO),
            Par("aviso de advertencia", p.advertencia, p.advertenciaFondo, TEXTO),
            Par("naranja / superficie", p.naranja, p.superficie, TEXTO),
            Par("daños (salmón) / superficie", p.salmon, p.superficie, TEXTO),
            Par("daños (salmón) / diálogo", p.salmon, p.dialogo, TEXTO),
            Par("aviso informativo", p.info, p.infoFondo, TEXTO),
            Par("incompleto / superficie", p.incompleto, p.superficie, TEXTO),
            Par("íconos de la barra superior", p.iconoBarra, p.barra, GRAFICO),
            Par("borde de campos / superficie", p.bordeCampo, p.superficie, GRAFICO),
            Par("borde de campos / superficieAlta", p.bordeCampo, p.superficieAlta, GRAFICO),
            Par("íconos de daño / superficie", p.errorFuerte, p.superficie, GRAFICO)
        )
        // Etiquetas de estado (DISPONIBLE, PRESTADO, DAÑADO, el código...) e insignias (VENCIDO, PENDIENTE...)
        val estados = listOf(
            "exito" to p.exito,
            "info" to p.info,
            "errorFuerte" to p.errorFuerte,
            "naranja" to p.naranja,
            "incompleto" to p.incompleto,
            "extraviado" to p.extraviado,
            "primario" to p.primario
        )
        val insignias = listOf(
            Triple("error", p.error, p.errorFondo),
            Triple("advertencia", p.advertencia, p.advertenciaFondo),
            Triple("naranja", p.naranja, p.advertenciaFondo),
            Triple("acentoTexto", p.acentoTexto, p.exitoFondo),
            Triple("primario", p.primario, p.exitoFondo)
        )
        if (p.estadosSolidos) {
            // Color lleno con texto blanco o negro, como Colores.textoEtiqueta
            for ((que, color) in estados + insignias.map { it.first to it.second }) {
                lista += Par("etiqueta sólida $que", sobre(color), color, TEXTO)
            }
        } else {
            for ((que, color) in estados) for (alfa in listOf(0.18f, 0.25f)) {
                lista += Par("etiqueta $que sobre su tinte de ${(alfa * 100).toInt()} %", color, mezcla(color, alfa, p.superficie), TEXTO)
            }
            for ((que, color, fondo) in insignias) lista += Par("insignia $que", color, fondo, TEXTO)
        }
        return lista
    }

    private fun exigir(tema: String, par: Par) {
        val r = contraste(par.adelante, par.atras)
        assertTrue("$tema, ${par.que}: ${"%.2f".format(r)}:1 (mínimo ${par.minimo}:1)", r >= par.minimo)
    }

    private companion object {
        const val TEXTO = 4.5
        const val GRAFICO = 3.0

        /** Luminancia relativa de WCAG 2.1. */
        fun luminancia(c: Color): Double {
            fun lineal(v: Float): Double = if (v <= 0.04045f) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
            return 0.2126 * lineal(c.red) + 0.7152 * lineal(c.green) + 0.0722 * lineal(c.blue)
        }

        fun contraste(a: Color, b: Color): Double {
            val la = luminancia(a)
            val lb = luminancia(b)
            return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
        }

        /** [color] con transparencia [alfa] ya pintado sobre [fondo], como se ve en pantalla. */
        fun mezcla(color: Color, alfa: Float, fondo: Color) = Color(
            red = color.red * alfa + fondo.red * (1 - alfa),
            green = color.green * alfa + fondo.green * (1 - alfa),
            blue = color.blue * alfa + fondo.blue * (1 - alfa)
        )

        /** Blanco o negro, el que más contraste hace (como Colores.contrasteSobre). */
        fun sobre(fondo: Color): Color = if (luminancia(fondo) > 0.179) Color.Black else Color.White
    }
}
