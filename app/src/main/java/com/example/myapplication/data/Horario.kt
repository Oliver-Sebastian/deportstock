package com.example.myapplication.data

import com.example.myapplication.R
import com.example.myapplication.utils.Idioma
import com.google.firebase.Timestamp

/** Documento de la colección `horarios`: una franja de atención de Bienestar Universitario. */
data class Horario(
    val id: String = "",
    val diaSemana: Int = 1,           // 1 = lunes ... 7 = domingo
    val horaInicio: String = "08:00", // Formato 24 h "HH:mm"
    val horaFin: String = "12:00"
)

object DiasSemana {
    /** Nombre de cada día, de lunes (1) a domingo (7), en res/values/strings.xml. */
    private val nombres = listOf(
        R.string.dia_lunes, R.string.dia_martes, R.string.dia_miercoles, R.string.dia_jueves,
        R.string.dia_viernes, R.string.dia_sabado, R.string.dia_domingo
    )

    /** Días en que Bienestar puede atender: de lunes (1) a sábado (6). El domingo no se ofrece. */
    val DIAS_ATENCION = 1..6

    /** Nombre del día en el idioma de la app ("Lunes" o "Monday"). */
    fun nombre(dia: Int): String =
        nombres.getOrNull(dia - 1)?.let { Idioma.texto(it) } ?: Idioma.texto(R.string.dia_numero, dia)
}

/** Documento `configuracion/general`. */
data class ConfiguracionGeneral(
    val fechaFinSemestre: Timestamp? = null   // Tope de la fecha límite de los préstamos (regla de negocio 2)
)
