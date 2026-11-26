package com.example.myapplication.ui.screens.inventario

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.data.Categorias
import com.example.myapplication.data.InventarioRepository
import com.example.myapplication.data.EstadoItem
import com.example.myapplication.data.ItemDeportivo
import com.example.myapplication.data.TipoArticulo
import com.example.myapplication.data.TipoControl
import com.example.myapplication.ui.components.DialogoApp
import com.example.myapplication.ui.components.ImagenArticulo
import com.example.myapplication.ui.components.ImagenCategoria
import com.example.myapplication.ui.components.ListaConEncabezado
import com.example.myapplication.ui.components.TarjetaElevadaApp
import com.example.myapplication.ui.components.TonoNeon
import com.example.myapplication.ui.components.estiloBrillo
import com.example.myapplication.ui.components.fondoIconoApp
import com.example.myapplication.ui.components.imagen
import com.example.myapplication.ui.theme.formaApp
import com.example.myapplication.ui.theme.formaBoton
import com.example.myapplication.utils.Idioma
import com.example.myapplication.viewmodel.InventarioViewModel
import com.example.myapplication.ui.components.ContadorCantidad
import com.example.myapplication.ui.theme.Colores

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventarioScreen(
    onVolver: () -> Unit,
    viewModel: InventarioViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Estados para diálogos
    var itemAEditar by remember { mutableStateOf<ItemDeportivo?>(null) }
    var itemAEliminar by remember { mutableStateOf<ItemDeportivo?>(null) }
    var mostrarDialogoNuevo by remember { mutableStateOf(false) }
    var itemConNovedad by remember { mutableStateOf<ItemDeportivo?>(null) }

    // Mostrar mensajes en Snackbar
    LaunchedEffect(uiState.mensajeExito, uiState.mensajeError) {
        uiState.mensajeExito?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarMensajes()
        }
        uiState.mensajeError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarMensajes()
        }
    }

    val categoriasFiltro = remember {
        listOf("Todas", "Balones", "Raquetas", "Petos", "Conos", "Ajedrez", "Otros")
    }

    // RF-06: filtro por estado y disponibilidad (valor del filtro -> texto del chip)
    val estadosFiltro = remember {
        listOf(
            "Todos" to R.string.inventario_filtro_todos_estados,
            "DISPONIBLE" to R.string.inventario_filtro_disponibles,
            "PRESTADO" to R.string.inventario_filtro_prestados,
            "DAÑADO" to R.string.inventario_filtro_danados,
            "INCOMPLETO" to R.string.inventario_filtro_incompletos,
            "EXTRAVIADO" to R.string.inventario_filtro_extraviados,
            "EN REPARACIÓN" to R.string.inventario_filtro_en_reparacion
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.inventario_titulo),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Colores.sobreBarra
                        )
                        Text(
                            text = stringResource(R.string.inventario_subtitulo),
                            style = MaterialTheme.typography.bodySmall,
                            color = Colores.sobreBarraSecundario
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onVolver,
                        modifier = Modifier.testTag("volver_inventario_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.comun_volver_inicio),
                            tint = Colores.iconoBarra
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Colores.barra
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { mostrarDialogoNuevo = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null, tint = Colores.sobrePrimario) },
                text = { Text(stringResource(R.string.inventario_nuevo), fontWeight = FontWeight.Bold, color = Colores.sobrePrimario) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Colores.sobrePrimario,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .testTag("agregar_item_fab")
                    .padding(bottom = 8.dp)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Colores.fondoPantalla
    ) { innerPadding ->
        // Resumen, búsqueda y filtros arriba; con el teléfono girado se desplazan con la lista
        ListaConEncabezado(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            espacioLista = 10.dp,
            margenLista = PaddingValues(top = 4.dp, bottom = 80.dp),
            mostrarEstado = uiState.cargando || uiState.itemsFiltrados.isEmpty(),
            estado = {
                // ==========================================
                // CARGANDO O ESTADO VACÍO
                // ==========================================
                if (uiState.cargando) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "📦",
                                fontSize = 54.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.inventario_sin_resultados),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Colores.texto
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val hayFiltros = uiState.busqueda.isNotBlank() ||
                                uiState.categoriaFiltro != "Todas" ||
                                uiState.estadoFiltro != "Todos"
                            Text(
                                text = if (hayFiltros) {
                                    stringResource(R.string.inventario_sin_resultados_filtros)
                                } else {
                                    stringResource(R.string.inventario_vacio)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Colores.textoTenue,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            if (hayFiltros) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.actualizarBusqueda("")
                                        viewModel.seleccionarCategoriaFiltro("Todas")
                                        viewModel.seleccionarEstadoFiltro("Todos")
                                    },
                                    shape = formaApp(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text(stringResource(R.string.inventario_restablecer_filtros))
                                }
                            } else {
                                // Inventario de demostración: solo se carga si el monitor lo pide.
                                OutlinedButton(
                                    onClick = { viewModel.cargarDatosEjemplo() },
                                    shape = formaApp(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.testTag("cargar_ejemplo_button")
                                ) {
                                    Text(stringResource(R.string.inventario_cargar_ejemplo))
                                }
                            }
                        }
                    }
                }
            },
            encabezado = {
                // ==========================================
                // RESUMEN RÁPIDO DE INVENTARIO
                // ==========================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ResumenMiniCard(
                        titulo = stringResource(R.string.inventario_resumen_tipos),
                        valor = "${uiState.totalArticulosDistintos}",
                        subtitulo = stringResource(R.string.inventario_resumen_articulos),
                        colorTexto = Colores.texto,
                        modifier = Modifier.weight(1f)
                    )
                    ResumenMiniCard(
                        titulo = stringResource(R.string.inventario_resumen_disponibles),
                        valor = "${uiState.totalUnidadesDisponibles}",
                        subtitulo = stringResource(R.string.inventario_resumen_en_bodega),
                        colorTexto = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    ResumenMiniCard(
                        titulo = stringResource(R.string.comun_novedades),
                        valor = "${uiState.totalUnidadesDanadas}",
                        subtitulo = stringResource(R.string.inventario_resumen_danados),
                        colorTexto = if (uiState.totalUnidadesDanadas > 0) Colores.error else Colores.textoSecundario,
                        modifier = Modifier.weight(1f)
                    )
                }

                // ==========================================
                // BARRA DE BÚSQUEDA (ID / NOMBRE)
                // ==========================================
                OutlinedTextField(
                    value = uiState.busqueda,
                    onValueChange = { viewModel.actualizarBusqueda(it) },
                    // Una línea: con letra grande del sistema el texto partido agrandaba el campo
                    placeholder = {
                        Text(
                            stringResource(R.string.inventario_buscar_placeholder),
                            color = Colores.textoTenue,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.comun_buscar), tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        if (uiState.busqueda.isNotBlank()) {
                            IconButton(onClick = { viewModel.actualizarBusqueda("") }) {
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
                        .testTag("busqueda_inventario_input")
                )

                // ==========================================
                // FILTRO DE CATEGORÍAS (CHIPS SCROLLABLES)
                // ==========================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categoriasFiltro.forEach { cat ->
                        val esSeleccionado = uiState.categoriaFiltro.equals(cat, ignoreCase = true)
                        // Las categorías de base llevan la foto de su implemento; "Otros", su emoji
                        val tipoCategoria = TipoArticulo.deCategoria(cat)
                        val iconoEmoji = if (cat == "Otros") "🏅 " else ""
                        FilterChip(
                            selected = esSeleccionado,
                            onClick = { viewModel.seleccionarCategoriaFiltro(cat) },
                            leadingIcon = tipoCategoria?.let { tipo ->
                                {
                                    Image(
                                        painter = painterResource(tipo.imagen()),
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = "$iconoEmoji${Categorias.etiqueta(cat)}",
                                    fontWeight = if (esSeleccionado) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Colores.sobrePrimario,
                                containerColor = Colores.superficie,
                                labelColor = Colores.textoSecundario
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = esSeleccionado,
                                borderColor = Colores.bordeCampo,
                                selectedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                // ==========================================
                // FILTRO POR ESTADO Y DISPONIBILIDAD (RF-06)
                // ==========================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    estadosFiltro.forEach { (valor, etiqueta) ->
                        FilterChip(
                            selected = uiState.estadoFiltro.equals(valor, ignoreCase = true),
                            onClick = { viewModel.seleccionarEstadoFiltro(valor) },
                            label = { Text(text = stringResource(etiqueta), fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                // Relleno neutro y no el color del borde: en Retro y Solar el borde es oscuro
                                selectedContainerColor = Colores.rellenoNeutro,
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                                containerColor = Colores.superficie,
                                labelColor = Colores.textoSecundario
                            ),
                            modifier = Modifier.testTag("filtro_estado_$valor")
                        )
                    }
                }
            }
        ) {
            // ==========================================
            // LISTADO DE ARTÍCULOS
            // ==========================================
            items(
                items = uiState.itemsFiltrados,
                key = { it.id }
            ) { item ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ItemInventarioCard(
                        item = item,
                        onEditar = { itemAEditar = item },
                        onEliminar = { itemAEliminar = item },
                        onMarcarDanado = { itemConNovedad = item }
                    )
                }
            }
        }
    }

    // ==========================================
    // DIÁLOGO PARA CREAR NUEVO ARTÍCULO (O LOTE)
    // ==========================================
    if (mostrarDialogoNuevo) {
        FormularioItemDialog(
            itemInicial = null,
            titulo = stringResource(R.string.inventario_registrar_nuevo),
            onSugerirId = { cat, nom -> viewModel.sugerirSiguienteId(cat, nom) },
            onSugerirSecuencia = { cat, nom, cant -> viewModel.sugerirSecuenciaIds(cat, nom, cant) },
            onDismiss = { mostrarDialogoNuevo = false },
            onGuardarItem = { nuevoItem ->
                viewModel.guardarItem(nuevoItem) { exitoso ->
                    if (exitoso) {
                        mostrarDialogoNuevo = false
                    }
                }
            },
            onGuardarLote = { listaItems ->
                viewModel.guardarVariosItems(listaItems) { exitoso ->
                    if (exitoso) {
                        mostrarDialogoNuevo = false
                    }
                }
            }
        )
    }

    // ==========================================
    // DIÁLOGO PARA EDITAR ARTÍCULO INDIVIDUAL
    // ==========================================
    itemAEditar?.let { item ->
        FormularioItemDialog(
            itemInicial = item,
            titulo = stringResource(R.string.inventario_editar_titulo, item.id),
            onSugerirId = { cat, nom -> viewModel.sugerirSiguienteId(cat, nom) },
            onSugerirSecuencia = { cat, nom, cant -> viewModel.sugerirSecuenciaIds(cat, nom, cant) },
            onDismiss = { itemAEditar = null },
            onGuardarItem = { itemActualizado ->
                viewModel.actualizarItem(itemActualizado) { exitoso ->
                    if (exitoso) {
                        itemAEditar = null
                    }
                }
            },
            onGuardarLote = { /* Solo edición individual */ }
        )
    }

    // ==========================================
    // DIÁLOGO DE CONFIRMACIÓN DE ELIMINACIÓN
    // ==========================================
    itemAEliminar?.let { item ->
        DialogoApp(
            onDismissRequest = { itemAEliminar = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Colores.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.inventario_baja_titulo),
                    fontWeight = FontWeight.Bold,
                    color = Colores.texto
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    // Con el teléfono girado el diálogo es bajo: el contenido se desplaza
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = stringResource(R.string.inventario_baja_texto),
                        color = Colores.textoSecundario,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ImagenArticulo(item.nombre, item.categoria, item.icono, tamano = 32.dp, tamanoEmoji = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${item.nombre} [${item.id}]",
                            fontWeight = FontWeight.Bold,
                            color = Colores.texto
                        )
                    }
                    Text(
                        text = stringResource(R.string.inventario_baja_detalle),
                        color = Colores.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val idBorrar = item.id
                        itemAEliminar = null
                        viewModel.eliminarItem(idBorrar) { }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Colores.errorBoton),
                    shape = formaBoton()
                ) {
                    Text(stringResource(R.string.comun_dar_de_baja), color = Colores.sobreError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { itemAEliminar = null }, shape = formaBoton()) {
                    Text(stringResource(R.string.comun_cancelar), color = Colores.textoSecundario)
                }
            },
            tonoNeon = TonoNeon.ROJO,
            containerColor = Colores.dialogo,
            shape = formaApp(18.dp)
        )
    }

    // ==========================================
    // DIÁLOGO DE NOVEDADES: DAÑADO, INCOMPLETO O EXTRAVIADO (RF-07)
    // ==========================================
    itemConNovedad?.let { item ->
        val cerrarSiExito: (Boolean) -> Unit = { exitoso -> if (exitoso) itemConNovedad = null }
        NovedadItemDialog(
            item = item,
            onDismiss = { itemConNovedad = null },
            onMarcarNovedad = { tipo, cantidad, observacion ->
                viewModel.marcarNovedad(item.id, tipo, cantidad, observacion, cerrarSiExito)
            },
            onResolver = { tipo, cantidad ->
                viewModel.resolverNovedad(item.id, tipo, cantidad, cerrarSiExito)
            },
            onDescontarExtraviadas = {
                viewModel.descontarExtraviadas(item.id, cerrarSiExito)
            }
        )
    }
}

