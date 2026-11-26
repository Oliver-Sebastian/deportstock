package com.example.myapplication.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.example.myapplication.R
import com.example.myapplication.data.TipoArticulo

/**
 * Foto de cada tipo de implemento, en `res/drawable-nodpi/` (fondo transparente, centrada y con
 * el mismo tamaño visual: se ve bien en el círculo de cualquier tema).
 */
@DrawableRes
fun TipoArticulo.imagen(): Int = when (this) {
    TipoArticulo.BALON_FUTBOL -> R.drawable.articulo_balon_futbol
    TipoArticulo.BALON_BALONCESTO -> R.drawable.articulo_balon_baloncesto
    TipoArticulo.BALON_VOLEIBOL -> R.drawable.articulo_balon_voleibol
    TipoArticulo.RAQUETA_TENIS_MESA -> R.drawable.articulo_raqueta_tenis_mesa
    TipoArticulo.RAQUETA_TENIS_CAMPO -> R.drawable.articulo_raqueta_tenis_campo
    TipoArticulo.PETO -> R.drawable.articulo_peto
    TipoArticulo.CONO -> R.drawable.articulo_cono
    TipoArticulo.AJEDREZ -> R.drawable.articulo_ajedrez
    TipoArticulo.CUERDA -> R.drawable.articulo_cuerda
    TipoArticulo.CRONOMETRO -> R.drawable.articulo_cronometro
    TipoArticulo.COLCHONETA -> R.drawable.articulo_colchoneta
}

/**
 * Foto del implemento en lugar del emoji. Si el tipo no se reconoce (ver [TipoArticulo.de]),
 * muestra el emoji guardado, del tamaño [tamanoEmoji], como antes.
 */
@Composable
fun ImagenArticulo(
    nombre: String,
    categoria: String,
    icono: String,
    tamano: Dp,
    tamanoEmoji: TextUnit,
    modifier: Modifier = Modifier
) {
    val tipo = remember(nombre, categoria, icono) { TipoArticulo.de(nombre, categoria, icono) }
    if (tipo != null) {
        Image(
            painter = painterResource(tipo.imagen()),
            contentDescription = null, // El nombre del artículo va al lado
            modifier = modifier.size(tamano)
        )
    } else {
        Text(text = icono.ifBlank { "🏅" }, fontSize = tamanoEmoji, modifier = modifier)
    }
}

/** Foto de una categoría de base (filtros y formulario del inventario); si no tiene, el emoji. */
@Composable
fun ImagenCategoria(categoria: String, emoji: String, tamano: Dp, tamanoEmoji: TextUnit) {
    val tipo = TipoArticulo.deCategoria(categoria)
    if (tipo != null) {
        Image(painter = painterResource(tipo.imagen()), contentDescription = null, modifier = Modifier.size(tamano))
    } else {
        Text(text = emoji, fontSize = tamanoEmoji)
    }
}
