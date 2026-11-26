package com.example.myapplication

import com.example.myapplication.data.AvisoModo
import com.example.myapplication.data.ModoUso
import com.example.myapplication.data.Usuario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Permiso de monitor y modo de uso: el permiso sale de `monitores_autorizados` (aquí,
 * `autorizado`), nunca de la copia guardada en el perfil.
 */
class ModoUsoTest {

    private val estudiante = Usuario(id = "u1")
    private val monitorEnModoMonitor = Usuario(id = "u1", esMonitor = true, modoActivo = ModoUso.MONITOR)
    private val monitorEnModoEstudiante = Usuario(id = "u1", esMonitor = true, modoActivo = ModoUso.ESTUDIANTE)

    @Test
    fun unaCuentaNuevaEsEstudiante() {
        assertFalse(estudiante.esMonitor)
        assertEquals(ModoUso.ESTUDIANTE, estudiante.modoActivo)
        assertFalse(estudiante.enModoMonitor)
    }

    @Test
    fun sinPermisoNuncaEstaEnModoMonitor() {
        // Perfil que dice ser monitor (copia atrasada o modificada) sin autorización vigente
        val efectivo = ModoUso.aplicarPermiso(monitorEnModoMonitor, autorizado = false)
        assertFalse(efectivo.esMonitor)
        assertEquals(ModoUso.ESTUDIANTE, efectivo.modoActivo)
        assertFalse(efectivo.enModoMonitor)
    }

    @Test
    fun conPermisoSeRespetaElModoElegido() {
        assertTrue(ModoUso.aplicarPermiso(monitorEnModoMonitor, autorizado = true).enModoMonitor)
        assertFalse(ModoUso.aplicarPermiso(monitorEnModoEstudiante, autorizado = true).enModoMonitor)
        // Recién autorizado: tiene el permiso, pero sigue en modo estudiante hasta que lo cambie
        val recienAutorizado = ModoUso.aplicarPermiso(estudiante, autorizado = true)
        assertTrue(recienAutorizado.esMonitor)
        assertFalse(recienAutorizado.enModoMonitor)
    }

    @Test
    fun laCopiaDelPerfilSigueALaListaBlanca() {
        assertEquals(mapOf("esMonitor" to true), ModoUso.cambiosPendientes(estudiante, autorizado = true))
        assertEquals(
            mapOf("esMonitor" to false, "modoActivo" to ModoUso.ESTUDIANTE),
            ModoUso.cambiosPendientes(monitorEnModoMonitor, autorizado = false)
        )
        assertTrue(ModoUso.cambiosPendientes(monitorEnModoMonitor, autorizado = true).isEmpty())
        assertTrue(ModoUso.cambiosPendientes(estudiante, autorizado = false).isEmpty())
    }

    @Test
    fun alTerminarElPermisoEnModoMonitorVuelveAlInicioDelEstudiante() {
        val despues = ModoUso.aplicarPermiso(monitorEnModoMonitor, autorizado = false)
        assertEquals(AvisoModo(R.string.modo_permiso_terminado, irAlInicio = true), ModoUso.aviso(monitorEnModoMonitor, despues))
    }

    @Test
    fun alTerminarElPermisoEnModoEstudianteSoloAvisa() {
        val despues = ModoUso.aplicarPermiso(monitorEnModoEstudiante, autorizado = false)
        assertEquals(AvisoModo(R.string.modo_permiso_terminado, irAlInicio = false), ModoUso.aviso(monitorEnModoEstudiante, despues))
    }

    @Test
    fun cambiarDeModoLlevaAlInicioDelModoNuevo() {
        assertEquals(
            AvisoModo(R.string.modo_estudiante_activado, irAlInicio = true),
            ModoUso.aviso(monitorEnModoMonitor, monitorEnModoEstudiante)
        )
        assertEquals(
            AvisoModo(R.string.modo_monitor_activado, irAlInicio = true),
            ModoUso.aviso(monitorEnModoEstudiante, monitorEnModoMonitor)
        )
    }

    @Test
    fun recibirElPermisoAvisaSinCambiarDePantalla() {
        val despues = ModoUso.aplicarPermiso(estudiante, autorizado = true)
        assertEquals(AvisoModo(R.string.modo_permiso_otorgado, irAlInicio = false), ModoUso.aviso(estudiante, despues))
    }

    @Test
    fun otrosCambiosDelPerfilNoAvisan() {
        assertNull(ModoUso.aviso(monitorEnModoMonitor, monitorEnModoMonitor.copy(nombre = "Otro nombre")))
        assertNull(ModoUso.aviso(estudiante, estudiante.copy(telefono = "3001234567")))
    }
}
