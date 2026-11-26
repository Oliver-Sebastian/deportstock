package com.example.myapplication.utils

import com.example.myapplication.R
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Utilidades de fechas compartidas por pantallas y repositorios. Las horas se muestran en
 * formato de 12 horas con "AM" y "PM" (en los dos idiomas); el horario de atención se guarda en
 * 24 horas ("14:30") y solo se convierte al mostrarlo.
 */
object Fechas {

    private const val AM = "AM"
    private const val PM = "PM"

    /** dd/MM/yyyy */
    fun corta(fecha: Date?): String =
        if (fecha == null) Idioma.texto(R.string.fecha_sin_fecha) else SimpleDateFormat("dd/MM/yyyy", Idioma.locale).format(fecha)

    /** "29/11/2026 - 3:45 PM" */
    fun conHora(fecha: Date?): String =
        if (fecha == null) Idioma.texto(R.string.fecha_sin_fecha) else formatoConHora().format(fecha)

    /** Formato de [conHora], para las pantallas que formatean muchas fechas seguidas. */
    fun formatoConHora(locale: Locale = Idioma.locale): SimpleDateFormat =
        SimpleDateFormat("dd/MM/yyyy - h:mm a", DateFormatSymbols(locale).apply { amPmStrings = arrayOf(AM, PM) })

    /** Hora del horario de atención, guardada como "14:30", en formato de 12 horas: "2:30 PM". */
    fun hora(horaMinuto: String): String {
        val partes = horaMinuto.trim().split(":")
        val hora = partes.getOrNull(0)?.toIntOrNull()?.takeIf { it in 0..23 } ?: return horaMinuto
        val minuto = partes.getOrNull(1)?.toIntOrNull()?.takeIf { it in 0..59 } ?: return horaMinuto
        val hora12 = if (hora % 12 == 0) 12 else hora % 12
        return String.format(Locale.US, "%d:%02d %s", hora12, minuto, if (hora < 12) AM else PM)
    }

    /** yyyy-MM-dd, para nombres de archivo. */
    fun paraArchivo(fecha: Date): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(fecha)

    /** Las 00:00:00 del día local de [fecha]. */
    fun inicioDelDia(fecha: Date): Date {
        val calendario = Calendar.getInstance().apply { time = fecha }
        calendario.set(Calendar.HOUR_OF_DAY, 0)
        calendario.set(Calendar.MINUTE, 0)
        calendario.set(Calendar.SECOND, 0)
        calendario.set(Calendar.MILLISECOND, 0)
        return calendario.time
    }

    /** true si ambas fechas caen en el mismo día local. */
    fun mismoDia(a: Date, b: Date): Boolean = inicioDelDia(a) == inicioDelDia(b)

    /**
     * El DatePicker de Material 3 entrega la fecha elegida como medianoche UTC.
     * Aquí se convierte al final de ese mismo día (23:59:59) en la hora local,
     * para que una fecha límite cubra el día completo.
     */
    fun finDelDiaLocalDesdeUtc(utcMillis: Long): Date {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
        val local = Calendar.getInstance()
        local.clear()
        local.set(
            utc.get(Calendar.YEAR),
            utc.get(Calendar.MONTH),
            utc.get(Calendar.DAY_OF_MONTH),
            23, 59, 59
        )
        return local.time
    }

    /** Inversa de [finDelDiaLocalDesdeUtc]: medianoche UTC del día local de [fecha]. */
    fun utcMedianocheDesdeLocal(fecha: Date): Long {
        val local = Calendar.getInstance().apply { time = fecha }
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utc.clear()
        utc.set(
            local.get(Calendar.YEAR),
            local.get(Calendar.MONTH),
            local.get(Calendar.DAY_OF_MONTH),
            0, 0, 0
        )
        return utc.timeInMillis
    }
}
