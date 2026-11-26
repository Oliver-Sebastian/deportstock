package com.example.myapplication.data

import androidx.annotation.StringRes
import com.example.myapplication.R
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Roles y modo de uso (regla de negocio 8: solo existen monitor y estudiante).
 *
 * Toda cuenta es de estudiante. Un estudiante es monitor mientras tenga un documento activo en
 * `monitores_autorizados` (lista blanca que solo edita Bienestar desde Firebase Console). Con ese
 * permiso puede alternar en Ajustes entre el modo monitor (gestionar préstamos y devoluciones) y
 * el modo estudiante (pedir implementos para sí mismo).
 */
object ModoUso {
    const val ESTUDIANTE = "estudiante"
    const val MONITOR = "monitor"

    /**
     * El perfil como lo usa la app: el permiso sale de la lista blanca ([autorizado]), no del
     * perfil. Sin permiso, el modo siempre es estudiante.
     */
    fun aplicarPermiso(perfil: Usuario, autorizado: Boolean): Usuario = perfil.copy(
        esMonitor = autorizado,
        modoActivo = if (autorizado && perfil.modoActivo == MONITOR) MONITOR else ESTUDIANTE
    )

    /**
     * Campos del perfil que hay que corregir para que su copia del permiso coincida con la
     * lista blanca (vacío si ya coincide).
     */
    fun cambiosPendientes(perfil: Usuario, autorizado: Boolean): Map<String, Any> = buildMap {
        if (perfil.esMonitor != autorizado) put("esMonitor", autorizado)
        if (!autorizado && perfil.modoActivo != ESTUDIANTE) put("modoActivo", ESTUDIANTE)
    }

    /** Aviso al pasar de [antes] a [ahora]; null si no cambió el permiso ni el modo. */
    fun aviso(antes: Usuario, ahora: Usuario): AvisoModo? = when {
        antes.esMonitor && !ahora.esMonitor ->
            AvisoModo(R.string.modo_permiso_terminado, irAlInicio = antes.enModoMonitor)
        !antes.esMonitor && ahora.esMonitor ->
            AvisoModo(R.string.modo_permiso_otorgado, irAlInicio = ahora.enModoMonitor)
        antes.enModoMonitor != ahora.enModoMonitor -> AvisoModo(
            if (ahora.enModoMonitor) R.string.modo_monitor_activado else R.string.modo_estudiante_activado,
            irAlInicio = true
        )
        else -> null
    }
}

/** Texto para el usuario y si la app debe pasar al inicio del modo nuevo. */
data class AvisoModo(@StringRes val mensaje: Int, val irAlInicio: Boolean)

/**
 * Documento de la colección `usuarios` (el ID del documento es el uid de Firebase Auth).
 * Los perfiles anteriores traen el campo `rol`, que ya no se usa (por eso se ignoran los
 * campos que no están aquí).
 */
@IgnoreExtraProperties
data class Usuario(
    val id: String = "",
    val nombre: String = "",
    val codigo: String = "",            // Código estudiantil
    val programa: String = "",          // Programa académico
    val telefono: String = "",          // Celular de contacto: 10 dígitos sin +57 (ver utils/Telefonos)
    val correo: String = "",
    /**
     * Copia del permiso de monitor. La fuente es `monitores_autorizados`: la app la mantiene al
     * día y las reglas de Firestore nunca la usan para dar permisos.
     */
    val esMonitor: Boolean = false,
    /** Vista que usa ahora: [ModoUso.ESTUDIANTE] o [ModoUso.MONITOR] (este, solo con permiso). */
    val modoActivo: String = ModoUso.ESTUDIANTE,
    val bloqueado: Boolean = false,     // true cuando tiene un daño sin resolver
    val motivoBloqueo: String = "",
    val prestamoActivoId: String = "",  // ID del préstamo sin devolver; vacío si no tiene
    val verificado: Boolean = false     // true desde el primer ingreso con el correo ya verificado
) {
    /** Usa la app como monitor: tiene permiso vigente y eligió ese modo en Ajustes. */
    @get:Exclude
    val enModoMonitor: Boolean
        get() = esMonitor && modoActivo == ModoUso.MONITOR
}
