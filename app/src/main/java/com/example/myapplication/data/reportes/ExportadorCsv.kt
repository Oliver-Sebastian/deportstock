package com.example.myapplication.data.reportes

import java.io.OutputStream

/**
 * Escribe un reporte como CSV.
 *
 * - Separador punto y coma: es el que espera Excel con configuración regional en español.
 * - UTF-8 con BOM, para que Excel muestre bien tildes y eñes.
 */
object ExportadorCsv {

    private const val SEPARADOR = ";"
    private const val FIN_DE_LINEA = "\r\n"
    private val BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())

    fun escribir(tabla: TablaReporte, salida: OutputStream) {
        val texto = buildString {
            append(linea(tabla.encabezados))
            tabla.filas.forEach { fila -> append(linea(fila)) }
        }
        salida.write(BOM)
        salida.write(texto.toByteArray(Charsets.UTF_8))
        salida.flush()
    }

    private fun linea(celdas: List<Any>): String =
        celdas.joinToString(SEPARADOR) { escapar(it.toString()) } + FIN_DE_LINEA

    /** Una celda con separador, comillas o saltos de línea va entre comillas dobles. */
    private fun escapar(valor: String): String {
        val requiereComillas = valor.contains(SEPARADOR) || valor.contains('"') ||
            valor.contains('\n') || valor.contains('\r')
        return if (requiereComillas) "\"" + valor.replace("\"", "\"\"") + "\"" else valor
    }
}
