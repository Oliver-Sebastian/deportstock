package com.example.myapplication.viewmodel

import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.R
import com.example.myapplication.data.AuthRepository
import com.example.myapplication.data.AutorizacionMonitor
import com.example.myapplication.data.AvisoModo
import com.example.myapplication.data.ModoUso
import com.example.myapplication.data.MonitoresRepository
import com.example.myapplication.data.PerfilRepository
import com.example.myapplication.data.Usuario
import com.example.myapplication.data.UsuariosRepository
import com.example.myapplication.utils.FotosPerfil
import com.example.myapplication.utils.SesionActual
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class SesionUiState {
    /** Se está comprobando si Firebase Auth conserva una sesión (o si es monitor). */
    object Cargando : SesionUiState()
    object SinSesion : SesionUiState()
    data class Activa(val usuario: Usuario) : SesionUiState()
}

/**
 * Sesión del usuario para toda la navegación. Al abrir la app recupera la sesión que
 * guarda Firebase Auth, así no hay que iniciar sesión cada vez y la app sobrevive a que
 * Android cierre el proceso en segundo plano.
 *
 * Mientras hay sesión escucha en Firestore:
 * - el perfil y la foto: si el usuario los cambia en Ajustes, se ve al instante en todas las
 *   pantallas;
 * - su documento en `monitores_autorizados`: el permiso de monitor sale de ahí, no del perfil.
 *   Si Bienestar lo desactiva, la sesión pasa en ese momento a estudiante (y la navegación lo
 *   saca del panel de monitor); si lo activa, aparece la opción de modo en Ajustes.
 */
