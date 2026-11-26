package com.example.myapplication.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.ui.theme.Colores
import com.example.myapplication.ui.theme.formaApp
import com.example.myapplication.utils.Idioma
import com.example.myapplication.utils.Telefonos

/**
 * Celular del estudiante con botones para llamarlo o escribirle por WhatsApp.
 * Llamar solo abre el marcador con el número escrito (no hace la llamada), así que no
 * necesita el permiso de llamadas. Si el estudiante no registró celular, lo dice.
 */
@Composable
fun ContactoTelefono(
    telefono: String,
    modifier: Modifier = Modifier
) {
    val contexto = LocalContext.current

    if (!Telefonos.esValido(telefono)) {
        Text(
            text = stringResource(R.string.contacto_sin_telefono),
            style = MaterialTheme.typography.labelSmall,
            color = Colores.advertencia,
            modifier = modifier.testTag("sin_telefono")
        )
        return
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("contacto_telefono")
    ) {
        Icon(
            imageVector = Icons.Default.Phone,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = Telefonos.formatear(telefono),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Colores.texto,
            modifier = Modifier.weight(1f)
        )
        BotonContacto(
            icono = rememberVectorPainter(Icons.Default.Phone),
            texto = stringResource(R.string.contacto_llamar),
            etiquetaPrueba = "llamar_button"
        ) {
            abrir(contexto, Intent(Intent.ACTION_DIAL, Uri.parse(Telefonos.uriLlamada(telefono))), Idioma.texto(R.string.contacto_sin_app_llamar))
        }
        if (Telefonos.esCelular(telefono)) {
            BotonContacto(
                icono = painterResource(R.drawable.ic_chat),
                texto = "WhatsApp",
                etiquetaPrueba = "whatsapp_button"
            ) {
                abrir(contexto, Intent(Intent.ACTION_VIEW, Uri.parse(Telefonos.enlaceWhatsApp(telefono))), Idioma.texto(R.string.contacto_error_whatsapp))
            }
        }
    }
}

@Composable
private fun BotonContacto(icono: Painter, texto: String, etiquetaPrueba: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = formaApp(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
        modifier = Modifier
            .height(32.dp)
            .testTag(etiquetaPrueba)
    ) {
        Icon(painter = icono, contentDescription = null, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(texto, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

private fun abrir(contexto: Context, intent: Intent, mensajeSiFalla: String) {
    try {
        contexto.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(contexto, mensajeSiFalla, Toast.LENGTH_SHORT).show()
    }
}
