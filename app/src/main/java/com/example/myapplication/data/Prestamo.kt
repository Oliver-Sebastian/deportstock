package com.example.myapplication.data

import com.example.myapplication.R
import com.example.myapplication.utils.Idioma
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import java.util.Date

object EstadoPrestamo {
    const val PENDIENTE = "PENDIENTE"   // Los implementos siguen en poder del estudiante
    const val DEVUELTO = "DEVUELTO"
}

object EstadoItemPrestamo {
    const val PENDIENTE = "PENDIENTE"
    const val BUEN_ESTADO = "BUEN_ESTADO"
    const val DANADO = "DANADO"
    const val INCOMPLETO = "INCOMPLETO"
    const val EXTRAVIADO = "EXTRAVIADO"

    /** Estados con los que puede regresar una unidad, además de BUEN_ESTADO. */
    val NOVEDADES = listOf(DANADO, INCOMPLETO, EXTRAVIADO)

    /** Estado del artículo en el inventario que corresponde a cada novedad de la devolución. */
    fun estadoInventario(novedad: String): String = when (novedad) {
        INCOMPLETO -> EstadoItem.INCOMPLETO
        EXTRAVIADO -> EstadoItem.EXTRAVIADO
        else -> EstadoItem.DANADO
    }

    fun etiqueta(estado: String): String = when (estado) {
        BUEN_ESTADO -> Idioma.texto(R.string.estado_buen_estado)
        DANADO -> Idioma.texto(R.string.estado_danado)
        INCOMPLETO -> Idioma.texto(R.string.estado_incompleto)
        EXTRAVIADO -> Idioma.texto(R.string.estado_extraviado)
        else -> Idioma.texto(R.string.estado_pendiente)
    }
}

/** Una línea del préstamo: un artículo (o varias piezas de un artículo por cantidad). */
data class ItemPrestamo(
    val itemId: String = "",
    val nombre: String = "",
    val tipoControl: String = TipoControl.UNIDAD,
    val cantidad: Int = 1,
    val cantidadDanada: Int = 0,
    val cantidadExtraviada: Int = 0,
    val estadoDevolucion: String = EstadoItemPrestamo.PENDIENTE,
    val observacion: String = "",
    /** INCOMPLETO si la unidad ya se prestó incompleta; vacío si se prestó en buen estado. */
    val estadoAlPrestar: String = "",
    /** Observación que tenía la unidad al prestarse (por ejemplo, qué piezas le faltaban). */
    val observacionAlPrestar: String = ""
) {
    @get:Exclude
    val seEntregoIncompleto: Boolean
        get() = estadoAlPrestar == EstadoItem.INCOMPLETO

    /** Regresó dañado, incompleto o extraviado (del todo o en parte). */
    @get:Exclude
    val tieneNovedad: Boolean
        get() = cantidadDanada > 0 || cantidadExtraviada > 0 ||
            estadoDevolucion == EstadoItemPrestamo.INCOMPLETO ||
            estadoDevolucion == EstadoItemPrestamo.EXTRAVIADO ||
            estadoDevolucion == EstadoItemPrestamo.DANADO

    /** "dañado", "incompleto", "2 dañadas, 1 extraviada"... Vacío si regresó bien. */
    @get:Exclude
    val descripcionNovedad: String
        get() = when {
            !tieneNovedad -> ""
            tipoControl == TipoControl.CANTIDAD -> listOfNotNull(
                cantidadDanada.takeIf { it > 0 }?.let { Idioma.plural(R.plurals.novedad_n_danadas, it, it) },
                cantidadExtraviada.takeIf { it > 0 }?.let { Idioma.plural(R.plurals.novedad_n_extraviadas, it, it) }
            ).joinToString(", ")
            else -> EstadoItemPrestamo.etiqueta(estadoDevolucion).lowercase()
        }
}

/**
 * Documento de la colección `prestamos`.
 *
 * Equivalencias con el modelo del documento de definición:
 * fechaLimite -> [fechaEntregaEsperada], fechaDevolucion -> [fechaEntregaReal], estado -> [estadoDevolucion].
 * Se conservan los nombres que ya usaba la app para no romper los préstamos guardados.
 */
