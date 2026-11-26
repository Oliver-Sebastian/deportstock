package com.example.myapplication.ui.screens.prestamo

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
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
import com.example.myapplication.data.EstadoItem
import com.example.myapplication.data.EstadoItemPrestamo
import com.example.myapplication.data.ItemPrestamo
import com.example.myapplication.data.Prestamo
import com.example.myapplication.data.ProgramasAcademicos
import com.example.myapplication.data.ResultadoDevolucion
import com.example.myapplication.data.TipoControl
import com.example.myapplication.ui.components.DialogoApp
import com.example.myapplication.ui.components.ListaConEncabezado
import com.example.myapplication.ui.components.TarjetaApp
import com.example.myapplication.ui.components.TonoNeon
import com.example.myapplication.ui.screens.inventario.colorNovedad
import com.example.myapplication.ui.theme.formaApp
import com.example.myapplication.ui.theme.formaBoton
import com.example.myapplication.utils.Fechas
import com.example.myapplication.utils.Idioma
import com.example.myapplication.viewmodel.DevolucionViewModel
import java.text.SimpleDateFormat
import com.example.myapplication.ui.components.ContadorCantidad
import com.example.myapplication.ui.components.ContactoTelefono
import com.example.myapplication.ui.theme.Colores

private const val FILTRO_ACTIVOS = "ACTIVOS"
private const val FILTRO_VENCIDOS = "VENCIDOS"
private const val FILTRO_DANOS = "DANOS"
private const val FILTRO_TODOS = "TODOS"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarDevolucionScreen(
    onVolver: () -> Unit,
    viewModel: DevolucionViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val todosLosPrestamos = uiState.prestamos

    var filtroTexto by remember { mutableStateOf("") }
    var filtroEstado by remember { mutableStateOf(FILTRO_ACTIVOS) }
    var prestamoADevolver by remember { mutableStateOf<Prestamo?>(null) }
    var prestamoAResolver by remember { mutableStateOf<Prestamo?>(null) }

    // a.m./p.m. según el idioma de la app
    val formatoFecha = remember(Idioma.actual) { Fechas.formatoConHora() }

    LaunchedEffect(uiState.mensajeError) {
        uiState.mensajeError?.let { mensaje ->
            Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
            viewModel.limpiarError()
        }
    }

    // Filtrar préstamos (RF-13: activos y vencidos; RF-18: historial por estudiante o artículo)
    val prestamosFiltrados = remember(todosLosPrestamos, filtroTexto, filtroEstado) {
        val texto = filtroTexto.trim()
        todosLosPrestamos.filter { prestamo ->
            val coincideEstado = when (filtroEstado) {
                FILTRO_ACTIVOS -> prestamo.estaActivo
                FILTRO_VENCIDOS -> prestamo.estaVencido()
                FILTRO_DANOS -> prestamo.danoPendiente
                else -> true
            }
            val coincideTexto = if (texto.isBlank()) true else {
                prestamo.codigoEstudiante.contains(texto, ignoreCase = true) ||
                prestamo.nombreEstudiante.contains(texto, ignoreCase = true) ||
                prestamo.id.contains(texto, ignoreCase = true) ||
                prestamo.itemsCodigos.any { it.contains(texto, ignoreCase = true) } ||
                prestamo.itemsNombres.any { it.contains(texto, ignoreCase = true) }
            }
            coincideEstado && coincideTexto
        }
    }

    val totalActivos = remember(todosLosPrestamos) { todosLosPrestamos.count { it.estaActivo } }
    val totalVencidos = remember(todosLosPrestamos) { todosLosPrestamos.count { it.estaVencido() } }
    val totalDanos = remember(todosLosPrestamos) { todosLosPrestamos.count { it.danoPendiente } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.comun_registrar_devolucion),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Colores.sobreBarra
                        )
                        Text(
                            text = pluralStringResource(R.plurals.devolucion_subtitulo, totalActivos, totalActivos),
                            style = MaterialTheme.typography.bodySmall,
                            color = Colores.sobreBarraSecundario
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onVolver,
                        modifier = Modifier.testTag("volver_menu_devolucion_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.comun_volver_menu),
                            tint = Colores.iconoBarra
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Colores.barra)
            )
        },
        containerColor = Colores.fondoPantalla
    ) { innerPadding ->
        // Búsqueda y filtros arriba; con el teléfono girado se desplazan junto con los préstamos
        ListaConEncabezado(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            espacioLista = 12.dp,
            mostrarEstado = uiState.cargando || prestamosFiltrados.isEmpty(),
            estado = {
                if (uiState.cargando) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                text = when {
                                    filtroTexto.isNotBlank() -> stringResource(R.string.devolucion_vacio_busqueda)
                                    filtroEstado == FILTRO_ACTIVOS -> stringResource(R.string.devolucion_vacio_activos)
                                    filtroEstado == FILTRO_VENCIDOS -> stringResource(R.string.devolucion_vacio_vencidos)
                                    filtroEstado == FILTRO_DANOS -> stringResource(R.string.devolucion_vacio_novedades)
                                    else -> stringResource(R.string.devolucion_vacio_historial)
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Colores.texto
                            )
                            Text(
                                text = if (filtroTexto.isNotBlank()) {
                                    stringResource(R.string.comun_otro_termino)
                                } else if (filtroEstado == FILTRO_ACTIVOS) {
                                    stringResource(R.string.devolucion_todo_devuelto)
                                } else {
                                    stringResource(R.string.devolucion_sin_registros_filtro)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Colores.textoTenue,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            },
            encabezado = {
                // Buscador por estudiante / código de implemento
                OutlinedTextField(
                    value = filtroTexto,
                    onValueChange = { filtroTexto = it },
                    label = { Text(stringResource(R.string.devolucion_buscar), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    placeholder = { Text(stringResource(R.string.devolucion_buscar_ejemplo), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        if (filtroTexto.isNotBlank()) {
                            IconButton(onClick = { filtroTexto = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Colores.textoTenue)
                            }
                        }
                    },
                    singleLine = true,
                    shape = formaApp(12.dp),
                    colors = outlinedFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("filtro_devolucion_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Selector rápido en cuadrícula de 2 x 2: activos, vencidos, con novedades e historial
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BotonFiltro(
                            texto = stringResource(R.string.devolucion_pestana_activos, totalActivos),
                            seleccionado = filtroEstado == FILTRO_ACTIVOS,
                            color = Colores.primario,
                            etiquetaPrueba = "filtro_activos",
                            modifier = Modifier.weight(1f)
                        ) { filtroEstado = FILTRO_ACTIVOS }
                        BotonFiltro(
                            texto = stringResource(R.string.devolucion_pestana_vencidos, totalVencidos),
                            seleccionado = filtroEstado == FILTRO_VENCIDOS,
                            color = Colores.error,
                            etiquetaPrueba = "filtro_vencidos",
                            modifier = Modifier.weight(1f)
                        ) { filtroEstado = FILTRO_VENCIDOS }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BotonFiltro(
                            texto = stringResource(R.string.devolucion_pestana_novedades, totalDanos),
                            seleccionado = filtroEstado == FILTRO_DANOS,
                            color = Colores.naranja,
                            etiquetaPrueba = "filtro_novedades",
                            modifier = Modifier.weight(1f)
                        ) { filtroEstado = FILTRO_DANOS }
                        BotonFiltro(
                            texto = stringResource(R.string.comun_historial_emoji),
                            seleccionado = filtroEstado == FILTRO_TODOS,
                            color = Colores.info,
                            etiquetaPrueba = "filtro_historial",
                            modifier = Modifier.weight(1f)
                        ) { filtroEstado = FILTRO_TODOS }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        ) {
            // Lista de préstamos
            items(prestamosFiltrados, key = { it.id }) { prestamo ->
                PrestamoDevolucionCard(
                    prestamo = prestamo,
                    formatoFecha = formatoFecha,
                    telefono = uiState.telefonoDe(prestamo),
                    onDevolverClick = { prestamoADevolver = prestamo },
                    onResolverDanoClick = { prestamoAResolver = prestamo }
                )
            }
        }
    }

    // Diálogo de devolución: estado de cada artículo (RF-11)
    prestamoADevolver?.let { prestamo ->
        DevolucionDialog(
            prestamo = prestamo,
            procesando = uiState.procesando,
            categorias = uiState.categorias,
            onDismiss = { prestamoADevolver = null },
            onConfirmar = { resultados, observaciones ->
                viewModel.registrarDevolucion(prestamo.id, resultados, observaciones) { resultado ->
                    resultado.onSuccess { mensaje ->
                        Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
                        prestamoADevolver = null
                    }.onFailure { error ->
                        Toast.makeText(context, Idioma.texto(R.string.comun_error_detalle, error.localizedMessage ?: Idioma.texto(R.string.comun_error_desconocido)), Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    // Diálogo para cerrar un caso de novedad y levantar el bloqueo (regla de negocio 4)
    prestamoAResolver?.let { prestamo ->
        ResolverDanoDialog(
            prestamo = prestamo,
            procesando = uiState.procesando,
            onDismiss = { prestamoAResolver = null },
            onConfirmar = { nota ->
                viewModel.resolverDano(prestamo.id, nota) { resultado ->
                    resultado.onSuccess { mensaje ->
                        Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
                        prestamoAResolver = null
                    }.onFailure { error ->
                        Toast.makeText(context, Idioma.texto(R.string.comun_error_detalle, error.localizedMessage ?: Idioma.texto(R.string.comun_error_desconocido)), Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }
}

/** Botón de filtro de la cuadrícula: relleno con su color cuando está seleccionado. */
@Composable
private fun BotonFiltro(
    texto: String,
    seleccionado: Boolean,
    color: Color,
    etiquetaPrueba: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = formaApp(24.dp),
        color = if (seleccionado) color else Colores.superficie,
        border = BorderStroke(1.5.dp, color),
        shadowElevation = if (seleccionado) 4.dp else 0.dp,
        modifier = modifier
            .height(48.dp)
            .testTag(etiquetaPrueba)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = texto,
                color = if (seleccionado) Colores.fondo else color,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

/**
 * Diálogo de devolución. El monitor revisa cada artículo e indica cómo regresa:
 * en buen estado, dañado, incompleto o extraviado. Toda novedad exige una observación.
 */
@Composable
fun DevolucionDialog(
    prestamo: Prestamo,
    procesando: Boolean,
    onDismiss: () -> Unit,
    onConfirmar: (resultados: List<ResultadoDevolucion>, observaciones: String) -> Unit,
    // Categoría de cada artículo (código -> categoría): balones y petos no pueden volver incompletos
    categorias: Map<String, String> = emptyMap()
) {
    val lineas = remember(prestamo.id) { prestamo.itemsEfectivos }
    // Unidades: estado con que regresa cada una (id del artículo -> BUEN_ESTADO, DANADO, INCOMPLETO...)
    val estados = remember(prestamo.id) { mutableStateMapOf<String, String>() }
    // Artículos por cantidad: piezas dañadas y piezas que no regresaron
    val danadas = remember(prestamo.id) { mutableStateMapOf<String, Int>() }
    val extraviadas = remember(prestamo.id) { mutableStateMapOf<String, Int>() }
    val observacionesItem = remember(prestamo.id) { mutableStateMapOf<String, String>() }
    var observacionGeneral by remember(prestamo.id) { mutableStateOf("") }

    fun tieneNovedad(linea: ItemPrestamo): Boolean =
        if (linea.tipoControl == TipoControl.CANTIDAD) {
            (danadas[linea.itemId] ?: 0) + (extraviadas[linea.itemId] ?: 0) > 0
        } else {
            (estados[linea.itemId] ?: EstadoItemPrestamo.BUEN_ESTADO) != EstadoItemPrestamo.BUEN_ESTADO
        }

    val hayNovedades = lineas.any { tieneNovedad(it) }
    val faltanObservaciones = lineas.any { linea ->
        tieneNovedad(linea) && observacionesItem[linea.itemId].isNullOrBlank()
    }

    DialogoApp(
        onDismissRequest = { if (!procesando) onDismiss() },
        icon = {
            Icon(
                imageVector = if (hayNovedades) Icons.Default.Warning else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (hayNovedades) Colores.naranja else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(stringResource(R.string.comun_registrar_devolucion), fontWeight = FontWeight.Bold, color = Colores.texto)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${prestamo.nombreEstudiante} (${prestamo.identificadorPrestatario})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Colores.acentoTexto
                )
                Text(
                    text = stringResource(R.string.devolucion_revisa),
                    style = MaterialTheme.typography.bodySmall,
                    color = Colores.texto
                )

                lineas.forEach { linea ->
                    LineaDevolucion(
                        linea = linea,
                        admiteIncompleto = EstadoItem.admiteIncompleto(categorias[linea.itemId]),
                        estado = estados[linea.itemId] ?: EstadoItemPrestamo.BUEN_ESTADO,
                        danadas = danadas[linea.itemId] ?: 0,
                        extraviadas = extraviadas[linea.itemId] ?: 0,
                        observacion = observacionesItem[linea.itemId].orEmpty(),
                        habilitado = !procesando,
                        onEstado = { estados[linea.itemId] = it },
                        onDanadas = { danadas[linea.itemId] = it },
                        onExtraviadas = { extraviadas[linea.itemId] = it },
                        onObservacion = { observacionesItem[linea.itemId] = it }
                    )
                }

                OutlinedTextField(
                    value = observacionGeneral,
                    onValueChange = { if (it.length <= 200) observacionGeneral = it },
                    label = { Text(stringResource(R.string.devolucion_observaciones)) },
                    enabled = !procesando,
                    shape = formaApp(12.dp),
                    colors = outlinedFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = if (hayNovedades) {
                        stringResource(R.string.devolucion_aviso_novedades)
                    } else {
                        stringResource(R.string.devolucion_aviso_entrega)
                    },
                    fontSize = 12.sp,
                    color = if (hayNovedades) Colores.salmon else Colores.advertencia,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val resultados = lineas
                        .filter { tieneNovedad(it) }
                        .map { linea ->
                            ResultadoDevolucion(
                                itemId = linea.itemId,
                                estado = estados[linea.itemId] ?: EstadoItemPrestamo.DANADO,
                                cantidadDanada = danadas[linea.itemId] ?: 0,
                                cantidadExtraviada = extraviadas[linea.itemId] ?: 0,
                                observacion = observacionesItem[linea.itemId].orEmpty().trim()
                            )
                        }
                    onConfirmar(resultados, observacionGeneral)
                },
                enabled = !procesando && !faltanObservaciones,
                // Deshabilitado ("Describe la novedad") debe leerse: gris claro sobre gris, no verde sobre gris.
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Colores.sobrePrimario,
                    disabledContainerColor = Colores.deshabilitado,
                    disabledContentColor = Colores.textoSecundario
                ),
                modifier = Modifier.testTag("confirmar_devolucion_button"),
                shape = formaBoton()
            ) {
                if (procesando) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Colores.sobrePrimario, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = when {
                        faltanObservaciones -> stringResource(R.string.devolucion_describe)
                        hayNovedades -> stringResource(R.string.devolucion_registrar_novedades)
                        else -> stringResource(R.string.devolucion_devolver_liberar)
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !procesando, shape = formaBoton()) {
                Text(stringResource(R.string.comun_cancelar), color = Colores.textoSecundario)
            }
        },
        tonoNeon = if (hayNovedades) TonoNeon.AMARILLO else TonoNeon.CIAN,
        containerColor = Colores.dialogo
    )
}

/**
 * Una fila del diálogo de devolución.
 * - Unidad: buen estado, dañado, incompleto o extraviado ("incompleto" solo si la categoría
 *   tiene piezas: no aparece para balones ni petos).
 * - Cantidad: cuántas piezas regresan dañadas y cuántas no regresan.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LineaDevolucion(
    linea: ItemPrestamo,
    admiteIncompleto: Boolean,
    estado: String,
    danadas: Int,
    extraviadas: Int,
    observacion: String,
    habilitado: Boolean,
    onEstado: (String) -> Unit,
    onDanadas: (Int) -> Unit,
    onExtraviadas: (Int) -> Unit,
    onObservacion: (String) -> Unit
) {
    val porCantidad = linea.tipoControl == TipoControl.CANTIDAD
    val conNovedad = if (porCantidad) danadas + extraviadas > 0 else estado != EstadoItemPrestamo.BUEN_ESTADO
    val colorBorde = when {
        !conNovedad -> Colores.borde
        porCantidad -> Colores.errorFuerte
        else -> colorNovedad(estado)
    }

    // El fondo toma un tinte suave del color de la novedad (morado si es incompleto, rojo si
    // es dañado...), en vez de rojo para todas: así coincide con la opción marcada.
    TarjetaApp(
        colors = CardDefaults.cardColors(
            containerColor = if (conNovedad) colorBorde.copy(alpha = 0.12f).compositeOver(Colores.superficie) else Colores.superficie
        ),
        border = BorderStroke(if (conNovedad) 1.5.dp else 1.dp, colorBorde),
        shape = formaApp(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = if (linea.cantidad > 1) "[${linea.itemId}] ${linea.nombre} (x${linea.cantidad})" else "[${linea.itemId}] ${linea.nombre}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Colores.texto
            )

            // Si ya salió incompleto, devolverlo igual no es una novedad del estudiante.
            if (linea.seEntregoIncompleto) {
                Text(
                    text = stringResource(
                        R.string.devolucion_se_presto_incompleto,
                        linea.observacionAlPrestar.ifBlank { stringResource(R.string.comun_sin_observacion_minuscula) }
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = colorNovedad(EstadoItemPrestamo.INCOMPLETO)
                )
            }

            if (porCantidad) {
                ContadorPiezas(
                    etiqueta = stringResource(R.string.devolucion_piezas_danadas),
                    valor = danadas,
                    maximo = linea.cantidad - extraviadas,
                    color = Colores.error,
                    habilitado = habilitado,
                    onCambio = onDanadas
                )
                ContadorPiezas(
                    etiqueta = stringResource(R.string.devolucion_piezas_no_regresaron),
                    valor = extraviadas,
                    maximo = linea.cantidad - danadas,
                    color = colorNovedad(EstadoItemPrestamo.EXTRAVIADO),
                    habilitado = habilitado,
                    onCambio = onExtraviadas
                )
            } else {
                // Filas de dos opciones para que quepan en pantallas angostas.
                // "Incompleto" solo para artículos con piezas (o si ya salió incompleto).
                val opciones = buildList {
                    add(EstadoItemPrestamo.BUEN_ESTADO)
                    add(EstadoItemPrestamo.DANADO)
                    if (admiteIncompleto || linea.seEntregoIncompleto) add(EstadoItemPrestamo.INCOMPLETO)
                    add(EstadoItemPrestamo.EXTRAVIADO)
                }
                opciones.chunked(2).forEach { fila ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        fila.forEach { opcion ->
                            val color = if (opcion == EstadoItemPrestamo.BUEN_ESTADO) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                colorNovedad(opcion)
                            }
                            FilterChip(
                                selected = estado == opcion,
                                onClick = { onEstado(opcion) },
                                enabled = habilitado,
                                label = {
                                    Text(
                                        text = when {
                                            !linea.seEntregoIncompleto -> EstadoItemPrestamo.etiqueta(opcion)
                                            opcion == EstadoItemPrestamo.BUEN_ESTADO -> stringResource(R.string.devolucion_como_se_presto)
                                            opcion == EstadoItemPrestamo.INCOMPLETO -> stringResource(R.string.devolucion_faltan_mas)
                                            else -> EstadoItemPrestamo.etiqueta(opcion)
                                        },
                                        fontSize = 12.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Colores.rellenoEtiqueta(color, 0.25f),
                                    selectedLabelColor = Colores.textoEtiqueta(color),
                                    containerColor = Colores.superficie,
                                    labelColor = Colores.textoSecundario
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chip_${opcion.lowercase()}_${linea.itemId}")
                            )
                        }
                        // Si la fila quedó con una sola opción, ocupa la mitad como las demás.
                        if (fila.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            if (conNovedad) {
                OutlinedTextField(
                    value = observacion,
                    onValueChange = { if (it.length <= 150) onObservacion(it) },
                    label = { Text(stringResource(R.string.devolucion_observacion)) },
                    placeholder = {
                        Text(
                            when {
                                porCantidad -> stringResource(R.string.devolucion_ejemplo_cantidad)
                                estado == EstadoItemPrestamo.INCOMPLETO -> stringResource(R.string.devolucion_ejemplo_incompleto)
                                estado == EstadoItemPrestamo.EXTRAVIADO -> stringResource(R.string.devolucion_ejemplo_extraviado)
                                else -> stringResource(R.string.devolucion_ejemplo_danado)
                            }
                        )
                    },
                    isError = observacion.isBlank(),
                    enabled = habilitado,
                    shape = formaApp(12.dp),
                    colors = outlinedFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("observacion_novedad_${linea.itemId}")
                )
            }
        }
    }
}

/** Contador con botones − y + para indicar piezas de un artículo por cantidad. */
@Composable
private fun ContadorPiezas(
    etiqueta: String,
    valor: Int,
    maximo: Int,
    color: Color,
    habilitado: Boolean,
    onCambio: (Int) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelSmall,
            color = Colores.textoSecundario,
            modifier = Modifier.weight(1f)
        )
        ContadorCantidad(
            valor = valor,
            onCambio = onCambio,
            minimo = 0,
            maximo = maximo,
            habilitado = habilitado,
            colorValor = if (valor > 0) color else MaterialTheme.colorScheme.primary,
            tamanoBoton = 32.dp,
            etiquetaPrueba = "contador_${etiqueta.filter { it.isLetter() }.lowercase()}"
        )
    }
}

@Composable
fun ResolverDanoDialog(
    prestamo: Prestamo,
    procesando: Boolean,
    onDismiss: () -> Unit,
    onConfirmar: (nota: String) -> Unit
) {
    var nota by remember(prestamo.id) { mutableStateOf("") }
    val conNovedad = remember(prestamo.id) { prestamo.itemsEfectivos.filter { it.tieneNovedad } }

    DialogoApp(
        onDismissRequest = { if (!procesando) onDismiss() },
        icon = {
            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
        },
        title = {
            Text(stringResource(R.string.devolucion_resolver_titulo), fontWeight = FontWeight.Bold, color = Colores.texto)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                // Con el teléfono girado el diálogo es bajo: el contenido se desplaza
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "${prestamo.nombreEstudiante} (${prestamo.identificadorPrestatario})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Colores.acentoTexto
                )
                conNovedad.forEach { linea ->
                    Text(
                        text = " • [${linea.itemId}] ${linea.nombre} (${linea.descripcionNovedad}): " +
                            linea.observacion.ifBlank { stringResource(R.string.comun_sin_observacion_minuscula) },
                        style = MaterialTheme.typography.labelSmall,
                        color = Colores.textoSecundario
                    )
                }
                OutlinedTextField(
                    value = nota,
                    onValueChange = { if (it.length <= 150) nota = it },
                    label = { Text(stringResource(R.string.devolucion_como_se_resolvio)) },
                    placeholder = { Text(stringResource(R.string.devolucion_resolucion_ejemplo)) },
                    enabled = !procesando,
                    shape = formaApp(12.dp),
                    colors = outlinedFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.devolucion_resolver_texto),
                    fontSize = 12.sp,
                    color = Colores.advertencia
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmar(nota) },
                enabled = !procesando,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = formaBoton()
            ) {
                if (procesando) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Colores.sobrePrimario, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(stringResource(R.string.devolucion_resolver_liberar), color = Colores.sobrePrimario, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !procesando, shape = formaBoton()) {
                Text(stringResource(R.string.comun_cancelar), color = Colores.textoSecundario)
            }
        },
        containerColor = Colores.dialogo
    )
}

@Composable
fun PrestamoDevolucionCard(
    prestamo: Prestamo,
    formatoFecha: SimpleDateFormat,
    onDevolverClick: () -> Unit,
    onResolverDanoClick: () -> Unit = {},
    // false para usar la tarjeta solo como consulta (historial en Reportes)
    mostrarAcciones: Boolean = true,
    // Celular del estudiante; por defecto, el que se copió al prestar
    telefono: String = prestamo.telefono
) {
    val esActivo = prestamo.estaActivo
    val vencido = prestamo.estaVencido()
    val conDano = prestamo.danoPendiente

    // Texto y colores de la etiqueta de estado
    val (etiqueta, colorEtiqueta, fondoEtiqueta) = when {
        vencido -> Triple(stringResource(R.string.comun_insignia_vencido), Colores.error, Colores.errorFondo)
        esActivo -> Triple(stringResource(R.string.devolucion_insignia_pendiente), Colores.advertencia, Colores.advertenciaFondo)
        conDano -> Triple(stringResource(R.string.devolucion_insignia_novedad), Colores.naranja, Colores.advertenciaFondo)
        else -> Triple(stringResource(R.string.comun_insignia_devuelto), Colores.acentoTexto, Colores.exitoFondo)
    }

    TarjetaApp(
        shape = formaApp(16.dp),
        colors = CardDefaults.cardColors(containerColor = Colores.superficie),
        border = BorderStroke(
            1.dp,
            when {
                vencido -> Colores.errorFuerte
                esActivo || conDano -> Colores.borde
                else -> Colores.borde
            }
        ),
        // En neón, barra a la izquierda del color del estado (vencido, pendiente, novedad, devuelto)
        acentoNeon = colorEtiqueta,
        modifier = Modifier.fillMaxWidth().testTag("prestamo_card_${prestamo.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Cabecera: ID Préstamo y Estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "#${prestamo.id.takeLast(6)}",
                        fontWeight = FontWeight.Bold,
                        color = Colores.acentoTexto,
                        fontSize = 13.sp
                    )
                    if (prestamo.carnetRetenido && (esActivo || conDano)) {
                        Surface(
                            shape = formaApp(6.dp),
                            color = Colores.rellenoEtiqueta(Colores.error, Colores.errorFondo),
                            border = BorderStroke(1.dp, Colores.errorFuerte)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("🪪", fontSize = 11.sp)
                                Text(
                                    text = stringResource(R.string.devolucion_carnet_retenido),
                                    color = Colores.textoEtiqueta(Colores.error),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Surface(
                    shape = formaApp(6.dp),
                    color = Colores.rellenoEtiqueta(colorEtiqueta, fondoEtiqueta)
                ) {
                    Text(
                        text = etiqueta,
                        color = Colores.textoEtiqueta(colorEtiqueta),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Datos del estudiante
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = formaApp(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(6.dp)
                    )
                }
                Column {
                    Text(
                        text = prestamo.nombreEstudiante,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Colores.texto
                    )
                    Text(
                        text = listOf(
                            stringResource(R.string.comun_codigo_estudiante_de, prestamo.identificadorPrestatario),
                            ProgramasAcademicos.etiqueta(prestamo.programaEstudiante)
                        )
                            .filter { it.isNotBlank() }
                            .joinToString(" • "),
                        style = MaterialTheme.typography.labelSmall,
                        color = Colores.textoTenue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Contacto: solo mientras hay algo pendiente (préstamo sin devolver o novedad).
            // En el historial de Reportes no se muestra: ahí solo está el número copiado al prestar.
            if (mostrarAcciones && (esActivo || conDano)) {
                ContactoTelefono(telefono = telefono)
            }

            HorizontalDivider(color = Colores.borde, thickness = 1.dp)

            // Implementos prestados y, si ya se devolvieron, en qué estado regresaron
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.devolucion_implementos_prestados, prestamo.cantidadTotalItems),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Colores.acentoTexto
                )
                prestamo.itemsEfectivos.forEach { linea ->
                    val danado = linea.tieneNovedad
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("•", color = if (danado) Colores.error else MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                        Text(
                            text = buildString {
                                append("${linea.itemId} - ${linea.nombre}")
                                if (linea.cantidad > 1) append(" (x${linea.cantidad})")
                                if (danado) {
                                    append(" — ${linea.descripcionNovedad}: ")
                                    append(linea.observacion.ifBlank { stringResource(R.string.comun_sin_observacion_minuscula) })
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (danado) Colores.salmon else Colores.texto,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Fechas del préstamo
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val fechaTexto = prestamo.fechaPrestamo?.toDate()?.let { formatoFecha.format(it) } ?: stringResource(R.string.devolucion_reciente)
                Text(
                    text = stringResource(R.string.devolucion_retiro, fechaTexto),
                    style = MaterialTheme.typography.labelSmall,
                    color = Colores.textoTenue
                )
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
                        text = stringResource(R.string.devolucion_fecha_devolucion, formatoFecha.format(devolucion)),
                        style = MaterialTheme.typography.labelSmall,
                        color = Colores.textoTenue
                    )
                }
            }

            // Acción según el estado
            if (!mostrarAcciones) {
                // Tarjeta de solo consulta: sin botones
            } else if (esActivo) {
                Button(
                    onClick = onDevolverClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("boton_devolver_${prestamo.id}"),
                    shape = formaApp(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Colores.sobrePrimario,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.comun_registrar_devolucion),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Colores.sobrePrimario
                    )
                }
            } else if (conDano) {
                OutlinedButton(
                    onClick = onResolverDanoClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("boton_resolver_dano_${prestamo.id}"),
                    shape = formaApp(10.dp),
                    border = BorderStroke(1.dp, Colores.naranja)
                ) {
                    Text(
                        text = stringResource(R.string.devolucion_resolver_liberar_largo),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Colores.naranja
                    )
                }
            }
        }
    }
}
