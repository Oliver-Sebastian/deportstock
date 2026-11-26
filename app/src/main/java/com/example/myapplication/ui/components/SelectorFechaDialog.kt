package com.example.myapplication.ui.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.example.myapplication.R
import com.example.myapplication.utils.Fechas
import java.util.Date

/**
 * Selector de fecha de Material 3 limitado a un rango.
 * Devuelve el final del día elegido (23:59:59 hora local), de modo que una fecha
 * límite cubra el día completo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorFechaDialog(
    fechaInicial: Date?,
    fechaMinima: Date? = null,
    fechaMaxima: Date? = null,
    onConfirmar: (Date) -> Unit,
    onDismiss: () -> Unit
) {
    val minimoUtc = remember(fechaMinima) { fechaMinima?.let { Fechas.utcMedianocheDesdeLocal(it) } }
    val maximoUtc = remember(fechaMaxima) { fechaMaxima?.let { Fechas.utcMedianocheDesdeLocal(it) } }

    // Con el teléfono girado el calendario no cabe en la pantalla: se pasa al modo de escribir
    // la fecha (el ícono de calendario del encabezado vuelve al modo de calendario).
    val compacta = alturaCompacta()
    val estado = rememberDatePickerState(
        initialSelectedDateMillis = fechaInicial?.let { Fechas.utcMedianocheDesdeLocal(it) },
        initialDisplayMode = if (compacta) DisplayMode.Input else DisplayMode.Picker,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                (minimoUtc == null || utcTimeMillis >= minimoUtc) &&
                    (maximoUtc == null || utcTimeMillis <= maximoUtc)
        }
    )
    LaunchedEffect(compacta) {
        if (compacta) estado.displayMode = DisplayMode.Input
    }

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    estado.selectedDateMillis?.let { onConfirmar(Fechas.finDelDiaLocalDesdeUtc(it)) }
                },
                enabled = estado.selectedDateMillis != null
            ) {
                Text(stringResource(R.string.comun_aceptar))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.comun_cancelar))
            }
        }
    ) {
        DatePicker(state = estado)
    }
}
