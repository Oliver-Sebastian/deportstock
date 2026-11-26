package com.example.myapplication.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await

/**
 * Documento de `monitores_autorizados`, la lista blanca de monitores. Lo crea y lo edita
 * Bienestar en Firebase Console; desde la app nadie lo puede escribir (ver firestore.rules).
 * El ID del documento es el código estudiantil o el uid del estudiante.
 */
data class AutorizacionMonitor(
    val activo: Boolean = false,
    val periodo: String = ""   // Por ejemplo "2026-2"; solo informativo
)

class MonitoresRepository {

    private val coleccion = FirebaseFirestore.getInstance().collection("monitores_autorizados")

    /**
     * Permiso de monitor del usuario, en tiempo real: el documento de su uid o el de su código
     * estudiantil, el que esté activo. Emite null si ninguno lo está (no existe, `activo` es
     * false o no se puede leer). Si Bienestar lo desactiva, emite null al instante.
     */
    fun autorizacionFlow(uid: String, codigo: String): Flow<AutorizacionMonitor?> =
        combine(ids(uid, codigo).map { documentoFlow(it) }) { autorizaciones ->
            autorizaciones.firstOrNull { it?.activo == true }
        }

    /** Lo mismo en una sola lectura (revisión diaria de recordatorios, con la app cerrada). */
    suspend fun autorizacion(uid: String, codigo: String): AutorizacionMonitor? =
        ids(uid, codigo).firstNotNullOfOrNull { id ->
            runCatching { leer(coleccion.document(id).get().await()) }.getOrNull()?.takeIf { it.activo }
        }

    private fun ids(uid: String, codigo: String): List<String> =
        listOf(uid, codigo.trim()).filter { it.isNotBlank() }.distinct()

    private fun documentoFlow(id: String): Flow<AutorizacionMonitor?> = callbackFlow {
        val registro = coleccion.document(id).addSnapshotListener { snapshot, error ->
            // Sin permiso de lectura (un código que no es suyo, reglas sin publicar): no autoriza.
            trySend(if (error != null) null else snapshot?.let { leer(it) })
        }
        awaitClose { registro.remove() }
    }

    /** Un documento mal escrito en la consola (por ejemplo, `activo` como texto) no autoriza. */
    private fun leer(documento: DocumentSnapshot): AutorizacionMonitor? =
        runCatching { documento.toObject(AutorizacionMonitor::class.java) }.getOrNull()
}
