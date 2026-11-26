package com.example.myapplication.data

import android.content.Context
import androidx.annotation.StringRes
import com.example.myapplication.R

/**
 * Aspecto visual de la app. Se elige en Ajustes y se guarda en el teléfono.
 * Para agregar un diseño nuevo: una constante aquí (con su nombre y descripción en
 * res/values/strings.xml), su paleta en ui/theme/Colores.kt y su esquema en Theme.kt.
 * Aparece solo en la lista de "Ver más diseños".
 */
enum class TemaApp(
    /** Nombre en el selector (texto de res/values/strings.xml, traducido según el idioma). */
    @StringRes val titulo: Int,
    @StringRes val descripcion: Int,
    /** Estilo neón: paneles HUD con borde brillante, fondo con cuadrícula y animaciones. */
    val esNeon: Boolean = false
) {
    OSCURO(R.string.tema_oscuro, R.string.tema_oscuro_descripcion),
    CLARO(R.string.tema_claro, R.string.tema_claro_descripcion),
    // Se muestra como "Udenar" (cabe en el selector); el nombre guardado sigue siendo INSTITUCIONAL.
    INSTITUCIONAL(R.string.tema_udenar, R.string.tema_udenar_descripcion),
    CYBERPUNK(R.string.tema_cyberpunk, R.string.tema_cyberpunk_descripcion, esNeon = true),
    RETRO(R.string.tema_retro, R.string.tema_retro_descripcion),
    VARSITY(R.string.tema_varsity, R.string.tema_varsity_descripcion),
    SOLAR(R.string.tema_solar, R.string.tema_solar_descripcion);

    companion object {
        /** Los que se ven directamente en Ajustes; todos están en "Ver más diseños". */
        val PRINCIPALES: List<TemaApp> = listOf(OSCURO, CLARO, INSTITUCIONAL)

        /** Convierte el valor guardado en un tema; si no hay nada guardado, el oscuro. */
        fun desde(nombre: String?): TemaApp = entries.firstOrNull { it.name == nombre } ?: OSCURO
    }
}

/**
 * Idioma de la app. Se elige en Ajustes y se guarda en el teléfono, como el tema.
 * El español es el idioma base (res/values): si un texto no está traducido, se ve en español.
 */
enum class IdiomaApp(
    /** Código ISO 639-1: carpeta de recursos (values-en) e idioma de los correos de Firebase. */
    val codigo: String,
    /** Nombre del idioma escrito en ese mismo idioma ("English" también con la app en español). */
    @StringRes val nombre: Int
) {
    ESPANOL("es", R.string.idioma_espanol),
    INGLES("en", R.string.idioma_ingles);

    companion object {
        /** Convierte el valor guardado en un idioma; si no hay nada guardado, español. */
        fun desde(nombre: String?): IdiomaApp = entries.firstOrNull { it.name == nombre } ?: ESPANOL
    }
}

/**
 * Preferencias locales de este teléfono (no se guardan en Firestore):
 * el tema, el idioma y si se muestran las notificaciones de préstamos y vencimientos.
 *
 * Usa SharedPreferences, que viene con Android, así que no hace falta otra dependencia.
 * La lectura es inmediata, por eso se puede leer antes de dibujar la primera pantalla
 * y la app arranca directamente con el tema guardado, sin parpadeos.
 */
class PreferenciasRepository(contexto: Context) {

    private val preferencias = contexto.applicationContext
        .getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    fun tema(): TemaApp = TemaApp.desde(preferencias.getString(CLAVE_TEMA, null))

    fun guardarTema(tema: TemaApp) {
        preferencias.edit().putString(CLAVE_TEMA, tema.name).apply()
    }

    fun idioma(): IdiomaApp = IdiomaApp.desde(preferencias.getString(CLAVE_IDIOMA, null))

    fun guardarIdioma(idioma: IdiomaApp) {
        preferencias.edit().putString(CLAVE_IDIOMA, idioma.name).apply()
    }

    /** Activadas por defecto: el recordatorio de devolución es un requisito (RF-19). */
    fun notificacionesActivas(): Boolean = preferencias.getBoolean(CLAVE_NOTIFICACIONES, true)

    fun guardarNotificaciones(activas: Boolean) {
        preferencias.edit().putBoolean(CLAVE_NOTIFICACIONES, activas).apply()
    }

    private companion object {
        const val ARCHIVO = "deportstock_preferencias"
        const val CLAVE_TEMA = "tema"
        const val CLAVE_IDIOMA = "idioma"
        const val CLAVE_NOTIFICACIONES = "notificaciones_prestamos"
    }
}
