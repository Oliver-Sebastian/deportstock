package com.example.myapplication.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Forma de tarjetas, campos y etiquetas según el tema: las esquinas redondeadas de siempre o,
 * con los temas neón (Cyberpunk), esquinas cortadas en diagonal como los paneles HUD.
 * Las pantallas usan formaApp(12.dp) donde antes decían RoundedCornerShape(12.dp); fuera de
 * los temas neón el resultado es exactamente el mismo.
 */
fun formaApp(radio: Dp): Shape =
    if (Colores.esNeon) CutCornerShape(corteNeon(radio)) else RoundedCornerShape(radio)

/**
 * Forma de los botones con fondo o borde: la píldora de Material (la que ya tenían) o, con
 * los temas neón, esquinas cortadas.
 */
fun formaBoton(): Shape = if (Colores.esNeon) CutCornerShape(8.dp) else CircleShape

/** Cuánto se corta cada esquina según el radio que tenía: un poco más de la mitad, entre 3 y 14 dp. */
private fun corteNeon(radio: Dp): Dp = (radio.value * 0.6f).coerceIn(3f, 14f).dp

/**
 * Formas de Material 3 para los temas neón. Las piezas que no fijan su forma (menús
 * desplegables, calendario, avisos, chips de filtro, campos y el botón flotante) salen con las
 * esquinas cortadas sin tocar cada pantalla.
 */
val FormasNeon = Shapes(
    extraSmall = CutCornerShape(4.dp),
    small = CutCornerShape(6.dp),
    medium = CutCornerShape(8.dp),
    large = CutCornerShape(12.dp),
    extraLarge = CutCornerShape(16.dp)
)
