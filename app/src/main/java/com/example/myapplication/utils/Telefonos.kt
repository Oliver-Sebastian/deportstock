package com.example.myapplication.utils

import com.example.myapplication.R
/**
 * Números de teléfono de Colombia. Desde 2021 todos tienen 10 dígitos:
 * celulares que empiezan por 3 y fijos que empiezan por 60.
 * Se guardan solo los 10 dígitos (sin +57 ni espacios) y se muestran como "300 123 4567".
 */
object Telefonos {

    const val DIGITOS = 10
    private const val INDICATIVO = "57"

    /** Deja solo los dígitos y quita el indicativo de Colombia si viene incluido. */
    fun normalizar(texto: String): String {
        val digitos = texto.filter { it.isDigit() }
        return if (digitos.length == DIGITOS + INDICATIVO.length && digitos.startsWith(INDICATIVO)) {
            digitos.substring(INDICATIVO.length)
        } else {
            digitos
        }
    }

    fun esValido(texto: String): Boolean {
        val numero = normalizar(texto)
        return numero.length == DIGITOS && (numero.startsWith("3") || numero.startsWith("60"))
    }

    /** Los celulares también reciben WhatsApp; los fijos no. */
    fun esCelular(texto: String): Boolean {
        val numero = normalizar(texto)
        return numero.length == DIGITOS && numero.startsWith("3")
    }

    /** "3001234567" -> "300 123 4567". Si no es un número válido lo devuelve tal cual. */
    fun formatear(texto: String): String {
        val numero = normalizar(texto)
        if (numero.length != DIGITOS) return texto.trim()
        return "${numero.substring(0, 3)} ${numero.substring(3, 6)} ${numero.substring(6)}"
    }

    /** Para el marcador del teléfono (no necesita permisos: solo abre la app de llamadas). */
    fun uriLlamada(texto: String): String = "tel:+$INDICATIVO${normalizar(texto)}"

    fun enlaceWhatsApp(texto: String): String = "https://wa.me/$INDICATIVO${normalizar(texto)}"

    /** Mensaje para el formulario; null si el número es válido. */
    fun error(texto: String): String? = when {
        texto.isBlank() -> Idioma.texto(R.string.telefono_escribe)
        normalizar(texto).length != DIGITOS -> Idioma.texto(R.string.telefono_digitos, DIGITOS)
        !esValido(texto) -> Idioma.texto(R.string.telefono_prefijo)
        else -> null
    }
}
