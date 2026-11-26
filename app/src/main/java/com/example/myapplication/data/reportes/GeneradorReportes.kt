package com.example.myapplication.data.reportes

import com.example.myapplication.R
import com.example.myapplication.data.Categorias
import com.example.myapplication.data.EstadoItem
import com.example.myapplication.data.ItemDeportivo
import com.example.myapplication.data.Prestamo
import com.example.myapplication.data.ProgramasAcademicos
import com.example.myapplication.utils.Fechas
import com.example.myapplication.utils.Idioma
import java.util.Date
import com.example.myapplication.utils.Telefonos

/**
 * RF-17: arma los reportes de préstamos y de inventario a partir de los datos ya cargados.
 * No depende de Android ni de Firebase, así que se puede probar con datos de ejemplo.
 */
object GeneradorReportes {

    /** Préstamos cuya fecha de entrega cae dentro del rango. Un extremo null no limita. */
    fun enRango(prestamos: List<Prestamo>, desde: Date?, hasta: Date?): List<Prestamo> {
        if (desde == null && hasta == null) return prestamos
        return prestamos.filter { prestamo ->
            val fecha = prestamo.fechaPrestamo?.toDate() ?: return@filter false
            (desde == null || !fecha.before(desde)) && (hasta == null || !fecha.after(hasta))
        }
    }

    fun descripcionRango(desde: Date?, hasta: Date?): String = when {
        desde != null && hasta != null -> Idioma.texto(R.string.reporte_rango, Fechas.corta(desde), Fechas.corta(hasta))
        desde != null -> Idioma.texto(R.string.reporte_desde, Fechas.corta(desde))
        hasta != null -> Idioma.texto(R.string.reporte_hasta, Fechas.corta(hasta))
        else -> Idioma.texto(R.string.reporte_todos)
    }

    /** Estado del préstamo en palabras, incluido el estado de la devolución. */
    fun estadoDe(prestamo: Prestamo, ahora: Date = Date()): String = when {
        prestamo.estaVencido(ahora) -> Idioma.texto(R.string.reporte_estado_vencido)
        prestamo.estaActivo -> Idioma.texto(R.string.reporte_estado_pendiente)
        prestamo.danoPendiente -> Idioma.texto(R.string.reporte_estado_sin_resolver)
        prestamo.itemsEfectivos.any { it.tieneNovedad } -> Idioma.texto(R.string.reporte_estado_resuelto)
        else -> Idioma.texto(R.string.reporte_estado_devuelto)
    }

    fun prestamos(
        prestamos: List<Prestamo>,
        desde: Date?,
        hasta: Date?,
        ahora: Date = Date()
    ): TablaReporte {
        val filas = enRango(prestamos, desde, hasta)
            .sortedBy { it.fechaPrestamo?.seconds ?: 0L }
            .map { prestamo ->
                val danos = prestamo.itemsEfectivos
                    .filter { it.tieneNovedad }
                    .joinToString("; ") {
                        "${it.itemId} ${it.descripcionNovedad}: ${it.observacion.ifBlank { Idioma.texto(R.string.comun_sin_observacion_minuscula) }}"
                    }
                listOf<Any>(
                    prestamo.fechaPrestamo?.toDate()?.let { Fechas.conHora(it) }.orEmpty(),
                    prestamo.nombreEstudiante,
                    prestamo.identificadorPrestatario,
                    Telefonos.formatear(prestamo.telefono),
                    ProgramasAcademicos.etiqueta(prestamo.programaEstudiante),
                    prestamo.itemsNombres.joinToString(", ").ifBlank { prestamo.idItem },
                    prestamo.cantidadTotalItems,
                    prestamo.fechaEntregaEsperada?.toDate()?.let { Fechas.corta(it) }.orEmpty(),
                    prestamo.fechaEntregaReal?.toDate()?.let { Fechas.conHora(it) }.orEmpty(),
                    estadoDe(prestamo, ahora),
                    if (prestamo.carnetRetenido) Idioma.texto(R.string.reporte_carnet_retenido) else Idioma.texto(R.string.reporte_carnet_devuelto),
                    prestamo.monitorNombre,
                    listOf(danos, prestamo.observaciones).filter { it.isNotBlank() }.joinToString(" | ")
                )
            }

        return TablaReporte(
            titulo = Idioma.texto(R.string.reporte_titulo_prestamos),
            subtitulo = Idioma.texto(
                R.string.reporte_resumen_prestamos,
                descripcionRango(desde, hasta),
                Idioma.plural(R.plurals.comun_n_prestamos, filas.size, filas.size),
                Fechas.conHora(ahora)
            ),
            nombreHoja = Idioma.texto(R.string.reporte_hoja_prestamos),
            encabezados = listOf(
                Idioma.texto(R.string.reporte_col_fecha_prestamo), Idioma.texto(R.string.reporte_col_estudiante), Idioma.texto(R.string.reporte_col_codigo_estudiante), Idioma.texto(R.string.reporte_col_celular), Idioma.texto(R.string.reporte_col_programa), Idioma.texto(R.string.reporte_col_articulos), Idioma.texto(R.string.reporte_col_piezas),
                Idioma.texto(R.string.reporte_col_fecha_limite), Idioma.texto(R.string.reporte_col_fecha_devolucion), Idioma.texto(R.string.reporte_col_estado), Idioma.texto(R.string.reporte_col_carnet), Idioma.texto(R.string.reporte_col_monitor), Idioma.texto(R.string.reporte_col_novedades)
            ),
            pesos = listOf(2.2f, 2.6f, 1.4f, 1.6f, 2.2f, 4f, 0.8f, 1.4f, 2.2f, 2f, 1.2f, 2f, 3.2f),
            filas = filas
        )
    }

