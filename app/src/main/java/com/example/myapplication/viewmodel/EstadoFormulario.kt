package com.example.myapplication.viewmodel

/** Estado de un formulario que se guarda en Firebase (contraseña, datos del perfil, foto). */
sealed class EstadoFormulario {
    object Inactivo : EstadoFormulario()
    object Guardando : EstadoFormulario()
    object Exito : EstadoFormulario()
    data class Error(val mensaje: String) : EstadoFormulario()
}
