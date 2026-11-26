package com.example.myapplication.notificaciones

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myapplication.R
import com.example.myapplication.data.ModoUso
import com.example.myapplication.data.MonitoresRepository
import com.example.myapplication.data.PreferenciasRepository
import com.example.myapplication.data.PrestamosRepository
import com.example.myapplication.data.UsuariosRepository
import com.example.myapplication.utils.Idioma
import com.example.myapplication.utils.Recordatorios
import com.google.firebase.auth.FirebaseAuth

/**
 * RF-19: tarea diaria de recordatorios. Revisa según el rol de quien tiene la sesión abierta:
 * - estudiante: si su préstamo vence pronto o ya venció, se lo recuerda;
 * - monitor: si hay préstamos vencidos sin devolver, le dice cuántos.
 *
 * Es una notificación local: no necesita servidor ni Cloud Functions. Si no hay sesión,
 * si se apagaron las notificaciones en Ajustes o no hay nada que avisar, no hace nada.
 */
class RecordatorioDevolucionWorker(
    contexto: Context,
    parametros: WorkerParameters
) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {
        if (!PreferenciasRepository(applicationContext).notificacionesActivas()) return Result.success()
        // Puede correr con la app cerrada: los avisos salen en el idioma guardado en Ajustes
        Idioma.iniciar(applicationContext)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()

        return try {
            val usuario = UsuariosRepository().obtenerUsuario(uid) ?: return Result.success()
            // El aviso de vencidos es para quien usa el modo monitor con permiso vigente (se
            // consulta la lista blanca: la copia del perfil puede estar atrasada). Todos, también
            // los monitores, reciben el recordatorio de sus propios préstamos.
            val autorizado = MonitoresRepository().autorizacion(uid, usuario.codigo) != null
            if (autorizado && usuario.modoActivo == ModoUso.MONITOR) avisarAlMonitor()
            avisarAlEstudiante(uid)
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "No se pudieron revisar los préstamos: ${e.message}")
            Result.retry()
        }
    }

    private suspend fun avisarAlEstudiante(uid: String) {
        val prestamo = PrestamosRepository().obtenerPrestamoActivoDe(uid)
        val mensaje = prestamo?.let { Recordatorios.mensajePara(it) } ?: return
        NotificadorRecordatorios.mostrar(applicationContext, mensaje)
    }

    private suspend fun avisarAlMonitor() {
        val vencidos = PrestamosRepository().contarPrestamosVencidos()
        val mensaje = Recordatorios.mensajeParaMonitor(vencidos) ?: return
        NotificadorRecordatorios.mostrar(
            applicationContext,
            mensaje,
            titulo = Idioma.texto(R.string.notificacion_titulo_vencidos),
            id = NotificadorRecordatorios.ID_VENCIDOS
        )
    }

    private companion object {
        const val TAG = "RecordatorioWorker"
    }
}
