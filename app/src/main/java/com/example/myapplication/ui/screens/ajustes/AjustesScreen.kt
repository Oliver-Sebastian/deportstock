package com.example.myapplication.ui.screens.ajustes

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.data.IdiomaApp
import com.example.myapplication.data.ModoUso
import com.example.myapplication.data.TemaApp
import com.example.myapplication.data.Usuario
import com.example.myapplication.data.ProgramasAcademicos
import com.example.myapplication.ui.components.CampoPassword
import com.example.myapplication.ui.components.CampoTelefono
import com.example.myapplication.ui.components.ChipNeon
import com.example.myapplication.ui.components.Desplegable
import com.example.myapplication.ui.components.DialogoApp
import com.example.myapplication.ui.components.FotoPerfilEditable
import com.example.myapplication.ui.components.EncabezadoInstitucional
import com.example.myapplication.ui.components.LogoUdenarCompleto
import com.example.myapplication.ui.components.MarcoIconoNeon
import com.example.myapplication.ui.components.MarcoPieNeon
import com.example.myapplication.ui.components.MiniaturaTema
import com.example.myapplication.ui.components.PanelNeon
import com.example.myapplication.ui.components.TituloSeccionNeon
import com.example.myapplication.ui.components.TonoNeon
import com.example.myapplication.ui.components.alturaCompacta
import com.example.myapplication.ui.components.brilloNeon
import com.example.myapplication.ui.components.faltaPermisoDeNotificaciones
import com.example.myapplication.ui.components.lineaInferiorNeon
import com.example.myapplication.ui.theme.Colores
import com.example.myapplication.ui.theme.CortesNeon
import com.example.myapplication.ui.theme.LocalRelojNeon
import com.example.myapplication.ui.theme.Neon
import com.example.myapplication.ui.theme.RelojNeon
import com.example.myapplication.ui.theme.formaApp
import com.example.myapplication.ui.theme.formaBoton
import com.example.myapplication.ui.theme.trazoNeon
import com.example.myapplication.utils.Idioma
import com.example.myapplication.utils.Recordatorios
import com.example.myapplication.utils.Telefonos
import com.example.myapplication.utils.ValidacionPassword
import com.example.myapplication.viewmodel.AjustesViewModel
import com.example.myapplication.viewmodel.EstadoFormulario
import kotlinx.coroutines.launch

/** Diálogo abierto en Ajustes. Es un enum para que sobreviva a la rotación de pantalla. */
private enum class DialogoAjustes { NINGUNO, DATOS, ACTUALIZAR, CONTRASENA, REGLAMENTO, ACERCA_DE, CERRAR_SESION }

