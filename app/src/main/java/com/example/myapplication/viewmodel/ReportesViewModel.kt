package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.R
import com.example.myapplication.data.InventarioRepository
import com.example.myapplication.data.ItemDeportivo
import com.example.myapplication.data.Prestamo
import com.example.myapplication.data.PrestamosRepository
import com.example.myapplication.data.reportes.AnalisisPrestamos
import com.example.myapplication.data.reportes.ExportadorCsv
import com.example.myapplication.data.reportes.ExportadorPdf
import com.example.myapplication.data.reportes.ExportadorXlsx
import com.example.myapplication.data.reportes.FormatoReporte
import com.example.myapplication.data.reportes.GeneradorReportes
import com.example.myapplication.data.reportes.GrupoHistorial
import com.example.myapplication.data.reportes.Indicadores
import com.example.myapplication.data.reportes.TipoReporte
import com.example.myapplication.utils.Idioma
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.util.Date

data class ReportesUiState(
    val cargando: Boolean = true,
    val prestamos: List<Prestamo> = emptyList(),
    /** Incluye los artículos dados de baja: también deben aparecer en el reporte. */
    val inventario: List<ItemDeportivo> = emptyList(),
    val indicadores: Indicadores = Indicadores(),
    /** Rango de fechas del reporte; null en un extremo significa "sin límite". */
    val desde: Date? = null,
    val hasta: Date? = null,
    val prestamosEnRango: Int = 0,
    val exportando: Boolean = false,
    val mensajeError: String? = null
)

/** Reportes exportables (RF-17), historial (RF-18) e indicadores (RF-20). */
class ReportesViewModel(
    private val prestamosRepository: PrestamosRepository = PrestamosRepository(),
    private val inventarioRepository: InventarioRepository = InventarioRepository(),
    private val despachadorArchivos: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportesUiState())
    val uiState: StateFlow<ReportesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            prestamosRepository.obtenerPrestamosFlow()
                .catch { error -> publicarError(Idioma.texto(R.string.comun_error_cargar_prestamos), error) }
                .collect { lista -> actualizar { it.copy(cargando = false, prestamos = lista) } }
        }
        viewModelScope.launch {
            inventarioRepository.obtenerInventarioFlow(incluirInactivos = true)
                .catch { error -> publicarError(Idioma.texto(R.string.comun_error_cargar_inventario), error) }
                .collect { lista -> actualizar { it.copy(inventario = lista) } }
        }
    }

    fun establecerDesde(fecha: Date?) = actualizar { estado ->
        // Si el nuevo inicio queda después del fin, se descarta el fin.
        val hasta = estado.hasta?.takeIf { fecha == null || !it.before(fecha) }
        estado.copy(desde = fecha, hasta = hasta)
    }

    fun establecerHasta(fecha: Date?) = actualizar { it.copy(hasta = fecha) }

    fun limpiarRango() = actualizar { it.copy(desde = null, hasta = null) }

    /** RF-18: grupos de préstamos por estudiante o por artículo que coinciden con la búsqueda. */
    fun historial(porArticulo: Boolean, consulta: String): List<GrupoHistorial> {
        val prestamos = _uiState.value.prestamos
        return if (porArticulo) {
            AnalisisPrestamos.porArticulo(prestamos, consulta)
        } else {
            AnalisisPrestamos.porEstudiante(prestamos, consulta)
        }
    }

    /**
     * RF-17: genera el reporte y lo escribe en el destino que eligió el monitor.
     * @param abrirSalida abre el archivo de destino; se invoca fuera del hilo principal.
     */
    fun exportar(
        tipo: TipoReporte,
        formato: FormatoReporte,
        abrirSalida: () -> OutputStream?,
        onResultado: (Result<String>) -> Unit
    ) {
        if (_uiState.value.exportando) return
        val estado = _uiState.value
        _uiState.update { it.copy(exportando = true) }

        viewModelScope.launch {
            val resultado = withContext(despachadorArchivos) {
                runCatching {
                    val tabla = when (tipo) {
                        TipoReporte.PRESTAMOS ->
                            GeneradorReportes.prestamos(estado.prestamos, estado.desde, estado.hasta)
                        TipoReporte.INVENTARIO ->
                            GeneradorReportes.inventario(estado.inventario, estado.prestamos, estado.desde, estado.hasta)
                    }
                    val salida = abrirSalida() ?: throw IOException(Idioma.texto(R.string.reportes_error_destino))
                    salida.use { flujo ->
                        when (formato) {
                            FormatoReporte.CSV -> ExportadorCsv.escribir(tabla, flujo)
                            FormatoReporte.EXCEL -> ExportadorXlsx.escribir(tabla, flujo)
                            FormatoReporte.PDF -> ExportadorPdf.escribir(tabla, flujo)
                        }
                    }
                    Idioma.plural(tipo.guardado, tabla.filas.size, formato.etiqueta, tabla.filas.size)
                }
            }
            _uiState.update { it.copy(exportando = false) }
            onResultado(resultado)
        }
    }

    fun limpiarError() {
        _uiState.update { it.copy(mensajeError = null) }
    }

    /** Aplica el cambio y recalcula lo que depende de los datos o del rango. */
    private fun actualizar(cambio: (ReportesUiState) -> ReportesUiState) {
        _uiState.update { anterior ->
            val nuevo = cambio(anterior)
            nuevo.copy(
                indicadores = AnalisisPrestamos.indicadores(nuevo.prestamos, nuevo.inventario.filter { it.activo }),
                prestamosEnRango = GeneradorReportes.enRango(nuevo.prestamos, nuevo.desde, nuevo.hasta).size
            )
        }
    }

    private fun publicarError(prefijo: String, error: Throwable) {
        _uiState.update {
            it.copy(cargando = false, mensajeError = "$prefijo: ${error.localizedMessage ?: Idioma.texto(R.string.comun_error_desconocido)}")
        }
    }
}
