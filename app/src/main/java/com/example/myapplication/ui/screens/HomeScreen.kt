package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.data.Usuario
import com.example.myapplication.ui.components.ChipNeon
import com.example.myapplication.ui.components.MarcoIconoNeon
import com.example.myapplication.ui.components.PanelNeon
import com.example.myapplication.ui.components.TarjetaElevadaApp
import com.example.myapplication.ui.components.TonoNeon
import com.example.myapplication.ui.components.estiloBrillo
import com.example.myapplication.ui.theme.CortesNeon
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.ui.components.ActivarRecordatorios
import com.example.myapplication.ui.components.AvatarPerfil
import com.example.myapplication.ui.components.FotoPerfilEditable
import com.example.myapplication.ui.components.EncabezadoInstitucional
import com.example.myapplication.ui.theme.Colores
import com.example.myapplication.ui.theme.Neon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    usuario: Usuario?,
    onRegistrarPrestamo: () -> Unit = {},
    onRegistrarDevolucion: () -> Unit = {},
    onMirarInventario: () -> Unit = {},
    onGestionarHorario: () -> Unit = {},
    onMirarReporte: () -> Unit = {},
    onAbrirAjustes: () -> Unit = {},
    foto: ImageBitmap? = null
) {
    val nombre = usuario?.nombre?.ifBlank { stringResource(R.string.inicio_nombre_por_defecto) } ?: stringResource(R.string.inicio_nombre_por_defecto)
    val correo = usuario?.correo?.ifBlank { "monitor@udenar.edu.co" } ?: "monitor@udenar.edu.co"
    val neon = Colores.esNeon

    // Revisión diaria de préstamos vencidos (si las notificaciones están activas en Ajustes).
    ActivarRecordatorios()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "DEPORTSTOCK",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Colores.sobreBarra
                    )
                },
                actions = {
                    IconButton(
                        onClick = onAbrirAjustes,
                        modifier = Modifier.testTag("abrir_ajustes_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.comun_ajustes),
                            tint = Colores.iconoBarra
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Colores.barra
                )
            )
        },
        containerColor = Colores.fondoPantalla
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            EncabezadoInstitucional()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // =========================================================================
                // ESPACIO DE PERFIL DE USUARIO DEL MONITOR (SOBRE EL TEXTO "BIENVENIDO")
                // Formato vertical: Ícono de usuario arriba, debajo Nombre, debajo Correo
                // =========================================================================
                // Contenido de la tarjeta de perfil; va dentro de un panel HUD con el tema neón.
                val contenidoPerfil: @Composable () -> Unit = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 22.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 1. Foto de perfil (al tocarla se cambia o se quita).
                        // En neón los arcos HUD salen del círculo: se deja aire arriba.
                        if (neon) Spacer(modifier = Modifier.height(6.dp))
                        if (usuario != null) {
                            FotoPerfilEditable(usuarioId = usuario.id, foto = foto, tamano = 80.dp)
                        } else {
                            AvatarPerfil(foto = null, tamano = 80.dp)
                        }

                        Spacer(modifier = Modifier.height(if (neon) 16.dp else 12.dp))

                        // 2. Debajo: Nombre
                        Text(
                            text = nombre,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Colores.texto,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("nombre_monitor_text")
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // 3. Debajo de nombre: Correo
                        Text(
                            text = correo,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (neon) Colores.textoSecundario else Colores.acentoTexto,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("correo_monitor_text")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (neon) {
                            ChipNeon(stringResource(R.string.inicio_monitor_en_turno), redondeo = 12.dp)
                        } else {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Colores.rellenoEtiqueta(MaterialTheme.colorScheme.primary, Colores.exitoFondo)
                            ) {
                                Text(
                                    text = stringResource(R.string.inicio_monitor_en_turno),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Colores.textoEtiqueta(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
                if (neon) {
                    PanelNeon(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("perfil_monitor_card")
                    ) {
                        contenidoPerfil()
                    }
                } else {
                    TarjetaElevadaApp(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = Colores.superficie
                        ),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("perfil_monitor_card")
                    ) {
                        contenidoPerfil()
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Texto Bienvenido debajo del Perfil
                Text(
                    text = stringResource(R.string.inicio_bienvenida, nombre),
                    // En neón: cian claro con brillo, como en el diseño
                    style = if (neon) {
                        MaterialTheme.typography.headlineMedium.merge(estiloBrillo(Neon.cian, 7.dp))
                    } else {
                        MaterialTheme.typography.headlineMedium
                    },
                    color = if (neon) Neon.tituloCian else Colores.texto,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.inicio_rol),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (neon) Colores.textoTenue else Colores.textoSecundario,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(30.dp))

                // Menú Principal de Acciones
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HomeMenuButton(
                        text = stringResource(R.string.comun_registrar_prestamo),
                        icon = Icons.Default.Add,
                        onClick = onRegistrarPrestamo
                    )
                    HomeMenuButton(
                        text = stringResource(R.string.comun_registrar_devolucion),
                        icon = Icons.Default.Refresh,
                        onClick = onRegistrarDevolucion
                    )
                    HomeMenuButton(
                        text = stringResource(R.string.inicio_mirar_inventario),
                        icon = Icons.AutoMirrored.Filled.List,
                        onClick = onMirarInventario
                    )
                    HomeMenuButton(
                        text = stringResource(R.string.comun_horario_semestre),
                        icon = Icons.Default.DateRange,
                        onClick = onGestionarHorario
                    )
                    HomeMenuButton(
                        text = stringResource(R.string.inicio_reportes),
                        icon = Icons.Default.Info,
                        onClick = onMirarReporte
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun HomeMenuButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    if (Colores.esNeon) {
        // Botón HUD: panel con esquinas cortadas, borde cian con brillo y el ícono en un marco
        PanelNeon(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp),
            cortes = CortesNeon.Boton,
            colorAcento = Neon.cian,
            detalles = false,
            onClick = onClick,
            rol = Role.Button
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MarcoIconoNeon(
                    icono = rememberVectorPainter(icon),
                    tono = TonoNeon.CIAN,
                    tamano = 54.dp,
                    tamanoIcono = 30.dp
                )
                Spacer(modifier = Modifier.width(20.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium.merge(estiloBrillo(Neon.tituloCian, 3.dp)),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    color = Colores.texto
                )
            }
        }
        return
    }
    TarjetaElevadaApp(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Colores.superficie,
            contentColor = Colores.texto
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Surface(
                modifier = Modifier.size(54.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(18.dp))

            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    MyApplicationTheme(darkTheme = true) {
        HomeScreen(usuario = Usuario(nombre = "Carlos Morales", correo = "carlos@udenar.edu.co"))
    }
}
