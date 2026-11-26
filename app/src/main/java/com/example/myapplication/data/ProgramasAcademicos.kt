package com.example.myapplication.data

import com.example.myapplication.R
import com.example.myapplication.utils.Idioma

/**
 * Programas profesionales de la sede regional Ipiales, para el desplegable del registro.
 * Fuente: Diario del Sur, "Universidad de Nariño consolida su oferta académica en Ipiales".
 * Si la sede abre o cierra programas, basta con actualizar esta lista.
 */
object ProgramasAcademicos {

    /** Opción para estudiantes de programas que no están en la lista (se escribe a mano). */
    const val OTRO = "Otro programa"

    val SEDE_IPIALES: List<String> = listOf(
        "Administración de Empresas",
        "Arquitectura",
        "Comercio Internacional",
        "Ingeniería de Sistemas",
        "Mercadeo"
    )

    val opciones: List<String> = SEDE_IPIALES + OTRO

    /**
     * Nombre para mostrar en el idioma de la app. En Firestore se guarda en español, como se
     * eligió; si el estudiante lo escribió a mano (con "Otro programa"), se muestra tal cual.
     */
    fun etiqueta(programa: String): String = when (programa) {
        "Administración de Empresas" -> Idioma.texto(R.string.programa_administracion)
        "Arquitectura" -> Idioma.texto(R.string.programa_arquitectura)
        "Comercio Internacional" -> Idioma.texto(R.string.programa_comercio)
        "Ingeniería de Sistemas" -> Idioma.texto(R.string.programa_sistemas)
        "Mercadeo" -> Idioma.texto(R.string.programa_mercadeo)
        OTRO -> Idioma.texto(R.string.programa_otro)
        else -> programa
    }
}
