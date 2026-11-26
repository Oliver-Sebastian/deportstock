package com.example.myapplication.data.reportes

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import com.example.myapplication.R

/**
 * Un reporte ya armado, independiente del formato de salida.
 * Los exportadores (CSV, Excel, PDF) solo saben escribir una [TablaReporte].
 */
data class TablaReporte(
    val titulo: String,
    val subtitulo: String,
    val nombreHoja: String,
    val encabezados: List<String>,
    /** Ancho relativo de cada columna; lo usan Excel y PDF. Mismo tamaño que [encabezados]. */
    val pesos: List<Float>,
    /** Cada celda es un String o un número (los números van como numéricos en Excel). */
    val filas: List<List<Any>>
)

/**
 * [guardado]: aviso al terminar ("Reporte de préstamos guardado en Excel (12 registros)").
 * [prefijoArchivo]: parte del nombre del archivo ("deportstock_prestamos_2026-11-22.xlsx").
 */
enum class TipoReporte(@PluralsRes val guardado: Int, @StringRes val prefijoArchivo: Int) {
    PRESTAMOS(R.plurals.reportes_guardado_prestamos, R.string.reporte_archivo_prestamos),
    INVENTARIO(R.plurals.reportes_guardado_inventario, R.string.reporte_archivo_inventario)
}

enum class FormatoReporte(val etiqueta: String, val extension: String, val mime: String) {
    CSV("CSV", "csv", "text/csv"),
    EXCEL("Excel", "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    PDF("PDF", "pdf", "application/pdf")
}
