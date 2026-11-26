package com.example.myapplication.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UsuariosRepository {

    private val db = FirebaseFirestore.getInstance()
    private val coleccionUsuarios = db.collection("usuarios")

    /**
     * Estudiantes registrados, en tiempo real (RF-08). Toda cuenta es de estudiante, también
     * la de los monitores, que pueden pedir implementos como cualquiera. La sede es pequeña,
     * así que se traen todos y la búsqueda por código o nombre se hace en memoria: Firestore
     * no ofrece búsqueda por texto parcial.
     */
    fun obtenerEstudiantesFlow(): Flow<List<Usuario>> = callbackFlow {
        val registro = coleccionUsuarios
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                // Solo cuentas con el correo verificado: las de correos mal escritos no aparecen.
                val estudiantes = snapshot?.documents.orEmpty()
                    .mapNotNull { doc -> doc.toObject(Usuario::class.java)?.copy(id = doc.id) }
                    .filter { it.verificado }
                    .sortedBy { it.nombre.lowercase() }
                trySend(estudiantes)
            }
        awaitClose { registro.remove() }
    }

    /** Perfil de un usuario, en una sola lectura (lo usa la revisión diaria de recordatorios). */
    suspend fun obtenerUsuario(uid: String): Usuario? =
        coleccionUsuarios.document(uid).get().await().toObject(Usuario::class.java)?.copy(id = uid)

    /** Perfil de un usuario en tiempo real (el estudiante ve aquí si está bloqueado). */
    fun obtenerUsuarioFlow(uid: String): Flow<Usuario?> = callbackFlow {
        val registro = coleccionUsuarios.document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(Usuario::class.java)?.copy(id = uid))
            }
        awaitClose { registro.remove() }
    }

    /** Ajustes: el monitor pasa a la vista de monitor o a la de estudiante. */
    suspend fun guardarModo(uid: String, modo: String) {
        coleccionUsuarios.document(uid).update(mapOf<String, Any>("modoActivo" to modo)).await()
    }

    /**
     * Corrige la copia del permiso en el perfil para que coincida con `monitores_autorizados`
     * (ver [ModoUso.cambiosPendientes]).
     */
    suspend fun sincronizarPermiso(uid: String, cambios: Map<String, Any>) {
        if (cambios.isNotEmpty()) coleccionUsuarios.document(uid).update(cambios).await()
    }
}
