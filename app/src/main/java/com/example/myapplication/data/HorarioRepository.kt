package com.example.myapplication.data

import com.example.myapplication.R
import com.example.myapplication.utils.Idioma
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

/** Horario de atención de Bienestar (colección `horarios`) y configuración general. */
class HorarioRepository {

    private val db = FirebaseFirestore.getInstance()
    private val coleccionHorarios = db.collection("horarios")
    private val configuracionGeneral = db.collection("configuracion").document("general")

    /** RF-14: franjas de atención en tiempo real, ordenadas por día y hora. */
    fun obtenerHorariosFlow(): Flow<List<Horario>> = callbackFlow {
        val registro = coleccionHorarios.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val horarios = snapshot?.documents.orEmpty()
                .mapNotNull { doc -> doc.toObject(Horario::class.java)?.copy(id = doc.id) }
                .sortedWith(compareBy({ it.diaSemana }, { it.horaInicio }))
            trySend(horarios)
        }
        awaitClose { registro.remove() }
    }

    /** Crea una franja nueva (id vacío) o actualiza una existente. */
    suspend fun guardarHorario(horario: Horario): Result<String> {
        return try {
            val referencia = if (horario.id.isBlank()) {
                coleccionHorarios.document()
            } else {
                coleccionHorarios.document(horario.id)
            }
            referencia.set(horario.copy(id = referencia.id)).await()
            Result.success(Idioma.texto(R.string.repo_hor_guardado))
        } catch (e: Exception) {
            falla(e, Idioma.texto(R.string.repo_hor_error_guardar))
        }
    }

    suspend fun eliminarHorario(id: String): Result<String> {
        return try {
            coleccionHorarios.document(id).delete().await()
            Result.success(Idioma.texto(R.string.repo_hor_franja_eliminada))
        } catch (e: Exception) {
            falla(e, Idioma.texto(R.string.repo_hor_error_eliminar))
        }
    }

    /** Configuración general en tiempo real (emite valores por defecto si aún no existe). */
    fun obtenerConfiguracionFlow(): Flow<ConfiguracionGeneral> = callbackFlow {
        val registro = configuracionGeneral.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.toObject(ConfiguracionGeneral::class.java) ?: ConfiguracionGeneral())
        }
        awaitClose { registro.remove() }
    }

    /** Regla de negocio 2: fecha de fin de semestre, tope de la fecha límite de los préstamos. */
    suspend fun guardarFechaFinSemestre(fecha: Date): Result<String> {
        return try {
            configuracionGeneral.set(ConfiguracionGeneral(fechaFinSemestre = Timestamp(fecha))).await()
            Result.success(Idioma.texto(R.string.repo_hor_semestre_guardado))
        } catch (e: Exception) {
            falla(e, Idioma.texto(R.string.repo_hor_error_semestre))
        }
    }
}
