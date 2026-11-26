package com.example.myapplication.ui.screens.estudiante

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.data.Categorias
import com.example.myapplication.data.Prestamo
import com.example.myapplication.data.Usuario
import com.example.myapplication.ui.components.ActivarRecordatorios
import com.example.myapplication.ui.components.Desplegable
import com.example.myapplication.ui.components.EncabezadoInstitucional
import com.example.myapplication.ui.components.ImagenArticulo
import com.example.myapplication.ui.components.ListaConEncabezado
import com.example.myapplication.ui.components.TarjetaApp
import com.example.myapplication.ui.components.TarjetaElevadaApp
import com.example.myapplication.ui.components.TituloSeccionNeon
import com.example.myapplication.ui.components.fondoIconoApp
import com.example.myapplication.ui.components.lineaSuperiorNeon
import com.example.myapplication.ui.screens.horario.HorarioLista
import com.example.myapplication.ui.theme.formaApp
import com.example.myapplication.utils.Fechas
import com.example.myapplication.utils.Idioma
import com.example.myapplication.utils.Recordatorios
import com.example.myapplication.utils.Telefonos
import com.example.myapplication.viewmodel.ArticuloCatalogo
import com.example.myapplication.viewmodel.EstudianteUiState
import com.example.myapplication.viewmodel.EstudianteViewModel
import com.example.myapplication.ui.theme.Colores

