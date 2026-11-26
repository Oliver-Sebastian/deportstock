package com.example.myapplication.ui.screens.inventario

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.data.EstadoItem
import com.example.myapplication.data.ItemDeportivo
import com.example.myapplication.ui.components.ContadorCantidad
import com.example.myapplication.ui.components.DialogoApp
import com.example.myapplication.ui.components.ImagenArticulo
import com.example.myapplication.ui.components.TonoNeon
import com.example.myapplication.ui.theme.Colores
import com.example.myapplication.ui.theme.formaApp
import com.example.myapplication.ui.theme.formaBoton
import com.example.myapplication.utils.Idioma

/**
 * RF-07: registra una novedad de un artículo (dañado, incompleto o extraviado) con una
 * observación obligatoria, y permite devolverlo a disponible cuando se resuelve.
 *
 * - Unidad: la novedad cambia el estado de la unidad. "Incompleto" no aparece para
 *   balones ni petos, que son de una sola pieza (ver [EstadoItem.admiteIncompleto]).
 * - Cantidad: se indica cuántas piezas quedan dañadas o extraviadas
 *   ("incompleto" no aplica a piezas sueltas).
 */
@Composable
fun NovedadItemDialog(
    item: ItemDeportivo,
    onDismiss: () -> Unit,
    onMarcarNovedad: (tipo: String, cantidad: Int, observacion: String) -> Unit,
    onResolver: (tipo: String, cantidad: Int) -> Unit,
    onDescontarExtraviadas: () -> Unit
) {
    val tipos = if (item.esPorCantidad) {
        listOf(EstadoItem.DANADO, EstadoItem.EXTRAVIADO)
    } else {
        EstadoItem.NOVEDADES.filter { it != EstadoItem.INCOMPLETO || EstadoItem.admiteIncompleto(item.categoria) }
    }
    var tipo by remember(item.id) { mutableStateOf(EstadoItem.DANADO) }
    var observacion by remember(item.id) { mutableStateOf("") }
    var cantidad by remember(item.id) { mutableIntStateOf(1) }

    val unidadPrestada = !item.esPorCantidad && item.estado.equals(EstadoItem.PRESTADO, ignoreCase = true)
    val puedeRegistrar = if (item.esPorCantidad) item.cantidadDisponible > 0 else !unidadPrestada

    DialogoApp(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Colores.naranja,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.novedad_titulo, item.id),
                fontWeight = FontWeight.Bold,
                color = Colores.texto
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ImagenArticulo(item.nombre, item.categoria, item.icono, tamano = 32.dp, tamanoEmoji = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.nombre,
                        fontWeight = FontWeight.Bold,
                        color = Colores.texto
                    )
                }

                // Novedad registrada actualmente y cómo resolverla
                if (item.tieneNovedad) {
                    Text(
                        text = stringResource(
                            R.string.novedad_registrado,
                            item.resumenNovedad,
                            item.observacion.ifBlank { stringResource(R.string.comun_sin_observacion) }
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = Colores.salmon
                    )
                    AccionesResolver(item, onResolver, onDescontarExtraviadas)
                    HorizontalDivider(color = Colores.borde)
                }

                if (unidadPrestada) {
                    Text(
                        text = stringResource(R.string.novedad_unidad_prestada),
                        style = MaterialTheme.typography.bodySmall,
                        color = Colores.textoSecundario
                    )
                }

                if (puedeRegistrar) {
                    Text(
                        text = stringResource(R.string.novedad_registrar),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Colores.acentoTexto
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        tipos.forEach { opcion ->
                            FilterChip(
                                selected = tipo == opcion,
                                onClick = { tipo = opcion },
                                label = {
                                    Text(
                                        text = if (item.esPorCantidad) etiquetaPiezas(opcion) else EstadoItem.etiqueta(opcion),
                                        fontSize = 12.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Colores.rellenoEtiqueta(colorNovedad(opcion), 0.25f),
                                    selectedLabelColor = Colores.textoEtiqueta(colorNovedad(opcion)),
                                    containerColor = Colores.superficie,
                                    labelColor = Colores.textoSecundario
                                ),
                                modifier = Modifier.testTag("tipo_novedad_$opcion")
                            )
                        }
                    }

                    if (item.esPorCantidad) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = pluralStringResource(R.plurals.novedad_piezas_de, item.cantidadDisponible, item.cantidadDisponible),
                                style = MaterialTheme.typography.bodySmall,
                                color = Colores.textoSecundario,
                                modifier = Modifier.weight(1f)
                            )
                            ContadorCantidad(
                                valor = cantidad,
                                onCambio = { cantidad = it },
                                minimo = 1,
                                maximo = item.cantidadDisponible,
                                colorValor = colorNovedad(tipo),
                                tamanoBoton = 32.dp,
                                etiquetaPrueba = "contador_piezas_novedad"
                            )
                        }
                    }

                    OutlinedTextField(
                        value = observacion,
                        onValueChange = { if (it.length <= 150) observacion = it },
                        label = { Text(stringResource(R.string.novedad_observacion)) },
                        placeholder = { Text(ejemploObservacion(tipo)) },
                        colors = dialogTextFieldColors(),
                        shape = formaApp(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_observacion_novedad")
                    )
                }
            }
        },
        confirmButton = {
            if (puedeRegistrar) {
                Button(
                    onClick = { onMarcarNovedad(tipo, cantidad, observacion) },
                    enabled = observacion.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Colores.errorBoton),
                    modifier = Modifier.testTag("confirmar_novedad_button"),
                    shape = formaBoton()
                ) {
                    Text(
                        text = stringResource(
                            R.string.novedad_marcar,
                            if (item.esPorCantidad) etiquetaPiezas(tipo).lowercase() else EstadoItem.etiqueta(tipo).lowercase()
                        ),
                        color = Colores.sobreError,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.comun_cerrar), color = Colores.textoSecundario)
            }
        },
        tonoNeon = TonoNeon.AMARILLO,
        containerColor = Colores.dialogo,
        shape = formaApp(18.dp)
    )
}

