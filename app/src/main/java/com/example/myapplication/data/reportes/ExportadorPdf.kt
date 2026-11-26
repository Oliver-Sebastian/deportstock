package com.example.myapplication.data.reportes

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.myapplication.R
import com.example.myapplication.utils.Idioma
import java.io.OutputStream

/**
 * Escribe un reporte como PDF con la clase PdfDocument de Android (sin bibliotecas externas).
 * Página A4 horizontal, tabla con encabezado repetido en cada página y texto recortado
 * con puntos suspensivos cuando no cabe en la columna.
 */
object ExportadorPdf {

    // A4 horizontal en puntos PostScript (1/72 de pulgada)
    private const val ANCHO_PAGINA = 842
    private const val ALTO_PAGINA = 595
    private const val MARGEN = 32f
    private const val ALTO_FILA = 16f
    private const val RELLENO_CELDA = 3f

    fun escribir(tabla: TablaReporte, salida: OutputStream) {
        val documento = PdfDocument()
        try {
            Dibujante(documento, tabla).dibujar()
            documento.writeTo(salida)
            salida.flush()
        } finally {
            documento.close()
        }
    }

    /** Lleva el estado del dibujo: página actual y posición vertical. */
    private class Dibujante(
        private val documento: PdfDocument,
        private val tabla: TablaReporte
    ) {
        private val pinturaTitulo = Paint().apply {
            isAntiAlias = true
            textSize = 15f
            isFakeBoldText = true
            color = Color.BLACK
        }
        private val pinturaSubtitulo = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            color = Color.DKGRAY
        }
        private val pinturaEncabezado = Paint().apply {
            isAntiAlias = true
            textSize = 8.5f
            isFakeBoldText = true
            color = Color.WHITE
        }
        private val pinturaCelda = Paint().apply {
            isAntiAlias = true
            textSize = 8f
            color = Color.BLACK
        }
        private val pinturaFondoEncabezado = Paint().apply { color = Color.rgb(38, 94, 40) }
        private val pinturaFondoFila = Paint().apply { color = Color.rgb(240, 245, 240) }
        private val pinturaLinea = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 0.5f
        }

        private val anchoUtil = ANCHO_PAGINA - 2 * MARGEN
        private val anchos: List<Float> = run {
            val pesos = tabla.encabezados.indices.map { tabla.pesos.getOrElse(it) { 1f } }
            val total = pesos.sum().takeIf { it > 0f } ?: 1f
            pesos.map { anchoUtil * it / total }
        }

        private var numeroPagina = 0
        private var pagina: PdfDocument.Page? = null
        private var y = MARGEN

        fun dibujar() {
            nuevaPagina()
            if (tabla.filas.isEmpty()) {
                lienzo().drawText(Idioma.texto(R.string.reporte_sin_registros), MARGEN, y + 14f, pinturaCelda)
            }
            tabla.filas.forEachIndexed { indice, fila ->
                if (y + ALTO_FILA > ALTO_PAGINA - MARGEN) {
                    nuevaPagina()
                }
                dibujarFila(fila.map { it.toString() }, pinturaCelda, if (indice % 2 == 1) pinturaFondoFila else null)
            }
            cerrarPagina()
        }

        private fun lienzo(): Canvas = checkNotNull(pagina) { "No hay página abierta" }.canvas

        private fun nuevaPagina() {
            cerrarPagina()
            numeroPagina++
            val info = PdfDocument.PageInfo.Builder(ANCHO_PAGINA, ALTO_PAGINA, numeroPagina).create()
            pagina = documento.startPage(info)
            y = MARGEN

            val canvas = lienzo()
            if (numeroPagina == 1) {
                canvas.drawText(tabla.titulo, MARGEN, y + 12f, pinturaTitulo)
                y += 22f
                canvas.drawText(recortar(tabla.subtitulo, pinturaSubtitulo, anchoUtil), MARGEN, y + 8f, pinturaSubtitulo)
                y += 18f
            }
            canvas.drawText(
                Idioma.texto(R.string.reporte_pagina, numeroPagina),
                ANCHO_PAGINA - MARGEN - 48f,
                ALTO_PAGINA - MARGEN / 2,
                pinturaSubtitulo
            )
            dibujarFila(tabla.encabezados, pinturaEncabezado, pinturaFondoEncabezado)
        }

        private fun cerrarPagina() {
            pagina?.let { documento.finishPage(it) }
            pagina = null
        }

        private fun dibujarFila(celdas: List<String>, pintura: Paint, fondo: Paint?) {
            val canvas = lienzo()
            if (fondo != null) {
                canvas.drawRect(MARGEN, y, MARGEN + anchoUtil, y + ALTO_FILA, fondo)
            }
            var x = MARGEN
            anchos.forEachIndexed { columna, ancho ->
                val texto = celdas.getOrElse(columna) { "" }
                canvas.drawText(recortar(texto, pintura, ancho - 2 * RELLENO_CELDA), x + RELLENO_CELDA, y + ALTO_FILA - 5f, pintura)
                x += ancho
            }
            y += ALTO_FILA
            canvas.drawLine(MARGEN, y, MARGEN + anchoUtil, y, pinturaLinea)
        }

        /** Recorta el texto con puntos suspensivos si no cabe en [anchoMaximo]. */
        private fun recortar(texto: String, pintura: Paint, anchoMaximo: Float): String {
            val limpio = texto.replace('\n', ' ')
            if (pintura.measureText(limpio) <= anchoMaximo) return limpio
            val caben = pintura.breakText(limpio, true, anchoMaximo - pintura.measureText("…"), null)
            return limpio.take(caben.coerceAtLeast(0)) + "…"
        }
    }
}
