package com.example.myapplication.data

import com.example.myapplication.R
import com.example.myapplication.utils.Idioma
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

/**
 * Categorías de base del inventario. En Firestore se guardan en español (son las claves de los
 * filtros y de los códigos); en pantalla y en los reportes se muestran en el idioma de la app.
 */
object Categorias {
    /** Nombre para mostrar: traducido si es una de base; si la escribió un monitor, tal cual. */
    fun etiqueta(categoria: String): String = when (categoria) {
        "Balones" -> Idioma.texto(R.string.categoria_balones)
        "Raquetas" -> Idioma.texto(R.string.categoria_raquetas)
        "Petos" -> Idioma.texto(R.string.categoria_petos)
        "Conos" -> Idioma.texto(R.string.categoria_conos)
        "Ajedrez" -> Idioma.texto(R.string.categoria_ajedrez)
        "Otros" -> Idioma.texto(R.string.categoria_otros)
        // Valores de "todas las categorías" de los filtros
        "Todas" -> Idioma.texto(R.string.categoria_todas)
        "Todos" -> Idioma.texto(R.string.categoria_todos)
        else -> categoria
    }
}

/** Regla de negocio 1: inventario mixto. */
object TipoControl {
    const val UNIDAD = "UNIDAD"       // Cada documento es una unidad física con código propio
    const val CANTIDAD = "CANTIDAD"   // Un documento agrupa N piezas iguales (conos, petos...)
}

object EstadoItem {
    const val DISPONIBLE = "DISPONIBLE"
    const val PRESTADO = "PRESTADO"
    const val DANADO = "DAÑADO"
    const val EN_REPARACION = "EN REPARACIÓN"
    const val INCOMPLETO = "INCOMPLETO"   // Le faltan piezas (por ejemplo, un juego de ajedrez)
    const val EXTRAVIADO = "EXTRAVIADO"   // Perdido: no está en Bienestar
    const val AGOTADO = "AGOTADO"     // Solo artículos por cantidad: no queda ninguno disponible

    /** Estados que se registran como novedad y llevan observación. */
    val NOVEDADES = listOf(DANADO, INCOMPLETO, EXTRAVIADO)

    /**
     * Estados con los que una unidad se puede prestar. Un artículo incompleto (por ejemplo,
     * un ajedrez al que le falta una pieza) se sigue prestando; la falta queda registrada.
     */
    val PRESTABLES = listOf(DISPONIBLE, INCOMPLETO)

    fun esPrestable(estado: String): Boolean = PRESTABLES.any { it.equals(estado, ignoreCase = true) }

    /** Estados que el monitor puede asignar a mano a una unidad desde el formulario. */
    val EDITABLES = listOf(DISPONIBLE, DANADO, INCOMPLETO, EXTRAVIADO, EN_REPARACION)

    /**
     * Categorías de artículos de una sola pieza: no pueden quedar "incompletos" porque no
     * tienen piezas que falten (un balón regresa bien, dañado o no regresa). Pedido del testeo.
     * Incompleto sigue disponible para ajedrez, sets de raquetas, kits y "Otros".
     */
    val CATEGORIAS_DE_UNA_PIEZA = listOf("Balones", "Petos")

    /** false para balones y petos. Si no se conoce la categoría, se permite. */
    fun admiteIncompleto(categoria: String?): Boolean =
        categoria.isNullOrBlank() || CATEGORIAS_DE_UNA_PIEZA.none { it.equals(categoria.trim(), ignoreCase = true) }

    /** Estados del formulario para una unidad de [categoria] (sin Incompleto si no aplica). */
    fun editablesPara(categoria: String?): List<String> =
        if (admiteIncompleto(categoria)) EDITABLES else EDITABLES - INCOMPLETO

    fun esNovedad(estado: String): Boolean = NOVEDADES.any { it.equals(estado, ignoreCase = true) }

    /** Estado que le corresponde a un artículo controlado por cantidad. */
    fun porCantidad(disponible: Int, total: Int, danada: Int, extraviada: Int = 0): String = when {
        disponible > 0 -> DISPONIBLE
        total > 0 && extraviada >= total -> EXTRAVIADO
        total > 0 && danada + extraviada >= total -> DANADO
        else -> AGOTADO
    }

    /** Texto para mostrar, en el idioma de la app: "Dañado", "Incompleto", "En reparación"... */
    fun etiqueta(estado: String): String = when (estado.uppercase()) {
        DISPONIBLE -> Idioma.texto(R.string.estado_disponible)
        PRESTADO -> Idioma.texto(R.string.estado_prestado)
        DANADO -> Idioma.texto(R.string.estado_danado)
        EN_REPARACION, "REPARACION" -> Idioma.texto(R.string.estado_en_reparacion)
        INCOMPLETO -> Idioma.texto(R.string.estado_incompleto)
        EXTRAVIADO -> Idioma.texto(R.string.estado_extraviado)
        AGOTADO -> Idioma.texto(R.string.estado_agotado)
        else -> estado.lowercase().replaceFirstChar { it.uppercase() }
    }
}

