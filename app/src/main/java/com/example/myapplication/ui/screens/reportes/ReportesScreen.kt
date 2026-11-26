package com.example.myapplication.ui.screens.reportes

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.data.Prestamo
import com.example.myapplication.data.reportes.FormatoReporte
import com.example.myapplication.data.reportes.GeneradorReportes
import com.example.myapplication.data.reportes.GrupoHistorial
import com.example.myapplication.data.reportes.Indicadores
import com.example.myapplication.data.reportes.TipoReporte
import com.example.myapplication.ui.components.ImagenArticulo
import com.example.myapplication.ui.components.ListaConEncabezado
import com.example.myapplication.ui.components.SelectorFechaDialog
import com.example.myapplication.ui.screens.inventario.ResumenMiniCard
import com.example.myapplication.ui.screens.prestamo.PrestamoDevolucionCard
import com.example.myapplication.ui.screens.prestamo.SeccionCard
import com.example.myapplication.ui.screens.prestamo.outlinedFieldColors
import com.example.myapplication.ui.theme.formaApp
import com.example.myapplication.utils.Fechas
import com.example.myapplication.utils.Idioma
import com.example.myapplication.viewmodel.ReportesUiState
import com.example.myapplication.viewmodel.ReportesViewModel
import java.text.SimpleDateFormat
import java.util.Date
import com.example.myapplication.ui.theme.Colores

private const val SECCION_INDICADORES = 0
private const val SECCION_EXPORTAR = 1
private const val SECCION_HISTORIAL = 2

/** Mensaje para el Snackbar; si trae [uri] ofrece abrir el archivo recién guardado. */
private data class AvisoReporte(
    val mensaje: String,
    val uri: Uri? = null,
    val mime: String = ""
)

