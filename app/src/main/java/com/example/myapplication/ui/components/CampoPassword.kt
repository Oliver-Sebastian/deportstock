package com.example.myapplication.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.ui.theme.formaApp
import com.example.myapplication.utils.Idioma

/**
 * Campo de contraseña con un botón de ojo a la derecha para mostrarla u ocultarla.
 *
 * - Oculta: muestra puntos y el ícono de ojo abierto ("Mostrar contraseña").
 * - Visible: muestra el texto y el ícono de ojo tachado ("Ocultar contraseña").
 *
 * El botón solo cambia la visibilidad: no inicia sesión ni registra, y conserva el foco
 * y el texto escrito. Cada campo recuerda su propio estado, también al girar la pantalla.
 */
@Composable
fun CampoPassword(
    valor: String,
    onValorChange: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    esError: Boolean = false,
    textoApoyo: (@Composable () -> Unit)? = null,
    etiquetaPrueba: String = "password_input"
) {
    var visible by rememberSaveable { mutableStateOf(false) }

    OutlinedTextField(
        value = valor,
        onValueChange = onValorChange,
        label = { Text(etiqueta) },
        singleLine = true,
        isError = esError,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        // Teclado de contraseña: sin autocorrección ni sugerencias que guarden la clave
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(
                onClick = { visible = !visible },
                modifier = Modifier.testTag("${etiquetaPrueba}_visibilidad")
            ) {
                Icon(
                    painter = painterResource(
                        id = if (visible) R.drawable.ic_visibility_off else R.drawable.ic_visibility
                    ),
                    contentDescription = if (visible) Idioma.texto(R.string.contrasena_ocultar) else Idioma.texto(R.string.contrasena_mostrar),
                    // Gris claro sobre el fondo oscuro del campo; rojo del tema si el campo tiene error
                    tint = if (esError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        supportingText = textoApoyo,
        shape = formaApp(12.dp),
        modifier = modifier.testTag(etiquetaPrueba)
    )
}
