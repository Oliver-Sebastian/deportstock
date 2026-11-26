package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.ui.theme.Colores

/**
 * Contador "−  N  +" para cantidades (unidades a crear, piezas a prestar, piezas dañadas...).
 *
 * Los dos botones son círculos con borde del color principal, a la misma altura que el
 * número, para que se lean como botones en cualquier tema. En el mínimo o el máximo el botón
 * correspondiente se apaga, así se ve hasta dónde se puede llegar.
 *
 * El área táctil es el mismo círculo ([tamanoBoton]): 40 dp en formularios y 30-32 dp en filas
 * con poco espacio (préstamo, devolución), para no quitarle ancho al nombre del artículo.
 */
@Composable
fun ContadorCantidad(
    valor: Int,
    onCambio: (Int) -> Unit,
    modifier: Modifier = Modifier,
    minimo: Int = 0,
    maximo: Int = Int.MAX_VALUE,
    habilitado: Boolean = true,
    colorValor: Color = MaterialTheme.colorScheme.primary,
    tamanoBoton: Dp = 36.dp,
    etiquetaPrueba: String = "contador"
) {
    val compacto = tamanoBoton <= 32.dp
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compacto) 4.dp else 6.dp),
        modifier = modifier.testTag(etiquetaPrueba)
    ) {
        BotonPaso(
            icono = painterResource(R.drawable.ic_quitar),
            descripcion = stringResource(R.string.contador_quitar),
            activo = habilitado && valor > minimo,
            tamano = tamanoBoton,
            etiquetaPrueba = "${etiquetaPrueba}_menos"
        ) { onCambio((valor - 1).coerceAtLeast(minimo)) }

        Text(
            text = "$valor",
            color = colorValor,
            fontWeight = FontWeight.Bold,
            fontSize = if (compacto) 16.sp else 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(min = if (compacto) 28.dp else 34.dp)
                .testTag("${etiquetaPrueba}_valor")
        )

        BotonPaso(
            icono = rememberVectorPainter(Icons.Default.Add),
            descripcion = stringResource(R.string.contador_agregar),
            activo = habilitado && valor < maximo,
            tamano = tamanoBoton,
            etiquetaPrueba = "${etiquetaPrueba}_mas"
        ) { onCambio((valor + 1).coerceAtMost(maximo)) }
    }
}

@Composable
private fun BotonPaso(
    icono: Painter,
    descripcion: String,
    activo: Boolean,
    tamano: Dp,
    etiquetaPrueba: String,
    onClick: () -> Unit
) {
    val primario = MaterialTheme.colorScheme.primary
    val color = if (activo) primario else Colores.textoTenue.copy(alpha = 0.5f)
    // Círculo en los temas normales; cuadro con esquinas cortadas en los neón
    val forma = if (Colores.esNeon) CutCornerShape(tamano * 0.22f) else CircleShape
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(tamano)
            .clip(forma)
            .background(if (activo) primario.copy(alpha = 0.14f) else Color.Transparent)
            .border(1.5.dp, color, forma)
            .clickable(enabled = activo, role = Role.Button, onClick = onClick)
            .testTag(etiquetaPrueba)
    ) {
        Icon(
            painter = icono,
            contentDescription = descripcion,
            tint = color,
            modifier = Modifier.size(tamano * 0.5f)
        )
    }
}
