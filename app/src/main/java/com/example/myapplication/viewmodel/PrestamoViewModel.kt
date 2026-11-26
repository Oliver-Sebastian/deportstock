package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.R
import com.example.myapplication.data.HorarioRepository
import com.example.myapplication.data.InventarioRepository
import com.example.myapplication.data.ItemDeportivo
import com.example.myapplication.data.Prestamo
import com.example.myapplication.data.PrestamosRepository
import com.example.myapplication.data.SeleccionPrestamo
import com.example.myapplication.data.Usuario
import com.example.myapplication.data.UsuariosRepository
import com.example.myapplication.utils.Fechas
import com.example.myapplication.utils.Idioma
import com.example.myapplication.utils.SesionActual
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.myapplication.data.PerfilRepository
import com.example.myapplication.utils.FotosPerfil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext

data class PrestamoUiState(
    val estudiantes: List<Usuario> = emptyList(),
    val inventario: List<ItemDeportivo> = emptyList(),
    val prestamos: List<Prestamo> = emptyList(),
    val fechaFinSemestre: Date? = null,
    val configuracionCargada: Boolean = false,
    val guardando: Boolean = false,
    val mensajeError: String? = null,
    /** Foto del estudiante elegido, para confirmar su identidad junto con el carnet. */
    val fotoEstudiante: ImageBitmap? = null
)

/** Motivo por el que un estudiante no puede recibir un préstamo (RF-12). */
data class BloqueoEstudiante(
    val titulo: String,
    val detalle: String
)