/**
 * Interfaz del estudiante. Tres pestañas de solo lectura:
 * catálogo con disponibilidad (RF-15), historial propio (RF-16) y horario de atención (RF-14).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstudianteHomeScreen(
    usuario: Usuario,
    onAbrirAjustes: () -> Unit,
    viewModel: EstudianteViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var pestana by rememberSaveable { mutableStateOf(0) }

    LaunchedEffect(usuario.id) {
        viewModel.cargarDatosDe(usuario.id)
    }

    // RF-19: recordatorio de devolución. Aviso dentro de la app y notificación diaria.
    val recordatorio = remember(uiState.prestamos, Idioma.actual) {
        uiState.prestamos.firstOrNull { it.estaActivo }?.let { Recordatorios.mensajePara(it) }
    }
    // Pide el permiso y programa la revisión diaria, si las notificaciones están activas en Ajustes.
    ActivarRecordatorios()

    LaunchedEffect(uiState.mensajeError) {
        uiState.mensajeError?.let { mensaje ->
            snackbarHostState.showSnackbar(mensaje)
            viewModel.limpiarError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "DEPORTSTOCK",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Colores.sobreBarra
                        )
                        Text(
                            text = usuario.nombre.ifBlank { usuario.correo },
                            style = MaterialTheme.typography.bodySmall,
                            color = Colores.sobreBarraSecundario,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Colores.barra)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Colores.barraInferior,
                // En neón, línea cian con brillo en el borde de arriba
                modifier = Modifier.lineaSuperiorNeon()
            ) {
                val colores = NavigationBarItemDefaults.colors(
                    selectedIconColor = Colores.sobrePrimario,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = Colores.textoSecundario,
                    unselectedTextColor = Colores.textoSecundario
                )
                NavigationBarItem(
                    selected = pestana == 0,
                    onClick = { pestana = 0 },
                    icon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = { Text(stringResource(R.string.estudiante_catalogo)) },
                    colors = colores,
                    modifier = Modifier.testTag("tab_catalogo")
                )
                NavigationBarItem(
                    selected = pestana == 1,
                    onClick = { pestana = 1 },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    label = { Text(stringResource(R.string.estudiante_mis_prestamos)) },
                    colors = colores,
                    modifier = Modifier.testTag("tab_mis_prestamos")
                )
                NavigationBarItem(
                    selected = pestana == 2,
                    onClick = { pestana = 2 },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                    label = { Text(stringResource(R.string.estudiante_horario)) },
                    colors = colores,
                    modifier = Modifier.testTag("tab_horario")
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Colores.fondoPantalla
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            EncabezadoInstitucional()

            // Sin celular, Bienestar no puede avisarle de un préstamo vencido o una novedad.
            if (!Telefonos.esValido(usuario.telefono)) {
                Surface(
                    color = Colores.infoFondo,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("aviso_sin_telefono")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.estudiante_agrega_telefono),
                            color = Colores.info,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onAbrirAjustes) {
                            Text(stringResource(R.string.comun_agregar), color = Colores.info, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Aviso de devolución próxima o vencida, visible en todas las pestañas
            if (recordatorio != null) {
                Surface(
                    color = Colores.advertenciaFondo,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("aviso_recordatorio")
                ) {
                    Text(
                        text = "⏰ $recordatorio",
                        color = Colores.advertencia,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (pestana) {
                    0 -> CatalogoTab(uiState)
                    1 -> MisPrestamosTab(uiState)
                    else -> HorarioTab(uiState)
                }
            }
        }
    }
}

// =============================================================================
// PESTAÑA 1: CATÁLOGO CON DISPONIBILIDAD (RF-15)
// =============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogoTab(uiState: EstudianteUiState) {
    var busqueda by rememberSaveable { mutableStateOf("") }
    var categoria by rememberSaveable { mutableStateOf("Todas") }
    var soloDisponibles by rememberSaveable { mutableStateOf(false) }
    val textoTodos = stringResource(R.string.estudiante_mostrar_todos)
    val textoSoloDisponibles = stringResource(R.string.estudiante_mostrar_disponibles)

    // Las categorías se guardan en español; se ordenan por el nombre que se ve en pantalla
    val categorias = remember(uiState.catalogo, Idioma.actual) {
        listOf("Todas") + uiState.catalogo.map { it.categoria }.distinct().sortedBy { Categorias.etiqueta(it) }
    }
    val articulos = remember(uiState.catalogo, busqueda, categoria, soloDisponibles) {
        val texto = busqueda.trim()
        uiState.catalogo.filter { articulo ->
            (texto.isBlank() || articulo.nombre.contains(texto, ignoreCase = true)) &&
                (categoria == "Todas" || articulo.categoria.equals(categoria, ignoreCase = true)) &&
                (!soloDisponibles || articulo.disponibles > 0)
        }
    }

    // Búsqueda y filtros arriba; con el teléfono girado se desplazan junto con el catálogo
    ListaConEncabezado(
        modifier = Modifier.fillMaxSize(),
        espacioLista = 10.dp,
        margenLista = PaddingValues(top = 4.dp, bottom = 16.dp),
        mostrarEstado = uiState.cargandoCatalogo || articulos.isEmpty(),
        estado = {
            if (uiState.cargandoCatalogo) {
                EstadoCentrado { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
            } else {
                EstadoCentrado {
                    Text("📦", fontSize = 48.sp)
                    Text(
                        text = if (uiState.catalogo.isEmpty()) stringResource(R.string.estudiante_catalogo_vacio) else stringResource(R.string.estudiante_catalogo_sin_resultados),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Colores.texto,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        encabezado = {
            OutlinedTextField(
                value = busqueda,
                onValueChange = { busqueda = it },
                placeholder = { Text(stringResource(R.string.estudiante_buscar), color = Colores.textoTenue, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = stringResource(R.string.comun_buscar), tint = MaterialTheme.colorScheme.primary)
                },
                trailingIcon = {
                    if (busqueda.isNotBlank()) {
                        IconButton(onClick = { busqueda = "" }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.comun_limpiar_busqueda), tint = Colores.textoSecundario)
                        }
                    }
                },
                singleLine = true,
                shape = formaApp(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Colores.bordeCampo,
                    focusedContainerColor = Colores.superficie,
                    unfocusedContainerColor = Colores.superficie,
                    focusedTextColor = Colores.texto,
                    unfocusedTextColor = Colores.texto,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp)
                    .testTag("busqueda_catalogo_input")
            )

            // Filtros en desplegables: categoría y disponibilidad
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Desplegable(
                    etiqueta = stringResource(R.string.comun_categoria),
                    opciones = categorias,
                    seleccion = categoria,
                    onSeleccion = { categoria = it },
                    colores = coloresDesplegable(),
                    etiquetaPrueba = "filtro_categoria",
                    textoOpcion = Categorias::etiqueta,
                    modifier = Modifier.weight(1f)
                )
                Desplegable(
                    etiqueta = stringResource(R.string.estudiante_mostrar),
                    opciones = listOf(MOSTRAR_TODOS, MOSTRAR_DISPONIBLES),
                    seleccion = if (soloDisponibles) MOSTRAR_DISPONIBLES else MOSTRAR_TODOS,
                    onSeleccion = { soloDisponibles = it == MOSTRAR_DISPONIBLES },
                    colores = coloresDesplegable(),
                    etiquetaPrueba = "filtro_disponibilidad",
                    textoOpcion = { if (it == MOSTRAR_DISPONIBLES) textoSoloDisponibles else textoTodos },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    ) {
        items(articulos, key = { "${it.categoria}|${it.nombre}" }) { articulo ->
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                ArticuloCatalogoCard(articulo)
            }
        }
    }
}

@Composable
private fun ArticuloCatalogoCard(articulo: ArticuloCatalogo) {
    val hayDisponibles = articulo.disponibles > 0
    val colorEstado = if (hayDisponibles) Colores.exito else Colores.naranja

    TarjetaElevadaApp(
        modifier = Modifier.fillMaxWidth(),
        shape = formaApp(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Colores.superficie),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
        acentoNeon = colorEstado
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .fondoIconoApp(Colores.superficieAlta),
                contentAlignment = Alignment.Center
            ) {
                ImagenArticulo(articulo.nombre, articulo.categoria, articulo.icono, tamano = 52.dp, tamanoEmoji = 26.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = articulo.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Colores.texto,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = Categorias.etiqueta(articulo.categoria),
                    style = MaterialTheme.typography.bodySmall,
                    color = Colores.acentoTexto
                )
                if (articulo.descripcion.isNotBlank()) {
                    Text(
                        text = articulo.descripcion,
                        style = MaterialTheme.typography.labelSmall,
                        color = Colores.textoSecundario,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (articulo.incompletos > 0) {
                    Text(
                        text = pluralStringResource(R.plurals.estudiante_unidades_incompletas, articulo.incompletos, articulo.incompletos),
                        style = MaterialTheme.typography.labelSmall,
                        color = Colores.incompleto
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = formaApp(8.dp),
                color = Colores.rellenoEtiqueta(colorEstado, 0.18f),
                border = BorderStroke(1.dp, Colores.rellenoEtiqueta(colorEstado, 0.7f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.estudiante_disponibles_de, articulo.disponibles, articulo.total),
                        fontWeight = FontWeight.Bold,
                        color = Colores.textoEtiqueta(colorEstado),
                        fontSize = 14.sp
                    )
                    Text(
                        text = stringResource(if (hayDisponibles) R.string.estudiante_disponibles else R.string.estudiante_agotado),
                        color = Colores.textoEtiqueta(colorEstado),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

// =============================================================================
// PESTAÑA 2: HISTORIAL DE PRÉSTAMOS PROPIOS (RF-16)
// =============================================================================
@Composable
private fun MisPrestamosTab(uiState: EstudianteUiState) {
    val activos = remember(uiState.prestamos) { uiState.prestamos.filter { it.estaActivo } }
    val finalizados = remember(uiState.prestamos) { uiState.prestamos.filter { !it.estaActivo } }
    val perfil = uiState.perfil

    if (uiState.cargandoPrestamos) {
        EstadoCentrado { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Aviso de bloqueo por novedad sin resolver (regla de negocio 4)
        if (perfil != null && perfil.bloqueado) {
            item(key = "aviso_bloqueo") {
                TarjetaApp(
                    colors = CardDefaults.cardColors(containerColor = Colores.errorFondo),
                    border = BorderStroke(1.5.dp, Colores.errorFuerte),
                    shape = formaApp(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("aviso_bloqueo")
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.estudiante_bloqueado_titulo),
                            fontWeight = FontWeight.ExtraBold,
                            color = Colores.error,
                            fontSize = 13.sp
                        )
                        Text(
                            // El motivo guardado en Firestore está en español: el texto sale del idioma de la app
                            text = stringResource(R.string.estudiante_bloqueado_detalle),
                            color = Colores.texto,
                            fontSize = 12.sp
                        )
                        Text(
                            text = stringResource(R.string.estudiante_bloqueado_accion),
                            color = Colores.salmon,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        item(key = "titulo_activos") { TituloSeccion(stringResource(R.string.estudiante_activos, activos.size)) }
        if (activos.isEmpty()) {
            item(key = "sin_activos") {
                Text(
                    text = stringResource(R.string.estudiante_sin_activos),
                    style = MaterialTheme.typography.bodySmall,
                    color = Colores.textoTenue
                )
            }
        } else {
            items(activos, key = { it.id }) { prestamo -> MiPrestamoCard(prestamo) }
        }

        item(key = "titulo_finalizados") { TituloSeccion(stringResource(R.string.estudiante_finalizados, finalizados.size)) }
        if (finalizados.isEmpty()) {
            item(key = "sin_finalizados") {
                Text(
                    text = stringResource(R.string.estudiante_sin_finalizados),
                    style = MaterialTheme.typography.bodySmall,
                    color = Colores.textoTenue
                )
            }
        } else {
            items(finalizados, key = { it.id }) { prestamo -> MiPrestamoCard(prestamo) }
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
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = Colores.acentoTexto,
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
private fun MiPrestamoCard(prestamo: Prestamo) {
    val vencido = prestamo.estaVencido()
    val (etiqueta, colorEtiqueta, fondoEtiqueta) = when {
        vencido -> Triple(stringResource(R.string.comun_insignia_vencido), Colores.error, Colores.errorFondo)
        prestamo.estaActivo -> Triple(stringResource(R.string.estudiante_insignia_por_devolver), Colores.advertencia, Colores.advertenciaFondo)
        prestamo.danoPendiente -> Triple(stringResource(R.string.estudiante_insignia_con_novedades), Colores.naranja, Colores.advertenciaFondo)
        else -> Triple(stringResource(R.string.comun_insignia_devuelto), Colores.acentoTexto, Colores.exitoFondo)
    }

    TarjetaApp(
        shape = formaApp(16.dp),
        colors = CardDefaults.cardColors(containerColor = Colores.superficie),
        border = BorderStroke(1.dp, if (vencido) Colores.errorFuerte else Colores.borde),
        // En neón, barra a la izquierda del color del estado
        acentoNeon = colorEtiqueta,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mi_prestamo_${prestamo.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.estudiante_prestamo_del, Fechas.corta(prestamo.fechaPrestamo?.toDate())),
                    fontWeight = FontWeight.Bold,
                    color = Colores.texto,
                    fontSize = 14.sp
                )
                Surface(shape = formaApp(6.dp), color = Colores.rellenoEtiqueta(colorEtiqueta, fondoEtiqueta)) {
                    Text(
                        text = etiqueta,
                        color = Colores.textoEtiqueta(colorEtiqueta),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Implementos y estado en que se devolvió cada uno
            prestamo.itemsEfectivos.forEach { linea ->
                val danado = linea.tieneNovedad
                Text(
                    text = buildString {
                        append("• ${linea.nombre}")
                        if (linea.cantidad > 1) append(" (x${linea.cantidad})")
                        if (!prestamo.estaActivo) {
                            append(
                                if (danado) " — ${linea.descripcionNovedad}: ${linea.observacion.ifBlank { stringResource(R.string.comun_sin_observacion_minuscula) }}"
                                else stringResource(R.string.estudiante_buen_estado)
                            )
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (danado) Colores.salmon else Colores.texto
                )
            }

            HorizontalDivider(color = Colores.borde, thickness = 1.dp)

            prestamo.fechaEntregaEsperada?.toDate()?.let { limite ->
                Text(
                    text = stringResource(R.string.comun_fecha_limite_de, Fechas.corta(limite)),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (vencido) Colores.error else Colores.textoTenue,
                    fontWeight = if (vencido) FontWeight.Bold else FontWeight.Normal
                )
            }
            prestamo.fechaEntregaReal?.toDate()?.let { devolucion ->
                Text(
                    text = stringResource(R.string.estudiante_devuelto_el, Fechas.conHora(devolucion)),
                    style = MaterialTheme.typography.labelSmall,
                    color = Colores.textoTenue
                )
            }
            if (prestamo.carnetRetenido && (prestamo.estaActivo || prestamo.danoPendiente)) {
                Text(
                    text = stringResource(R.string.estudiante_carnet_retenido),
                    style = MaterialTheme.typography.labelSmall,
                    color = Colores.advertencia
                )
            }
        }
    }
}

// =============================================================================
// PESTAÑA 3: HORARIO DE ATENCIÓN (RF-14)
// =============================================================================
@Composable
private fun HorarioTab(uiState: EstudianteUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.comun_horario_atencion),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Colores.texto
        )
        Text(
            text = stringResource(R.string.estudiante_horario_info),
            style = MaterialTheme.typography.bodySmall,
            color = Colores.acentoTexto
        )
        HorarioLista(horarios = uiState.horarios)
    }
}

// =============================================================================
// COMPONENTES COMPARTIDOS
// =============================================================================
@Composable
private fun EstadoCentrado(contenido: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = contenido
        )
    }
}

private const val MOSTRAR_TODOS = "Todos"
private const val MOSTRAR_DISPONIBLES = "Solo disponibles"

@Composable
private fun coloresDesplegable() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = Colores.bordeCampo,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = Colores.textoSecundario,
    focusedTextColor = Colores.texto,
    unfocusedTextColor = Colores.texto,
    focusedContainerColor = Colores.superficie,
    unfocusedContainerColor = Colores.superficie
)