/**
 * Ajustes, común a monitor y estudiante.
 *
 * - Foto de perfil: se cambia tocando el círculo de la tarjeta de la cuenta.
 * - Modo de uso: solo para monitores con permiso vigente (`usuario.esMonitor`); alterna entre
 *   el modo monitor y el modo estudiante. A un estudiante no se le muestra.
 * - Opciones de usuario: datos de la cuenta (según el rol), actualizar información (nombre,
 *   programa y celular), cambiar contraseña y cerrar sesión.
 * - Opciones de la aplicación: apariencia (Oscuro, Claro, Udenar), idioma (Español o English),
 *   notificaciones de préstamos y vencimientos, reglamento y acerca de.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesScreen(
    usuario: Usuario,
    onVolver: () -> Unit,
    onCerrarSesion: () -> Unit,
    foto: ImageBitmap? = null,
    onVerMasDisenos: () -> Unit = {},
    periodoMonitor: String? = null,
    onCambiarModo: (String) -> Unit = {},
    viewModel: AjustesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val contexto = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var dialogo by rememberSaveable { mutableStateOf(DialogoAjustes.NINGUNO) }

    // El rol que muestra la cuenta es el modo en uso (un monitor en modo estudiante es estudiante)
    val rol = if (usuario.enModoMonitor) stringResource(R.string.rol_monitor) else stringResource(R.string.rol_estudiante)

    val permisoNotificaciones = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (!concedido) {
            scope.launch {
                snackbarHostState.showSnackbar(Idioma.texto(R.string.ajustes_sin_permiso_notificaciones))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.comun_ajustes),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Colores.sobreBarra
                        )
                        Text(
                            text = stringResource(R.string.ajustes_subtitulo),
                            style = MaterialTheme.typography.bodySmall,
                            color = Colores.sobreBarraSecundario
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVolver, modifier = Modifier.testTag("volver_ajustes_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.comun_volver),
                            tint = Colores.iconoBarra
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Colores.barra)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TarjetaCuenta(usuario = usuario, rol = rol, foto = foto)

                // ============================== MODO DE USO ==============================
                // Solo existe para monitores con permiso vigente en monitores_autorizados.
                if (usuario.esMonitor) {
                    TituloSeccion(stringResource(R.string.ajustes_seccion_modo))
                    GrupoAjustes {
                        SelectorModo(
                            modoActual = usuario.modoActivo,
                            periodo = periodoMonitor,
                            onSeleccion = onCambiarModo
                        )
                    }
                }

                // =========================== OPCIONES DE USUARIO ===========================
                TituloSeccion(stringResource(R.string.ajustes_seccion_usuario))
                GrupoAjustes {
                    FilaAjuste(
                        icono = rememberVectorPainter(Icons.Default.Person),
                        titulo = stringResource(R.string.ajustes_datos_de, rol),
                        subtitulo = stringResource(R.string.ajustes_datos_subtitulo),
                        onClick = { dialogo = DialogoAjustes.DATOS },
                        etiquetaPrueba = "ajuste_datos",
                        tono = TonoNeon.LIMA
                    )
                    SeparadorFila()
                    val faltaTelefono = !Telefonos.esValido(usuario.telefono)
                    FilaAjuste(
                        icono = rememberVectorPainter(Icons.Default.Edit),
                        titulo = stringResource(R.string.ajustes_actualizar),
                        subtitulo = if (faltaTelefono) stringResource(R.string.ajustes_falta_telefono) else stringResource(R.string.ajustes_actualizar_subtitulo),
                        subtituloDestacado = faltaTelefono,
                        onClick = { dialogo = DialogoAjustes.ACTUALIZAR },
                        etiquetaPrueba = "ajuste_actualizar",
                        tono = TonoNeon.CIAN
                    )
                    SeparadorFila()
                    FilaAjuste(
                        icono = rememberVectorPainter(Icons.Default.Lock),
                        titulo = stringResource(R.string.ajustes_cambiar_contrasena),
                        subtitulo = stringResource(R.string.ajustes_cambiar_contrasena_subtitulo),
                        onClick = { dialogo = DialogoAjustes.CONTRASENA },
                        etiquetaPrueba = "ajuste_contrasena",
                        tono = TonoNeon.AMARILLO
                    )
                    SeparadorFila()
                    FilaAjuste(
                        icono = rememberVectorPainter(Icons.AutoMirrored.Filled.ExitToApp),
                        titulo = stringResource(R.string.comun_cerrar_sesion),
                        subtitulo = stringResource(R.string.ajustes_cerrar_sesion_subtitulo),
                        onClick = { dialogo = DialogoAjustes.CERRAR_SESION },
                        peligro = true,
                        etiquetaPrueba = "ajuste_cerrar_sesion"
                    )
                }

                // ========================= OPCIONES DE LA APLICACIÓN =========================
                TituloSeccion(stringResource(R.string.ajustes_seccion_app))
                GrupoAjustes {
                    SelectorTema(
                        // Colores.tema es el tema aplicado; también cambia desde "Ver más diseños".
                        temaActual = Colores.tema,
                        onSeleccion = viewModel::cambiarTema,
                        onVerMas = onVerMasDisenos
                    )
                    SeparadorFila()
                    SelectorIdioma(
                        // Idioma.actual es el idioma aplicado; al cambiarlo, toda la app se actualiza.
                        idiomaActual = Idioma.actual,
                        onSeleccion = viewModel::cambiarIdioma
                    )
                    SeparadorFila()
                    FilaInterruptor(
                        icono = rememberVectorPainter(Icons.Default.Notifications),
                        titulo = stringResource(R.string.ajustes_notificaciones),
                        subtitulo = if (usuario.enModoMonitor) {
                            stringResource(R.string.ajustes_notificaciones_monitor)
                        } else {
                            stringResource(R.string.ajustes_notificaciones_estudiante, Recordatorios.DIAS_AVISO)
                        },
                        activo = uiState.notificaciones,
                        onCambio = { activar ->
                            viewModel.cambiarNotificaciones(activar)
                            if (activar && faltaPermisoDeNotificaciones(contexto)) {
                                permisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        etiquetaPrueba = "ajuste_notificaciones",
                        tono = TonoNeon.TENUE_AMARILLO
                    )
                    SeparadorFila()
                    FilaAjuste(
                        icono = rememberVectorPainter(Icons.Default.Check),
                        titulo = stringResource(R.string.ajustes_reglamento),
                        subtitulo = stringResource(R.string.ajustes_reglamento_subtitulo),
                        onClick = { dialogo = DialogoAjustes.REGLAMENTO },
                        etiquetaPrueba = "ajuste_reglamento",
                        tono = TonoNeon.CIAN
                    )
                    SeparadorFila()
                    FilaAjuste(
                        icono = rememberVectorPainter(Icons.Default.Info),
                        titulo = stringResource(R.string.ajustes_acerca_de),
                        subtitulo = stringResource(R.string.ajustes_acerca_de_subtitulo),
                        onClick = { dialogo = DialogoAjustes.ACERCA_DE },
                        etiquetaPrueba = "ajuste_acerca_de",
                        tono = TonoNeon.CIAN
                    )
                }

                val pie = stringResource(R.string.ajustes_pie)
                if (Colores.esNeon) {
                    MarcoPieNeon(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 8.dp)
                    ) {
                        Text(pie, style = MaterialTheme.typography.labelMedium, color = Colores.textoTenue, textAlign = TextAlign.Center)
                    }
                } else {
                    Text(
                        text = pie,
                        style = MaterialTheme.typography.labelSmall,
                        color = Colores.textoTenue,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 8.dp)
                    )
                }
            }
        }
    }

    val cerrar = { dialogo = DialogoAjustes.NINGUNO }
    when (dialogo) {
        DialogoAjustes.NINGUNO -> Unit
        DialogoAjustes.DATOS -> DatosUsuarioDialog(usuario = usuario, rol = rol, onCerrar = cerrar)
        DialogoAjustes.ACTUALIZAR -> ActualizarInformacionDialog(
            usuario = usuario,
            estado = uiState.actualizacion,
            onGuardar = { nombre, programa, telefono ->
                viewModel.actualizarInformacion(usuario.id, nombre, programa, telefono)
            },
            onCerrar = {
                viewModel.reiniciarActualizacion()
                cerrar()
            },
            onExito = {
                viewModel.reiniciarActualizacion()
                cerrar()
                scope.launch { snackbarHostState.showSnackbar(Idioma.texto(R.string.ajustes_informacion_actualizada)) }
            }
        )
        DialogoAjustes.CONTRASENA -> CambiarPasswordDialog(
            estado = uiState.cambioPassword,
            onGuardar = viewModel::cambiarPassword,
            onCerrar = {
                viewModel.reiniciarCambioPassword()
                cerrar()
            },
            onExito = {
                viewModel.reiniciarCambioPassword()
                cerrar()
                scope.launch { snackbarHostState.showSnackbar(Idioma.texto(R.string.ajustes_contrasena_actualizada)) }
            }
        )
        DialogoAjustes.REGLAMENTO -> ReglamentoDialog(onCerrar = cerrar)
        DialogoAjustes.ACERCA_DE -> AcercaDeDialog(onCerrar = cerrar)
        DialogoAjustes.CERRAR_SESION -> CerrarSesionDialog(
            nombre = usuario.nombre.ifBlank { usuario.correo },
            onConfirmar = {
                cerrar()
                onCerrarSesion()
            },
            onCancelar = cerrar
        )
    }
}

// =============================================================================
// PIEZAS DE LA PANTALLA
// =============================================================================

@Composable
private fun TarjetaCuenta(usuario: Usuario, rol: String, foto: ImageBitmap?) {
    if (Colores.esNeon) {
        // Diseño Cyberpunk: panel HUD con la foto arriba (con sus arcos), nombre, correo y rol
        PanelNeon(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tarjeta_cuenta")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 22.dp)
            ) {
                Spacer(Modifier.height(6.dp))
                FotoPerfilEditable(usuarioId = usuario.id, foto = foto, tamano = 84.dp)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = usuario.nombre.ifBlank { rol },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Colores.texto,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = usuario.correo,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Colores.textoSecundario,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                ChipNeon(rol.uppercase())
            }
        }
        return
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Colores.superficie,
        border = BorderStroke(Colores.anchoBorde(1.dp), Colores.borde),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tarjeta_cuenta")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            FotoPerfilEditable(usuarioId = usuario.id, foto = foto, tamano = 60.dp)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = usuario.nombre.ifBlank { rol },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Colores.texto
                )
                Text(
                    text = usuario.correo,
                    style = MaterialTheme.typography.bodySmall,
                    color = Colores.acentoTexto
                )
                Spacer(Modifier.height(6.dp))
                Surface(shape = RoundedCornerShape(20.dp), color = Colores.rellenoEtiqueta(Colores.acentoTexto, Colores.exitoFondo)) {
                    Text(
                        text = rol.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Colores.textoEtiqueta(Colores.acentoTexto),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TituloSeccion(texto: String) {
    if (Colores.esNeon) {
        TituloSeccionNeon(texto)
        return
    }
    Text(
        text = texto,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        color = Colores.acentoTexto,
        modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 2.dp)
    )
}

/** Tarjeta que agrupa filas de ajustes. */
@Composable
private fun GrupoAjustes(contenido: @Composable ColumnScope.() -> Unit) {
    if (Colores.esNeon) {
        PanelNeon(modifier = Modifier.fillMaxWidth(), cortes = CortesNeon.Grupo) {
            Column(content = contenido)
        }
        return
    }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Colores.superficie,
        border = BorderStroke(Colores.anchoBorde(1.dp), Colores.borde),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(content = contenido)
    }
}