@Composable
fun ResumenMiniCard(
    titulo: String,
    valor: String,
    subtitulo: String,
    colorTexto: Color,
    modifier: Modifier = Modifier
) {
    TarjetaElevadaApp(
        modifier = modifier,
        shape = formaApp(14.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Colores.superficie
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.labelSmall,
                color = Colores.textoTenue
            )
            Text(
                text = valor,
                // En neón el número brilla con su color
                style = if (Colores.esNeon) {
                    MaterialTheme.typography.titleLarge.merge(estiloBrillo(colorTexto, 4.dp))
                } else {
                    MaterialTheme.typography.titleLarge
                },
                fontWeight = FontWeight.Bold,
                color = colorTexto
            )
            Text(
                text = subtitulo,
                style = MaterialTheme.typography.labelSmall,
                color = Colores.textoTenue
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemInventarioCard(
    item: ItemDeportivo,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
    onMarcarDanado: () -> Unit = {}
) {
    val colorEstado = when (item.estado.uppercase()) {
        "DISPONIBLE" -> Colores.exito
        "PRESTADO" -> Colores.info
        "DAÑADO", "INCOMPLETO", "EXTRAVIADO" -> colorNovedad(item.estado)
        "EN REPARACIÓN", "REPARACION" -> Colores.naranja
        "AGOTADO" -> Colores.extraviado
        else -> MaterialTheme.colorScheme.primary
    }

    TarjetaElevadaApp(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("item_card_${item.id}"),
        shape = formaApp(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Colores.superficie
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
        acentoNeon = colorEstado
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. ÍCONO REPRESENTATIVO
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .fondoIconoApp(
                            color = Colores.superficieAlta,
                            borde = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Foto del implemento (si no se reconoce, el emoji guardado)
                    ImagenArticulo(
                        nombre = item.nombre,
                        categoria = item.categoria,
                        icono = item.icono.ifBlank { InventarioRepository.iconoRepresentativo(item.categoria, item.nombre) },
                        tamano = 52.dp,
                        tamanoEmoji = 26.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 2. INFORMACIÓN PRINCIPAL (ID INDIVIDUAL, NOMBRE, CATEGORÍA)
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Si el código y el estado no caben en una línea, el estado pasa a la siguiente
                    // (antes se partía la palabra: "DISPONIB-LE", "EN REPARAC-IÓN")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // ID_articulo ÚNICO (en Solar, etiqueta de color sólido)
                        Surface(
                            shape = formaApp(6.dp),
                            color = Colores.rellenoEtiqueta(MaterialTheme.colorScheme.primary, 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Colores.rellenoEtiqueta(MaterialTheme.colorScheme.primary, 0.6f))
                        ) {
                            Text(
                                text = item.id,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Colores.textoEtiqueta(MaterialTheme.colorScheme.primary),
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }

                        // Estado actual
                        Surface(
                            shape = formaApp(6.dp),
                            color = Colores.rellenoEtiqueta(colorEstado, 0.18f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Colores.rellenoEtiqueta(colorEstado, 0.7f))
                        ) {
                            Text(
                                text = EstadoItem.etiqueta(item.estado).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Colores.textoEtiqueta(colorEstado),
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Nombre del Producto
                    Text(
                        text = item.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Colores.texto,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Categoría
                    Text(
                        text = stringResource(R.string.inventario_categoria_de, Categorias.etiqueta(item.categoria)),
                        style = MaterialTheme.typography.bodySmall,
                        color = Colores.acentoTexto
                    )
                }

                // 3. ACCIONES ESPECÍFICAS DE ESTE ARTÍCULO (EDITAR / ELIMINAR ESTA UNIDAD)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onEditar,
                        modifier = Modifier
                            .size(36.dp)
                            .fondoIconoApp(Colores.superficieAlta)
                            .testTag("editar_item_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.inventario_cd_editar, item.id),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onMarcarDanado,
                        modifier = Modifier
                            .size(36.dp)
                            .fondoIconoApp(Colores.advertenciaFondo, tono = TonoNeon.AMARILLO)
                            .testTag("danar_item_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = stringResource(R.string.inventario_cd_novedad, item.id),
                            tint = Colores.naranja,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onEliminar,
                        modifier = Modifier
                            .size(36.dp)
                            .fondoIconoApp(Colores.errorFondo, tono = TonoNeon.ROJO)
                            .testTag("eliminar_item_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.inventario_cd_baja, item.id),
                            tint = Colores.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Banner si el artículo está prestado actualmente a un estudiante
            if (item.estado.uppercase() == "PRESTADO" && item.estudiantePrestamoNombre.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = formaApp(8.dp),
                    color = Colores.infoFondo,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Colores.info.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("👤", fontSize = 13.sp)
                        Text(
                            text = stringResource(R.string.inventario_en_prestamo_a, item.estudiantePrestamoNombre, item.estudiantePrestamoId),
                            style = MaterialTheme.typography.labelSmall,
                            color = Colores.info,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Colores.borde, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // 4. DETALLES DE UNIDAD FÍSICA Y OBSERVACIONES
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = formaApp(4.dp),
                        color = Colores.superficieAlta
                    ) {
                        Text(
                            text = if (item.esPorCantidad) stringResource(R.string.inventario_control_cantidad) else stringResource(R.string.inventario_control_unidad),
                            style = MaterialTheme.typography.labelSmall,
                            color = Colores.acentoTexto,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = when {
                            item.esPorCantidad -> stringResource(R.string.inventario_disponibles_de, item.cantidadDisponible, item.cantidadTotal)
                            item.esIncompleto -> stringResource(R.string.inventario_se_presta_incompleto)
                            item.sePuedePrestar -> stringResource(R.string.inventario_disponible_prestamo)
                            else -> stringResource(R.string.inventario_no_disponible)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.piezasPrestables > 0) Colores.exito else Colores.naranja
                    )
                }

                if (item.descripcion.isNotBlank()) {
                    Text(
                        text = item.descripcion,
                        style = MaterialTheme.typography.labelSmall,
                        color = Colores.textoSecundario,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 140.dp)
                    )
                }
            }

            // Novedad registrada y su observación (RF-07)
            if (item.tieneNovedad) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "⚠️ ${item.resumenNovedad}. ${item.observacion}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Colores.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FormularioItemDialog(
    itemInicial: ItemDeportivo?,
    titulo: String,
    onSugerirId: (categoria: String, nombre: String) -> String,
    onSugerirSecuencia: (categoria: String, nombre: String, cantidad: Int) -> List<String>,
    onDismiss: () -> Unit,
    onGuardarItem: (ItemDeportivo) -> Unit,
    onGuardarLote: (List<ItemDeportivo>) -> Unit
) {
    val categoriasDisponibles = remember {
        listOf(
            "Balones" to "⚽",
            "Raquetas" to "🎾",
            "Petos" to "🎽",
            "Conos" to "🔶",
            "Ajedrez" to "♟️",
            "Otros" to "🏅"
        )
    }

    var categoriaSeleccionada by remember {
        mutableStateOf(itemInicial?.categoria ?: "Balones")
    }

    var nombre by remember {
        mutableStateOf(itemInicial?.nombre ?: "")
    }

    // Regla de negocio 1: control por unidad (cada pieza con su código) o por cantidad
    // (un solo registro con N piezas iguales). No se cambia después de crear el artículo.
    var porCantidad by remember {
        mutableStateOf(itemInicial?.esPorCantidad ?: false)
    }

    // Unidades a crear (control por unidad) o total de piezas (control por cantidad)
    var cantidadUnidades by remember {
        mutableIntStateOf(itemInicial?.takeIf { it.esPorCantidad }?.cantidadTotal ?: 1)
    }
    val maximoUnidades = if (porCantidad) 500 else 50

    // Si es nuevo elemento, se autocalcula según categoría y nombre; si es edición, se mantiene el ID
    var idArticulo by remember {
        mutableStateOf(itemInicial?.id ?: onSugerirId(categoriaSeleccionada, nombre))
    }

    var icono by remember {
        mutableStateOf(itemInicial?.icono ?: InventarioRepository.iconoRepresentativo(categoriaSeleccionada, nombre))
    }

    // El estado PRESTADO lo asigna únicamente el flujo de préstamos.
    val unidadPrestada = itemInicial?.estado.equals("PRESTADO", ignoreCase = true)
    // Balones y petos no pueden quedar incompletos. Si uno ya lo estaba (datos anteriores),
    // se muestra su estado actual para que el monitor lo vea y lo corrija.
    val estadoGuardado = itemInicial?.estado.orEmpty()
    val estados = when {
        unidadPrestada -> listOf("PRESTADO")
        else -> EstadoItem.editablesPara(categoriaSeleccionada).let { lista ->
            if (estadoGuardado.equals(EstadoItem.INCOMPLETO, ignoreCase = true) && EstadoItem.INCOMPLETO !in lista) {
                lista + EstadoItem.INCOMPLETO
            } else {
                lista
            }
        }
    }
    var estadoSeleccionado by remember {
        mutableStateOf(itemInicial?.estado ?: "DISPONIBLE")
    }
    // Al cambiar a una categoría de una sola pieza, un "Incompleto" recién elegido vuelve a Disponible.
    LaunchedEffect(categoriaSeleccionada) {
        if (estadoSeleccionado !in estados && !unidadPrestada) estadoSeleccionado = EstadoItem.DISPONIBLE
    }

    var descripcion by remember {
        mutableStateOf(itemInicial?.descripcion ?: "")
    }

    var errorValidacion by remember { mutableStateOf<String?>(null) }

    val nombreValido = nombre.trim().length >= 3
    val idValido = idArticulo.trim().length >= 2

    // Previsualización de secuencia de códigos si se crean múltiples unidades
    val codigosSecuencia = remember(categoriaSeleccionada, nombre, cantidadUnidades, itemInicial, porCantidad) {
        if (itemInicial == null && !porCantidad && cantidadUnidades > 1) {
            onSugerirSecuencia(categoriaSeleccionada, nombre, cantidadUnidades)
        } else {
            emptyList()
        }
    }

    // Actualiza sugerencia de ID e ícono automáticamente cuando cambia la categoría (solo si es nuevo)
    LaunchedEffect(categoriaSeleccionada) {
        if (itemInicial == null) {
            idArticulo = onSugerirId(categoriaSeleccionada, nombre)
            icono = InventarioRepository.iconoRepresentativo(categoriaSeleccionada, nombre)
        }
    }

    DialogoApp(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Foto según la categoría y el nombre que se va escribiendo
                ImagenArticulo(nombre, categoriaSeleccionada, icono, tamano = 36.dp, tamanoEmoji = 24.sp)
                Text(
                    text = titulo,
                    fontWeight = FontWeight.Bold,
                    color = Colores.texto,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Selector de Categoría con Emojis
                Text(
                    text = stringResource(R.string.inventario_form_categoria),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Colores.acentoTexto
                )

                // Todas las categorías a la vista (antes quedaban cortadas en una fila con desplazamiento)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categoriasDisponibles.forEach { (cat, emoji) ->
                        val esSeleccionada = cat.equals(categoriaSeleccionada, ignoreCase = true)
                        Surface(
                            shape = formaApp(10.dp),
                            color = if (esSeleccionada) MaterialTheme.colorScheme.primary else Colores.superficieAlta,
                            modifier = Modifier
                                .clip(formaApp(10.dp))
                                .clickable {
                                    categoriaSeleccionada = cat
                                    icono = emoji
                                    if (itemInicial == null) {
                                        idArticulo = onSugerirId(cat, nombre)
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                ImagenCategoria(cat, emoji, tamano = 20.dp, tamanoEmoji = 14.sp)
                                Text(
                                    text = Categorias.etiqueta(cat),
                                    color = if (esSeleccionada) Colores.sobrePrimario else Colores.texto,
                                    fontSize = 12.sp,
                                    fontWeight = if (esSeleccionada) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // Nombre del Producto
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { input ->
                        if (input.length <= 60) {
                            nombre = input
                            if (itemInicial == null && idArticulo.isBlank()) {
                                idArticulo = onSugerirId(categoriaSeleccionada, input)
                            }
                        }
                    },
                    label = { Text(stringResource(R.string.inventario_form_nombre)) },
                    placeholder = { Text(stringResource(R.string.inventario_form_nombre_ejemplo)) },
                    singleLine = true,
                    isError = nombre.isNotBlank() && !nombreValido,
                    supportingText = {
                        if (nombre.isNotBlank() && !nombreValido) {
                            Text(stringResource(R.string.comun_minimo_3_caracteres), color = Colores.error, fontSize = 11.sp)
                        } else {
                            Text(stringResource(R.string.inventario_form_maximo_60), color = Colores.textoTenue, fontSize = 11.sp)
                        }
                    },
                    colors = dialogTextFieldColors(),
                    shape = formaApp(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_nombre_producto")
                )

                if (itemInicial == null) {
                    // Tipo de control del artículo
                    Text(
                        text = stringResource(R.string.inventario_form_tipo_control),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Colores.acentoTexto
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                    ) {
                        listOf(
                            Triple(false, stringResource(R.string.inventario_por_unidad), stringResource(R.string.inventario_por_unidad_desc)),
                            Triple(true, stringResource(R.string.inventario_por_cantidad), stringResource(R.string.inventario_por_cantidad_desc))
                        ).forEach { (valor, etiqueta, detalle) ->
                            val esSeleccionado = porCantidad == valor
                            Surface(
                                onClick = {
                                    porCantidad = valor
                                    if (!valor) {
                                        cantidadUnidades = cantidadUnidades.coerceAtMost(50)
                                    }
                                },
                                shape = formaApp(12.dp),
                                color = if (esSeleccionado) MaterialTheme.colorScheme.primary else Colores.superficieAlta,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (esSeleccionado) MaterialTheme.colorScheme.primary else Colores.borde
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .testTag(if (valor) "tipo_control_cantidad" else "tipo_control_unidad")
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = etiqueta,
                                        color = if (esSeleccionado) Colores.sobrePrimario else Colores.texto,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Text(
                                        text = detalle,
                                        color = if (esSeleccionado) Colores.sobrePrimario else Colores.textoTenue,
                                        fontSize = 11.sp,
                                        lineHeight = 13.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                if (itemInicial == null || porCantidad) {
                    // Cuántas unidades se crean (o cuántas piezas tiene el artículo por cantidad).
                    // El texto va arriba y el contador debajo, a todo el ancho: así los botones
                    // − y + quedan alineados con el número y no se salen de la tarjeta.
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Colores.superficie, formaApp(12.dp))
                            .border(1.dp, Colores.borde, formaApp(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column {
                            Text(
                                text = if (porCantidad) stringResource(R.string.inventario_form_cantidad_total) else stringResource(R.string.inventario_form_unidades),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Colores.texto
                            )
                            Text(
                                text = if (porCantidad) {
                                    stringResource(R.string.inventario_form_un_codigo)
                                } else {
                                    stringResource(R.string.inventario_form_codigo_por_unidad)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = Colores.acentoTexto
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ContadorCantidad(
                                valor = cantidadUnidades,
                                onCambio = { cantidadUnidades = it },
                                minimo = 1,
                                maximo = maximoUnidades,
                                tamanoBoton = 40.dp,
                                etiquetaPrueba = "contador_unidades"
                            )
                            if (porCantidad) {
                                OutlinedButton(
                                    onClick = { cantidadUnidades = (cantidadUnidades + 10).coerceAtMost(maximoUnidades) },
                                    enabled = cantidadUnidades < maximoUnidades,
                                    shape = formaApp(20.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                    contentPadding = PaddingValues(horizontal = 14.dp),
                                    modifier = Modifier
                                        .height(36.dp)
                                        .testTag("sumar_diez_button")
                                ) {
                                    Text("+10", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text(
                                    text = stringResource(R.string.inventario_form_maximo, maximoUnidades),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Colores.textoTenue
                                )
                            }
                        }

                        if (cantidadUnidades > 1 && codigosSecuencia.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.inventario_form_codigos),
                                style = MaterialTheme.typography.labelSmall,
                                color = Colores.textoSecundario
                            )
                            if (codigosSecuencia.size <= 8) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    codigosSecuencia.forEach { cod -> ChipCodigo(cod) }
                                }
                            } else {
                                // Con muchas unidades basta el rango: del primero al último.
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    ChipCodigo(codigosSecuencia.first())
                                    Text(stringResource(R.string.inventario_form_rango_a), color = Colores.textoSecundario, fontSize = 12.sp)
                                    ChipCodigo(codigosSecuencia.last())
                                    Text(
                                        text = stringResource(R.string.inventario_form_n_codigos, codigosSecuencia.size),
                                        color = Colores.textoTenue,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // ID del Artículo (Para unidad única o edición)
                if (itemInicial != null || porCantidad || cantidadUnidades == 1) {
                    OutlinedTextField(
                        value = idArticulo,
                        onValueChange = { input ->
                            val filtrado = input.uppercase().filter { it.isLetterOrDigit() || it == '-' || it == '_' }
                            if (filtrado.length <= 15) {
                                idArticulo = filtrado
                            }
                        },
                        label = { Text(stringResource(R.string.inventario_form_codigo)) },
                        placeholder = { Text(stringResource(R.string.inventario_form_codigo_ejemplo)) },
                        singleLine = true,
                        // El código identifica al artículo en los préstamos: no se cambia al editar.
                        readOnly = itemInicial != null,
                        isError = idArticulo.isNotBlank() && !idValido,
                        supportingText = {
                            if (idArticulo.isNotBlank() && !idValido) {
                                Text(stringResource(R.string.inventario_form_codigo_minimo), color = Colores.error, fontSize = 11.sp)
                            } else {
                                Text(
                                    text = if (itemInicial != null) stringResource(R.string.inventario_form_codigo_fijo) else stringResource(R.string.inventario_form_codigo_sugerido),
                                    color = Colores.textoTenue,
                                    fontSize = 11.sp
                                )
                            }
                        },
                        trailingIcon = {
                            if (itemInicial == null) {
                                TextButton(
                                    onClick = {
                                        idArticulo = onSugerirId(categoriaSeleccionada, nombre)
                                    }
                                ) {
                                    Text(stringResource(R.string.inventario_form_auto), color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                }
                            }
                        },
                        colors = dialogTextFieldColors(),
                        shape = formaApp(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_id_articulo")
                    )
                }

                // Selector de Estado (solo control por unidad; en control por cantidad se calcula solo)
                if (!porCantidad) {
                Text(
                    text = stringResource(R.string.inventario_form_estado),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Colores.acentoTexto
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    estados.forEach { est ->
                        val esSeleccionado = est.equals(estadoSeleccionado, ignoreCase = true)
                        val colorEst = when (est) {
                            "DISPONIBLE" -> Colores.exito
                            "PRESTADO" -> Colores.info
                            "DAÑADO", "INCOMPLETO", "EXTRAVIADO" -> colorNovedad(est)
                            "EN REPARACIÓN" -> Colores.naranja
                            else -> Colores.textoTenue
                        }
                        Surface(
                            shape = formaApp(10.dp),
                            color = if (esSeleccionado) Colores.rellenoEtiqueta(colorEst, 0.25f) else Colores.superficie,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (esSeleccionado) colorEst else Colores.borde
                            ),
                            modifier = Modifier
                                .clip(formaApp(10.dp))
                                .clickable {
                                    estadoSeleccionado = est
                                }
                        ) {
                            Text(
                                text = EstadoItem.etiqueta(est).uppercase(),
                                color = if (esSeleccionado) Colores.textoEtiqueta(colorEst) else Colores.textoSecundario,
                                fontSize = 11.sp,
                                fontWeight = if (esSeleccionado) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                }

                // Descripción / Observaciones
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text(stringResource(R.string.inventario_form_observaciones)) },
                    placeholder = { Text(stringResource(R.string.inventario_form_observaciones_ayuda)) },
                    colors = dialogTextFieldColors(),
                    shape = formaApp(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Mensaje de error de validación si existe
                errorValidacion?.let { error ->
                    Text(
                        text = error,
                        color = Colores.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            val requiereId = itemInicial != null || porCantidad || cantidadUnidades == 1
            val formularioValido = nombreValido && (!requiereId || idValido)
            Button(
                onClick = {
                    if (!nombreValido) {
                        errorValidacion = Idioma.texto(R.string.inventario_form_error_nombre)
                        return@Button
                    }

                    if (requiereId && !idValido) {
                        errorValidacion = Idioma.texto(R.string.inventario_form_error_codigo)
                        return@Button
                    }

                    errorValidacion = null
                    val emojiIcono = icono.ifBlank {
                        InventarioRepository.iconoRepresentativo(categoriaSeleccionada, nombre)
                    }

                    if (itemInicial != null) {
                        // Edición de un artículo existente (el código y el tipo de control no cambian)
                        val itemActualizado = itemInicial.copy(
                            nombre = nombre.trim(),
                            categoria = categoriaSeleccionada,
                            icono = emojiIcono,
                            estado = estadoSeleccionado,
                            cantidadTotal = if (porCantidad) cantidadUnidades else 1,
                            descripcion = descripcion.trim()
                        )
                        onGuardarItem(itemActualizado)
                    } else if (porCantidad) {
                        // Artículo nuevo controlado por cantidad: un registro con N piezas
                        val nuevo = ItemDeportivo(
                            id = idArticulo.trim().uppercase(),
                            nombre = nombre.trim(),
                            categoria = categoriaSeleccionada,
                            icono = emojiIcono,
                            tipoControl = TipoControl.CANTIDAD,
                            cantidadTotal = cantidadUnidades,
                            cantidadDisponible = cantidadUnidades,
                            descripcion = descripcion.trim()
                        )
                        onGuardarItem(nuevo)
                    } else {
                        // Creación de artículo nuevo por unidad (individual o en lote secuencial)
                        if (cantidadUnidades == 1) {
                            val nuevo = ItemDeportivo(
                                id = idArticulo.trim().uppercase(),
                                nombre = nombre.trim(),
                                categoria = categoriaSeleccionada,
                                icono = emojiIcono,
                                cantidadTotal = 1,
                                cantidadDisponible = if (estadoSeleccionado == "DISPONIBLE") 1 else 0,
                                estado = estadoSeleccionado,
                                descripcion = descripcion.trim()
                            )
                            onGuardarItem(nuevo)
                        } else {
                            val codigos = onSugerirSecuencia(categoriaSeleccionada, nombre, cantidadUnidades)
                            val lote = codigos.mapIndexed { index, cod ->
                                ItemDeportivo(
                                    id = cod,
                                    nombre = "${nombre.trim()} (Unidad ${index + 1})",
                                    categoria = categoriaSeleccionada,
                                    icono = emojiIcono,
                                    cantidadTotal = 1,
                                    cantidadDisponible = if (estadoSeleccionado == "DISPONIBLE") 1 else 0,
                                    estado = estadoSeleccionado,
                                    descripcion = descripcion.trim()
                                )
                            }
                            onGuardarLote(lote)
                        }
                    }
                },
                enabled = formularioValido,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = Colores.deshabilitado,
                    disabledContentColor = Colores.textoTenue
                ),
                modifier = Modifier.testTag("guardar_item_dialog_button"),
                shape = formaBoton()
            ) {
                val textoBoton = if (itemInicial != null) {
                    stringResource(R.string.comun_guardar_cambios)
                } else if (!porCantidad && cantidadUnidades > 1) {
                    stringResource(R.string.inventario_crear_n, cantidadUnidades)
                } else {
                    stringResource(R.string.inventario_guardar_articulo)
                }
                Text(textoBoton, color = Colores.sobrePrimario, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = formaBoton()) {
                Text(stringResource(R.string.comun_cancelar), color = Colores.textoSecundario)
            }
        },
        containerColor = Colores.superficie,
        shape = formaApp(22.dp)
    )
}

/** Código de inventario dentro de una pastilla (vista previa de los códigos a crear). */
@Composable
private fun ChipCodigo(codigo: String) {
    Surface(
        shape = formaApp(6.dp),
        color = Colores.rellenoEtiqueta(MaterialTheme.colorScheme.primary, 0.2f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Colores.rellenoEtiqueta(MaterialTheme.colorScheme.primary, 0.5f))
    ) {
        Text(
            text = codigo,
            color = Colores.textoEtiqueta(MaterialTheme.colorScheme.primary),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun dialogTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    // El borde de campo, como en el resto de la app: el de separadores (Colores.borde) casi no se veía
    unfocusedBorderColor = Colores.bordeCampo,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = Colores.textoSecundario,
    focusedTextColor = Colores.texto,
    unfocusedTextColor = Colores.texto,
    focusedContainerColor = Colores.superficie,
    unfocusedContainerColor = Colores.superficie,
    cursorColor = MaterialTheme.colorScheme.primary
)