data class Prestamo(
    val id: String = "",
    val codigoEstudiante: String = "",       // Código estudiantil
    val nombreEstudiante: String = "",       // Nombre del estudiante
    val cedula: String = "",                 // Legado: préstamos antiguos sin cuenta de estudiante
    val telefono: String = "",               // Celular del estudiante al prestar (copia para reportes)
    val idItem: String = "",                 // Código del primer ítem (resumen)
    val itemsCodigos: List<String> = emptyList(), // IDs prestados: ["BA-001", "RQ-002"]
    val itemsNombres: List<String> = emptyList(), // Descripciones listas para mostrar
    val cantidadTotalItems: Int = 1,         // Total de piezas prestadas en esta solicitud
    val fechaPrestamo: Timestamp? = null,
    val fechaEntregaEsperada: Timestamp? = null,  // Fecha límite (no supera el fin de semestre)
    val fechaEntregaReal: Timestamp? = null,      // Fecha de devolución
    val estadoDevolucion: String = EstadoPrestamo.PENDIENTE,
    val observaciones: String = "",
    val documentoRetenido: Boolean = true,   // Legado: espejo de carnetRetenido
    val carnetRetenido: Boolean = true,      // true mientras Bienestar tiene el carnet
    val tipoDocumento: String = "CARNET",
    val estudianteId: String = "",           // uid del estudiante (vacío en préstamos antiguos)
    val programaEstudiante: String = "",
    val monitorId: String = "",              // uid del monitor que entregó
    val monitorNombre: String = "",
    val monitorDevolucionId: String = "",    // uid del monitor que recibió
    val items: List<ItemPrestamo> = emptyList(),
    val danoPendiente: Boolean = false       // true si se devolvió con novedades (daño, faltante o pérdida) sin resolver
) {
    /**
     * Identificador para mostrar en pantallas y reportes:
     * usa el código de estudiante si existe; si no, cae en la cédula.
     */
    @get:Exclude
    val identificadorPrestatario: String
        get() = codigoEstudiante.ifBlank { cedula }

    @get:Exclude
    val estaActivo: Boolean
        get() = estadoDevolucion.equals(EstadoPrestamo.PENDIENTE, ignoreCase = true)

    /**
     * Líneas del préstamo. Los préstamos creados con versiones anteriores solo tienen
     * [itemsCodigos]/[itemsNombres]; aquí se convierten para tratarlos igual que los nuevos.
     */
    @get:Exclude
    val itemsEfectivos: List<ItemPrestamo>
        get() = items.ifEmpty {
            itemsCodigos.mapIndexed { indice, codigo ->
                val descripcion = itemsNombres.getOrNull(indice).orEmpty()
                ItemPrestamo(
                    itemId = codigo,
                    nombre = descripcion.substringAfter(" - ", descripcion).ifBlank { codigo }
                )
            }
        }

    /** Vencido = sigue sin devolverse y ya pasó la fecha límite. */
    fun estaVencido(ahora: Date = Date()): Boolean {
        val limite = fechaEntregaEsperada?.toDate() ?: return false
        return estaActivo && ahora.after(limite)
    }
}

/** Artículo elegido en la pantalla de préstamo, con la cantidad a entregar. */
data class SeleccionPrestamo(
    val item: ItemDeportivo,
    val cantidad: Int = 1
)

/**
 * Resultado de revisar un artículo al momento de la devolución (RF-11).
 * Solo se envían los artículos con novedad.
 * - Unidad: [estado] es DANADO, INCOMPLETO o EXTRAVIADO (de [EstadoItemPrestamo]).
 * - Cantidad: se usan [cantidadDanada] y [cantidadExtraviada].
 */
data class ResultadoDevolucion(
    val itemId: String,
    val estado: String = EstadoItemPrestamo.DANADO,
    val cantidadDanada: Int = 0,
    val cantidadExtraviada: Int = 0,
    val observacion: String = ""
)
