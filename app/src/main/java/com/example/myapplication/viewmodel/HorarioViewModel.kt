package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.R
import com.example.myapplication.data.DiasSemana
import com.example.myapplication.data.Horario
import com.example.myapplication.data.HorarioRepository
import com.example.myapplication.utils.Idioma
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date

data class HorarioUiState(
    val cargando: Boolean = true,
    val horarios: List<Horario> = emptyList(),
    val fechaFinSemestre: Date? = null,
    val mensaje: String? = null
)

/** Horario de atención (RF-14) y fecha de fin de semestre. Lo administra el monitor. */
class HorarioViewModel(
    private val repository: HorarioRepository = HorarioRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HorarioUiState())
    val uiState: StateFlow<HorarioUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.obtenerHorariosFlow()
                .catch { error -> publicar(Idioma.texto(R.string.horario_error_cargar, error.localizedMessage ?: Idioma.texto(R.string.comun_error_desconocido))) }
                .collect { lista -> _uiState.update { it.copy(cargando = false, horarios = lista) } }
        }
        viewModelScope.launch {
            repository.obtenerConfiguracionFlow()
                .catch { error -> publicar(Idioma.texto(R.string.horario_error_configuracion, error.localizedMessage ?: Idioma.texto(R.string.comun_error_desconocido))) }
                .collect { configuracion ->
                    _uiState.update { it.copy(fechaFinSemestre = configuracion.fechaFinSemestre?.toDate()) }
                }
        }
    }

    /** Valida y guarda una franja de atención. Devuelve false si los datos no son válidos. */
    fun guardarFranja(horario: Horario): Boolean {
        if (horario.diaSemana !in DiasSemana.DIAS_ATENCION) {
            publicar(Idioma.texto(R.string.horario_error_dia))
            return false
        }
        // Las horas van en formato "HH:mm", así que se pueden comparar como texto.
        if (horario.horaInicio >= horario.horaFin) {
            publicar(Idioma.texto(R.string.horario_error_horas))
            return false
        }
        viewModelScope.launch { informar(repository.guardarHorario(horario)) }
        return true
    }

    fun eliminarFranja(id: String) {
        viewModelScope.launch { informar(repository.eliminarHorario(id)) }
    }

    fun guardarFinSemestre(fecha: Date) {
        viewModelScope.launch { informar(repository.guardarFechaFinSemestre(fecha)) }
    }

    fun limpiarMensaje() {
        _uiState.update { it.copy(mensaje = null) }
    }

    private fun informar(resultado: Result<String>) {
        publicar(resultado.fold(onSuccess = { it }, onFailure = { it.localizedMessage ?: Idioma.texto(R.string.comun_ocurrio_error) }))
    }

    private fun publicar(mensaje: String) {
        _uiState.update { it.copy(cargando = false, mensaje = mensaje) }
    }
}
