package com.example.myapplication.ui.screens.horario

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.data.DiasSemana
import com.example.myapplication.data.Horario
import com.example.myapplication.ui.components.DialogoApp
import com.example.myapplication.ui.components.SelectorFechaDialog
import com.example.myapplication.ui.screens.prestamo.SeccionCard
import com.example.myapplication.ui.theme.formaApp
import com.example.myapplication.ui.theme.formaBoton
import com.example.myapplication.utils.Fechas
import com.example.myapplication.utils.Idioma
import com.example.myapplication.viewmodel.HorarioViewModel
import java.util.Date
import com.example.myapplication.ui.theme.Colores

/**
 * Pantalla del monitor para gestionar el horario de atención de Bienestar (RF-14)
 * y la fecha de fin de semestre que limita los préstamos (regla de negocio 2).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HorarioAdminScreen(
    onVolver: () -> Unit,
    viewModel: HorarioViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var mostrarSelectorSemestre by remember { mutableStateOf(false) }
    var mostrarDialogoFranja by remember { mutableStateOf(false) }
    var franjaAEliminar by remember { mutableStateOf<Horario?>(null) }

    LaunchedEffect(uiState.mensaje) {
        uiState.mensaje?.let { mensaje ->
            snackbarHostState.showSnackbar(mensaje)
            viewModel.limpiarMensaje()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.comun_horario_semestre),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Colores.sobreBarra
                        )
                        Text(
                            text = stringResource(R.string.horario_subtitulo),
                            style = MaterialTheme.typography.bodySmall,
                            color = Colores.sobreBarraSecundario
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onVolver,
                        modifier = Modifier.testTag("volver_horario_button")
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==========================================
            // FIN DE SEMESTRE
            // ==========================================
            SeccionCard(
                titulo = stringResource(R.string.horario_fin_semestre),
                subtitulo = stringResource(R.string.horario_fin_semestre_ayuda),
                icono = Icons.Default.DateRange
            ) {
                val finSemestre = uiState.fechaFinSemestre
                val vencido = finSemestre != null && finSemestre.before(Date())
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Colores.superficieAlta, formaApp(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (finSemestre == null) stringResource(R.string.horario_sin_configurar) else Fechas.corta(finSemestre),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (finSemestre == null || vencido) Colores.error else Colores.texto,
                            modifier = Modifier.testTag("fin_semestre_texto")
                        )
                        Text(
                            text = when {
                                finSemestre == null -> stringResource(R.string.horario_sin_fecha)
                                vencido -> stringResource(R.string.horario_fecha_pasada)
                                else -> stringResource(R.string.horario_fecha_defecto)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Colores.acentoTexto
                        )
                    }
                    Button(
                        onClick = { mostrarSelectorSemestre = true },
                        shape = formaApp(8.dp),
                        // Relleno neutro: con el color del borde, en Retro y Solar el texto no se leía
                        colors = ButtonDefaults.buttonColors(containerColor = Colores.rellenoNeutro),
                        modifier = Modifier.testTag("cambiar_fin_semestre_button")
                    ) {
                        Text(if (finSemestre == null) stringResource(R.string.horario_definir) else stringResource(R.string.comun_cambiar), fontSize = 12.sp, color = Colores.texto)
                    }
                }
            }

            // ==========================================
            // HORARIO DE ATENCIÓN
            // ==========================================
            SeccionCard(
                titulo = stringResource(R.string.comun_horario_atencion),
                subtitulo = stringResource(R.string.horario_visible_todos),
                icono = Icons.Default.Info
            ) {
                if (uiState.cargando) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    HorarioLista(
                        horarios = uiState.horarios,
                        onEliminar = { franjaAEliminar = it }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { mostrarDialogoFranja = true },
                    shape = formaApp(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("agregar_franja_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Colores.sobrePrimario)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.horario_agregar_franja), color = Colores.sobrePrimario, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (mostrarSelectorSemestre) {
        SelectorFechaDialog(
            fechaInicial = uiState.fechaFinSemestre,
            fechaMinima = Date(),
            onConfirmar = { fecha ->
                viewModel.guardarFinSemestre(fecha)
                mostrarSelectorSemestre = false
            },
            onDismiss = { mostrarSelectorSemestre = false }
        )
    }

    if (mostrarDialogoFranja) {
        FranjaDialog(
            onDismiss = { mostrarDialogoFranja = false },
            onGuardar = { franja -> viewModel.guardarFranja(franja) }
        )
    }

    franjaAEliminar?.let { franja ->
        DialogoApp(
            onDismissRequest = { franjaAEliminar = null },
            title = { Text(stringResource(R.string.horario_eliminar_franja_pregunta), fontWeight = FontWeight.Bold, color = Colores.texto) },
            text = {
                Text(
                    text = stringResource(R.string.horario_franja_detalle, DiasSemana.nombre(franja.diaSemana), Fechas.hora(franja.horaInicio), Fechas.hora(franja.horaFin)),
                    color = Colores.textoSecundario
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.eliminarFranja(franja.id)
                        franjaAEliminar = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Colores.errorBoton),
                    shape = formaBoton()
                ) {
                    Text(stringResource(R.string.comun_eliminar), color = Colores.sobreError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { franjaAEliminar = null }, shape = formaBoton()) {
                    Text(stringResource(R.string.comun_cancelar), color = Colores.textoSecundario)
                }
            },
            containerColor = Colores.dialogo,
            shape = formaApp(18.dp)
        )
    }
}

/**
 * Horario agrupado por día. La usan el monitor (con [onEliminar]) y el estudiante (solo lectura).
 */