/** Documento de la colección `inventario`. */
data class ItemDeportivo(
    val id: String = "",                         // Código único (ej: BA-001, CN-001)
    val nombre: String = "",                     // Nombre o referencia del elemento
    val categoria: String = "Balones",           // Categoría deportiva
    val icono: String = "⚽",                    // Ícono representativo
    val estado: String = EstadoItem.DISPONIBLE,  // Ver EstadoItem
    val estudiantePrestamoId: String = "",       // Solo para mostrar al monitor; ya no se guarda en Firestore
    val estudiantePrestamoNombre: String = "",   // Solo para mostrar al monitor; ya no se guarda en Firestore
    val cantidadTotal: Int = 1,                  // 1 en control por unidad; N en control por cantidad
    val cantidadDisponible: Int = 1,             // Piezas que se pueden prestar ahora mismo
    val descripcion: String = "",                // Marca, serial o detalles
    val fechaCreacion: Timestamp? = null,
    val fechaActualizacion: Timestamp? = null,
    val tipoControl: String = TipoControl.UNIDAD,
    val cantidadDanada: Int = 0,                 // Piezas dañadas (0 o 1 en control por unidad)
    val cantidadExtraviada: Int = 0,             // Piezas extraviadas (0 o 1 en control por unidad)
    val observacion: String = "",                // Observación de la novedad: daño, faltante o pérdida (RF-07)
    val activo: Boolean = true,                  // false = dado de baja (RF-05)
    val prestamoActivoId: String = ""            // Préstamo que tiene esta unidad; vacío si no está prestada
) {
    @get:Exclude
    val esPorCantidad: Boolean
        get() = tipoControl == TipoControl.CANTIDAD

    /** Piezas actualmente prestadas. Solo tiene sentido en control por cantidad. */
    @get:Exclude
    val cantidadPrestada: Int
        get() = (cantidadTotal - cantidadDisponible - cantidadDanada - cantidadExtraviada).coerceAtLeast(0)

    @get:Exclude
    val estaPrestado: Boolean
        get() = if (esPorCantidad) cantidadPrestada > 0 else estado.equals(EstadoItem.PRESTADO, ignoreCase = true)

    @get:Exclude
    val tieneDanos: Boolean
        get() = cantidadDanada > 0 || estado.equals(EstadoItem.DANADO, ignoreCase = true)

    /** Dañado, incompleto o extraviado (del todo o en parte). */
    @get:Exclude
    val tieneNovedad: Boolean
        get() = if (esPorCantidad) cantidadDanada > 0 || cantidadExtraviada > 0 else EstadoItem.esNovedad(estado)

    /** Piezas con novedad: en control por unidad es 0 o 1. */
    @get:Exclude
    val piezasConNovedad: Int
        get() = if (esPorCantidad) cantidadDanada + cantidadExtraviada else if (tieneNovedad) 1 else 0

    /** true si se puede prestar ahora mismo (disponible, o incompleto en el caso de una unidad). */
    @get:Exclude
    val sePuedePrestar: Boolean
        get() = if (esPorCantidad) cantidadDisponible > 0 else EstadoItem.esPrestable(estado)

    /** Piezas que se pueden prestar ahora mismo. */
    @get:Exclude
    val piezasPrestables: Int
        get() = if (esPorCantidad) cantidadDisponible else if (sePuedePrestar) 1 else 0

    @get:Exclude
    val esIncompleto: Boolean
        get() = !esPorCantidad && estado.equals(EstadoItem.INCOMPLETO, ignoreCase = true)

    /** Piezas que existen y se pueden usar: prestables más prestadas (lo que ve el estudiante como total). */
    @get:Exclude
    val piezasEnCirculacion: Int
        get() = when {
            esPorCantidad -> cantidadDisponible + cantidadPrestada
            sePuedePrestar || estado.equals(EstadoItem.PRESTADO, ignoreCase = true) -> 1
            else -> 0
        }

    /** Descripción corta de la novedad, por ejemplo "2 dañadas, 1 extraviada" o "Incompleto". */
    @get:Exclude
    val resumenNovedad: String
        get() = if (esPorCantidad) {
            listOfNotNull(
                cantidadDanada.takeIf { it > 0 }?.let { Idioma.plural(R.plurals.novedad_n_danadas, it, it) },
                cantidadExtraviada.takeIf { it > 0 }?.let { Idioma.plural(R.plurals.novedad_n_extraviadas, it, it) }
            ).joinToString(", ")
        } else if (tieneNovedad) {
            EstadoItem.etiqueta(estado)
        } else {
            ""
        }
}