@Composable
private fun SeparadorFila() {
    // Empieza donde empieza el texto de la fila (en neón el marco del ícono es más grande)
    val inicio = if (Colores.esNeon) 72.dp else 64.dp
    HorizontalDivider(color = Colores.borde, thickness = 1.dp, modifier = Modifier.padding(start = inicio))
}

/**
 * Ícono de la fila dentro de un cuadro redondeado del color de acento.
 * Con el tema neón, en un marco HUD del [tono] de la fila.
 */
@Composable
private fun IconoFila(icono: Painter, color: Color, tono: TonoNeon = TonoNeon.CIAN) {
    if (Colores.esNeon) {
        MarcoIconoNeon(icono = icono, tono = tono)
        return
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
    ) {
        Icon(painter = icono, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun FilaAjuste(
    icono: Painter,
    titulo: String,
    subtitulo: String,
    onClick: () -> Unit,
    etiquetaPrueba: String,
    peligro: Boolean = false,
    subtituloDestacado: Boolean = false,
    tono: TonoNeon = TonoNeon.CIAN
) {
    val acento = if (peligro) Colores.error else MaterialTheme.colorScheme.primary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag(etiquetaPrueba)
    ) {
        IconoFila(icono, acento, if (peligro) TonoNeon.ROJO else tono)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (peligro) Colores.error else Colores.texto
            )
            Text(
                text = subtitulo,
                style = MaterialTheme.typography.bodySmall,
                color = if (subtituloDestacado) Colores.advertencia else Colores.textoTenue
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = if (Colores.esNeon) Neon.chevron else Colores.textoTenue
        )
    }
}

/** Fila con interruptor: toda la fila responde al toque, no solo el switch. */
@Composable
private fun FilaInterruptor(
    icono: Painter,
    titulo: String,
    subtitulo: String,
    activo: Boolean,
    onCambio: (Boolean) -> Unit,
    etiquetaPrueba: String,
    tono: TonoNeon = TonoNeon.CIAN
) {
    val reloj = LocalRelojNeon.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = activo, onValueChange = onCambio, role = Role.Switch)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag(etiquetaPrueba)
    ) {
        IconoFila(icono, MaterialTheme.colorScheme.primary, tono)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Colores.texto
            )
            Text(text = subtitulo, style = MaterialTheme.typography.bodySmall, color = Colores.textoTenue)
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = activo,
            onCheckedChange = null,
            // En neón, la pista encendida brilla en lima
            modifier = Modifier.brilloNeon(reloj, MaterialTheme.colorScheme.primary, encendido = Colores.esNeon && activo),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Colores.sobrePrimario,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = Colores.textoTenue,
                uncheckedTrackColor = Colores.deshabilitado,
                uncheckedBorderColor = Colores.textoTenue
            )
        )
    }
}

