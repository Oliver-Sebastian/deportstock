package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.R
import com.example.myapplication.data.InventarioRepository
import com.example.myapplication.data.Prestamo
import com.example.myapplication.data.PrestamosRepository
import com.example.myapplication.data.ResultadoDevolucion
import com.example.myapplication.data.UsuariosRepository
import com.example.myapplication.utils.Idioma
import com.example.myapplication.utils.SesionActual
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DevolucionUiState(
    val cargando: Boolean = true,
    val prestamos: List<Prestamo> = emptyList(),
    val procesando: Boolean = false,
    val mensajeError: String? = null,
    /** Celular actual de cada estudiante (uid -> número), para contactarlo desde la tarjeta. */
    val telefonos: Map<String, String> = emptyMap(),
    /** Categoría de cada artículo (código -> categoría): define si puede volver "incompleto". */
    val categorias: Map<String, String> = emptyMap()
) {
    /** El celular actual del perfil; si no se pudo leer, el que se copió al prestar. */
    fun telefonoDe(prestamo: Prestamo): String =
        telefonos[prestamo.estudianteId]?.takeIf { it.isNotBlank() } ?: prestamo.telefono
}

/** Préstamos activos, vencidos e historial (RF-13), devoluciones (RF-11) y daños. */
class DevolucionViewModel(
    private val repository: PrestamosRepository = PrestamosRepository(),
    private val usuariosRepository: UsuariosRepository = UsuariosRepository(),
    private val inventarioRepository: InventarioRepository = InventarioRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DevolucionUiState())
    val uiState: StateFlow<DevolucionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.obtenerPrestamosFlow()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            cargando = false,
                            mensajeError = Idioma.texto(
                                R.string.devolucion_error_cargar,
                                error.localizedMessage ?: Idioma.texto(R.string.comun_error_desconocido)
                            )
                        )
                    }
                }
                .collect { lista -> _uiState.update { it.copy(cargando = false, prestamos = lista) } }
        }
        // Categorías de los artículos (incluye los dados de baja, que pueden estar prestados).
        viewModelScope.launch {
            inventarioRepository.obtenerInventarioFlow(incluirInactivos = true)
                .catch { /* Sin categorías se muestran todas las opciones; el repositorio valida. */ }
                .collect { items ->
                    _uiState.update { estado -> estado.copy(categorias = items.associate { it.id to it.categoria }) }
                }
        }
        // Celulares en vivo: si el estudiante lo actualiza en Ajustes, el monitor ve el nuevo.
        viewModelScope.launch {
            usuariosRepository.obtenerEstudiantesFlow()
                .catch { /* Sin la lista se usa el celular copiado en el préstamo. */ }
                .collect { estudiantes ->
                    _uiState.update { estado -> estado.copy(telefonos = estudiantes.associate { it.id to it.telefono }) }
                }
        }
    }

    fun registrarDevolucion(
        prestamoId: String,
        resultados: List<ResultadoDevolucion>,
        observaciones: String,
        onResultado: (Result<String>) -> Unit
    ) {
        procesar(onResultado) {
            repository.registrarDevolucion(prestamoId, resultados, observaciones, SesionActual.usuario)
        }
    }

    fun resolverDano(prestamoId: String, nota: String, onResultado: (Result<String>) -> Unit) {
        procesar(onResultado) { repository.resolverDano(prestamoId, nota) }
    }

    fun limpiarError() {
        _uiState.update { it.copy(mensajeError = null) }
    }

    private fun procesar(onResultado: (Result<String>) -> Unit, operacion: suspend () -> Result<String>) {
        if (_uiState.value.procesando) return
        _uiState.update { it.copy(procesando = true) }
        viewModelScope.launch {
            val resultado = operacion()
            _uiState.update { it.copy(procesando = false) }
            onResultado(resultado)
        }
    }
}