@Composable
fun HorarioLista(
    horarios: List<Horario>,
    modifier: Modifier = Modifier,
    onEliminar: ((Horario) -> Unit)? = null
) {
    if (horarios.isEmpty()) {
        Text(
            text = Idioma.texto(R.string.horario_sin_publicar),
            style = MaterialTheme.typography.bodySmall,
            color = Colores.textoTenue,
            modifier = modifier
        )
        return
    }

    val porDia = horarios.groupBy { it.diaSemana }.toList().sortedBy { it.first }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        porDia.forEach { (dia, franjas) ->
            Surface(
                shape = formaApp(12.dp),
                color = Colores.superficieAlta,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("horario_dia_$dia")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = DiasSemana.nombre(dia),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Colores.texto,
                        modifier = Modifier.width(96.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        franjas.forEach { franja ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${Fechas.hora(franja.horaInicio)} – ${Fechas.hora(franja.horaFin)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Colores.acentoTexto,
                                    modifier = Modifier.weight(1f)
                                )
                                if (onEliminar != null) {
                                    IconButton(
                                        onClick = { onEliminar(franja) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = Idioma.texto(R.string.horario_eliminar_franja),
                                            tint = Colores.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Diálogo para crear una franja: día de la semana, hora de apertura y hora de cierre. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FranjaDialog(
    onDismiss: () -> Unit,
    onGuardar: (Horario) -> Boolean
) {
    var dia by remember { mutableIntStateOf(1) }
    // Formato de 12 horas (con AM y PM); la hora se guarda igual en 24 horas ("14:30")
    val apertura = rememberTimePickerState(initialHour = 8, initialMinute = 0, is24Hour = false)
    val cierre = rememberTimePickerState(initialHour = 12, initialMinute = 0, is24Hour = false)

    DialogoApp(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.horario_nueva_franja), fontWeight = FontWeight.Bold, color = Colores.texto) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(stringResource(R.string.horario_dia), style = MaterialTheme.typography.bodySmall, color = Colores.acentoTexto)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Lunes a sábado: el domingo no se ofrece
                    DiasSemana.DIAS_ATENCION.forEach { numero ->
                        FilterChip(
                            selected = dia == numero,
                            onClick = { dia = numero },
                            label = { Text(DiasSemana.nombre(numero).take(3), fontSize = 12.sp) },
                            modifier = Modifier.testTag("dia_chip_$numero")
                        )
                    }
                }

                Text(stringResource(R.string.horario_apertura), style = MaterialTheme.typography.bodySmall, color = Colores.acentoTexto)
                TimeInput(state = apertura)

                Text(stringResource(R.string.horario_cierre), style = MaterialTheme.typography.bodySmall, color = Colores.acentoTexto)
                TimeInput(state = cierre)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val franja = Horario(
                        diaSemana = dia,
                        horaInicio = "%02d:%02d".format(apertura.hour, apertura.minute),
                        horaFin = "%02d:%02d".format(cierre.hour, cierre.minute)
                    )
                    if (onGuardar(franja)) {
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("guardar_franja_button"),
                shape = formaBoton()
            ) {
                Text(stringResource(R.string.comun_guardar), color = Colores.sobrePrimario, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = formaBoton()) {
                Text(stringResource(R.string.comun_cancelar), color = Colores.textoSecundario)
            }
        },
        containerColor = Colores.dialogo,
        shape = formaApp(18.dp)
    )
}
