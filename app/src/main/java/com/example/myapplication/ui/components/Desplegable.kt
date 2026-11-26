package com.example.myapplication.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.formaApp

/**
 * Lista desplegable de solo lectura (Material 3). Se usa en el registro (programa académico)
 * y en los filtros del catálogo del estudiante. [textoOpcion] da el texto que se ve de cada
 * opción: así una categoría o un programa guardado en español se muestra en el idioma de la app,
 * y [onSeleccion] sigue recibiendo el valor guardado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Desplegable(
    etiqueta: String,
    opciones: List<String>,
    seleccion: String,
    onSeleccion: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    esError: Boolean = false,
    colores: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    etiquetaPrueba: String = "desplegable",
    textoOpcion: (String) -> String = { it }
) {
    var expandido by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = { expandido = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = textoOpcion(seleccion),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            isError = esError,
            label = { Text(etiqueta) },
            placeholder = placeholder?.let { texto -> { Text(texto) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            shape = formaApp(12.dp),
            colors = colores,
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .testTag(etiquetaPrueba)
        )
        ExposedDropdownMenu(
            expanded = expandido,
            onDismissRequest = { expandido = false }
        ) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(textoOpcion(opcion)) },
                    onClick = {
                        onSeleccion(opcion)
                        expandido = false
                    },
                    modifier = Modifier.testTag("${etiquetaPrueba}_opcion_$opcion")
                )
            }
        }
    }
}
