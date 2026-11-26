package com.example.myapplication.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.ui.theme.formaApp
import com.example.myapplication.utils.Idioma
import com.example.myapplication.utils.Telefonos

/**
 * Campo de celular con el indicativo +57 fijo: solo acepta dígitos (máximo 10) y abre el
 * teclado numérico. Debajo explica para qué se pide o qué le falta al número.
 */
@Composable
fun CampoTelefono(
    valor: String,
    onValorChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    etiqueta: String = Idioma.texto(R.string.comun_celular),
    etiquetaPrueba: String = "telefono_input"
) {
    val error = if (valor.isNotEmpty()) Telefonos.error(valor) else null
    OutlinedTextField(
        value = valor,
        onValueChange = { entrada ->
            val digitos = entrada.filter { it.isDigit() }
            if (digitos.length <= Telefonos.DIGITOS) onValorChange(digitos)
        },
        label = { Text(etiqueta) },
        prefix = { Text("+57 ") },
        singleLine = true,
        isError = error != null,
        supportingText = {
            Text(error ?: stringResource(R.string.telefono_ayuda))
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        shape = formaApp(12.dp),
        modifier = modifier.testTag(etiquetaPrueba)
    )
}
