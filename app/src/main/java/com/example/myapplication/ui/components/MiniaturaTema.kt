package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.TemaApp
import com.example.myapplication.ui.theme.Colores
import com.example.myapplication.ui.theme.Neon
import com.example.myapplication.ui.theme.caminoCortado
import com.example.myapplication.ui.theme.trazoNeon

/**
 * Pantalla en miniatura de un tema, dibujada con SUS colores (no con los del tema activo):
 * barra, una tarjeta y dos botones. Los temas neón se dibujan con su panel HUD y su brillo.
 * La usan el selector de Ajustes y la lista de "Ver más diseños".
 */
@Composable
fun MiniaturaTema(tema: TemaApp, modifier: Modifier = Modifier) {
    val p = Colores.paletaDe(tema)
    val forma = RoundedCornerShape(8.dp)
    if (tema.esNeon) {
        Box(
            modifier = modifier
                .clip(forma)
                .drawBehind { miniaturaNeon() }
                .border(1.dp, Neon.cian.copy(alpha = 0.35f), forma)
        )
        return
    }
    Column(
        modifier = modifier
            .clip(forma)
            .background(p.fondo)
            .border(1.dp, p.borde, forma)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .background(p.barra)
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .fillMaxWidth()
                .height(20.dp)
                .background(p.superficie, RoundedCornerShape(4.dp))
                .border(1.dp, p.borde, RoundedCornerShape(4.dp))
        )
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.padding(horizontal = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.width(26.dp).height(8.dp).background(p.primario, RoundedCornerShape(4.dp)))
            Box(Modifier.width(14.dp).height(8.dp).background(p.textoTenue, RoundedCornerShape(4.dp)))
        }
    }
}

/** Miniatura Cyberpunk: fondo con cuadrícula, panel HUD cian y magenta, botón lima con brillo. */
private fun DrawScope.miniaturaNeon() {
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(listOf(Neon.panel, Colores.paletaDe(TemaApp.CYBERPUNK).fondo)))
    val paso = 8.dp.toPx()
    var x = 0f
    while (x < w) { drawLine(Neon.cian, Offset(x, 0f), Offset(x, h), strokeWidth = 1f, alpha = 0.07f); x += paso }
    var y = 0f
    while (y < h) { drawLine(Neon.cian, Offset(0f, y), Offset(w, y), strokeWidth = 1f, alpha = 0.07f); y += paso }
    // Título de la barra
    drawLine(Neon.cian, Offset(6.dp.toPx(), 7.dp.toPx()), Offset(24.dp.toPx(), 7.dp.toPx()), strokeWidth = 2.dp.toPx(), alpha = 0.9f)
    // Panel HUD
    val m = 6.dp.toPx()
    val panelAlto = h * 0.42f
    val corte = 4.dp.toPx()
    val panel = caminoCortado(w - m * 2, panelAlto, floatArrayOf(corte, corte, corte, corte)).apply {
        translate(Offset(m, 14.dp.toPx()))
    }
    drawPath(panel, Neon.panel)
    trazoNeon(
        panel,
        Brush.linearGradient(listOf(Neon.cian, Neon.cian, Neon.magenta), start = Offset(0f, h), end = Offset(w, 0f)),
        1.dp.toPx()
    )
    // Botones: lima (principal) y cian
    val yb = 14.dp.toPx() + panelAlto + 6.dp.toPx()
    val alto = 7.dp.toPx()
    drawRoundRect(Neon.lima, Offset(m, yb), Size(26.dp.toPx(), alto), CornerRadius(alto / 2f), alpha = 0.25f, style = Stroke(4.dp.toPx()))
    drawRoundRect(Neon.lima, Offset(m, yb), Size(26.dp.toPx(), alto), CornerRadius(alto / 2f))
    drawRoundRect(Neon.cian, Offset(m + 30.dp.toPx(), yb), Size(14.dp.toPx(), alto), CornerRadius(alto / 2f), alpha = 0.8f)
}
