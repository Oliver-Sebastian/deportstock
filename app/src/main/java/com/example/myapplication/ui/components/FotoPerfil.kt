package com.example.myapplication.ui.components

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.ui.theme.Colores
import com.example.myapplication.ui.theme.LocalRelojNeon
import com.example.myapplication.ui.theme.Neon
import com.example.myapplication.ui.theme.RelojNeon
import com.example.myapplication.ui.theme.formaApp
import com.example.myapplication.ui.theme.trazoNeon
import com.example.myapplication.utils.Idioma
import com.example.myapplication.viewmodel.EstadoFormulario
import com.example.myapplication.viewmodel.FotoPerfilViewModel
import kotlin.math.PI
import kotlin.math.sin

/**
 * Círculo de perfil: la foto si hay, o el ícono de persona si no.
 * Con [editable] muestra un lápiz en la esquina para indicar que se puede cambiar.
 * Con los temas neón el anillo brilla en cian y, si el avatar es grande, lleva alrededor
 * los arcos HUD que giran (como en los diseños del equipo).
 */
@Composable
fun AvatarPerfil(
    foto: ImageBitmap?,
    tamano: Dp,
    modifier: Modifier = Modifier,
    editable: Boolean = false,
    cargando: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val primario = MaterialTheme.colorScheme.primary
    val neon = Colores.esNeon
    val reloj = LocalRelojNeon.current
    Box(
        modifier = modifier
            .size(tamano)
            // Los arcos HUD van detrás; el aro brillante, encima de la foto (ver más abajo)
            .then(if (neon && tamano >= 72.dp) Modifier.hudNeon(reloj) else Modifier)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(if (neon) Neon.insignia else primario.copy(alpha = 0.18f))
                .then(
                    // En neón el anillo lo dibuja anilloNeon, con brillo
                    if (neon) Modifier else Modifier.border(if (tamano >= 60.dp) 2.5.dp else 2.dp, primario, CircleShape)
                )
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        ) {
            if (foto != null) {
                Image(
                    bitmap = foto,
                    contentDescription = Idioma.texto(R.string.foto_perfil),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = Idioma.texto(R.string.foto_sin_foto),
                    tint = if (neon) Neon.cian else primario,
                    modifier = Modifier.size(tamano * 0.56f)
                )
            }
            if (cargando) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Colores.fondo.copy(alpha = 0.6f))
                ) {
                    CircularProgressIndicator(color = primario, strokeWidth = 2.dp, modifier = Modifier.size(tamano * 0.4f))
                }
            }
        }
        if (neon) {
            // Aro cian con brillo sobre el borde de la foto (no recibe toques: pasan a la foto)
            Spacer(modifier = Modifier.fillMaxSize().aroNeon(reloj))
        }
        if (editable) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(tamano * 0.32f)
                    .background(if (neon) Neon.insignia else primario, CircleShape)
                    .border(if (neon) 1.5.dp else 2.dp, if (neon) Neon.cian else Colores.superficie, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = if (neon) Neon.cian else Colores.sobrePrimario,
                    modifier = Modifier.size(tamano * 0.17f)
                )
            }
        }
    }
}

/** Aro neón del avatar: cian, con brillo hacia afuera y hacia adentro. */
private fun Modifier.aroNeon(reloj: RelojNeon?): Modifier = drawWithCache {
    val aro = Path().apply { addOval(Rect(size.center, size.minDimension / 2f - 1.5.dp.toPx())) }
    onDrawBehind {
        trazoNeon(aro, Neon.cian, 3.dp.toPx(), reloj?.pulso?.value ?: 0.85f)
    }
}

/**
 * Adornos HUD alrededor de un avatar grande: arcos a los lados que se mecen en sentidos
 * opuestos (como un radar), un aro fino y marcas de medición. Quedan a los lados, así no
 * tapan el nombre de abajo; salen del círculo de la foto hasta ~1,5 veces el radio (unos
 * 12 dp arriba y a los lados), por eso quien lo usa deja ese aire.
 * Los arcos se crean una vez; en cada cuadro solo cambian el giro y el brillo.
 */