/**
 * Apariencia: los tres temas principales en miniatura y el botón "Ver más diseños", que abre
 * la lista completa (ahí van los diseños nuevos, como Cyberpunk). El cambio se ve al instante
 * y queda guardado.
 */
@Composable
private fun SelectorTema(temaActual: TemaApp, onSeleccion: (TemaApp) -> Unit, onVerMas: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconoFila(painterResource(R.drawable.ic_paleta), MaterialTheme.colorScheme.primary, TonoNeon.TENUE)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = stringResource(R.string.ajustes_apariencia),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Colores.texto
                )
                Text(
                    text = stringResource(temaActual.descripcion),
                    style = MaterialTheme.typography.bodySmall,
                    color = Colores.textoTenue
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
        ) {
            TemaApp.PRINCIPALES.forEach { tema ->
                OpcionTema(
                    tema = tema,
                    seleccionado = tema == temaActual,
                    onClick = { onSeleccion(tema) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        BotonVerMasDisenos(temaActual = temaActual, onClick = onVerMas)
    }
}

/**
 * "Ver más diseños": abre la lista de todos los diseños. Si el tema en uso no es uno de los
 * tres principales (por ejemplo, Cyberpunk), lo indica aquí.
 */
@Composable
private fun BotonVerMasDisenos(temaActual: TemaApp, onClick: () -> Unit) {
    val enLista = temaActual !in TemaApp.PRINCIPALES
    val contenido: @Composable () -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.ajustes_ver_mas_disenos),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Colores.texto
                )
                Text(
                    text = if (enLista) {
                        stringResource(R.string.ajustes_en_uso_tema, stringResource(temaActual.titulo))
                    } else {
                        pluralStringResource(R.plurals.ajustes_disenos_disponibles, TemaApp.entries.size, TemaApp.entries.size)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (enLista) FontWeight.Bold else FontWeight.Normal,
                    color = if (enLista) MaterialTheme.colorScheme.primary else Colores.textoTenue
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = if (Colores.esNeon) Neon.chevron else Colores.textoTenue
            )
        }
    }
    if (Colores.esNeon) {
        PanelNeon(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ver_mas_disenos"),
            cortes = CortesNeon.Boton,
            colorBorde = if (enLista) Neon.lima else Neon.cian,
            colorAcento = if (enLista) Neon.lima else Neon.magenta,
            detalles = false,
            luz = false,
            onClick = onClick,
            rol = Role.Button
        ) { contenido() }
    } else {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            color = Colores.superficieAlta,
            border = BorderStroke(if (enLista) 2.dp else 1.dp, if (enLista) MaterialTheme.colorScheme.primary else Colores.borde),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ver_mas_disenos")
        ) { contenido() }
    }
}

