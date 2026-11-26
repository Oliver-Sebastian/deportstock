package com.example.myapplication.data.reportes

import com.example.myapplication.R
import com.example.myapplication.data.ItemDeportivo
import com.example.myapplication.data.Prestamo
import com.example.myapplication.utils.Fechas
import com.example.myapplication.utils.Idioma
import java.util.Date

data class ConteoArticulo(
    val itemId: String,
    val nombre: String,
    val veces: Int
)

/** RF-20: indicadores del panel del monitor. */
data class Indicadores(
    val prestamosHoy: Int = 0,
    val activos: Int = 0,
    val vencidos: Int = 0,
    /** Piezas dañadas, incompletas o extraviadas. */
    val piezasConNovedad: Int = 0,
    val articulosConNovedad: List<ItemDeportivo> = emptyList(),
    val masPrestados: List<ConteoArticulo> = emptyList()
)

/** RF-18: los préstamos de un estudiante o de un artículo. */
data class GrupoHistorial(
    val clave: String,
    val titulo: String,
    val subtitulo: String,
    val prestamos: List<Prestamo>
)

/** Cálculos sobre los préstamos ya cargados. No depende de Android ni de Firebase. */
object AnalisisPrestamos {

    fun indicadores(
        prestamos: List<Prestamo>,
        inventarioActivo: List<ItemDeportivo>,
        ahora: Date = Date()
    ): Indicadores {
        val conNovedad = inventarioActivo.filter { it.tieneNovedad }
        return Indicadores(
            prestamosHoy = prestamos.count { prestamo ->
                prestamo.fechaPrestamo?.toDate()?.let { Fechas.mismoDia(it, ahora) } ?: false
            },
            activos = prestamos.count { it.estaActivo },
            vencidos = prestamos.count { it.estaVencido(ahora) },
            piezasConNovedad = conNovedad.sumOf { it.piezasConNovedad },
            articulosConNovedad = conNovedad,
            masPrestados = masPrestados(prestamos)
        )
    }

    /** Artículos ordenados por el número de préstamos en los que aparecen. */
    fun masPrestados(prestamos: List<Prestamo>, limite: Int = 5): List<ConteoArticulo> =
        prestamos
            .flatMap { prestamo -> prestamo.itemsEfectivos.distinctBy { it.itemId } }
            .groupBy { it.itemId }
            .map { (itemId, lineas) -> ConteoArticulo(itemId, lineas.last().nombre, lineas.size) }
            .sortedWith(compareByDescending<ConteoArticulo> { it.veces }.thenBy { it.itemId })
            .take(limite)

    /** Historial agrupado por estudiante, filtrado por código o nombre. */
    fun porEstudiante(prestamos: List<Prestamo>, consulta: String): List<GrupoHistorial> {
        val texto = consulta.trim()
        return prestamos
            .groupBy { claveEstudiante(it) }
            .map { (clave, lista) ->
                val referencia = lista.first()
                GrupoHistorial(
                    clave = clave,
                    titulo = referencia.nombreEstudiante.ifBlank { Idioma.texto(R.string.reporte_estudiante_sin_nombre) },
                    subtitulo = Idioma.texto(
                        R.string.comun_codigo_estudiante_de,
                        referencia.identificadorPrestatario.ifBlank { Idioma.texto(R.string.comun_sin_codigo) }
                    ),
                    prestamos = lista
                )
            }
            .filter { grupo ->
                texto.isBlank() ||
                    grupo.titulo.contains(texto, ignoreCase = true) ||
                    grupo.subtitulo.contains(texto, ignoreCase = true)
            }
            .sortedBy { it.titulo.lowercase() }
    }

    /** Historial agrupado por artículo, filtrado por código o nombre del artículo. */
    fun porArticulo(prestamos: List<Prestamo>, consulta: String): List<GrupoHistorial> {
        val texto = consulta.trim()
        val nombres = mutableMapOf<String, String>()
        val porItem = mutableMapOf<String, MutableList<Prestamo>>()
        prestamos.forEach { prestamo ->
            prestamo.itemsEfectivos.distinctBy { it.itemId }.forEach { linea ->
                nombres.getOrPut(linea.itemId) { linea.nombre }
                porItem.getOrPut(linea.itemId) { mutableListOf() }.add(prestamo)
            }
        }
        return porItem
            .map { (itemId, lista) ->
                GrupoHistorial(
                    clave = itemId,
                    titulo = nombres[itemId].orEmpty().ifBlank { itemId },
                    subtitulo = Idioma.texto(R.string.comun_codigo_articulo_de, itemId),
                    prestamos = lista
                )
            }
            .filter { grupo ->
                texto.isBlank() ||
                    grupo.titulo.contains(texto, ignoreCase = true) ||
                    grupo.clave.contains(texto, ignoreCase = true)
            }
            .sortedBy { it.clave }
    }

    /** Agrupa por código estudiantil; los préstamos antiguos sin código usan el uid o el nombre. */
    private fun claveEstudiante(prestamo: Prestamo): String =
        prestamo.identificadorPrestatario.trim().lowercase()
            .ifBlank { prestamo.estudianteId }
            .ifBlank { prestamo.nombreEstudiante.trim().lowercase() }
}
