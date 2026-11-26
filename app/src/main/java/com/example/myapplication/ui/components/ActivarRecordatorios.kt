package com.example.myapplication.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.myapplication.data.PreferenciasRepository
import com.example.myapplication.notificaciones.ProgramadorRecordatorios

/**
 * RF-19: al entrar al inicio (monitor o estudiante) deja lista la revisión diaria de préstamos.
 * Si las notificaciones están activas en Ajustes, pide el permiso (Android 13+) y la programa;
 * si están apagadas, la cancela. Sin permiso, el aviso dentro de la app sigue funcionando.
 */
@Composable
fun ActivarRecordatorios() {
    val contexto = LocalContext.current
    val permiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(Unit) {
        val activas = PreferenciasRepository(contexto).notificacionesActivas()
        if (activas && faltaPermisoDeNotificaciones(contexto)) {
            permiso.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        ProgramadorRecordatorios.sincronizar(contexto.applicationContext)
    }
}

/** true en Android 13 o superior cuando el usuario todavía no permite notificaciones. */
fun faltaPermisoDeNotificaciones(contexto: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
