package com.example.myapplication.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// =============================================================================
// Adaptación al teléfono girado (horizontal): con poca altura, los diálogos y las pantallas con
// encabezado fijo se reorganizan para que todo se pueda ver desplazando.
// =============================================================================

/**
 * true cuando la pantalla tiene poca altura (menos de 480 dp, el límite "compacto" de Material),
 * que es lo normal con el teléfono girado.
 */
@Composable
fun alturaCompacta(): Boolean = LocalConfiguration.current.screenHeightDp < 480

/**
 * Encabezado (búsqueda, filtros, resumen) con una lista debajo.
 *
 * Con el teléfono derecho el encabezado queda fijo arriba y solo se desplaza la lista, como
 * siempre. Con poca altura (teléfono girado) el encabezado pasa a ser el primer elemento de la
 * lista y se desplaza con ella; si no, la lista quedaba reducida a una franja de pocos dp.
 *
 * - [espacioEncabezado]: espacio entre las piezas del encabezado (y entre el encabezado y la lista).
 * - [espacioLista] y [margenLista]: espacio entre elementos y márgenes de la lista. Los márgenes
 *   laterales los pone cada elemento (o el [modifier]), no la lista, para que el encabezado no
 *   quede con doble margen al desplazarse junto con ella.
 * - [estado]: con [mostrarEstado] (cargando, lista vacía), se muestra en lugar de la lista.
 */
@Composable
fun ListaConEncabezado(
    modifier: Modifier = Modifier,
    espacioEncabezado: Dp = 0.dp,
    espacioLista: Dp = 10.dp,
    margenLista: PaddingValues = PaddingValues(0.dp),
    mostrarEstado: Boolean = false,
    estado: @Composable () -> Unit = {},
    encabezado: @Composable () -> Unit,
    lista: LazyListScope.() -> Unit
) {
    if (alturaCompacta()) {
        LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(espacioLista),
            contentPadding = margenLista
        ) {
            item(key = "encabezado") {
                Column(verticalArrangement = Arrangement.spacedBy(espacioEncabezado)) { encabezado() }
            }
            if (mostrarEstado) {
                item(key = "estado") {
                    Box(modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp)) { estado() }
                }
            } else {
                lista()
            }
        }
    } else {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(espacioEncabezado)) {
            encabezado()
            if (mostrarEstado) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) { estado() }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(espacioLista),
                    contentPadding = margenLista
                ) {
                    lista()
                }
            }
        }
    }
}
