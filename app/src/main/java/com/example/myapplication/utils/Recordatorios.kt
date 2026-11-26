package com.example.myapplication.utils

import com.example.myapplication.R
import com.example.myapplication.data.Prestamo
import java.util.Date

/**
 * RF-19: texto del recordatorio de devolución. Lo comparten el aviso dentro de la app
 * y la notificación, para que digan exactamente lo mismo.
 */
object Recordatorios {

    /** Días antes de la fecha límite en los que se empieza a recordar. */
    const val DIAS_AVISO = 3

    private const val MILISEGUNDOS_POR_DIA = 24L * 60 * 60 * 1000

    /**
     * Devuelve el recordatorio que corresponde al préstamo, o null si no hace falta avisar
     * (ya se devolvió, no tiene fecha límite o aún faltan más de [DIAS_AVISO] días).
     */
    fun mensajePara(prestamo: Prestamo, ahora: Date = Date()): String? {
        if (!prestamo.estaActivo) return null
        val limite = prestamo.fechaEntregaEsperada?.toDate() ?: return null
        val fecha = Fechas.corta(limite)

        if (ahora.after(limite)) {
            return Idioma.texto(R.string.recordatorio_vencido, fecha)
        }

        // La fecha límite es el final del día, así que se cuentan días de calendario.
        val diasRestantes = ((Fechas.inicioDelDia(limite).time - Fechas.inicioDelDia(ahora).time) / MILISEGUNDOS_POR_DIA).toInt()
        return when {
            diasRestantes > DIAS_AVISO -> null
            diasRestantes <= 0 -> Idioma.texto(R.string.recordatorio_hoy, fecha)
            diasRestantes == 1 -> Idioma.texto(R.string.recordatorio_manana, fecha)
            else -> Idioma.texto(R.string.recordatorio_en_dias, diasRestantes, fecha)
        }
    }

    /** Aviso diario del monitor: null si no hay préstamos vencidos. */
    fun mensajeParaMonitor(vencidos: Int): String? = when {
        vencidos <= 0 -> null
        vencidos == 1 -> Idioma.texto(R.string.recordatorio_monitor_uno)
        else -> Idioma.texto(R.string.recordatorio_monitor_varios, vencidos)
    }
}