/** Un tema principal: su miniatura (con sus propios colores) y su nombre. */
@Composable
private fun OpcionTema(tema: TemaApp, seleccionado: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val forma = RoundedCornerShape(14.dp)
    val neon = Colores.esNeon
    val reloj = LocalRelojNeon.current
    val colorMarco = when {
        neon && seleccionado -> Neon.lima
        neon -> Neon.apagado
        seleccionado -> MaterialTheme.colorScheme.primary
        else -> Colores.borde
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            // En neón el marco elegido brilla (se dibuja antes del recorte para que el brillo se vea)
            .then(if (neon && seleccionado) Modifier.marcoNeonRedondeado(reloj, colorMarco, 14.dp) else Modifier)
            .clip(forma)
            .then(if (neon) Modifier.background(Neon.panel) else Modifier)
            .border(
                width = if (seleccionado) 2.dp else 1.dp,
                color = colorMarco,
                shape = forma
            )
            .selectable(selected = seleccionado, onClick = onClick, role = Role.RadioButton)
            .padding(8.dp)
            .testTag("tema_${tema.name.lowercase()}")
    ) {
        // La marca de "elegido" va sobre la miniatura y no junto al nombre: así el nombre
        // tiene todo el ancho y no se corta (antes "Institucional" quedaba en "Institucio").
        Box(modifier = Modifier.fillMaxWidth()) {
            MiniaturaTema(
                tema = tema,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
            )
            // En neón la marca va junto al nombre ("✓ Oscuro"), como en el diseño Cyberpunk;
            // los nombres de los temas principales son cortos y caben.
            if (seleccionado && !neon) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(18.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .border(1.5.dp, Colores.superficie, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Colores.sobrePrimario,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (seleccionado && neon) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Neon.lima,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(3.dp))
            }
            // Una sola línea; si con letra grande del sistema no cabe, termina en "…" en vez de cortarse.
            Text(
                text = stringResource(tema.titulo),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    seleccionado -> Colores.texto
                    neon -> Colores.textoTenue
                    else -> Colores.textoSecundario
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
    }
}

/**
 * Idioma: Español o English, cada uno con su propio nombre. El cambio se ve al instante en
 * toda la app y queda guardado. Misma estructura que [SelectorTema].
 */
@Composable
private fun SelectorIdioma(idiomaActual: IdiomaApp, onSeleccion: (IdiomaApp) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconoFila(painterResource(R.drawable.ic_idioma), MaterialTheme.colorScheme.primary, TonoNeon.TENUE)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = stringResource(R.string.ajustes_idioma),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Colores.texto
                )
                Text(
                    text = stringResource(R.string.ajustes_idioma_subtitulo),
                    style = MaterialTheme.typography.bodySmall,
                    color = Colores.textoTenue
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
        ) {
            IdiomaApp.entries.forEach { idioma ->
                OpcionSelector(
                    texto = stringResource(idioma.nombre),
                    seleccionado = idioma == idiomaActual,
                    onClick = { onSeleccion(idioma) },
                    etiquetaPrueba = "idioma_${idioma.codigo}",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Modo de uso del monitor: "Monitor" (gestiona préstamos, devoluciones e inventario) o
 * "Estudiante" (pide implementos para sí mismo). Misma estructura que [SelectorIdioma]. Al
 * elegir, la app pasa al inicio de ese modo sin cerrar sesión.
 */
@Composable
private fun SelectorModo(modoActual: String, periodo: String?, onSeleccion: (String) -> Unit) {
    val enModoMonitor = modoActual == ModoUso.MONITOR
    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconoFila(painterResource(R.drawable.ic_modo), MaterialTheme.colorScheme.primary, TonoNeon.LIMA)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = stringResource(R.string.ajustes_modo),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Colores.texto
                )
                Text(
                    text = stringResource(
                        if (enModoMonitor) R.string.ajustes_modo_monitor_detalle else R.string.ajustes_modo_estudiante_detalle
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = Colores.textoTenue
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
        ) {
            listOf(ModoUso.MONITOR to R.string.rol_monitor, ModoUso.ESTUDIANTE to R.string.rol_estudiante).forEach { (modo, nombre) ->
                OpcionSelector(
                    texto = stringResource(nombre),
                    seleccionado = modo == modoActual,
                    onClick = { onSeleccion(modo) },
                    etiquetaPrueba = "modo_$modo",
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = if (periodo != null) {
                stringResource(R.string.ajustes_modo_autorizado_periodo, periodo)
            } else {
                stringResource(R.string.ajustes_modo_autorizado)
            },
            style = MaterialTheme.typography.labelSmall,
            color = Colores.textoTenue
        )
    }
}

/**
 * Una opción de [SelectorIdioma] o [SelectorModo]: su nombre y la marca de elegido, con el mismo
 * marco que [OpcionTema].
 */
@Composable
private fun OpcionSelector(
    texto: String,
    seleccionado: Boolean,
    onClick: () -> Unit,
    etiquetaPrueba: String,
    modifier: Modifier
) {
    val forma = RoundedCornerShape(14.dp)
    val neon = Colores.esNeon
    val reloj = LocalRelojNeon.current
    val colorMarco = when {
        neon && seleccionado -> Neon.lima
        neon -> Neon.apagado
        seleccionado -> MaterialTheme.colorScheme.primary
        else -> Colores.borde
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .then(if (neon && seleccionado) Modifier.marcoNeonRedondeado(reloj, colorMarco, 14.dp) else Modifier)
            .clip(forma)
            .background(if (neon) Neon.panel else Colores.superficieAlta)
            .border(
                width = if (seleccionado) 2.dp else 1.dp,
                color = colorMarco,
                shape = forma
            )
            .selectable(selected = seleccionado, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 8.dp, vertical = 12.dp)
            .testTag(etiquetaPrueba)
    ) {
        if (seleccionado) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = if (neon) Neon.lima else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = texto,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Normal,
            color = when {
                seleccionado -> Colores.texto
                neon -> Colores.textoTenue
                else -> Colores.textoSecundario
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

/** Brillo neón alrededor de un elemento redondeado (la miniatura elegida). */
internal fun Modifier.marcoNeonRedondeado(reloj: RelojNeon?, color: Color, radio: Dp): Modifier = drawWithCache {
    val camino = Path().apply {
        addRoundRect(RoundRect(Rect(Offset.Zero, size), CornerRadius(radio.toPx())))
    }
    onDrawBehind {
        trazoNeon(camino, color, 1.5.dp.toPx(), reloj?.pulso?.value ?: 0.85f)
    }
}

// =============================================================================
// DIÁLOGOS
// =============================================================================

@Composable
private fun DatosUsuarioDialog(usuario: Usuario, rol: String, onCerrar: () -> Unit) {
    DialogoApp(
        onDismissRequest = onCerrar,
        icon = {
            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
        },
        title = { Text(stringResource(R.string.ajustes_datos_de, rol), fontWeight = FontWeight.Bold, color = Colores.texto) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                // Con el teléfono girado el diálogo es bajo: el contenido se desplaza
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                DatoItem(etiqueta = stringResource(R.string.ajustes_dato_nombre), valor = usuario.nombre.ifBlank { stringResource(R.string.comun_sin_registrar) })
                DatoItem(etiqueta = stringResource(R.string.ajustes_dato_correo), valor = usuario.correo.ifBlank { stringResource(R.string.comun_sin_registrar) })
                DatoItem(etiqueta = stringResource(R.string.ajustes_dato_rol), valor = rol.uppercase())
                DatoItem(etiqueta = stringResource(R.string.ajustes_dato_id), valor = usuario.codigo.ifBlank { stringResource(R.string.comun_sin_registrar) })
                DatoItem(etiqueta = stringResource(R.string.ajustes_dato_programa), valor = ProgramasAcademicos.etiqueta(usuario.programa).ifBlank { stringResource(R.string.comun_sin_registrar) })
                DatoItem(
                    etiqueta = stringResource(R.string.ajustes_dato_celular),
                    valor = if (Telefonos.esValido(usuario.telefono)) Telefonos.formatear(usuario.telefono) else stringResource(R.string.comun_sin_registrar)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onCerrar,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("cerrar_datos_button"),
                shape = formaBoton()
            ) {
                Text(stringResource(R.string.comun_cerrar), color = Colores.sobrePrimario, fontWeight = FontWeight.Bold)
            }
        },
        tonoNeon = TonoNeon.LIMA,
        containerColor = Colores.dialogo
    )
}

@Composable
private fun DatoItem(etiqueta: String, valor: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // En neón cada dato lleva una línea tenue debajo, como una ficha HUD
            .then(if (Colores.esNeon) Modifier.lineaInferiorNeon().padding(bottom = 8.dp) else Modifier),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodyMedium,
            color = Colores.textoTenue,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = valor,
            style = MaterialTheme.typography.bodyMedium,
            color = Colores.texto,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Actualizar información: nombre, programa y celular. El correo y el código se muestran pero
 * no se editan (el correo es la cuenta; el código quedó reservado al verificar la cuenta).
 */
@Composable
private fun ActualizarInformacionDialog(
    usuario: Usuario,
    estado: EstadoFormulario,
    onGuardar: (nombre: String, programa: String, telefono: String) -> Unit,
    onCerrar: () -> Unit,
    onExito: () -> Unit
) {
    val programaEnLista = usuario.programa in ProgramasAcademicos.SEDE_IPIALES
    var nombre by rememberSaveable { mutableStateOf(usuario.nombre) }
    var programaSeleccionado by rememberSaveable {
        mutableStateOf(
            when {
                programaEnLista -> usuario.programa
                usuario.programa.isNotBlank() -> ProgramasAcademicos.OTRO
                else -> ""
            }
        )
    }
    var programaOtro by rememberSaveable { mutableStateOf(if (programaEnLista) "" else usuario.programa) }
    var telefono by rememberSaveable { mutableStateOf(Telefonos.normalizar(usuario.telefono)) }
    val programa = if (programaSeleccionado == ProgramasAcademicos.OTRO) programaOtro else programaSeleccionado
    val guardando = estado is EstadoFormulario.Guardando

    LaunchedEffect(estado) {
        if (estado is EstadoFormulario.Exito) onExito()
    }

    DialogoApp(
        onDismissRequest = { if (!guardando) onCerrar() },
        icon = {
            Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
        },
        title = { Text(stringResource(R.string.ajustes_actualizar), fontWeight = FontWeight.Bold, color = Colores.texto) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { if (it.length <= 80) nombre = it },
                    label = { Text(stringResource(R.string.comun_nombre_completo)) },
                    singleLine = true,
                    shape = formaApp(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("actualizar_nombre_input")
                )
                Desplegable(
                    etiqueta = stringResource(R.string.comun_programa_academico),
                    opciones = ProgramasAcademicos.opciones,
                    seleccion = programaSeleccionado,
                    onSeleccion = { programaSeleccionado = it },
                    placeholder = stringResource(R.string.comun_selecciona_programa),
                    etiquetaPrueba = "actualizar_programa_input",
                    textoOpcion = ProgramasAcademicos::etiqueta,
                    modifier = Modifier.fillMaxWidth()
                )
                if (programaSeleccionado == ProgramasAcademicos.OTRO) {
                    OutlinedTextField(
                        value = programaOtro,
                        onValueChange = { if (it.length <= 60) programaOtro = it },
                        label = { Text(stringResource(R.string.comun_cual_programa)) },
                        singleLine = true,
                        shape = formaApp(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("actualizar_programa_otro_input")
                    )
                }
                CampoTelefono(
                    valor = telefono,
                    onValorChange = { telefono = it },
                    etiquetaPrueba = "actualizar_telefono_input",
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(
                        R.string.ajustes_correo_y_codigo,
                        usuario.correo,
                        usuario.codigo.ifBlank { stringResource(R.string.comun_sin_registrar_minuscula) }
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = Colores.textoTenue
                )
                if (estado is EstadoFormulario.Error) {
                    Text(
                        text = estado.mensaje,
                        style = MaterialTheme.typography.bodySmall,
                        color = Colores.error,
                        modifier = Modifier.testTag("error_actualizar_informacion")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onGuardar(nombre, programa, telefono) },
                enabled = !guardando,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("guardar_informacion_button"),
                shape = formaBoton()
            ) {
                if (guardando) {
                    CircularProgressIndicator(
                        color = Colores.sobrePrimario,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text(stringResource(R.string.comun_guardar), color = Colores.sobrePrimario, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar, enabled = !guardando) {
                Text(stringResource(R.string.comun_cancelar), color = Colores.textoSecundario)
            }
        },
        containerColor = Colores.dialogo
    )
}

@Composable
private fun CambiarPasswordDialog(
    estado: EstadoFormulario,
    onGuardar: (actual: String, nueva: String, repetir: String) -> Unit,
    onCerrar: () -> Unit,
    onExito: () -> Unit
) {
    var actual by rememberSaveable { mutableStateOf("") }
    var nueva by rememberSaveable { mutableStateOf("") }
    var repetir by rememberSaveable { mutableStateOf("") }
    val guardando = estado is EstadoFormulario.Guardando

    LaunchedEffect(estado) {
        if (estado is EstadoFormulario.Exito) onExito()
    }

    DialogoApp(
        onDismissRequest = { if (!guardando) onCerrar() },
        icon = {
            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
        },
        title = { Text(stringResource(R.string.ajustes_cambiar_contrasena), fontWeight = FontWeight.Bold, color = Colores.texto) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                CampoPassword(
                    valor = actual,
                    onValorChange = { actual = it },
                    etiqueta = stringResource(R.string.ajustes_contrasena_actual),
                    etiquetaPrueba = "password_actual_input",
                    modifier = Modifier.fillMaxWidth()
                )
                CampoPassword(
                    valor = nueva,
                    onValorChange = { nueva = it },
                    etiqueta = stringResource(R.string.ajustes_contrasena_nueva),
                    esError = nueva.isNotEmpty() && !ValidacionPassword.esSegura(nueva),
                    textoApoyo = {
                        Column {
                            ValidacionPassword.requisitos.forEach { requisito ->
                                val cumple = requisito.cumple(nueva)
                                Text(
                                    text = (if (cumple) "✓ " else "• ") + requisito.descripcion,
                                    color = if (cumple) MaterialTheme.colorScheme.primary else Colores.textoTenue
                                )
                            }
                        }
                    },
                    etiquetaPrueba = "password_nueva_input",
                    modifier = Modifier.fillMaxWidth()
                )
                val noCoinciden = repetir.isNotEmpty() && repetir != nueva
                CampoPassword(
                    valor = repetir,
                    onValorChange = { repetir = it },
                    etiqueta = stringResource(R.string.ajustes_contrasena_repetir),
                    esError = noCoinciden,
                    textoApoyo = if (noCoinciden) {
                        { Text(Idioma.texto(R.string.comun_no_coinciden)) }
                    } else null,
                    etiquetaPrueba = "password_repetir_input",
                    modifier = Modifier.fillMaxWidth()
                )
                if (estado is EstadoFormulario.Error) {
                    Text(
                        text = estado.mensaje,
                        style = MaterialTheme.typography.bodySmall,
                        color = Colores.error,
                        modifier = Modifier.testTag("error_cambio_password")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onGuardar(actual, nueva, repetir) },
                enabled = !guardando,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("guardar_password_button"),
                shape = formaBoton()
            ) {
                if (guardando) {
                    CircularProgressIndicator(
                        color = Colores.sobrePrimario,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text(stringResource(R.string.comun_guardar), color = Colores.sobrePrimario, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar, enabled = !guardando) {
                Text(stringResource(R.string.comun_cancelar), color = Colores.textoSecundario)
            }
        },
        containerColor = Colores.dialogo
    )
}

@Composable
private fun ReglamentoDialog(onCerrar: () -> Unit) {
    val reglas = listOf(
        stringResource(R.string.ajustes_regla_1),
        stringResource(R.string.ajustes_regla_2),
        stringResource(R.string.ajustes_regla_3),
        stringResource(R.string.ajustes_regla_4),
        stringResource(R.string.ajustes_regla_5)
    )
    DialogoApp(
        onDismissRequest = onCerrar,
        icon = {
            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
        },
        title = { Text(stringResource(R.string.ajustes_reglamento_titulo), fontWeight = FontWeight.Bold, color = Colores.texto) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                reglas.forEachIndexed { indice, regla ->
                    Text(
                        text = "${indice + 1}. $regla",
                        style = MaterialTheme.typography.bodySmall,
                        color = Colores.textoSecundario
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onCerrar,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = formaBoton()
            ) {
                Text(stringResource(R.string.comun_entendido), color = Colores.sobrePrimario, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Colores.dialogo
    )
}

@Composable
private fun AcercaDeDialog(onCerrar: () -> Unit) {
    // Con el teléfono girado el diálogo no muestra íconos (no cabrían): el logo va, más chico,
    // dentro del texto, que se desplaza.
    val compacta = alturaCompacta()
    DialogoApp(
        onDismissRequest = onCerrar,
        icon = { LogoUdenarCompleto(modifier = Modifier.width(150.dp)) },
        title = { Text(stringResource(R.string.ajustes_acerca_de_titulo), fontWeight = FontWeight.Bold, color = Colores.texto) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (compacta) {
                    LogoUdenarCompleto(
                        modifier = Modifier
                            .width(110.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                }
                Text("DEPORTSTOCK · Udenar", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.ajustes_version), style = MaterialTheme.typography.bodySmall, color = Colores.texto)
                Text(
                    stringResource(R.string.ajustes_proposito),
                    style = MaterialTheme.typography.bodySmall,
                    color = Colores.textoSecundario
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onCerrar,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = formaBoton()
            ) {
                Text(stringResource(R.string.comun_cerrar), color = Colores.sobrePrimario, fontWeight = FontWeight.Bold)
            }
        },
        tonoNeon = null,
        containerColor = Colores.dialogo
    )
}

@Composable
private fun CerrarSesionDialog(nombre: String, onConfirmar: () -> Unit, onCancelar: () -> Unit) {
    DialogoApp(
        onDismissRequest = onCancelar,
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = null,
                tint = Colores.error,
                modifier = Modifier.size(36.dp)
            )
        },
        title = { Text(stringResource(R.string.ajustes_cerrar_sesion_pregunta), fontWeight = FontWeight.Bold, color = Colores.texto) },
        text = {
            Text(
                text = stringResource(R.string.ajustes_cerrar_sesion_texto, nombre),
                style = MaterialTheme.typography.bodyMedium,
                color = Colores.textoSecundario
            )
        },
        dismissButton = {
            OutlinedButton(onClick = onCancelar, shape = formaBoton()) {
                Text(stringResource(R.string.comun_cancelar), color = Colores.textoSecundario)
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmar,
                colors = ButtonDefaults.buttonColors(containerColor = Colores.errorBoton),
                modifier = Modifier.testTag("confirmar_cerrar_sesion_button"),
                shape = formaBoton()
            ) {
                Text(stringResource(R.string.ajustes_cerrar_sesion_confirmar), color = Colores.sobreError, fontWeight = FontWeight.Bold)
            }
        },
        tonoNeon = TonoNeon.ROJO,
        containerColor = Colores.dialogo
    )
}
