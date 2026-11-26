package com.example.myapplication.data

import com.example.myapplication.R
import com.example.myapplication.utils.Idioma
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Datos que cada usuario edita de su propio perfil desde Ajustes:
 * nombre, programa y teléfono (colección `usuarios`) y la foto (colección `fotosPerfil`).
 *
 * La foto va en un documento aparte, `fotosPerfil/{uid}` con el campo `imagen` (JPEG en
 * Base64), para que la lista de estudiantes del monitor no descargue todas las fotos.
 */
class PerfilRepository {

    private val db = FirebaseFirestore.getInstance()
    private val coleccionUsuarios = db.collection("usuarios")
    private val coleccionFotos = db.collection("fotosPerfil")

    suspend fun actualizarDatos(uid: String, nombre: String, programa: String, telefono: String): Result<Unit> =
        try {
            coleccionUsuarios.document(uid).update(
                mapOf(
                    "nombre" to nombre,
                    "programa" to programa,
                    "telefono" to telefono
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            fallaPerfil(e, Idioma.texto(R.string.comun_error_actualizar_info))
        }

    /** Foto en tiempo real (Base64), o null si no tiene. Un error se trata como "sin foto". */
    fun obtenerFotoFlow(uid: String): Flow<String?> = callbackFlow {
        val registro = coleccionFotos.document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(null)
                return@addSnapshotListener
            }
            trySend(snapshot?.getString(CAMPO_IMAGEN))
        }
        awaitClose { registro.remove() }
    }

    /** Lectura puntual: el monitor ve la foto del estudiante al prestar. */
    suspend fun obtenerFoto(uid: String): String? = try {
        coleccionFotos.document(uid).get().await().getString(CAMPO_IMAGEN)
    } catch (e: Exception) {
        null
    }

    suspend fun guardarFoto(uid: String, imagenBase64: String): Result<Unit> = try {
        coleccionFotos.document(uid).set(
            mapOf(
                CAMPO_IMAGEN to imagenBase64,
                "actualizada" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: Exception) {
        fallaPerfil(e, Idioma.texto(R.string.foto_error_guardar))
    }

    suspend fun quitarFoto(uid: String): Result<Unit> = try {
        coleccionFotos.document(uid).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        fallaPerfil(e, Idioma.texto(R.string.foto_error_quitar))
    }

    /**
     * Un PERMISSION_DENIED aquí casi siempre significa que en Firebase siguen publicadas las
     * reglas anteriores, que no conocen `fotosPerfil` ni el campo `telefono`. El rol no influye:
     * cada quien puede cambiar su propia foto y su información.
     */
    private fun <T> fallaPerfil(error: Throwable, porDefecto: String): Result<T> =
        if (error is FirebaseFirestoreException && error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
            Result.failure(
                Exception(Idioma.texto(R.string.perfil_reglas), error)
            )
        } else {
            falla(error, porDefecto)
        }

    private companion object {
        const val CAMPO_IMAGEN = "imagen"
    }
}
