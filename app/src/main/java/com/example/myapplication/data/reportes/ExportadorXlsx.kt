package com.example.myapplication.data.reportes

import com.example.myapplication.R
import com.example.myapplication.utils.Idioma
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Escribe un reporte como libro de Excel (.xlsx).
 *
 * Un .xlsx es un ZIP con archivos XML. Para una sola hoja con texto y números basta con
 * generarlos directamente, sin añadir una biblioteca como Apache POI (que pesa varios MB
 * y da problemas en Android).
 */
object ExportadorXlsx {

    private const val FILA_ENCABEZADOS = 4
    private const val ESTILO_NORMAL = 0
    private const val ESTILO_NEGRITA = 1

    fun escribir(tabla: TablaReporte, salida: OutputStream) {
        val zip = ZipOutputStream(salida)
        zip.agregar("[Content_Types].xml", TIPOS_DE_CONTENIDO)
        zip.agregar("_rels/.rels", RELACIONES_RAIZ)
        zip.agregar("xl/workbook.xml", libro(tabla.nombreHoja))
        zip.agregar("xl/_rels/workbook.xml.rels", RELACIONES_LIBRO)
        zip.agregar("xl/styles.xml", ESTILOS)
        zip.agregar("xl/worksheets/sheet1.xml", hoja(tabla))
        // finish() cierra el ZIP sin cerrar el flujo de salida, que pertenece a quien llama.
        zip.finish()
        salida.flush()
    }

    private fun ZipOutputStream.agregar(ruta: String, contenido: String) {
        putNextEntry(ZipEntry(ruta))
        write(contenido.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun hoja(tabla: TablaReporte): String = buildString {
        append(CABECERA_XML)
        append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")

        // Anchos de columna proporcionales a los pesos del reporte
        append("<cols>")
        tabla.encabezados.indices.forEach { indice ->
            val ancho = ((tabla.pesos.getOrElse(indice) { 1.5f }) * 9f).coerceIn(8f, 60f)
            append("<col min=\"${indice + 1}\" max=\"${indice + 1}\" width=\"$ancho\" customWidth=\"1\"/>")
        }
        append("</cols>")

        append("<sheetData>")
        append(fila(1, listOf(tabla.titulo), ESTILO_NEGRITA))
        append(fila(2, listOf(tabla.subtitulo), ESTILO_NORMAL))
        append(fila(FILA_ENCABEZADOS, tabla.encabezados, ESTILO_NEGRITA))
        tabla.filas.forEachIndexed { indice, celdas ->
            append(fila(FILA_ENCABEZADOS + 1 + indice, celdas, ESTILO_NORMAL))
        }
        append("</sheetData>")
        append("</worksheet>")
    }

    private fun fila(numero: Int, celdas: List<Any>, estilo: Int): String = buildString {
        append("<row r=\"$numero\">")
        celdas.forEachIndexed { columna, valor ->
            val referencia = letraDeColumna(columna) + numero
            if (valor is Number) {
                append("<c r=\"$referencia\" s=\"$estilo\"><v>$valor</v></c>")
            } else {
                append("<c r=\"$referencia\" s=\"$estilo\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                append(escaparXml(valor.toString()))
                append("</t></is></c>")
            }
        }
        append("</row>")
    }

    /** 0 -> A, 25 -> Z, 26 -> AA... */
    internal fun letraDeColumna(indice: Int): String {
        var numero = indice
        val letras = StringBuilder()
        while (numero >= 0) {
            letras.insert(0, 'A' + numero % 26)
            numero = numero / 26 - 1
        }
        return letras.toString()
    }

    /** Escapa los caracteres reservados y descarta los de control que XML no admite. */
    internal fun escaparXml(texto: String): String = buildString {
        texto.forEach { caracter ->
            when {
                caracter == '&' -> append("&amp;")
                caracter == '<' -> append("&lt;")
                caracter == '>' -> append("&gt;")
                caracter == '"' -> append("&quot;")
                caracter == '\n' || caracter == '\t' -> append(caracter)
                caracter.code < 0x20 -> Unit
                else -> append(caracter)
            }
        }
    }

    private fun libro(nombreHoja: String): String {
        // Excel no admite estos caracteres en el nombre de una hoja ni más de 31 letras.
        val nombre = nombreHoja.filterNot { it in "[]:*?/\\" }.take(31).ifBlank { Idioma.texto(R.string.reporte_hoja_defecto) }
        return CABECERA_XML +
            "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" " +
            "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">" +
            "<sheets><sheet name=\"${escaparXml(nombre)}\" sheetId=\"1\" r:id=\"rId1\"/></sheets>" +
            "</workbook>"
    }

    private const val CABECERA_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"

    private const val TIPOS_DE_CONTENIDO = CABECERA_XML +
        "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
        "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
        "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
        "<Override PartName=\"/xl/workbook.xml\" " +
        "ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
        "<Override PartName=\"/xl/worksheets/sheet1.xml\" " +
        "ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" +
        "<Override PartName=\"/xl/styles.xml\" " +
        "ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>" +
        "</Types>"

    private const val RELACIONES_RAIZ = CABECERA_XML +
        "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
        "<Relationship Id=\"rId1\" " +
        "Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" " +
        "Target=\"xl/workbook.xml\"/>" +
        "</Relationships>"

    private const val RELACIONES_LIBRO = CABECERA_XML +
        "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
        "<Relationship Id=\"rId1\" " +
        "Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" " +
        "Target=\"worksheets/sheet1.xml\"/>" +
        "<Relationship Id=\"rId2\" " +
        "Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" " +
        "Target=\"styles.xml\"/>" +
        "</Relationships>"

    // Estilo 0: texto normal. Estilo 1: negrita (título y encabezados).
    private const val ESTILOS = CABECERA_XML +
        "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">" +
        "<fonts count=\"2\">" +
        "<font><sz val=\"11\"/><name val=\"Calibri\"/></font>" +
        "<font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font>" +
        "</fonts>" +
        "<fills count=\"2\">" +
        "<fill><patternFill patternType=\"none\"/></fill>" +
        "<fill><patternFill patternType=\"gray125\"/></fill>" +
        "</fills>" +
        "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>" +
        "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>" +
        "<cellXfs count=\"2\">" +
        "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>" +
        "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>" +
        "</cellXfs>" +
        "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>" +
        "</styleSheet>"
}
