package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.R
import com.example.myapplication.data.Horario
import com.example.myapplication.data.HorarioRepository
import com.example.myapplication.data.InventarioRepository
import com.example.myapplication.data.ItemDeportivo
import com.example.myapplication.data.Prestamo
import com.example.myapplication.data.PrestamosRepository
import com.example.myapplication.data.Usuario
import com.example.myapplication.data.UsuariosRepository
import com.example.myapplication.utils.Idioma
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Un artículo del catálogo tal como lo ve el estudiante: agrupa las unidades iguales. */
data class ArticuloCatalogo(
    val nombre: String,
    val categoria: String,
    val icono: String,
    val descripcion: String,
    val total: Int,
    val disponibles: Int,
    /** Unidades que se prestan aunque les falten piezas (por ejemplo, un ajedrez). */
    val incompletos: Int = 0
)

data class EstudianteUiState(
    val cargandoCatalogo: Boolean = true,
    val catalogo: List<ArticuloCatalogo> = emptyList(),
    val cargandoPrestamos: Boolean = true,
    val prestamos: List<Prestamo> = emptyList(),
    val horarios: List<Horario> = emptyList(),
    val perfil: Usuario? = null,
    val mensajeError: String? = null
)

/** Módulo del estudiante: catálogo (RF-15), historial propio (RF-16) y horario (RF-14). */
class EstudianteViewModel(
    private val inventarioRepository: InventarioRepository = InventarioRepository(),
    private val prestamosRepository: PrestamosRepository = PrestamosRepository(),
    private val horarioRepository: HorarioRepository = HorarioRepository(),
    private val usuariosRepository: UsuariosRepository = UsuariosRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(EstudianteUiState())
    val uiState: StateFlow<EstudianteUiState> = _uiState.asStateFlow()

    private var estudianteId: String? = null
    private var trabajosPersonales: List<Job> = emptyList()

    init {
        viewModelScope.launch {
            inventarioRepository.obtenerInventarioFlow()
                .catch { error ->
                    _uiState.update { it.copy(cargandoCatalogo = false) }
                    publicarError(Idioma.texto(R.string.estudiante_error_catalogo), error)
                }
                .collect { items ->
                    _uiState.update { it.copy(cargandoCatalogo = false, catalogo = agrupar(items)) }
                }
        }
        viewModelScope.launch {
            horarioRepository.obtenerHorariosFlow()
                .catch { error -> publicarError(Idioma.texto(R.string.comun_error_cargar_horario), error) }
                .collect { lista -> _uiState.update { it.copy(horarios = lista) } }
        }
    }

    /** Empieza a escuchar los datos propios del estudiante. Se puede llamar varias veces. */
    fun cargarDatosDe(uid: String) {
        if (uid.isBlank() || uid == estudianteId) return
        estudianteId = uid
        trabajosPersonales.forEach { it.cancel() }
        trabajosPersonales = listOf(
            viewModelScope.launch {
                prestamosRepository.obtenerPrestamosDeEstudianteFlow(uid)
                    .catch { error ->
                        _uiState.update { it.copy(cargandoPrestamos = false) }
                        publicarError(Idioma.texto(R.string.estudiante_error_historial), error)
                    }
                    .collect { lista -> _uiState.update { it.copy(cargandoPrestamos = false, prestamos = lista) } }
            },
            viewModelScope.launch {
                usuariosRepository.obtenerUsuarioFlow(uid)
                    .catch { error -> publicarError(Idioma.texto(R.string.estudiante_error_perfil), error) }
                    .collect { usuario -> _uiState.update { it.copy(perfil = usuario) } }
            }
        )
    }

    fun limpiarError() {
        _uiState.update { it.copy(mensajeError = null) }
    }

    /**
     * Las unidades con código propio se llaman "Balón X (Unidad 1)", "(Unidad 2)"...
     * Al estudiante le interesa cuántas hay disponibles, no cada código, así que se agrupan.
     */
    private fun agrupar(items: List<ItemDeportivo>): List<ArticuloCatalogo> =
        items
            .groupBy { it.categoria to it.nombre.replace(SUFIJO_UNIDAD, "").trim() }
            .map { (clave, unidades) ->
                val primero = unidades.first()
                // Las piezas dañadas, incompletas, extraviadas o en reparación no cuentan en el total.
                ArticuloCatalogo(
                    nombre = clave.second,
                    categoria = clave.first,
                    icono = primero.icono,
                    descripcion = primero.descripcion,
                    total = unidades.sumOf { it.piezasEnCirculacion },
                    disponibles = unidades.sumOf { it.piezasPrestables },
                    incompletos = unidades.count { it.esIncompleto }
                )
            }
            .sortedWith(compareBy({ it.categoria }, { it.nombre }))

    private fun publicarError(prefijo: String, error: Throwable) {
        _uiState.update { it.copy(mensajeError = "$prefijo: ${error.localizedMessage ?: Idioma.texto(R.string.comun_error_desconocido)}") }
    }

    private companion object {
        val SUFIJO_UNIDAD = Regex("""\s*\(Unidad \d+\)\s*$""")
    }
}
