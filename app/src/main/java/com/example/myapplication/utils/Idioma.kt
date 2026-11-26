package com.example.myapplication.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.myapplication.data.IdiomaApp
import com.example.myapplication.data.PreferenciasRepository
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Idioma activo de la app y acceso a sus textos (res/values en español, res/values-en en inglés).
 *
 * Funciona como `Colores.tema`: [actual] es un estado de Compose, así que al cambiarlo en Ajustes
 * toda pantalla se vuelve a dibujar con el idioma nuevo, sin reiniciar la app. En las pantallas
 * se usa `stringResource(R.string.x)` (lo traduce `ProveedorIdioma`, y en diálogos, menús y el
 * calendario, [RecursosConIdioma]); en ViewModels, repositorios, reportes y notificaciones,
 * [texto] y [plural].
 */
object Idioma {

    var actual: IdiomaApp by mutableStateOf(IdiomaApp.ESPANOL)

    private var aplicacion: Context? = null
    private val recursosPorIdioma = ConcurrentHashMap<IdiomaApp, Resources>()

    /**
     * Lee el idioma guardado. Se llama al abrir la app ([com.example.myapplication.MainActivity])
     * y al empezar la revisión de recordatorios, que puede correr con la app cerrada.
     */
    fun iniciar(contexto: Context) {
        aplicacion = contexto.applicationContext
        actual = PreferenciasRepository(contexto).idioma()
    }

    /** Ya se leyó el idioma guardado (ver [iniciar]). */
    val iniciado: Boolean get() = aplicacion != null

    /** Para fechas y horas ("p. m." o "PM") y para el idioma de los correos de Firebase. */
    val locale: Locale get() = Locale.forLanguageTag(actual.codigo)

    /** [configuracion] ya está en [idioma] (por ejemplo, teléfono en español y app en español). */
    fun usaIdioma(configuracion: Configuration, idioma: IdiomaApp = actual): Boolean =
        configuracion.locales[0]?.language == idioma.codigo

    /**
     * [base] con el idioma [idioma]: mismo tamaño de pantalla, letra y modo, otros textos. Si ya
     * está en ese idioma se deja igual, con su región (es-CO).
     */
    fun configuracion(base: Configuration, idioma: IdiomaApp = actual): Configuration =
        if (usaIdioma(base, idioma)) base
        else Configuration(base).apply { setLocale(Locale.forLanguageTag(idioma.codigo)) }

    /** Textos de la app en el idioma activo (se crean una vez por idioma). */
    fun recursos(): Resources {
        val contexto = checkNotNull(aplicacion) { "Falta llamar a Idioma.iniciar(contexto) al abrir la app" }
        val idioma = actual
        return recursosPorIdioma.getOrPut(idioma) {
            contexto.createConfigurationContext(configuracion(contexto.resources.configuration, idioma)).resources
        }
    }

    /** Texto de res/values en el idioma activo; [args] reemplaza %1$s, %2$s... en orden. */
    fun texto(@StringRes id: Int, vararg args: Any): String = recursos().getString(id, *args)

    /** Texto con singular y plural ("1 préstamo" / "3 préstamos") según [cantidad]. */
    fun plural(@PluralsRes id: Int, cantidad: Int, vararg args: Any): String =
        recursos().getQuantityString(id, cantidad, *args)
}

/**
 * Los textos de una actividad en el idioma de la app.
 *
 * Compose dibuja cada diálogo, menú desplegable y el calendario en una ventana propia, y esa
 * ventana toma los textos de la actividad, no de `ProveedorIdioma`. Por eso `MainActivity`
 * devuelve estos recursos en `getResources()`: así los diálogos también salen en el idioma
 * elegido. Se crean una vez por idioma y por configuración (girar el teléfono cambia la
 * configuración).
 */
class RecursosConIdioma(private val actividad: Context) {

    private var idioma: IdiomaApp? = null
    private var configuracionBase: Configuration? = null
    private var recursos: Resources? = null

    /** [base] son los recursos propios de la actividad (los del idioma del teléfono). */
    fun de(base: Resources): Resources {
        if (!Idioma.iniciado) return base
        val idiomaActual = Idioma.actual
        val configuracion = base.configuration
        if (Idioma.usaIdioma(configuracion, idiomaActual)) return base
        val guardados = recursos
        if (guardados != null && idioma == idiomaActual && configuracion == configuracionBase) return guardados
        return actividad.createConfigurationContext(Idioma.configuracion(configuracion, idiomaActual)).resources.also {
            idioma = idiomaActual
            configuracionBase = Configuration(configuracion)
            recursos = it
        }
    }
}
