package com.example.myapplication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Los textos en inglés (res/values-en) deben corresponder uno a uno con los de español
 * (res/values): mismas claves y mismos marcadores (%1$s, %2$d...). Si falta una clave, Android
 * muestra el texto en español; si falta un marcador, la app falla al formatear el texto.
 */
class TraduccionesTest {

    /** Un texto: su valor (o los valores de cada cantidad, si es plural) y si se traduce. */
    private class Texto(val valores: Map<String, String>, val traducible: Boolean)

    private val espanol = leer("values")
    private val ingles = leer("values-en")

    @Test
    fun cadaTextoTieneSuTraduccion() {
        val traducibles = espanol.filterValues { it.traducible }.keys
        assertEquals("Textos sin traducir", emptySet<String>(), traducibles - ingles.keys)
        assertEquals("Textos en inglés que no existen en español", emptySet<String>(), ingles.keys - espanol.keys)
    }

    @Test
    fun losMarcadoresCoinciden() {
        val distintos = ingles.keys.filter { clave ->
            marcadores(espanol.getValue(clave)) != marcadores(ingles.getValue(clave))
        }
        assertTrue("Marcadores distintos en: $distintos", distintos.isEmpty())
    }

    @Test
    fun losPluralesTienenSingularYPlural() {
        listOf(espanol, ingles).forEach { textos ->
            textos.filterKeys { it.startsWith("plurals/") }.forEach { (clave, texto) ->
                assertTrue("$clave necesita one y other", texto.valores.keys.containsAll(listOf("one", "other")))
            }
        }
    }

    @Test
    fun losNombresDeLosIdiomasNoSeTraducen() {
        // El selector de Ajustes siempre muestra "Español" y "English", en cualquier idioma
        assertEquals("Español", espanol.getValue("string/idioma_espanol").valores.getValue(""))
        assertEquals("English", espanol.getValue("string/idioma_ingles").valores.getValue(""))
        assertTrue(listOf("string/idioma_espanol", "string/idioma_ingles").none { it in ingles })
    }

    /** Marcadores de un texto; en los plurales, los de "other" (el singular puede omitir el número). */
    private fun marcadores(texto: Texto): Set<String> {
        val valor = texto.valores["other"] ?: texto.valores.getValue("")
        return Regex("""%\d+\$[a-z]""").findAll(valor).map { it.value }.toSet()
    }

    private fun leer(carpeta: String): Map<String, Texto> {
        // Gradle corre las pruebas desde la carpeta del módulo (app); por si acaso, también desde la raíz
        val archivo = listOf("src/main/res", "app/src/main/res")
            .map { File(it, "$carpeta/strings.xml") }
            .first { it.exists() }
        val raiz = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(archivo).documentElement
        val textos = mutableMapOf<String, Texto>()
        val nodos = raiz.childNodes
        for (i in 0 until nodos.length) {
            val nodo = nodos.item(i) as? Element ?: continue
            val traducible = nodo.getAttribute("translatable") != "false"
            when (nodo.tagName) {
                "string" -> textos["string/${nodo.getAttribute("name")}"] =
                    Texto(mapOf("" to nodo.textContent), traducible)
                "plurals" -> {
                    val items = nodo.getElementsByTagName("item")
                    val valores = (0 until items.length).associate { j ->
                        val item = items.item(j) as Element
                        item.getAttribute("quantity") to item.textContent
                    }
                    textos["plurals/${nodo.getAttribute("name")}"] = Texto(valores, traducible)
                }
            }
        }
        return textos
    }
}
