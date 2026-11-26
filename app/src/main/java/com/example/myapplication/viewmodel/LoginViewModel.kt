package com.example.myapplication.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.R
import com.example.myapplication.data.AuthRepository
import com.example.myapplication.data.CorreoNoVerificadoException
import com.example.myapplication.data.Usuario
import com.example.myapplication.utils.Idioma
import com.example.myapplication.utils.Telefonos
import com.example.myapplication.utils.ValidacionPassword
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LoginUiState {
    object Inactivo : LoginUiState()
    object Cargando : LoginUiState()
    data class Exito(val usuario: Usuario) : LoginUiState()
    data class RegistroPendienteVerificacion(val correo: String) : LoginUiState()
    data class RecuperacionEnviada(val correo: String) : LoginUiState()
    /** Intentó entrar sin verificar el correo: la pantalla ofrece reenviar el enlace. */
    data class CorreoNoVerificado(val correo: String) : LoginUiState()
    data class VerificacionReenviada(val correo: String) : LoginUiState()
    data class Error(val mensaje: String) : LoginUiState()
}

/** Valida que el correo tenga un formato real (usuario@dominio.ext). */
private fun esCorreoValido(correo: String): Boolean =
    Patterns.EMAIL_ADDRESS.matcher(correo.trim()).matches()

/** Traduce los errores de Firebase a mensajes claros. También lo usa Ajustes. */
internal fun traducirErrorFirebase(e: Throwable): String {
    val msg = e.message ?: ""
    return when {
        msg.contains("PERMISSION_DENIED", ignoreCase = true) ->
            Idioma.texto(R.string.error_perfil_permisos)
        msg.contains("offline", ignoreCase = true) ->
            Idioma.texto(R.string.error_sin_conexion)
        msg.contains("password is invalid", ignoreCase = true) ||
        msg.contains("wrong-password", ignoreCase = true) ||
        msg.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ->
            Idioma.texto(R.string.error_credenciales)
        msg.contains("user-not-found", ignoreCase = true) ||
        msg.contains("no user record", ignoreCase = true) ->
            Idioma.texto(R.string.error_sin_cuenta)
        msg.contains("email-already-in-use", ignoreCase = true) ->
            Idioma.texto(R.string.error_cuenta_existe)
        msg.contains("network error", ignoreCase = true) ||
        msg.contains("UNAVAILABLE", ignoreCase = true) ->
            Idioma.texto(R.string.error_red)
        msg.contains("too-many-requests", ignoreCase = true) ||
        msg.contains("TOO_MANY_ATTEMPTS", ignoreCase = true) ||
        msg.contains("unusual activity", ignoreCase = true) ->
            Idioma.texto(R.string.error_demasiados_intentos)
        msg.contains("WEAK_PASSWORD", ignoreCase = true) ->
            Idioma.texto(R.string.error_contrasena_debil)
        msg.contains("badly formatted", ignoreCase = true) ->
            Idioma.texto(R.string.error_correo_formato)
        msg.isNotBlank() -> msg
        else -> Idioma.texto(R.string.error_iniciar_sesion)
    }
}

class LoginViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Inactivo)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(correo: String, password: String) {
        if (correo.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error(Idioma.texto(R.string.ingreso_completa))
            return
        }
        if (!esCorreoValido(correo)) {
            _uiState.value = LoginUiState.Error(Idioma.texto(R.string.comun_correo_valido))
            return
        }
        _uiState.value = LoginUiState.Cargando
        viewModelScope.launch {
            val resultado = repository.login(correo, password)
            _uiState.value = resultado.fold(
                onSuccess = { LoginUiState.Exito(it) },
                onFailure = { error ->
                    if (error is CorreoNoVerificadoException) {
                        LoginUiState.CorreoNoVerificado(error.correo)
                    } else {
                        LoginUiState.Error(traducirErrorFirebase(error))
                    }
                }
            )
        }
    }

    /** Reenvía el enlace de verificación (necesita la contraseña para iniciar sesión un momento). */
    fun reenviarVerificacion(correo: String, password: String) {
        if (correo.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error(Idioma.texto(R.string.ingreso_para_reenviar))
            return
        }
        _uiState.value = LoginUiState.Cargando
        viewModelScope.launch {
            val resultado = repository.reenviarVerificacion(correo, password)
            _uiState.value = resultado.fold(
                onSuccess = { LoginUiState.VerificacionReenviada(correo.trim()) },
                onFailure = { LoginUiState.Error(traducirErrorFirebase(it)) }
            )
        }
    }

    /**
     * RF-02: registro de un estudiante (regla de negocio 6: el estudiante se autoregistra).
     * Toda cuenta creada aquí queda con rol ESTUDIANTE. Un monitor se habilita cambiando
     * el campo `rol` de su usuario a MONITOR desde Firebase Console.
     */
    fun registrar(
        nombre: String,
        codigo: String,
        programa: String,
        telefono: String,
        correo: String,
        password: String,
        repetirPassword: String = password,
        confirmarCorreo: String = correo
    ) {
        if (nombre.isBlank() || codigo.isBlank() || programa.isBlank() || telefono.isBlank() ||
            correo.isBlank() || password.isBlank() || repetirPassword.isBlank()
        ) {
            _uiState.value = LoginUiState.Error(Idioma.texto(R.string.registro_completa))
            return
        }
        if (nombre.trim().length < 5) {
            _uiState.value = LoginUiState.Error(Idioma.texto(R.string.comun_escribe_nombre))
            return
        }
        val codigoLimpio = codigo.trim()
        if (codigoLimpio.length !in 4..15 || !codigoLimpio.all { it.isLetterOrDigit() }) {
            _uiState.value = LoginUiState.Error(Idioma.texto(R.string.registro_codigo_invalido))
            return
        }
        // El celular sirve para que Bienestar contacte al estudiante (préstamos vencidos, novedades).
        Telefonos.error(telefono)?.let { mensaje ->
            _uiState.value = LoginUiState.Error(mensaje)
            return
        }
        if (!esCorreoValido(correo)) {
            _uiState.value = LoginUiState.Error(Idioma.texto(R.string.comun_correo_valido))
            return
        }
        // Un correo mal escrito impide verificar la cuenta: se pide escribirlo dos veces.
        if (!correo.trim().equals(confirmarCorreo.trim(), ignoreCase = true)) {
            _uiState.value = LoginUiState.Error(Idioma.texto(R.string.registro_correos_no_coinciden))
            return
        }
        val faltantes = ValidacionPassword.faltantes(password)
        if (faltantes.isNotEmpty()) {
            _uiState.value = LoginUiState.Error(
                Idioma.texto(R.string.contrasena_necesita, faltantes.joinToString(", ").lowercase())
            )
            return
        }
        if (password != repetirPassword) {
            _uiState.value = LoginUiState.Error(Idioma.texto(R.string.comun_contrasenas_no_coinciden))
            return
        }
        _uiState.value = LoginUiState.Cargando
        viewModelScope.launch {
            val nuevoUsuario = Usuario(
                nombre = nombre.trim(),
                codigo = codigoLimpio,
                programa = programa.trim(),
                telefono = Telefonos.normalizar(telefono),
                correo = correo.trim()
            )
            val resultado = repository.registrarEstudiante(nuevoUsuario, password)
            _uiState.value = resultado.fold(
                onSuccess = { LoginUiState.RegistroPendienteVerificacion(correo.trim()) },
                onFailure = { LoginUiState.Error(traducirErrorFirebase(it)) }
            )
        }
    }

    /** RF-03: envía el correo para restablecer la contraseña. */
    fun recuperarPassword(correo: String) {
        if (!esCorreoValido(correo)) {
            _uiState.value = LoginUiState.Error(Idioma.texto(R.string.ingreso_escribe_correo_recuperacion))
            return
        }
        _uiState.value = LoginUiState.Cargando
        viewModelScope.launch {
            val resultado = repository.recuperarPassword(correo)
            _uiState.value = resultado.fold(
                onSuccess = { LoginUiState.RecuperacionEnviada(correo.trim()) },
                onFailure = { LoginUiState.Error(traducirErrorFirebase(it)) }
            )
        }
    }

    /** Reinicia el estado, útil al volver a la pantalla de login tras el registro. */
    fun reiniciarEstado() {
        _uiState.value = LoginUiState.Inactivo
    }
}
