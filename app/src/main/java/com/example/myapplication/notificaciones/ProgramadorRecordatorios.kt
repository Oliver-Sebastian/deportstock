package com.example.myapplication.notificaciones

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.data.PreferenciasRepository
import com.example.myapplication.utils.Idioma
import java.util.concurrent.TimeUnit

/**
 * Programa y cancela la revisión diaria del recordatorio de devolución (RF-19).
 * La misma revisión sirve a los dos roles: al estudiante le avisa de su préstamo y al
 * monitor de los préstamos vencidos. Se puede apagar desde Ajustes.
 */
object ProgramadorRecordatorios {

    private const val NOMBRE_TRABAJO = "recordatorio_devolucion"

    /**
     * Deja programada la revisión una vez al día. Si ya estaba programada no se duplica.
     * La primera revisión se ejecuta poco después de programarla.
     */
    fun programar(contexto: Context) {
        val solicitud = PeriodicWorkRequestBuilder<RecordatorioDevolucionWorker>(1, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(contexto).enqueueUniquePeriodicWork(
            NOMBRE_TRABAJO,
            ExistingPeriodicWorkPolicy.KEEP,
            solicitud
        )
    }

    /**
     * Programa la revisión si las notificaciones están activas en Ajustes, o la cancela si no.
     * Se llama al entrar a la app y cada vez que se cambia el interruptor.
     */
    fun sincronizar(contexto: Context) {
        if (PreferenciasRepository(contexto).notificacionesActivas()) programar(contexto) else cancelar(contexto)
    }

    /** Se llama al cerrar sesión para que el teléfono deje de revisar. */
    fun cancelar(contexto: Context) {
        WorkManager.getInstance(contexto).cancelUniqueWork(NOMBRE_TRABAJO)
    }
}

/** Muestra la notificación del recordatorio. */
object NotificadorRecordatorios {

    private const val CANAL = "recordatorios_devolucion"
    private const val ID_NOTIFICACION = 1001

    /** Aviso de préstamos vencidos del monitor: aparte, para no tapar el recordatorio propio. */
    const val ID_VENCIDOS = 1002

    fun mostrar(
        contexto: Context,
        mensaje: String,
        titulo: String = Idioma.texto(R.string.notificacion_titulo),
        id: Int = ID_NOTIFICACION
    ) {
        val gestor = contexto.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Sin permiso de notificaciones (Android 13+) no se muestra nada; el aviso dentro de la app sí.
        if (!gestor.areNotificationsEnabled()) return

        // Se puede llamar siempre: si el canal ya existe, solo se actualizan su nombre y su
        // descripción (así quedan en el idioma elegido en Ajustes).
        val canal = NotificationChannel(
            CANAL,
            Idioma.texto(R.string.notificacion_canal),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = Idioma.texto(R.string.notificacion_canal_descripcion)
        }
        gestor.createNotificationChannel(canal)

        val abrirApp = PendingIntent.getActivity(
            contexto,
            0,
            Intent(contexto, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificacion = NotificationCompat.Builder(contexto, CANAL)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
            .setContentIntent(abrirApp)
            .setAutoCancel(true)
            .build()

        gestor.notify(id, notificacion)
    }
}
