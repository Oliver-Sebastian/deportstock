package com.example.myapplication.ui.screens.prestamo

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.data.Categorias
import com.example.myapplication.data.EstadoItem
import com.example.myapplication.data.ItemDeportivo
import com.example.myapplication.data.ProgramasAcademicos
import com.example.myapplication.data.SeleccionPrestamo
import com.example.myapplication.ui.components.AvatarPerfil
import com.example.myapplication.ui.components.ContactoTelefono
import com.example.myapplication.ui.components.ContadorCantidad
import com.example.myapplication.ui.components.Desplegable
import com.example.myapplication.ui.components.DialogoApp
import com.example.myapplication.ui.components.EstiloNeon
import com.example.myapplication.ui.components.ImagenArticulo
import com.example.myapplication.ui.components.MarcoIconoNeon
import com.example.myapplication.ui.components.SelectorFechaDialog
import com.example.myapplication.ui.components.TarjetaApp
import com.example.myapplication.ui.components.TarjetaElevadaApp
import com.example.myapplication.ui.components.TonoNeon
import com.example.myapplication.ui.screens.inventario.colorNovedad
import com.example.myapplication.ui.theme.formaApp
import com.example.myapplication.ui.theme.formaBoton
import com.example.myapplication.utils.Fechas
import com.example.myapplication.utils.Idioma
import com.example.myapplication.viewmodel.PrestamoViewModel
import java.util.Date
import com.example.myapplication.ui.theme.Colores

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarPrestamoScreen(
    onVolver: () -> Unit,
    onConfigurarSemestre: () -> Unit = {},
    viewModel: PrestamoViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val itemsInventario = uiState.inventario
    val guardandoPrestamo = uiState.guardando

    LaunchedEffect(uiState.mensajeError) {
        uiState.mensajeError?.let { mensaje ->
            Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
            viewModel.limpiarError()
        }
    }

    // 1. Estudiante que recibe el préstamo (RF-08: se busca por código o nombre)
    var busquedaEstudiante by remember { mutableStateOf("") }
    var estudianteId by remember { mutableStateOf<String?>(null) }
    // Se toma siempre de la lista en tiempo real, para ver al instante si queda bloqueado.
    val estudianteSeleccionado = uiState.estudiantes.find { it.id == estudianteId }
    LaunchedEffect(estudianteId) { viewModel.cargarFotoDe(estudianteId) }
    val resultadosBusqueda = remember(busquedaEstudiante, uiState.estudiantes) {
        viewModel.buscarEstudiantes(busquedaEstudiante)
    }

    // RF-12: bloqueo por préstamo sin devolver, devolución vencida o daño sin resolver
    val bloqueo = remember(estudianteSeleccionado, uiState.prestamos, Idioma.actual) {
        estudianteSeleccionado?.let { viewModel.bloqueoDe(it) }
    }
    val estudianteBloqueado = bloqueo != null

    // Piezas a prestar de cada artículo controlado por cantidad (id del artículo -> piezas)
    val cantidades = remember { mutableStateMapOf<String, Int>() }

    // 2. Estados de Selección de Artículos (Múltiples elementos posibles)
    val articulosSeleccionados = remember { mutableStateListOf<ItemDeportivo>() }
    var categoriaFiltro by remember { mutableStateOf("Todos") }
    var busquedaItem by remember { mutableStateOf("") }

    // Categorías del desplegable: las de siempre más cualquier otra que exista en el inventario
    val categoriasDisponibles = remember(itemsInventario) {
        val base = listOf("Balones", "Raquetas", "Petos", "Conos", "Ajedrez", "Otros")
        listOf("Todos") + (base + itemsInventario.map { it.categoria }).filter { it.isNotBlank() }.distinct()
    }

    // Filtrar implementos que se pueden prestar (disponibles, o incompletos como un ajedrez
    // al que le falta una pieza) y que no estén ya seleccionados
    val itemsDisponibles = remember(itemsInventario, categoriaFiltro, busquedaItem, articulosSeleccionados.size) {
        itemsInventario.filter { item ->
            val estaDisponible = item.sePuedePrestar
            val noEstaEnCanasta = articulosSeleccionados.none { it.id.equals(item.id, ignoreCase = true) }
            val cumpleCategoria = if (categoriaFiltro == "Todos") true else item.categoria.equals(categoriaFiltro, ignoreCase = true)
            val cumpleBusqueda = if (busquedaItem.isBlank()) true else {
                item.id.contains(busquedaItem.trim(), ignoreCase = true) ||
                item.nombre.contains(busquedaItem.trim(), ignoreCase = true)
            }
            estaDisponible && noEstaEnCanasta && cumpleCategoria && cumpleBusqueda
        }
    }

    fun cantidadDe(item: ItemDeportivo): Int =
        if (item.esPorCantidad) cantidades[item.id] ?: 1 else 1

    val totalPiezas = articulosSeleccionados.sumOf { cantidadDe(it) }

    // 3. Estados de Detalles del Préstamo
    // Regla de negocio 2: la fecha límite no puede superar el fin de semestre.
    val finSemestre = uiState.fechaFinSemestre
    val semestreVigente = finSemestre != null && finSemestre.after(Date())
    var fechaLimiteElegida by remember { mutableStateOf<Date?>(null) }
    // Por defecto la fecha límite es el fin de semestre; el monitor puede elegir una anterior.
    val fechaLimite = fechaLimiteElegida?.takeIf { finSemestre == null || !it.after(finSemestre) } ?: finSemestre
    var mostrarDialogoFecha by remember { mutableStateOf(false) }
    var carnetRetenido by remember { mutableStateOf(true) }

    // 4. Estados de Validación y Firma
    var compromisoAceptado by remember { mutableStateOf(true) }
    val trazosFirma = remember { mutableStateListOf<List<Offset>>() }
    var trazoActual by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var mostrarConfirmacionDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.comun_registrar_prestamo),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Colores.sobreBarra
                        )
                        Text(
                            text = stringResource(R.string.prestamo_subtitulo),
                            style = MaterialTheme.typography.bodySmall,
                            color = Colores.sobreBarraSecundario
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onVolver,
                        modifier = Modifier.testTag("volver_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.comun_volver_menu),
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ==========================================
            // SECCIÓN 1: DATOS DEL SOLICITANTE
            // ==========================================
            SeccionCard(
                titulo = stringResource(R.string.prestamo_estudiante_titulo),
                subtitulo = stringResource(R.string.prestamo_estudiante_ayuda),
                icono = Icons.Default.Person
            ) {
                if (estudianteSeleccionado == null) {
                    OutlinedTextField(
                        value = busquedaEstudiante,
                        onValueChange = { input ->
                            if (input.length <= 50) {
                                busquedaEstudiante = input
                            }
                        },
                        label = { Text(stringResource(R.string.prestamo_buscar_estudiante)) },
                        placeholder = { Text(stringResource(R.string.prestamo_buscar_estudiante_ejemplo)) },
                        singleLine = true,
                        supportingText = {
                            Text(stringResource(R.string.prestamo_estudiante_requiere_cuenta), color = Colores.textoTenue, fontSize = 11.sp)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            if (busquedaEstudiante.isNotBlank()) {
                                IconButton(onClick = { busquedaEstudiante = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.comun_limpiar_busqueda), tint = Colores.textoTenue)
                                }
                            }
                        },
                        shape = formaApp(12.dp),
                        colors = outlinedFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("codigo_estudiante_input")
                    )

                    if (busquedaEstudiante.trim().length >= 2) {
                        Spacer(modifier = Modifier.height(6.dp))
                        if (resultadosBusqueda.isEmpty()) {
                            Text(
                                text = stringResource(R.string.prestamo_sin_estudiantes),
                                style = MaterialTheme.typography.bodySmall,
                                color = Colores.naranja
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                resultadosBusqueda.forEach { estudiante ->
                                    Surface(
                                        shape = formaApp(10.dp),
                                        color = Colores.superficie,
                                        border = BorderStroke(1.dp, Colores.borde),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(formaApp(10.dp))
                                            .clickable {
                                                estudianteId = estudiante.id
                                                busquedaEstudiante = ""
                                            }
                                            .testTag("resultado_estudiante_${estudiante.codigo}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = estudiante.nombre,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Colores.texto,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = stringResource(
                                                        R.string.prestamo_codigo_programa,
                                                        estudiante.codigo.ifBlank { stringResource(R.string.comun_sin_codigo) },
                                                        ProgramasAcademicos.etiqueta(estudiante.programa)
                                                            .ifBlank { stringResource(R.string.prestamo_programa_sin_registrar) }
                                                    ),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Colores.acentoTexto,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Estudiante elegido: el monitor valida su identidad con el carnet físico.
                    Surface(
                        shape = formaApp(12.dp),
                        color = Colores.superficieAlta,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("estudiante_seleccionado")
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Foto del perfil: ayuda a confirmar que quien pide es el dueño del carnet.
                                AvatarPerfil(foto = uiState.fotoEstudiante, tamano = 52.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = estudianteSeleccionado.nombre,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Colores.texto
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.comun_codigo_estudiante_de,
                                            estudianteSeleccionado.codigo.ifBlank { stringResource(R.string.comun_sin_codigo) }
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Colores.acentoTexto
                                    )
                                    Text(
                                        text = ProgramasAcademicos.etiqueta(estudianteSeleccionado.programa)
                                            .ifBlank { stringResource(R.string.prestamo_programa_sin_registrar) },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Colores.textoSecundario
                                    )
                                }
                                TextButton(onClick = { estudianteId = null }) {
                                    Text(stringResource(R.string.comun_cambiar), color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            ContactoTelefono(telefono = estudianteSeleccionado.telefono)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.prestamo_verifica_carnet),
                        style = MaterialTheme.typography.labelSmall,
                        color = Colores.advertencia
                    )

                    // ALERTA VISUAL DE BLOQUEO (RF-12)
                    if (bloqueo != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        TarjetaApp(
                            colors = CardDefaults.cardColors(containerColor = Colores.errorFondo),
                            border = BorderStroke(1.5.dp, Colores.errorFuerte),
                            shape = formaApp(14.dp),
                            modifier = Modifier.fillMaxWidth().testTag("alerta_carnet_retenido")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = stringResource(R.string.prestamo_estudiante_bloqueado),
                                    tint = Colores.errorFuerte,
                                    modifier = Modifier.size(32.dp)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = bloqueo.titulo,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Colores.error,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = bloqueo.detalle,
                                        color = Colores.texto,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = stringResource(R.string.prestamo_bloqueado_detalle),
                                        color = Colores.salmon,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // SECCIÓN 2: SELECCIÓN DE ARTÍCULOS (MÚLTIPLES)
            // ==========================================
            SeccionCard(
                titulo = pluralStringResource(R.plurals.prestamo_articulos_titulo, articulosSeleccionados.size, articulosSeleccionados.size),
                subtitulo = stringResource(R.string.prestamo_articulos_ayuda),
                icono = Icons.Default.CheckCircle
            ) {
                // 1. Canasta de artículos seleccionados
                if (articulosSeleccionados.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.prestamo_asignados),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        articulosSeleccionados.forEach { item ->
                            Surface(
                                shape = formaApp(10.dp),
                                color = Colores.superficieAlta,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        ImagenArticulo(item.nombre, item.categoria, item.icono, tamano = 36.dp, tamanoEmoji = 20.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Surface(
                                                    shape = formaApp(4.dp),
                                                    color = Colores.rellenoEtiqueta(MaterialTheme.colorScheme.primary, 0.25f)
                                                ) {
                                                    Text(
                                                        text = item.id,
                                                        color = Colores.textoEtiqueta(MaterialTheme.colorScheme.primary),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Text(
                                                    text = item.nombre,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Colores.texto,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(
                                                text = if (item.esIncompleto) {
                                                    stringResource(
                                                        R.string.prestamo_se_entrega_incompleto,
                                                        item.observacion.ifBlank { stringResource(R.string.comun_sin_observacion_minuscula) }
                                                    )
                                                } else {
                                                    stringResource(R.string.inventario_categoria_de, Categorias.etiqueta(item.categoria))
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (item.esIncompleto) colorNovedad(EstadoItem.INCOMPLETO) else Colores.acentoTexto,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    if (item.esPorCantidad) {
                                        // Artículo por cantidad: se eligen cuántas piezas se entregan.
                                        val cantidad = cantidadDe(item)
                                        val disponibles = itemsInventario.find { it.id == item.id }?.cantidadDisponible
                                            ?: item.cantidadDisponible
                                        ContadorCantidad(
                                            valor = cantidad,
                                            onCambio = { cantidades[item.id] = it },
                                            minimo = 1,
                                            maximo = disponibles,
                                            tamanoBoton = 30.dp,
                                            etiquetaPrueba = "contador_${item.id}"
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            articulosSeleccionados.remove(item)
                                            cantidades.remove(item.id)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = stringResource(R.string.prestamo_quitar),
                                            tint = Colores.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                } else {
                    Surface(
                        shape = formaApp(10.dp),
                        color = Colores.superficie,
                        border = BorderStroke(1.dp, Colores.borde),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("📦", fontSize = 20.sp)
                            Text(
                                text = stringResource(R.string.prestamo_sin_articulos),
                                style = MaterialTheme.typography.bodySmall,
                                color = Colores.textoSecundario
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // 2. Buscador y Filtro de Categoría para agregar implementos
                Text(
                    text = stringResource(R.string.prestamo_seleccionar),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Colores.texto
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = busquedaItem,
                    onValueChange = { busquedaItem = it },
                    placeholder = { Text(stringResource(R.string.prestamo_buscar_placeholder)) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        if (busquedaItem.isNotBlank()) {
                            IconButton(onClick = { busquedaItem = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Colores.textoTenue)
                            }
                        }
                    },
                    singleLine = true,
                    shape = formaApp(12.dp),
                    colors = outlinedFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Desplegable de categoría
                Desplegable(
                    etiqueta = stringResource(R.string.comun_categoria),
                    opciones = categoriasDisponibles,
                    seleccion = categoriaFiltro,
                    onSeleccion = { categoriaFiltro = it },
                    colores = outlinedFieldColors(),
                    etiquetaPrueba = "categoria_prestamo",
                    textoOpcion = Categorias::etiqueta,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Desplegable de artículos: al elegir uno se agrega al préstamo
                if (itemsDisponibles.isEmpty()) {
                    Text(
                        text = stringResource(R.string.prestamo_sin_mas),
                        style = MaterialTheme.typography.labelSmall,
                        color = Colores.textoTenue,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                } else {
                    // Cada opción lleva el código, así que la etiqueta identifica al artículo
                    val opcionesArticulos = remember(itemsDisponibles, Idioma.actual) {
                        itemsDisponibles.associateBy { item ->
                            buildString {
                                append("${item.id} · ${item.nombre}")
                                when {
                                    item.esPorCantidad -> append(Idioma.plural(R.plurals.prestamo_opcion_disponibles, item.cantidadDisponible, item.cantidadDisponible))
                                    item.esIncompleto -> append(Idioma.texto(R.string.prestamo_opcion_incompleto))
                                }
                            }
                        }
                    }
                    Desplegable(
                        etiqueta = pluralStringResource(R.plurals.prestamo_agregar_articulo, itemsDisponibles.size, itemsDisponibles.size),
                        opciones = opcionesArticulos.keys.toList(),
                        seleccion = "",
                        onSeleccion = { etiqueta ->
                            opcionesArticulos[etiqueta]?.let { articulosSeleccionados.add(it) }
                        },
                        placeholder = stringResource(R.string.prestamo_elige_articulo),
                        colores = outlinedFieldColors(),
                        etiquetaPrueba = "articulo_prestamo",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ==========================================
            // SECCIÓN 3: DETALLES DEL PRÉSTAMO
            // ==========================================
            SeccionCard(
                titulo = stringResource(R.string.prestamo_detalles),
                subtitulo = stringResource(R.string.prestamo_detalles_subtitulo),
                icono = Icons.Default.DateRange
            ) {
                // Fecha límite de devolución (no puede superar el fin de semestre)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Colores.superficieAlta, formaApp(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.prestamo_fecha_limite),
                                style = MaterialTheme.typography.bodySmall,
                                color = Colores.textoTenue
                            )
                            Text(
                                text = when {
                                    !uiState.configuracionCargada -> stringResource(R.string.comun_cargando)
                                    !semestreVigente -> stringResource(R.string.prestamo_semestre_sin_configurar)
                                    else -> Fechas.corta(fechaLimite)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (uiState.configuracionCargada && !semestreVigente) Colores.error else Colores.texto,
                                modifier = Modifier.testTag("fecha_limite_texto")
                            )
                            if (semestreVigente) {
                                Text(
                                    text = stringResource(R.string.prestamo_fin_semestre, Fechas.corta(finSemestre)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Colores.acentoTexto
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (semestreVigente) mostrarDialogoFecha = true else onConfigurarSemestre()
                        },
                        shape = formaApp(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        // Relleno neutro: con el color del borde, en Retro y Solar el texto no se leía
                        colors = ButtonDefaults.buttonColors(containerColor = Colores.rellenoNeutro)
                    ) {
                        Text(if (semestreVigente) stringResource(R.string.comun_cambiar) else stringResource(R.string.comun_configurar), fontSize = 12.sp, color = Colores.texto)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Retención de Carnet / Documento Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Colores.superficieAlta, formaApp(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.prestamo_retencion),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Colores.texto
                        )
                        Text(
                            text = if (carnetRetenido) stringResource(R.string.prestamo_carnet_retenido_si) else stringResource(R.string.prestamo_carnet_debes_retener),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (carnetRetenido) MaterialTheme.colorScheme.primary else Colores.error
                        )
                    }

                    Switch(
                        checked = carnetRetenido,
                        onCheckedChange = { carnetRetenido = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Colores.sobrePrimario,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = Colores.textoSecundario,
                            uncheckedTrackColor = Colores.deshabilitado
                        ),
                        modifier = Modifier.testTag("carnet_retenido_switch")
                    )
                }
            }

            // ==========================================
            // SECCIÓN 4: VALIDACIÓN Y CONFIRMACIÓN
            // ==========================================
            SeccionCard(
                titulo = stringResource(R.string.prestamo_validacion),
                subtitulo = stringResource(R.string.prestamo_compromiso_subtitulo),
                icono = Icons.Default.Check
            ) {
                // Nota legal con Checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(formaApp(10.dp))
                        .clickable { compromisoAceptado = !compromisoAceptado }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = compromisoAceptado,
                        onCheckedChange = { compromisoAceptado = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            checkmarkColor = Colores.sobrePrimario,
                            uncheckedColor = Colores.textoTenue
                        ),
                        modifier = Modifier.testTag("compromiso_checkbox")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.prestamo_acepto),
                        style = MaterialTheme.typography.bodySmall,
                        color = Colores.textoSecundario
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Recuadro interactivo para dibujo de Firma Digital
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.prestamo_firma),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Colores.acentoTexto
                    )

                    if (trazosFirma.isNotEmpty() || trazoActual.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                trazosFirma.clear()
                                trazoActual = emptyList()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(stringResource(R.string.prestamo_limpiar_firma), color = Colores.error, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(Colores.superficie, formaApp(12.dp))
                        .border(1.dp, Colores.borde, formaApp(12.dp))
                        .clip(formaApp(12.dp))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    trazoActual = listOf(offset)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    trazoActual = trazoActual + change.position
                                },
                                onDragEnd = {
                                    if (trazoActual.isNotEmpty()) {
                                        trazosFirma.add(trazoActual)
                                        trazoActual = emptyList()
                                    }
                                },
                                onDragCancel = {
                                    trazoActual = emptyList()
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        for (trazo in trazosFirma) {
                            if (trazo.size > 1) {
                                val path = Path().apply {
                                    moveTo(trazo.first().x, trazo.first().y)
                                    for (i in 1 until trazo.size) {
                                        lineTo(trazo[i].x, trazo[i].y)
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = Colores.primario,
                                    style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                            }
                        }
                        if (trazoActual.size > 1) {
                            val path = Path().apply {
                                moveTo(trazoActual.first().x, trazoActual.first().y)
                                for (i in 1 until trazoActual.size) {
                                    lineTo(trazoActual[i].x, trazoActual[i].y)
                                }
                            }
                            drawPath(
                                path = path,
                                color = Colores.primario,
                                style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                    }

                    if (trazosFirma.isEmpty() && trazoActual.isEmpty()) {
                        Text(
                            text = Idioma.texto(R.string.prestamo_firma_aqui),
                            style = MaterialTheme.typography.bodySmall,
                            color = Colores.textoTenue,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ==========================================
            // BOTÓN DE ACCIÓN PRINCIPAL
            // ==========================================
            val puedeRegistrar = estudianteSeleccionado != null &&
                !estudianteBloqueado &&
                articulosSeleccionados.isNotEmpty() &&
                semestreVigente &&
                fechaLimite != null &&
                carnetRetenido &&
                compromisoAceptado

            Button(
                onClick = { mostrarConfirmacionDialog = true },
                enabled = puedeRegistrar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("boton_registrar_prestamo"),
                shape = formaApp(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (puedeRegistrar) MaterialTheme.colorScheme.primary else Colores.deshabilitado,
                    disabledContainerColor = Colores.deshabilitado,
                    disabledContentColor = Colores.textoTenue
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Icon(
                    imageVector = if (estudianteBloqueado) Icons.Default.Warning else Icons.Default.Check,
                    contentDescription = null,
                    tint = if (puedeRegistrar) Colores.sobrePrimario else Colores.textoTenue
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        estudianteSeleccionado == null -> stringResource(R.string.prestamo_boton_selecciona_estudiante)
                        estudianteBloqueado -> stringResource(R.string.prestamo_boton_bloqueado)
                        articulosSeleccionados.isEmpty() -> stringResource(R.string.prestamo_boton_selecciona_articulo)
                        !semestreVigente || fechaLimite == null -> stringResource(R.string.prestamo_boton_configura_semestre)
                        !carnetRetenido -> stringResource(R.string.prestamo_boton_confirma_carnet)
                        !compromisoAceptado -> stringResource(R.string.prestamo_boton_acepta)
                        else -> pluralStringResource(R.plurals.prestamo_boton_registrar, totalPiezas, totalPiezas)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (puedeRegistrar) Colores.sobrePrimario else Colores.textoTenue
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Selector de la fecha límite: entre hoy y el fin de semestre
    if (mostrarDialogoFecha && finSemestre != null) {
        SelectorFechaDialog(
            fechaInicial = fechaLimite,
            fechaMinima = Date(),
            fechaMaxima = finSemestre,
            onConfirmar = { fecha ->
                fechaLimiteElegida = fecha
                mostrarDialogoFecha = false
            },
            onDismiss = { mostrarDialogoFecha = false }
        )
    }

    // Diálogo de confirmación del Préstamo
    if (mostrarConfirmacionDialog) {
        DialogoApp(
            onDismissRequest = { if (!guardandoPrestamo) mostrarConfirmacionDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.prestamo_confirmar_titulo),
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
                        text = stringResource(R.string.prestamo_resumen),
                        fontWeight = FontWeight.SemiBold,
                        color = Colores.acentoTexto
                    )
                    Text(
                        text = stringResource(R.string.prestamo_resumen_estudiante, estudianteSeleccionado?.nombre.orEmpty(), estudianteSeleccionado?.codigo.orEmpty()),
                        style = MaterialTheme.typography.bodySmall,
                        color = Colores.textoSecundario
                    )
                    Text(
                        text = stringResource(R.string.prestamo_resumen_articulos, totalPiezas),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Colores.texto
                    )
                    articulosSeleccionados.forEach { item ->
                        val sufijo = when {
                            item.esPorCantidad -> " x${cantidadDe(item)}"
                            item.esIncompleto -> stringResource(
                                R.string.prestamo_resumen_incompleto,
                                item.observacion.ifBlank { stringResource(R.string.comun_sin_observacion_minuscula) }
                            )
                            else -> ""
                        }
                        Text(
                            text = "   - [${item.id}] ${item.nombre}$sufijo",
                            style = MaterialTheme.typography.labelSmall,
                            color = Colores.acentoTexto
                        )
                    }
                    Text(
                        text = stringResource(R.string.prestamo_resumen_carnet),
                        style = MaterialTheme.typography.bodySmall,
                        color = Colores.textoSecundario
                    )
                    Text(
                        text = stringResource(R.string.prestamo_resumen_fecha, Fechas.corta(fechaLimite)),
                        style = MaterialTheme.typography.bodySmall,
                        color = Colores.textoSecundario
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.prestamo_resumen_nota),
                        style = MaterialTheme.typography.labelSmall,
                        color = Colores.acentoTexto
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val estudiante = estudianteSeleccionado
                        val limite = fechaLimite
                        if (estudiante != null && limite != null) {
                            viewModel.registrarPrestamo(
                                estudiante = estudiante,
                                seleccion = articulosSeleccionados.map { SeleccionPrestamo(it, cantidadDe(it)) },
                                fechaLimite = limite,
                                observaciones = ""
                            ) { resultado ->
                                mostrarConfirmacionDialog = false
                                resultado.onSuccess { mensaje ->
                                    Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
                                    onVolver()
                                }.onFailure { error ->
                                    Toast.makeText(
                                        context,
                                        error.localizedMessage ?: Idioma.texto(R.string.prestamo_error_registrar),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    },
                    enabled = !guardandoPrestamo,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = formaBoton()
                ) {
                    if (guardandoPrestamo) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Colores.sobrePrimario, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Idioma.texto(R.string.comun_guardando), color = Colores.sobrePrimario, fontWeight = FontWeight.Bold)
                    } else {
                        Text(Idioma.texto(R.string.prestamo_confirmar_guardar), color = Colores.sobrePrimario, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { mostrarConfirmacionDialog = false },
                    enabled = !guardandoPrestamo,
                    shape = formaBoton()
                ) {
                    Text(stringResource(R.string.comun_cancelar), color = Colores.textoSecundario)
                }
            },
            containerColor = Colores.dialogo
        )
    }
}

@Composable
fun SeccionCard(
    titulo: String,
    subtitulo: String,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    TarjetaElevadaApp(
        modifier = Modifier.fillMaxWidth(),
        shape = formaApp(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Colores.superficie
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        // En neón, cada sección es un panel HUD con el borde que respira
        estiloNeon = EstiloNeon.TARJETA
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (Colores.esNeon) {
                    MarcoIconoNeon(icono = rememberVectorPainter(icono), tono = TonoNeon.CIAN, tamano = 40.dp, tamanoIcono = 22.dp)
                } else {
                    Surface(
                        modifier = Modifier.size(38.dp),
                        shape = formaApp(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    ) {
                        Icon(
                            imageVector = icono,
                            contentDescription = null,
                            modifier = Modifier.padding(8.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Colores.texto
                    )
                    Text(
                        text = subtitulo,
                        style = MaterialTheme.typography.bodySmall,
                        color = Colores.textoTenue
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            content()
        }
    }
}

@Composable
fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = Colores.bordeCampo,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = Colores.textoSecundario,
    focusedTextColor = Colores.texto,
    unfocusedTextColor = Colores.texto,
    focusedContainerColor = Colores.superficieAlta,
    unfocusedContainerColor = Colores.superficieAlta,
    cursorColor = MaterialTheme.colorScheme.primary
)
