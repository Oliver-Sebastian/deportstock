package com.example.myapplication.ui.theme

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.example.myapplication.utils.Idioma

/**
 * Aplica el idioma elegido en Ajustes ([Idioma.actual]) a todo lo que está dentro: los textos de
 * la app (`stringResource`) y los propios de Material (calendario, descripciones de accesibilidad).
 * Al cambiar el idioma, todo se vuelve a dibujar con el nuevo, sin reiniciar la app.
 *
 * Los diálogos, menús desplegables y el calendario se dibujan en ventanas propias, que vuelven a
 * tomar el contexto de la actividad: a ellos el idioma les llega por `MainActivity.getResources()`
 * ([com.example.myapplication.utils.RecursosConIdioma]).
 *
 * Con la versión de Compose del proyecto (BOM 2024.12.01, Compose UI 1.7), `stringResource` lee
 * LocalContext y LocalConfiguration. Desde Compose UI 1.9 lee LocalResources: al actualizar el BOM
 * hay que agregar `LocalResources provides contextoConIdioma.resources` a la lista de abajo.
 */
@Composable
fun ProveedorIdioma(content: @Composable () -> Unit) {
    val contexto = LocalContext.current
    val configuracionDelTelefono = LocalConfiguration.current
    val idioma = Idioma.actual
    val configuracion = remember(configuracionDelTelefono, idioma) {
        Idioma.configuracion(configuracionDelTelefono, idioma)
    }
    val contextoConIdioma = remember(contexto, configuracion) {
        ContextoConIdioma(contexto, contexto.createConfigurationContext(configuracion).resources)
    }
    CompositionLocalProvider(
        LocalContext provides contextoConIdioma,
        LocalConfiguration provides configuracion,
        content = content
    )
}

/**
 * El mismo contexto de la pantalla (abre el marcador, la cámara, compartir...) pero con los textos
 * del idioma elegido. Solo cambia [getResources].
 */
private class ContextoConIdioma(base: Context, private val recursos: Resources) : ContextWrapper(base) {
    override fun getResources(): Resources = recursos
}
