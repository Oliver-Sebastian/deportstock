package com.example.myapplication.utils

import androidx.annotation.StringRes
import com.example.myapplication.R

/**
 * Reglas de contraseña segura para las cuentas nuevas.
 * Solo se aplican al registrarse: las cuentas existentes siguen entrando con su contraseña.
 */
object ValidacionPassword {

    const val LONGITUD_MINIMA = 8

    /** [texto] es el requisito en res/values/strings.xml; [descripcion] lo da en el idioma de la app. */
    data class Requisito(@StringRes val texto: Int, val cumple: (String) -> Boolean) {
        val descripcion: String get() = Idioma.texto(texto, LONGITUD_MINIMA)
    }

    val requisitos: List<Requisito> = listOf(
        Requisito(R.string.contrasena_req_longitud) { it.length >= LONGITUD_MINIMA },
        Requisito(R.string.contrasena_req_mayuscula) { texto -> texto.any { it.isUpperCase() } },
        Requisito(R.string.contrasena_req_minuscula) { texto -> texto.any { it.isLowerCase() } },
        Requisito(R.string.contrasena_req_numero) { texto -> texto.any { it.isDigit() } },
        Requisito(R.string.contrasena_req_especial) { texto ->
            texto.any { !it.isLetterOrDigit() && !it.isWhitespace() }
        },
        Requisito(R.string.contrasena_req_sin_espacios) { texto -> texto.isNotEmpty() && texto.none { it.isWhitespace() } }
    )

    /** Requisitos que la contraseña todavía no cumple; vacía si es segura. */
    fun faltantes(password: String): List<String> =
        requisitos.filterNot { it.cumple(password) }.map { it.descripcion }

    fun esSegura(password: String): Boolean = faltantes(password).isEmpty()
}
