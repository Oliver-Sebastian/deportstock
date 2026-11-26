package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.R
import com.example.myapplication.data.EstadoItem
import com.example.myapplication.data.InventarioRepository
import com.example.myapplication.data.ItemDeportivo
import com.example.myapplication.data.Prestamo
import com.example.myapplication.data.PrestamosRepository
import com.example.myapplication.utils.Idioma
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InventarioUiState(
    val cargando: Boolean = true,
    val items: List<ItemDeportivo> = emptyList(),
    val itemsFiltrados: List<ItemDeportivo> = emptyList(),
    val busqueda: String = "",
    val categoriaFiltro: String = "Todas",
    val estadoFiltro: String = "Todos",
    val mensajeExito: String? = null,
    val mensajeError: String? = null,
    val totalArticulosDistintos: Int = 0,
    val totalUnidadesDisponibles: Int = 0,
    val totalUnidadesTotales: Int = 0,
    val totalUnidadesDanadas: Int = 0
)

class InventarioViewModel(
    private val repository: InventarioRepository = InventarioRepository(),
    private val prestamosRepository: PrestamosRepository = PrestamosRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventarioUiState())
    val uiState: StateFlow<InventarioUiState> = _uiState.asStateFlow()

    /** Incluye los artículos dados de baja: sus códigos no se deben volver a sugerir. */
    private var todosLosItems: List<ItemDeportivo> = emptyList()

    init {
        escucharInventario()
    }

    private fun escucharInventario() {
        viewModelScope.launch {
            // Los préstamos solo se usan para mostrar al monitor quién tiene cada unidad;
            // si esa consulta falla, el inventario se muestra igual.
            val prestamos = prestamosRepository.obtenerPrestamosFlow()
                .catch { emit(emptyList()) }

            repository.obtenerInventarioFlow(incluirInactivos = true)
                .combine(prestamos) { items, listaPrestamos -> items to listaPrestamos }
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            cargando = false,
                            mensajeError = Idioma.texto(
                                R.string.inventario_error_cargar,
                                error.localizedMessage ?: Idioma.texto(R.string.comun_desconocido)
                            )
                        )
                    }
                }
                .collect { (items, listaPrestamos) ->
                    todosLosItems = items
                    val activos = conPrestatario(items.filter { it.activo }, listaPrestamos)

                    _uiState.update { estadoActual ->
                        estadoActual.copy(
                            cargando = false,
                            items = activos,
                            itemsFiltrados = aplicarFiltros(
                                items = activos,
                                query = estadoActual.busqueda,
                                cat = estadoActual.categoriaFiltro,
                                est = estadoActual.estadoFiltro
                            ),
                            totalArticulosDistintos = activos.size,
                            totalUnidadesDisponibles = activos.sumOf { it.piezasPrestables },
                            totalUnidadesTotales = activos.sumOf { it.cantidadTotal },
                            // Piezas dañadas, incompletas o extraviadas
                            totalUnidadesDanadas = activos.sumOf { it.piezasConNovedad }
                        )
                    }
                }
        }
    }

    /** Completa, solo en memoria, a quién está prestada cada unidad. */
    private fun conPrestatario(items: List<ItemDeportivo>, prestamos: List<Prestamo>): List<ItemDeportivo> {
        val porId = prestamos.associateBy { it.id }
        return items.map { item ->
            val prestamo = porId[item.prestamoActivoId]
            if (prestamo != null && item.estudiantePrestamoNombre.isBlank()) {
                item.copy(
                    estudiantePrestamoNombre = prestamo.nombreEstudiante,
                    estudiantePrestamoId = prestamo.codigoEstudiante
                )
            } else {
                item
            }
        }
    }

    fun actualizarBusqueda(nuevaBusqueda: String) {
        _uiState.update { estado ->
            estado.copy(
                busqueda = nuevaBusqueda,
                itemsFiltrados = aplicarFiltros(estado.items, nuevaBusqueda, estado.categoriaFiltro, estado.estadoFiltro)
            )
        }
    }

    fun seleccionarCategoriaFiltro(categoria: String) {
        _uiState.update { estado ->
            estado.copy(
                categoriaFiltro = categoria,
                itemsFiltrados = aplicarFiltros(estado.items, estado.busqueda, categoria, estado.estadoFiltro)
            )
        }
    }

    fun seleccionarEstadoFiltro(estadoFiltro: String) {
        _uiState.update { estado ->
            estado.copy(
                estadoFiltro = estadoFiltro,
                itemsFiltrados = aplicarFiltros(estado.items, estado.busqueda, estado.categoriaFiltro, estadoFiltro)
            )
        }
    }

    private fun aplicarFiltros(
        items: List<ItemDeportivo>,
        query: String,
        cat: String,
        est: String
    ): List<ItemDeportivo> {
        val q = query.trim().lowercase()
        return items.filter { item ->
            // Filtro por búsqueda de ID o Nombre o Categoría
            val coincideTexto = if (q.isBlank()) {
                true
            } else {
                item.id.lowercase().contains(q) ||
                item.nombre.lowercase().contains(q) ||
                item.categoria.lowercase().contains(q) ||
                item.descripcion.lowercase().contains(q)
            }

            // Filtro por categoría
            val coincideCategoria = if (cat == "Todas") {
                true
            } else {
                item.categoria.equals(cat, ignoreCase = true)
            }

            // Filtro por estado y disponibilidad (RF-06). En artículos por cantidad se mira
            // cuántas piezas hay en cada situación, no solo el estado general.
            val coincideEstado = when (est.uppercase()) {
                "TODOS" -> true
                EstadoItem.DISPONIBLE -> item.sePuedePrestar
                EstadoItem.PRESTADO -> item.estaPrestado
                EstadoItem.DANADO -> item.tieneDanos
                EstadoItem.EXTRAVIADO -> if (item.esPorCantidad) item.cantidadExtraviada > 0
                    else item.estado.equals(EstadoItem.EXTRAVIADO, ignoreCase = true)
                else -> item.estado.equals(est, ignoreCase = true)
            }

            coincideTexto && coincideCategoria && coincideEstado
        }
    }

    fun sugerirSiguienteId(categoria: String, nombre: String): String {
        return InventarioRepository.generarSiguienteId(
            categoria = categoria,
            nombre = nombre,
            itemsExistentes = todosLosItems
        )
    }

    fun sugerirSecuenciaIds(categoria: String, nombre: String, cantidad: Int): List<String> {
        return InventarioRepository.generarSecuenciaIds(
            categoria = categoria,
            nombre = nombre,
            cantidad = cantidad,
            itemsExistentes = todosLosItems
        )
    }

    /** Registra un artículo nuevo. */
    fun guardarItem(item: ItemDeportivo, onResultado: (Boolean) -> Unit) {
        ejecutar(Idioma.texto(R.string.comun_no_se_pudo_guardar), onResultado) { repository.crearItems(listOf(item)) }
    }

    /** Registra un lote de unidades nuevas con códigos consecutivos. */
    fun guardarVariosItems(items: List<ItemDeportivo>, onResultado: (Boolean) -> Unit) {
        ejecutar(Idioma.texto(R.string.inventario_error_lote), onResultado) { repository.crearItems(items) }
    }

    /** Guarda los cambios de un artículo existente. */
    fun actualizarItem(item: ItemDeportivo, onResultado: (Boolean) -> Unit) {
        ejecutar(Idioma.texto(R.string.comun_no_se_pudo_actualizar), onResultado) { repository.actualizarItem(item) }
    }

    /** Da de baja el artículo (baja lógica: no se borra de Firestore). */
    fun eliminarItem(id: String, onResultado: (Boolean) -> Unit) {
        ejecutar(Idioma.texto(R.string.inventario_error_baja), onResultado) { repository.darDeBaja(id) }
    }

    /** RF-07: dañado, incompleto o extraviado, con observación. */
    fun marcarNovedad(id: String, tipo: String, cantidad: Int, observacion: String, onResultado: (Boolean) -> Unit) {
        ejecutar(Idioma.texto(R.string.novedad_error_registrar), onResultado) {
            repository.marcarNovedad(id, tipo, cantidad, observacion)
        }
    }

    /** Reparado, completado o encontrado: vuelve a estar disponible. */
    fun resolverNovedad(id: String, tipo: String, cantidad: Int, onResultado: (Boolean) -> Unit) {
        ejecutar(Idioma.texto(R.string.comun_no_se_pudo_actualizar), onResultado) { repository.resolverNovedad(id, tipo, cantidad) }
    }

    fun descontarExtraviadas(id: String, onResultado: (Boolean) -> Unit) {
        ejecutar(Idioma.texto(R.string.comun_no_se_pudo_actualizar), onResultado) { repository.descontarExtraviadas(id) }
    }

    fun cargarDatosEjemplo() {
        ejecutar(Idioma.texto(R.string.inventario_error_ejemplo), {}) { repository.cargarDatosEjemplo() }
    }

    fun limpiarMensajes() {
        _uiState.update { it.copy(mensajeExito = null, mensajeError = null) }
    }

    /** Ejecuta una operación del repositorio y publica su resultado como mensaje. */
    private fun ejecutar(
        prefijoError: String,
        onResultado: (Boolean) -> Unit,
        operacion: suspend () -> Result<String>
    ) {
        viewModelScope.launch {
            operacion()
                .onSuccess { mensaje ->
                    _uiState.update { it.copy(mensajeExito = mensaje) }
                    onResultado(true)
                }
                .onFailure { e ->
                    _uiState.update { it.copy(mensajeError = "$prefijoError: ${e.localizedMessage}") }
                    onResultado(false)
                }
        }
    }
}
