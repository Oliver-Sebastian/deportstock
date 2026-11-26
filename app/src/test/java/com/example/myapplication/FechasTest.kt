package com.example.myapplication

import com.example.myapplication.utils.Fechas
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.Locale

/** Las horas se muestran en formato de 12 horas con "AM" y "PM", en español y en inglés. */
class FechasTest {

    @Test
    fun horaDelHorarioEnFormatoDe12Horas() {
        assertEquals("12:00 AM", Fechas.hora("00:00"))
        assertEquals("8:00 AM", Fechas.hora("08:00"))
        assertEquals("11:59 AM", Fechas.hora("11:59"))
        assertEquals("12:00 PM", Fechas.hora("12:00"))
        assertEquals("2:30 PM", Fechas.hora("14:30"))
        assertEquals("11:05 PM", Fechas.hora("23:05"))
    }

    @Test
    fun unaHoraMalGuardadaSeMuestraTalCual() {
        assertEquals("25:00", Fechas.hora("25:00"))
        assertEquals("", Fechas.hora(""))
        assertEquals("mañana", Fechas.hora("mañana"))
    }

    @Test
    fun fechaConHoraUsaAmYPmEnLosDosIdiomas() {
        val tarde = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 29, 15, 45, 0) }.time
        val manana = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 29, 9, 5, 0) }.time
        for (locale in listOf(Locale.forLanguageTag("es"), Locale.ENGLISH)) {
            assertEquals("29/11/2026 - 3:45 PM", Fechas.formatoConHora(locale).format(tarde))
            assertEquals("29/11/2026 - 9:05 AM", Fechas.formatoConHora(locale).format(manana))
        }
    }
}