class PrestamoViewModel(
    private val prestamosRepository: PrestamosRepository = PrestamosRepository(),
    private val inventarioRepository: InventarioRepository = InventarioRepository(),
    private val usuariosRepository: UsuariosRepository = UsuariosRepository(),
    private val horarioRepository: HorarioRepository = HorarioRepository(),
    private val perfilRepository: PerfilRepository = PerfilRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrestamoUiState())
    val uiState: StateFlow<PrestamoUiState> = _uiState.asStateFlow()

    private var cargaFoto: Job? = null

    init {
        viewModelScope.launch {
            usuariosRepository.obtenerEstudiantesFlow()
                .catch { error -> publicarError(Idioma.texto(R.string.prestamo_error_estudiantes), error) }
                // Un monitor no se registra préstamos a sí mismo: lo hace otro monitor
                .collect { lista ->
                    val propio = SesionActual.usuario?.id
                    _uiState.update { it.copy(estudiantes = lista.filter { estudiante -> estudiante.id != propio }) }
                }
        }
        viewModelScope.launch {
            inventarioRepository.obtenerInventarioFlow()
                .catch { error -> publicarError(Idioma.texto(R.string.comun_error_cargar_inventario), error) }
                .collect { lista -> _uiState.update { it.copy(inventario = lista) } }
        }
        viewModelScope.launch {
            prestamosRepository.obtenerPrestamosFlow()
                .catch { error -> publicarError(Idioma.texto(R.string.comun_error_cargar_prestamos), error) }
                .collect { lista -> _uiState.update { it.copy(prestamos = lista) } }
        }
        viewModelScope.launch {
            horarioRepository.obtenerConfiguracionFlow()
                .catch { error -> publicarError(Idioma.texto(R.string.comun_error_cargar_configuracion), error) }
                .collect { configuracion ->
                    _uiState.update {
                        it.copy(
                            fechaFinSemestre = configuracion.fechaFinSemestre?.toDate(),
                            configuracionCargada = true
                        )
                    }
                }
        }
    }

    /** Carga la foto del estudiante elegido (una sola lectura); null al quitar la selección. */
    fun cargarFotoDe(estudianteId: String?) {
        cargaFoto?.cancel()
        _uiState.update { it.copy(fotoEstudiante = null) }
        if (estudianteId.isNullOrBlank()) return
        cargaFoto = viewModelScope.launch {
            val foto = perfilRepository.obtenerFoto(estudianteId)?.let { base64 ->
                withContext(Dispatchers.Default) { FotosPerfil.decodificar(base64)?.asImageBitmap() }
            }
            _uiState.update { it.copy(fotoEstudiante = foto) }
        }
    }

    /** RF-08: busca estudiantes por código o por nombre. */
    fun buscarEstudiantes(consulta: String, limite: Int = 6): List<Usuario> {
        val texto = consulta.trim().lowercase()
        if (texto.length < 2) return emptyList()
        return _uiState.value.estudiantes
            .filter { it.codigo.lowercase().contains(texto) || it.nombre.lowercase().contains(texto) }
            .take(limite)
    }

    /**
     * RF-12: devuelve el motivo de bloqueo del estudiante, o null si puede recibir un préstamo.
     * Es la verificación que ve el monitor en pantalla; el repositorio la repite dentro de la
     * transacción para que sea definitiva.
     */
    fun bloqueoDe(estudiante: Usuario): BloqueoEstudiante? {
        val prestamos = _uiState.value.prestamos.filter { prestamo ->
            prestamo.estudianteId == estudiante.id ||
                (estudiante.codigo.isNotBlank() && prestamo.codigoEstudiante.equals(estudiante.codigo, ignoreCase = true))
        }
        val activo = prestamos.firstOrNull { it.estaActivo }
        val conDano = prestamos.firstOrNull { it.danoPendiente }

        return when {
            activo != null && activo.estaVencido() -> BloqueoEstudiante(
                titulo = Idioma.texto(R.string.prestamo_denegado_vencida),
                detalle = Idioma.texto(
                    R.string.prestamo_denegado_vencida_detalle,
                    Fechas.corta(activo.fechaEntregaEsperada?.toDate()),
                    activo.itemsNombres.joinToString().ifBlank { activo.idItem }
                )
            )
            activo != null -> BloqueoEstudiante(
                titulo = Idioma.texto(R.string.prestamo_denegado_carnet),
                detalle = Idioma.texto(
                    R.string.prestamo_denegado_carnet_detalle,
                    activo.itemsNombres.joinToString().ifBlank { activo.idItem }
                )
            )
            conDano != null || estudiante.bloqueado -> BloqueoEstudiante(
                titulo = Idioma.texto(R.string.prestamo_denegado_novedad),
                // El motivo guardado en Firestore está en español: el detalle sale del idioma de la app
                detalle = Idioma.texto(R.string.prestamo_denegado_novedad_detalle)
            )
            estudiante.prestamoActivoId.isNotBlank() -> BloqueoEstudiante(
                titulo = Idioma.texto(R.string.prestamo_denegado_carnet),
                detalle = Idioma.texto(R.string.prestamo_denegado_carnet_simple)
            )
            else -> null
        }
    }

    /** RF-09: registra el préstamo. El monitor que lo entrega es el usuario con sesión iniciada. */
    fun registrarPrestamo(
        estudiante: Usuario,
        seleccion: List<SeleccionPrestamo>,
        fechaLimite: Date,
        observaciones: String,
        onResultado: (Result<String>) -> Unit
    ) {
        if (_uiState.value.guardando) return
        _uiState.update { it.copy(guardando = true) }
        viewModelScope.launch {
            val resultado = prestamosRepository.registrarPrestamo(
                estudiante = estudiante,
                monitor = SesionActual.usuario,
                seleccion = seleccion,
                fechaLimite = fechaLimite,
                observaciones = observaciones
            )
            _uiState.update { it.copy(guardando = false) }
            onResultado(resultado)
        }
    }

    fun limpiarError() {
        _uiState.update { it.copy(mensajeError = null) }
    }

    private fun publicarError(prefijo: String, error: Throwable) {
        _uiState.update { it.copy(mensajeError = "$prefijo: ${error.localizedMessage ?: Idioma.texto(R.string.comun_error_desconocido)}") }
    }
}
