package com.example.myapplication.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.R
import com.example.myapplication.data.AuthRepository
import com.example.myapplication.data.IdiomaApp
import com.example.myapplication.data.PerfilRepository
import com.example.myapplication.data.PreferenciasRepository
import com.example.myapplication.data.TemaApp
import com.example.myapplication.notificaciones.ProgramadorRecordatorios
import com.example.myapplication.ui.theme.Colores
import com.example.myapplication.utils.Idioma
import com.example.myapplication.utils.Telefonos
import com.example.myapplication.utils.ValidacionPassword
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AjustesUiState(
    val tema: TemaApp = TemaApp.OSCURO,
    val notificaciones: Boolean = true,
    val cambioPassword: EstadoFormulario = EstadoFormulario.Inactivo,
    val actualizacion: EstadoFormulario = EstadoFormulario.Inactivo
)

/**
 * Ajustes, común a monitor y estudiante. Las preferencias (tema, idioma y notificaciones) se guardan
 * en el teléfono con [PreferenciasRepository]; la contraseña se cambia en Firebase Auth y
 * los datos del perfil (nombre, programa, teléfono) en Firestore con [PerfilRepository].
 * Usa AndroidViewModel porque necesita el contexto de la app para las preferencias.
 */
class AjustesViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val preferencias = PreferenciasRepository(application)
    private val authRepository = AuthRepository()
    private val perfilRepository = PerfilRepository()

    private val _uiState = MutableStateFlow(
        AjustesUiState(
            tema = preferencias.tema(),
            notificaciones = preferencias.notificacionesActivas()
        )
    )
    val uiState: StateFlow<AjustesUiState> = _uiState.asStateFlow()

    /** Guarda el tema y lo aplica en toda la app al instante. */
    fun cambiarTema(tema: TemaApp) {
        preferencias.guardarTema(tema)
        Colores.tema = tema
        _uiState.update { it.copy(tema = tema) }
    }

    /** Cambia el idioma de toda la app al instante y lo deja guardado para la próxima vez. */
    fun cambiarIdioma(idioma: IdiomaApp) {
        preferencias.guardarIdioma(idioma)
        Idioma.actual = idioma
    }

    /** Guarda la preferencia y programa o cancela la revisión diaria según el valor. */
    fun cambiarNotificaciones(activas: Boolean) {
        preferencias.guardarNotificaciones(activas)
        ProgramadorRecordatorios.sincronizar(getApplication())
        _uiState.update { it.copy(notificaciones = activas) }
    }

    fun cambiarPassword(actual: String, nueva: String, repetir: String) {
        val error = when {
            actual.isBlank() || nueva.isBlank() || repetir.isBlank() -> Idioma.texto(R.string.ajustes_completa_tres)
            ValidacionPassword.faltantes(nueva).isNotEmpty() ->
                Idioma.texto(
                    R.string.ajustes_contrasena_necesita,
                    ValidacionPassword.faltantes(nueva).joinToString(", ").lowercase()
                )
            nueva != repetir -> Idioma.texto(R.string.ajustes_contrasenas_nuevas_no_coinciden)
            nueva == actual -> Idioma.texto(R.string.ajustes_contrasena_diferente)
            else -> null
        }
        if (error != null) {
            _uiState.update { it.copy(cambioPassword = EstadoFormulario.Error(error)) }
            return
        }

        _uiState.update { it.copy(cambioPassword = EstadoFormulario.Guardando) }
        viewModelScope.launch {
            val resultado = authRepository.cambiarPassword(actual, nueva).fold(
                onSuccess = { EstadoFormulario.Exito },
                onFailure = { EstadoFormulario.Error(traducirErrorFirebase(it)) }
            )
            _uiState.update { it.copy(cambioPassword = resultado) }
        }
    }

    /** Se llama al cerrar el diálogo o después de mostrar el resultado. */
    fun reiniciarCambioPassword() {
        _uiState.update { it.copy(cambioPassword = EstadoFormulario.Inactivo) }
    }

    /**
     * "Actualizar información": nombre, programa y celular. El correo y el código no se
     * editan aquí (el correo es la cuenta y el código está reservado para el estudiante).
     * La sesión escucha el perfil, así que el cambio se ve de inmediato en toda la app.
     */
    fun actualizarInformacion(uid: String, nombre: String, programa: String, telefono: String) {
        val error = when {
            nombre.trim().length < 5 -> Idioma.texto(R.string.comun_escribe_nombre)
            programa.isBlank() -> Idioma.texto(R.string.comun_selecciona_programa_academico)
            else -> Telefonos.error(telefono)
        }
        if (error != null) {
            _uiState.update { it.copy(actualizacion = EstadoFormulario.Error(error)) }
            return
        }

        _uiState.update { it.copy(actualizacion = EstadoFormulario.Guardando) }
        viewModelScope.launch {
            val resultado = perfilRepository.actualizarDatos(
                uid = uid,
                nombre = nombre.trim(),
                programa = programa.trim(),
                telefono = Telefonos.normalizar(telefono)
            ).fold(
                onSuccess = { EstadoFormulario.Exito },
                onFailure = { EstadoFormulario.Error(it.message ?: Idioma.texto(R.string.comun_error_actualizar_info)) }
            )
            _uiState.update { it.copy(actualizacion = resultado) }
        }
    }

    fun reiniciarActualizacion() {
        _uiState.update { it.copy(actualizacion = EstadoFormulario.Inactivo) }
    }
}