class SesionViewModel(
    private val repository: AuthRepository = AuthRepository(),
    private val usuariosRepository: UsuariosRepository = UsuariosRepository(),
    private val perfilRepository: PerfilRepository = PerfilRepository(),
    private val monitoresRepository: MonitoresRepository = MonitoresRepository()
) : ViewModel() {

    // Sin sesión guardada se sabe desde el principio: la app abre directo en el ingreso, sin logo.
    private val _estado = MutableStateFlow<SesionUiState>(
        if (repository.haySesionGuardada()) SesionUiState.Cargando else SesionUiState.SinSesion
    )
    val estado: StateFlow<SesionUiState> = _estado.asStateFlow()

    /** Foto de perfil del usuario con sesión; null si no tiene. */
    private val _foto = MutableStateFlow<ImageBitmap?>(null)
    val foto: StateFlow<ImageBitmap?> = _foto.asStateFlow()

    /** Periodo de la autorización de monitor vigente ("2026-2"); null si no es monitor o no lo dice. */
    private val _periodoMonitor = MutableStateFlow<String?>(null)
    val periodoMonitor: StateFlow<String?> = _periodoMonitor.asStateFlow()

    /** Cambios de permiso o de modo: la navegación muestra el aviso y, si toca, vuelve al inicio. */
    private val _avisos = Channel<AvisoModo>(Channel.BUFFERED)
    val avisos: Flow<AvisoModo> = _avisos.receiveAsFlow()

    private var escucha: Job? = null
    private var sesionDefinida = false

    /** Última corrección del permiso enviada al perfil: si las reglas la rechazan, no se repite. */
    private var correccionEnviada: Map<String, Any>? = null

    init {
        viewModelScope.launch {
            val usuario = repository.restaurarSesion()
            // Si el usuario ya inició sesión manualmente mientras tanto, no se pisa.
            if (!sesionDefinida) establecer(usuario)
        }
    }

    fun iniciar(usuario: Usuario) = establecer(usuario)

    fun cerrarSesion() {
        repository.cerrarSesion()
        establecer(null)
    }

    /**
     * Ajustes: el monitor pasa al modo monitor o al modo estudiante. El cambio se guarda en el
     * perfil; la escucha del perfil lo recibe al instante (también sin conexión) y la navegación
     * lleva al inicio del modo elegido.
     */
    fun cambiarModo(modo: String) {
        val usuario = (_estado.value as? SesionUiState.Activa)?.usuario ?: return
        if (modo == usuario.modoActivo) return
        if (modo == ModoUso.MONITOR && !usuario.esMonitor) return
        viewModelScope.launch {
            try {
                usuariosRepository.guardarModo(usuario.id, modo)
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo cambiar el modo", e)
                _avisos.send(AvisoModo(R.string.modo_error_cambiar, irAlInicio = false))
            }
        }
    }

    private fun establecer(usuario: Usuario?) {
        sesionDefinida = true
        escucha?.cancel()
        _foto.value = null
        _periodoMonitor.value = null
        correccionEnviada = null
        if (usuario == null) {
            SesionActual.usuario = null
            _estado.value = SesionUiState.SinSesion
            return
        }
        // Hasta saber si tiene permiso de monitor se muestra la pantalla de carga: así nadie
        // entra, ni por un instante, a un panel que ya no le corresponde.
        _estado.value = SesionUiState.Cargando
        escucha = viewModelScope.launch { escuchar(usuario) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun escuchar(inicial: Usuario) = coroutineScope {
        val uid = inicial.id
        val perfil = MutableStateFlow(inicial)

        launch {
            usuariosRepository.obtenerUsuarioFlow(uid)
                .catch { /* Sin conexión o reglas sin publicar: se conserva el último perfil. */ }
                .collect { actual -> if (actual != null) perfil.value = actual }
        }
        launch {
            perfilRepository.obtenerFotoFlow(uid).collect { base64 ->
                _foto.value = withContext(Dispatchers.Default) {
                    base64?.let { FotosPerfil.decodificar(it)?.asImageBitmap() }
                }
            }
        }
        // Sin conexión y sin nada guardado, el permiso puede tardar: entonces se usa la copia del
        // perfil, y se corrige cuando llegue (las reglas del servidor deciden de todos modos).
        launch {
            delay(ESPERA_PERMISO_MS)
            if (_estado.value is SesionUiState.Cargando) {
                mostrar(ModoUso.aplicarPermiso(perfil.value, perfil.value.esMonitor))
            }
        }

        // El permiso se busca por uid y por código estudiantil; el código puede llegar después
        // (cuentas antiguas), así que la escucha se renueva si cambia.
        perfil.map { it.codigo }
            .distinctUntilChanged()
            .flatMapLatest { codigo -> monitoresRepository.autorizacionFlow(uid, codigo) }
            .combine(perfil) { autorizacion, actual -> actual to autorizacion }
            .collect { (actual, autorizacion) -> aplicar(actual, autorizacion) }
    }

    private fun aplicar(perfil: Usuario, autorizacion: AutorizacionMonitor?) {
        val autorizado = autorizacion != null
        _periodoMonitor.value = autorizacion?.periodo?.takeIf { it.isNotBlank() }
        mostrar(ModoUso.aplicarPermiso(perfil, autorizado))

        // La copia del permiso en el perfil sigue a la lista blanca: con permiso, esMonitor =
        // true; sin permiso, esMonitor = false y modo estudiante.
        val correccion = ModoUso.cambiosPendientes(perfil, autorizado)
        if (correccion.isNotEmpty() && correccion != correccionEnviada) {
            correccionEnviada = correccion
            viewModelScope.launch {
                try {
                    usuariosRepository.sincronizarPermiso(perfil.id, correccion)
                } catch (e: Exception) {
                    Log.w(TAG, "No se pudo actualizar el permiso en el perfil", e)
                }
            }
        }
    }

    private fun mostrar(usuario: Usuario) {
        val anterior = (_estado.value as? SesionUiState.Activa)?.usuario
        SesionActual.usuario = usuario
        _estado.value = SesionUiState.Activa(usuario)
        if (anterior != null) ModoUso.aviso(anterior, usuario)?.let { _avisos.trySend(it) }
    }

    private companion object {
        const val TAG = "SesionViewModel"
        const val ESPERA_PERMISO_MS = 2_500L
    }
}
