package com.example.myapplication.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.R
import com.example.myapplication.data.PerfilRepository
import com.example.myapplication.utils.FotosPerfil
import com.example.myapplication.utils.Idioma
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Cambia o quita la foto de perfil. La foto que se muestra no sale de aquí sino de la sesión
 * ([SesionViewModel.foto]), que escucha Firestore: así el inicio y Ajustes se actualizan juntos.
 * Usa AndroidViewModel porque necesita el contexto para leer la imagen elegida.
 */
class FotoPerfilViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = PerfilRepository()

    private val _estado = MutableStateFlow<EstadoFormulario>(EstadoFormulario.Inactivo)
    val estado: StateFlow<EstadoFormulario> = _estado.asStateFlow()

    /** Imagen elegida en la galería. */
    fun usarImagen(uid: String, uri: Uri) = guardar(uid) {
        FotosPerfil.desdeUri(getApplication(), uri)
    }

    /** Foto tomada con la cámara (vista previa que entrega el sistema). */
    fun usarFotoCamara(uid: String, foto: Bitmap) = guardar(uid) {
        FotosPerfil.desdeBitmap(foto)
    }

    fun quitar(uid: String) {
        if (_estado.value is EstadoFormulario.Guardando) return
        _estado.value = EstadoFormulario.Guardando
        viewModelScope.launch {
            _estado.value = repository.quitarFoto(uid).fold(
                onSuccess = { EstadoFormulario.Exito },
                onFailure = { EstadoFormulario.Error(it.message ?: Idioma.texto(R.string.foto_error_quitar)) }
            )
        }
    }

    fun reiniciar() {
        _estado.value = EstadoFormulario.Inactivo
    }

    private fun guardar(uid: String, preparar: () -> String) {
        if (_estado.value is EstadoFormulario.Guardando) return
        _estado.value = EstadoFormulario.Guardando
        viewModelScope.launch {
            _estado.value = try {
                // Recortar y comprimir es trabajo pesado: fuera del hilo principal.
                val imagen = withContext(Dispatchers.Default) { preparar() }
                repository.guardarFoto(uid, imagen).fold(
                    onSuccess = { EstadoFormulario.Exito },
                    onFailure = { EstadoFormulario.Error(it.message ?: Idioma.texto(R.string.foto_error_guardar)) }
                )
            } catch (e: Exception) {
                EstadoFormulario.Error(Idioma.texto(R.string.foto_error_leer_otra))
            }
        }
    }
}