/** Botones para dejar disponible de nuevo lo que tenía novedad. */
@Composable
private fun AccionesResolver(
    item: ItemDeportivo,
    onResolver: (tipo: String, cantidad: Int) -> Unit,
    onDescontarExtraviadas: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (!item.esPorCantidad) {
            val texto = when {
                item.estado.equals(EstadoItem.EXTRAVIADO, ignoreCase = true) -> stringResource(R.string.novedad_resuelta_encontrado)
                item.estado.equals(EstadoItem.INCOMPLETO, ignoreCase = true) -> stringResource(R.string.novedad_resuelta_completo)
                else -> stringResource(R.string.novedad_resuelta_reparado)
            }
            OutlinedButton(
                onClick = { onResolver(item.estado, 1) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("resolver_novedad_button"),
                    shape = formaBoton()
            ) {
                Text(texto, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
            }
            if (item.estado.equals(EstadoItem.EXTRAVIADO, ignoreCase = true)) {
                Text(
                    text = stringResource(R.string.novedad_si_no_aparece),
                    style = MaterialTheme.typography.labelSmall,
                    color = Colores.textoTenue
                )
            }
        } else {
            if (item.cantidadDanada > 0) {
                OutlinedButton(
                    onClick = { onResolver(EstadoItem.DANADO, item.cantidadDanada) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("resolver_danadas_button"),
                        shape = formaBoton()
                ) {
                    Text(pluralStringResource(R.plurals.novedad_reparadas, item.cantidadDanada, item.cantidadDanada), color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                }
            }
            if (item.cantidadExtraviada > 0) {
                OutlinedButton(
                    onClick = { onResolver(EstadoItem.EXTRAVIADO, item.cantidadExtraviada) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("resolver_extraviadas_button"),
                        shape = formaBoton()
                ) {
                    Text(pluralStringResource(R.plurals.novedad_encontradas, item.cantidadExtraviada, item.cantidadExtraviada), color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = onDescontarExtraviadas,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("descontar_extraviadas_button"),
                        shape = formaBoton()
                ) {
                    Text(stringResource(R.string.novedad_descontar), color = Colores.error, fontSize = 13.sp)
                }
            }
        }
    }
}

private fun etiquetaPiezas(tipo: String): String =
    if (tipo == EstadoItem.EXTRAVIADO) Idioma.texto(R.string.novedad_piezas_extraviadas) else Idioma.texto(R.string.novedad_piezas_danadas)

private fun ejemploObservacion(tipo: String): String = when (tipo) {
    EstadoItem.INCOMPLETO -> Idioma.texto(R.string.novedad_ejemplo_incompleto)
    EstadoItem.EXTRAVIADO -> Idioma.texto(R.string.novedad_ejemplo_extraviado)
    else -> Idioma.texto(R.string.novedad_ejemplo_danado)
}

/** Color que identifica cada estado de novedad en el inventario y la devolución. */
fun colorNovedad(tipo: String): Color = when (tipo.uppercase()) {
    EstadoItem.INCOMPLETO -> Colores.incompleto
    EstadoItem.EXTRAVIADO -> Colores.extraviado
    else -> Colores.errorFuerte
}