    /**
     * Estado actual del inventario. El rango de fechas se usa para la columna
     * "Préstamos en el periodo": cuántas veces se prestó cada artículo en ese lapso.
     */
    fun inventario(
        items: List<ItemDeportivo>,
        prestamos: List<Prestamo>,
        desde: Date?,
        hasta: Date?,
        ahora: Date = Date()
    ): TablaReporte {
        val vecesPrestado = enRango(prestamos, desde, hasta)
            .flatMap { prestamo -> prestamo.itemsEfectivos.map { it.itemId }.distinct() }
            .groupingBy { it }
            .eachCount()

        val filas = items
            .sortedWith(compareBy({ it.categoria }, { it.id }))
            .map { item ->
                val prestadas = if (item.esPorCantidad) item.cantidadPrestada else if (item.estaPrestado) 1 else 0
                val danadas = if (item.esPorCantidad) item.cantidadDanada else if (item.tieneDanos) 1 else 0
                val extraviadas = if (item.esPorCantidad) item.cantidadExtraviada
                    else if (item.estado.equals(EstadoItem.EXTRAVIADO, ignoreCase = true)) 1 else 0
                listOf<Any>(
                    item.id,
                    item.nombre,
                    Categorias.etiqueta(item.categoria),
                    if (item.esPorCantidad) Idioma.texto(R.string.inventario_por_cantidad) else Idioma.texto(R.string.inventario_por_unidad),
                    if (item.activo) EstadoItem.etiqueta(item.estado).uppercase() else Idioma.texto(R.string.reporte_dado_de_baja),
                    item.cantidadTotal,
                    item.piezasPrestables,
                    prestadas,
                    danadas,
                    extraviadas,
                    vecesPrestado[item.id] ?: 0,
                    listOf(item.observacion, item.descripcion).filter { it.isNotBlank() }.joinToString(" | ")
                )
            }

        return TablaReporte(
            titulo = Idioma.texto(R.string.reporte_titulo_inventario),
            subtitulo = Idioma.texto(
                R.string.reporte_resumen_inventario,
                Idioma.plural(R.plurals.reporte_n_articulos, filas.size, filas.size),
                descripcionRango(desde, hasta).lowercase(),
                Fechas.conHora(ahora)
            ),
            nombreHoja = Idioma.texto(R.string.reporte_hoja_inventario),
            encabezados = listOf(
                Idioma.texto(R.string.reporte_col_codigo), Idioma.texto(R.string.reporte_col_nombre), Idioma.texto(R.string.reporte_col_categoria), Idioma.texto(R.string.reporte_col_control), Idioma.texto(R.string.reporte_col_estado), Idioma.texto(R.string.reporte_col_total), Idioma.texto(R.string.reporte_col_disponibles),
                Idioma.texto(R.string.reporte_col_prestadas), Idioma.texto(R.string.reporte_col_danadas), Idioma.texto(R.string.reporte_col_extraviadas), Idioma.texto(R.string.reporte_col_prestamos_periodo), Idioma.texto(R.string.reporte_col_observaciones)
            ),
            pesos = listOf(1.2f, 3.6f, 1.6f, 1.5f, 1.8f, 0.9f, 1.3f, 1.2f, 1.1f, 1.3f, 1.6f, 3.6f),
            filas = filas
        )
    }

    fun nombreArchivo(tipo: TipoReporte, formato: FormatoReporte, ahora: Date = Date()): String =
        "deportstock_${Idioma.texto(tipo.prefijoArchivo)}_${Fechas.paraArchivo(ahora)}.${formato.extension}"
}
