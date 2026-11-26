package com.example.myapplication.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.data.TemaApp
import com.example.myapplication.ui.theme.Colores

/**
 * Sello de la Universidad de Nariño dentro de un círculo blanco con borde verde,
 * como los íconos de la app institucional. Imagen: res/drawable-nodpi/logo_udenar.png
 * (sin escalado por densidad: Compose la ajusta al tamaño pedido).
 */
@Composable
fun LogoUdenar(tamano: Dp = 44.dp, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(tamano)
            .background(Color.White, CircleShape)
            .border(2.dp, Colores.primario, CircleShape)
    ) {
        Image(
            painter = painterResource(R.drawable.logo_udenar),
            contentDescription = stringResource(R.string.encabezado_logo),
            modifier = Modifier.size(tamano * 0.86f)
        )
    }
}

/**
 * Franja con el nombre de la universidad, al estilo del encabezado de la app institucional
 * ("Universidad de Nariño / FUNDADA EN 1904" en blanco sobre verde).
 * Solo se muestra con el tema Institucional; con los otros temas no ocupa espacio.
 */
@Composable
fun EncabezadoInstitucional(modifier: Modifier = Modifier) {
    if (Colores.tema != TemaApp.INSTITUCIONAL) return

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(Colores.barra)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("encabezado_institucional")
    ) {
        LogoUdenar(tamano = 40.dp)
        Column {
            // Igual que el logotipo oficial: "Universidad de" normal y "Nariño" en negrita.
            Text(
                text = buildAnnotatedString {
                    append("Universidad de ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Nariño") }
                },
                style = MaterialTheme.typography.titleSmall,
                color = Colores.sobreBarra
            )
            Text(
                text = stringResource(R.string.encabezado_fundada),
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.2.sp,
                color = Colores.sobreBarraSecundario
            )
        }
    }
}

/**
 * Logo completo (sello y "Universidad de Nariño · FUNDADA EN 1904").
 * Es gris oficial sobre fondo transparente; en el tema oscuro se pinta del color del texto
 * para que se lea sobre fondo oscuro.
 */
@Composable
fun LogoUdenarCompleto(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.logo_udenar_completo),
        contentDescription = stringResource(R.string.encabezado_logo_completo),
        colorFilter = if (Colores.paleta.esOscura) ColorFilter.tint(Colores.texto) else null,
        modifier = modifier.testTag("logo_udenar_completo")
    )
}
