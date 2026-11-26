package com.example.myapplication

import com.example.myapplication.data.DatosEjemplo
import com.example.myapplication.data.TipoArticulo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Cada implemento muestra la foto que le corresponde (en lugar del emoji). */
class TipoArticuloTest {

    @Test
    fun cadaArticuloDeEjemploTieneSuFoto() {
        val esperados = mapOf(
            "BA-001" to TipoArticulo.BALON_FUTBOL,
            "BA-003" to TipoArticulo.BALON_BALONCESTO,
            "BA-005" to TipoArticulo.BALON_VOLEIBOL,
            "RQ-001" to TipoArticulo.RAQUETA_TENIS_MESA,
            "RQ-003" to TipoArticulo.RAQUETA_TENIS_CAMPO,
            "AJ-001" to TipoArticulo.AJEDREZ,
            "PT-001" to TipoArticulo.PETO,
            "CN-001" to TipoArticulo.CONO
        )
        val inventario = DatosEjemplo.inventario()
        for ((id, tipo) in esperados) {
            val item = inventario.first { it.id == id }
            assertEquals(item.nombre, tipo, TipoArticulo.de(item.nombre, item.categoria, item.icono))
        }
        assertTrue(inventario.all { TipoArticulo.de(it.nombre, it.categoria, it.icono) != null })
    }

    @Test
    fun seReconocePorNombreAunqueElEmojiSeaElDeLaCategoria() {
        // Al crear un artículo, el emoji guardado es el de la categoría elegida
        assertEquals(TipoArticulo.BALON_BALONCESTO, TipoArticulo.de("Balón de baloncesto", "Balones", "⚽"))
        assertEquals(TipoArticulo.RAQUETA_TENIS_MESA, TipoArticulo.de("Raqueta de ping pong", "Raquetas", "🎾"))
        assertEquals(TipoArticulo.CUERDA, TipoArticulo.de("Cuerda para saltar", "Otros", "🏅"))
        assertEquals(TipoArticulo.CRONOMETRO, TipoArticulo.de("Cronómetro digital", "Otros", "🏅"))
        assertEquals(TipoArticulo.COLCHONETA, TipoArticulo.de("COLCHONETA AZUL", "Otros", "🏅"))
        assertEquals(TipoArticulo.CUERDA, TipoArticulo.de("Lazo", "Otros", ""))
    }

    @Test
    fun sinElDeporteEnElNombreSeUsaElEmojiOLaCategoria() {
        assertEquals(TipoArticulo.BALON_FUTBOL, TipoArticulo.de("Balon golty", "Balones", "⚽"))
        assertEquals(TipoArticulo.BALON_VOLEIBOL, TipoArticulo.de("Balón Mikasa", "Balones", "🏐"))
        assertEquals(TipoArticulo.BALON_FUTBOL, TipoArticulo.de("Balón", "Balones", "🏅"))
        assertEquals(TipoArticulo.PETO, TipoArticulo.de("Rojo talla M", "Petos", ""))
    }

    @Test
    fun loQueNoSeReconoceConservaSuEmoji() {
        assertNull(TipoArticulo.de("Aro de hula hula", "Otros", "🏅"))
        // "cono" solo como palabra: "Reconocimiento" no es un cono
        assertNull(TipoArticulo.de("Reconocimiento", "Otros", "🏅"))
        assertNull(TipoArticulo.deCategoria("Otros"))
    }

    @Test
    fun cadaTipoTieneSuImagenEnLosRecursos() {
        val carpeta = listOf("src/main/res/drawable-nodpi", "app/src/main/res/drawable-nodpi")
            .map(::File).first { it.isDirectory }
        for (tipo in TipoArticulo.values()) {
            val archivo = File(carpeta, "articulo_${tipo.name.lowercase()}.webp")
            assertTrue("Falta ${archivo.name}", archivo.isFile)
        }
    }
}