/**
 * Reportes del monitor: indicadores (RF-20), exportación en CSV, Excel y PDF con rango
 * de fechas (RF-17) e historial por estudiante o por artículo (RF-18).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportesScreen(
    onVolver: () -> Unit,
    viewModel: ReportesViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var seccion by rememberSaveable { mutableStateOf(SECCION_INDICADORES) }
    // Se guarda con rememberSaveable porque el selector de archivos abre otra pantalla del sistema.
    var tipoPendiente by rememberSaveable { mutableStateOf(TipoReporte.PRESTAMOS) }
    var aviso by remember { mutableStateOf<AvisoReporte?>(null) }

    // El monitor ya eligió dónde guardar: se genera el reporte y se escribe en ese archivo.
    val alElegirDestino: (Uri?, FormatoReporte) -> Unit = { uri, formato ->
        if (uri != null) {
            viewModel.exportar(
                tipo = tipoPendiente,
                formato = formato,
                abrirSalida = { context.contentResolver.openOutputStream(uri) }
            ) { resultado ->
                aviso = resultado.fold(
                    onSuccess = { mensaje -> AvisoReporte(mensaje, uri, formato.mime) },
                    onFailure = { error ->
                        AvisoReporte(
                            Idioma.texto(
                                R.string.reportes_error_exportar,
                                error.localizedMessage ?: Idioma.texto(R.string.comun_error_desconocido)
                            )
                        )
                    }
                )
            }
        }
    }

    // Un selector de destino por formato (cada uno propone la extensión correcta).
    val lanzadorCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(FormatoReporte.CSV.mime)
    ) { uri -> alElegirDestino(uri, FormatoReporte.CSV) }
    val lanzadorExcel = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(FormatoReporte.EXCEL.mime)
    ) { uri -> alElegirDestino(uri, FormatoReporte.EXCEL) }
    val lanzadorPdf = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(FormatoReporte.PDF.mime)
    ) { uri -> alElegirDestino(uri, FormatoReporte.PDF) }

    val solicitarExportacion: (TipoReporte, FormatoReporte) -> Unit = { tipo, formato ->
        tipoPendiente = tipo
        val nombre = GeneradorReportes.nombreArchivo(tipo, formato)
        try {
            when (formato) {
                FormatoReporte.CSV -> lanzadorCsv.launch(nombre)
                FormatoReporte.EXCEL -> lanzadorExcel.launch(nombre)
                FormatoReporte.PDF -> lanzadorPdf.launch(nombre)
            }
        } catch (_: ActivityNotFoundException) {
            aviso = AvisoReporte(Idioma.texto(R.string.reportes_sin_app_guardar))
        }
    }

    LaunchedEffect(uiState.mensajeError) {
        uiState.mensajeError?.let { mensaje ->
            aviso = AvisoReporte(mensaje)
            viewModel.limpiarError()
        }
    }

    LaunchedEffect(aviso) {
        val actual = aviso ?: return@LaunchedEffect
        val resultado = snackbarHostState.showSnackbar(
            message = actual.mensaje,
            actionLabel = if (actual.uri != null) Idioma.texto(R.string.comun_abrir) else null,
            duration = SnackbarDuration.Long
        )
        if (resultado == SnackbarResult.ActionPerformed && actual.uri != null) {
            abrirArchivo(context, actual.uri, actual.mime)
        }
        aviso = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.reportes_titulo),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Colores.sobreBarra
                        )
                        Text(
                            text = stringResource(R.string.reportes_subtitulo),
                            style = MaterialTheme.typography.bodySmall,
                            color = Colores.sobreBarraSecundario
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onVolver,
                        modifier = Modifier.testTag("volver_reportes_button")
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Colores.fondoPantalla
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChipReporte(stringResource(R.string.reportes_pestana_indicadores), seccion == SECCION_INDICADORES, "seccion_indicadores") { seccion = SECCION_INDICADORES }
                ChipReporte(stringResource(R.string.reportes_pestana_exportar), seccion == SECCION_EXPORTAR, "seccion_exportar") { seccion = SECCION_EXPORTAR }
                ChipReporte(stringResource(R.string.comun_historial_emoji), seccion == SECCION_HISTORIAL, "seccion_historial") { seccion = SECCION_HISTORIAL }
            }

            when (seccion) {
                SECCION_INDICADORES -> IndicadoresSeccion(uiState.indicadores, uiState.cargando)
                SECCION_EXPORTAR -> ExportarSeccion(
                    uiState = uiState,
                    onDesde = viewModel::establecerDesde,
                    onHasta = viewModel::establecerHasta,
                    onLimpiarRango = viewModel::limpiarRango,
                    onExportar = solicitarExportacion
                )
                else -> HistorialSeccion(
                    prestamos = uiState.prestamos,
                    buscar = viewModel::historial
                )
            }
        }
    }
}

/** Abre el archivo exportado con la app que el usuario tenga para ese formato. */
private fun abrirArchivo(context: Context, uri: Uri, mime: String) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mime)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        android.widget.Toast.makeText(
            context,
            Idioma.texto(R.string.reportes_sin_app_abrir),
            android.widget.Toast.LENGTH_LONG
        ).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChipReporte(texto: String, seleccionado: Boolean, etiquetaPrueba: String, onClick: () -> Unit) {
    FilterChip(
        selected = seleccionado,
        onClick = onClick,
        label = { Text(texto, fontSize = 13.sp, fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Normal) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = Colores.sobrePrimario,
            containerColor = Colores.superficie,
            labelColor = Colores.textoSecundario
        ),
        modifier = Modifier.testTag(etiquetaPrueba)
    )
}