private fun Modifier.hudNeon(reloj: RelojNeon?): Modifier = drawWithCache {
    val r = size.minDimension / 2f
    val c = size.center
    val r1 = r * 1.30f
    val arcosGruesos = listOf(155f to 70f, 305f to 50f).map { (inicio, barrido) ->
        Path().apply { arcTo(Rect(c, r1), inicio, barrido, true) }
    }
    val r2 = r * 1.46f
    val esquinaArco = Offset(c.x - r2, c.y - r2)
    val tamanoArco = Size(r2 * 2f, r2 * 2f)
    val fino = Stroke(1.dp.toPx())
    val medio = Stroke(1.2.dp.toPx())
    val r3 = r * 1.55f
    val marca = 5.dp.toPx()
    val xIzq = c.x - r * 1.75f
    val yDatos = c.y + r * 0.55f
    val xDer = c.x + r * 1.42f
    val yArriba = c.y - r * 0.92f
    onDrawBehind {
        val intensidad = reloj?.pulso?.value ?: 0.85f
        val vaiven = sin((reloj?.ciclo?.value ?: 0f) * 2f * PI.toFloat())
        drawCircle(Neon.cian, radius = r * 1.16f, center = c, alpha = 0.30f, style = fino)
        // Arcos gruesos: uno a la izquierda y otro arriba a la derecha
        rotate(vaiven * 20f, c) {
            arcosGruesos.forEach { arco -> trazoNeon(arco, Neon.cian, 2.dp.toPx(), intensidad * 0.9f) }
        }
        // Arcos finos en trocitos, que se mecen al revés
        rotate(-vaiven * 15f, c) {
            for (k in 0 until 6) {
                drawArc(
                    Neon.cian,
                    startAngle = 165f + k * 11f,
                    sweepAngle = 6f,
                    useCenter = false,
                    topLeft = esquinaArco,
                    size = tamanoArco,
                    alpha = 0.55f * intensidad,
                    style = fino
                )
            }
            drawArc(
                Neon.cian,
                startAngle = 318f,
                sweepAngle = 38f,
                useCenter = false,
                topLeft = esquinaArco,
                size = tamanoArco,
                alpha = 0.6f * intensidad,
                style = medio
            )
        }
        // Marcas fijas a izquierda y derecha, y rayitas de "datos"
        val linea = 1.dp.toPx()
        drawLine(Neon.cian, Offset(c.x - r3 - marca, c.y), Offset(c.x - r3, c.y), strokeWidth = linea, alpha = 0.5f)
        drawLine(Neon.cian, Offset(c.x + r3, c.y), Offset(c.x + r3 + marca, c.y), strokeWidth = linea, alpha = 0.5f)
        drawLine(Neon.cian, Offset(xIzq, yDatos), Offset(xIzq + 14.dp.toPx(), yDatos), strokeWidth = linea, alpha = 0.45f)
        drawLine(Neon.magenta, Offset(xIzq, yDatos + 4.dp.toPx()), Offset(xIzq + 9.dp.toPx(), yDatos + 4.dp.toPx()), strokeWidth = linea, alpha = 0.45f)
        drawLine(Neon.cian, Offset(xDer, yArriba), Offset(xDer + 16.dp.toPx(), yArriba), strokeWidth = linea, alpha = 0.45f)
    }
}

/**
 * Avatar del usuario con sesión: al tocarlo ofrece elegir una foto de la galería,
 * tomar una con la cámara o quitar la actual. No pide permisos: el selector de fotos y la
 * cámara del sistema le entregan a la app solo la imagen elegida.
 */
@Composable
fun FotoPerfilEditable(
    usuarioId: String,
    foto: ImageBitmap?,
    tamano: Dp,
    modifier: Modifier = Modifier,
    viewModel: FotoPerfilViewModel = viewModel()
) {
    val contexto = LocalContext.current
    val estado by viewModel.estado.collectAsState()
    var mostrarOpciones by remember { mutableStateOf(false) }

    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.usarImagen(usuarioId, uri)
    }
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { imagen ->
        if (imagen != null) viewModel.usarFotoCamara(usuarioId, imagen)
    }

    LaunchedEffect(estado) {
        when (val actual = estado) {
            is EstadoFormulario.Error -> {
                Toast.makeText(contexto, actual.mensaje, Toast.LENGTH_LONG).show()
                viewModel.reiniciar()
            }
            is EstadoFormulario.Exito -> viewModel.reiniciar()
            else -> Unit
        }
    }

    AvatarPerfil(
        foto = foto,
        tamano = tamano,
        editable = true,
        cargando = estado is EstadoFormulario.Guardando,
        onClick = { mostrarOpciones = true },
        modifier = modifier.testTag("foto_perfil_editable")
    )

    if (mostrarOpciones) {
        DialogoApp(
            onDismissRequest = { mostrarOpciones = false },
            title = { Text(stringResource(R.string.foto_perfil), fontWeight = FontWeight.Bold, color = Colores.texto) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    // Con el teléfono girado el diálogo es bajo: el contenido se desplaza
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OpcionFoto(
                        icono = painterResource(R.drawable.ic_galeria),
                        texto = stringResource(R.string.foto_galeria),
                        etiquetaPrueba = "foto_galeria"
                    ) {
                        mostrarOpciones = false
                        galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                    OpcionFoto(
                        icono = painterResource(R.drawable.ic_camara),
                        texto = stringResource(R.string.foto_tomar),
                        etiquetaPrueba = "foto_camara"
                    ) {
                        mostrarOpciones = false
                        try {
                            camara.launch(null)
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(contexto, Idioma.texto(R.string.foto_sin_camara), Toast.LENGTH_LONG).show()
                        }
                    }
                    if (foto != null) {
                        OpcionFoto(
                            icono = rememberVectorPainter(Icons.Default.Delete),
                            texto = stringResource(R.string.foto_quitar),
                            peligro = true,
                            etiquetaPrueba = "foto_quitar"
                        ) {
                            mostrarOpciones = false
                            viewModel.quitar(usuarioId)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { mostrarOpciones = false }) {
                    Text(stringResource(R.string.comun_cancelar), color = Colores.textoSecundario)
                }
            },
            containerColor = Colores.dialogo
        )
    }
}

@Composable
private fun OpcionFoto(
    icono: Painter,
    texto: String,
    etiquetaPrueba: String,
    peligro: Boolean = false,
    onClick: () -> Unit
) {
    val color = if (peligro) Colores.error else MaterialTheme.colorScheme.primary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(formaApp(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp)
            .testTag(etiquetaPrueba)
    ) {
        Icon(painter = icono, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyLarge,
            color = if (peligro) Colores.error else Colores.texto
        )
    }
}
