package com.example.myapplication.data

import com.example.myapplication.R
import com.example.myapplication.utils.Idioma
import com.google.firebase.firestore.FirebaseFirestoreException

/**
 * Error de regla de negocio lanzado dentro de una transacción.
 * Se usa INVALID_ARGUMENT porque Firestore lo trata como error definitivo y no
 * reintenta la transacción (con ABORTED la repetiría varias veces antes de fallar).
 */
internal fun reglaNegocio(mensaje: String): FirebaseFirestoreException =
    FirebaseFirestoreException(mensaje, FirebaseFirestoreException.Code.INVALID_ARGUMENT)

/** Convierte un error de Firestore en un mensaje que el monitor o el estudiante puedan entender. */
internal fun mensajeDeError(error: Throwable, porDefecto: String): String {
    if (error is FirebaseFirestoreException) {
        return when (error.code) {
            FirebaseFirestoreException.Code.INVALID_ARGUMENT -> error.message ?: porDefecto
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                Idioma.texto(R.string.error_permisos)
            FirebaseFirestoreException.Code.UNAVAILABLE ->
                Idioma.texto(R.string.error_sin_servidor)
            else -> error.localizedMessage ?: porDefecto
        }
    }
    return error.localizedMessage ?: porDefecto
}

/** Envuelve el error original con un mensaje legible, para devolverlo en un Result. */
internal fun <T> falla(error: Throwable, porDefecto: String): Result<T> =
    Result.failure(Exception(mensajeDeError(error, porDefecto), error))