// =============================================================================
// SECCIÓN 1: PANEL DE INDICADORES (RF-20)
// =============================================================================
@Composable
private fun IndicadoresSeccion(indicadores: Indicadores, cargando: Boolean) {
    if (cargando) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ResumenMiniCard(
                titulo = stringResource(R.string.reportes_hoy),
                valor = "${indicadores.prestamosHoy}",
                subtitulo = stringResource(R.string.reportes_hoy_sub),
                colorTexto = Colores.texto,
                modifier = Modifier.weight(1f)
            )
            ResumenMiniCard(
                titulo = stringResource(R.string.reportes_activos),
                valor = "${indicadores.activos}",
                subtitulo = stringResource(R.string.reportes_activos_sub),
                colorTexto = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ResumenMiniCard(
                titulo = stringResource(R.string.reportes_vencidos),
                valor = "${indicadores.vencidos}",
                subtitulo = stringResource(R.string.reportes_vencidos_sub),
                colorTexto = if (indicadores.vencidos > 0) Colores.error else Colores.textoSecundario,
                modifier = Modifier.weight(1f)
            )
            ResumenMiniCard(
                titulo = stringResource(R.string.comun_novedades),
                valor = "${indicadores.piezasConNovedad}",
                subtitulo = stringResource(R.string.reportes_novedades_sub),
                colorTexto = if (indicadores.piezasConNovedad > 0) Colores.naranja else Colores.textoSecundario,
                modifier = Modifier.weight(1f)
            )
        }

        SeccionCard(
            titulo = stringResource(R.string.reportes_mas_prestados),
            subtitulo = stringResource(R.string.reportes_mas_prestados_sub),
            icono = Icons.Default.CheckCircle
        ) {
            if (indicadores.masPrestados.isEmpty()) {
                Text(
                    text = stringResource(R.string.comun_sin_prestamos),
                    style = MaterialTheme.typography.bodySmall,
                    color = Colores.textoTenue
                )
            } else {
                val maximo = indicadores.masPrestados.maxOf { it.veces }.coerceAtLeast(1)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    indicadores.masPrestados.forEachIndexed { indice, articulo ->
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${indice + 1}. ${articulo.nombre}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Colores.texto,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = pluralStringResource(R.plurals.reportes_veces, articulo.veces, articulo.veces),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = stringResource(R.string.comun_codigo_articulo_de, articulo.itemId),
                                style = MaterialTheme.typography.labelSmall,
                                color = Colores.textoTenue
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth((articulo.veces.toFloat() / maximo).coerceIn(0.05f, 1f))
                                    .height(6.dp)
                                    .background(MaterialTheme.colorScheme.primary, formaApp(3.dp))
                            )
                        }
                    }
                }
            }
        }

        SeccionCard(
            titulo = stringResource(R.string.reportes_con_novedad),
            subtitulo = stringResource(R.string.reportes_con_novedad_sub),
            icono = Icons.Default.Warning
        ) {
            if (indicadores.articulosConNovedad.isEmpty()) {
                Text(
                    text = stringResource(R.string.reportes_sin_novedades),
                    style = MaterialTheme.typography.bodySmall,
                    color = Colores.textoTenue
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    indicadores.articulosConNovedad.forEach { item ->
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ImagenArticulo(item.nombre, item.categoria, item.icono, tamano = 28.dp, tamanoEmoji = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = buildString {
                                        append("[${item.id}] ${item.nombre}")
                                        append(" · ${item.resumenNovedad}")
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Colores.texto,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = item.observacion.ifBlank { stringResource(R.string.comun_sin_observacion) },
                                style = MaterialTheme.typography.labelSmall,
                                color = Colores.salmon
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

// =============================================================================
// SECCIÓN 2: EXPORTAR REPORTES (RF-17)
// =============================================================================
@Composable
private fun ExportarSeccion(
    uiState: ReportesUiState,
    onDesde: (Date?) -> Unit,
    onHasta: (Date?) -> Unit,
    onLimpiarRango: () -> Unit,
    onExportar: (TipoReporte, FormatoReporte) -> Unit
) {
    var eligiendoDesde by remember { mutableStateOf(false) }
    var eligiendoHasta by remember { mutableStateOf(false) }
    val hayRango = uiState.desde != null || uiState.hasta != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SeccionCard(
            titulo = stringResource(R.string.reportes_periodo),
            subtitulo = stringResource(R.string.reportes_periodo_sub),
            icono = Icons.Default.DateRange
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { eligiendoDesde = true },
                    shape = formaApp(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("fecha_desde_button")
                ) {
                    Text(
                        text = stringResource(
                            R.string.reportes_desde,
                            uiState.desde?.let { Fechas.corta(it) } ?: stringResource(R.string.reportes_desde_inicio)
                        ),
                        color = Colores.texto,
                        fontSize = 12.sp
                    )
                }
                OutlinedButton(
                    onClick = { eligiendoHasta = true },
                    shape = formaApp(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("fecha_hasta_button")
                ) {
                    Text(
                        text = stringResource(
                            R.string.reportes_hasta,
                            uiState.hasta?.let { Fechas.corta(it) } ?: stringResource(R.string.reportes_hasta_hoy)
                        ),
                        color = Colores.texto,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = pluralStringResource(R.plurals.reportes_prestamos_periodo, uiState.prestamosEnRango, uiState.prestamosEnRango),
                    style = MaterialTheme.typography.bodySmall,
                    color = Colores.acentoTexto,
                    modifier = Modifier.weight(1f)
                )
                if (hayRango) {
                    TextButton(onClick = onLimpiarRango) {
                        Text(stringResource(R.string.reportes_quitar_filtro), color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                    }
                }
            }
        }

        SeccionCard(
            titulo = stringResource(R.string.reportes_tipo_prestamos),
            subtitulo = stringResource(R.string.reportes_tipo_prestamos_sub),
            icono = Icons.Default.CheckCircle
        ) {
            BotonesFormato(
                habilitado = !uiState.exportando,
                prefijoPrueba = "exportar_prestamos"
            ) { formato -> onExportar(TipoReporte.PRESTAMOS, formato) }
        }

        SeccionCard(
            titulo = stringResource(R.string.reportes_tipo_inventario),
            subtitulo = stringResource(R.string.reportes_tipo_inventario_sub),
            icono = Icons.Default.Info
        ) {
            BotonesFormato(
                habilitado = !uiState.exportando,
                prefijoPrueba = "exportar_inventario"
            ) { formato -> onExportar(TipoReporte.INVENTARIO, formato) }
        }

        if (uiState.exportando) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
                Text(stringResource(R.string.reportes_generando), color = Colores.textoSecundario, fontSize = 13.sp)
            }
        }

        Text(
            text = stringResource(R.string.reportes_ayuda_formato),
            style = MaterialTheme.typography.labelSmall,
            color = Colores.textoTenue
        )

        Spacer(modifier = Modifier.height(12.dp))
    }

    if (eligiendoDesde) {
        SelectorFechaDialog(
            fechaInicial = uiState.desde,
            fechaMaxima = Date(),
            onConfirmar = { fecha ->
                // El selector entrega el final del día; el inicio del rango es a las 00:00.
                onDesde(Fechas.inicioDelDia(fecha))
                eligiendoDesde = false
            },
            onDismiss = { eligiendoDesde = false }
        )
    }

    if (eligiendoHasta) {
        SelectorFechaDialog(
            fechaInicial = uiState.hasta,
            fechaMinima = uiState.desde,
            fechaMaxima = Date(),
            onConfirmar = { fecha ->
                onHasta(fecha)
                eligiendoHasta = false
            },
            onDismiss = { eligiendoHasta = false }
        )
    }
}

@Composable
private fun BotonesFormato(
    habilitado: Boolean,
    prefijoPrueba: String,
    onFormato: (FormatoReporte) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FormatoReporte.values().forEach { formato ->
            Button(
                onClick = { onFormato(formato) },
                enabled = habilitado,
                shape = formaApp(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = Colores.deshabilitado,
                    disabledContentColor = Colores.textoTenue
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("${prefijoPrueba}_${formato.extension}")
            ) {
                Text(
                    text = formato.etiqueta,
                    color = if (habilitado) Colores.sobrePrimario else Colores.textoTenue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// =============================================================================
// SECCIÓN 3: HISTORIAL POR ESTUDIANTE O POR ARTÍCULO (RF-18)
// =============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistorialSeccion(
    prestamos: List<Prestamo>,
    buscar: (porArticulo: Boolean, consulta: String) -> List<GrupoHistorial>
) {
    var porArticulo by rememberSaveable { mutableStateOf(false) }
    var consulta by rememberSaveable { mutableStateOf("") }
    var claveSeleccionada by rememberSaveable { mutableStateOf<String?>(null) }

    // Los grupos traen textos ya traducidos ("Código: …"): se recalculan al cambiar el idioma
    val idioma = Idioma.actual
    val formatoFecha = remember(idioma) { Fechas.formatoConHora() }
    val grupos = remember(prestamos, porArticulo, consulta, idioma) { buscar(porArticulo, consulta) }
    val seleccionado = remember(prestamos, porArticulo, claveSeleccionada, idioma) {
        claveSeleccionada?.let { clave -> buscar(porArticulo, "").find { it.clave == clave } }
    }

    // Filtros y búsqueda arriba; con el teléfono girado se desplazan junto con los resultados
    ListaConEncabezado(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        espacioEncabezado = 10.dp,
        espacioLista = if (seleccionado == null) 8.dp else 12.dp,
        encabezado = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChipReporte(stringResource(R.string.reportes_por_estudiante), !porArticulo, "historial_por_estudiante") {
                    porArticulo = false
                    claveSeleccionada = null
                    consulta = ""
                }
                ChipReporte(stringResource(R.string.reportes_por_articulo), porArticulo, "historial_por_articulo") {
                    porArticulo = true
                    claveSeleccionada = null
                    consulta = ""
                }
            }

            if (seleccionado == null) {
                OutlinedTextField(
                    value = consulta,
                    onValueChange = { consulta = it },
                    label = { Text(if (porArticulo) stringResource(R.string.reportes_buscar_articulo) else stringResource(R.string.reportes_buscar_estudiante)) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        if (consulta.isNotBlank()) {
                            IconButton(onClick = { consulta = "" }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.comun_limpiar_busqueda), tint = Colores.textoTenue)
                            }
                        }
                    },
                    singleLine = true,
                    shape = formaApp(12.dp),
                    colors = outlinedFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("historial_busqueda_input")
                )

                if (grupos.isEmpty()) {
                    Text(
                        text = if (prestamos.isEmpty()) stringResource(R.string.comun_sin_prestamos) else stringResource(R.string.reportes_sin_resultados),
                        style = MaterialTheme.typography.bodySmall,
                        color = Colores.textoTenue
                    )
                }
            } else {
                // Historial del estudiante o artículo elegido
                Surface(
                    shape = formaApp(12.dp),
                    color = Colores.superficieAlta,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = seleccionado.titulo,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Colores.texto
                            )
                            Text(
                                text = seleccionado.subtitulo,
                                style = MaterialTheme.typography.bodySmall,
                                color = Colores.acentoTexto
                            )
                            Text(
                                text = pluralStringResource(R.plurals.comun_n_prestamos, seleccionado.prestamos.size, seleccionado.prestamos.size) +
                                    " · " + stringResource(R.string.reportes_n_sin_devolver, seleccionado.prestamos.count { it.estaActivo }) +
                                    " · " + stringResource(
                                        R.string.reportes_n_con_novedades,
                                        seleccionado.prestamos.count { prestamo -> prestamo.itemsEfectivos.any { it.tieneNovedad } }
                                    ),
                                style = MaterialTheme.typography.labelSmall,
                                color = Colores.textoSecundario
                            )
                        }
                        TextButton(onClick = { claveSeleccionada = null }) {
                            Text(stringResource(R.string.comun_cambiar), color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    ) {
        if (seleccionado == null) {
            items(grupos, key = { it.clave }) { grupo ->
                Surface(
                    shape = formaApp(12.dp),
                    color = Colores.superficie,
                    border = BorderStroke(Colores.anchoBorde(1.dp), Colores.borde),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(formaApp(12.dp))
                        .clickable { claveSeleccionada = grupo.clave }
                        .testTag("historial_grupo_${grupo.clave}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = grupo.titulo,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Colores.texto,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = grupo.subtitulo,
                                style = MaterialTheme.typography.labelSmall,
                                color = Colores.acentoTexto
                            )
                        }
                        Text(
                            text = pluralStringResource(R.plurals.comun_n_prestamos, grupo.prestamos.size, grupo.prestamos.size),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        } else {
            items(seleccionado.prestamos, key = { it.id }) { prestamo ->
                PrestamoDevolucionCard(
                    prestamo = prestamo,
                    formatoFecha = formatoFecha,
                    onDevolverClick = {},
                    mostrarAcciones = false
                )
            }
        }
    }
}
