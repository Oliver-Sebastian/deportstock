package com.example.myapplication.ui.screens.ajustes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.data.TemaApp
import com.example.myapplication.ui.components.MiniaturaTema
import com.example.myapplication.ui.components.PanelNeon
import com.example.myapplication.ui.theme.Colores
import com.example.myapplication.ui.theme.CortesNeon
import com.example.myapplication.ui.theme.Neon
import com.example.myapplication.viewmodel.AjustesViewModel

/**
 * "Ver más diseños": todos los temas de la app en una lista, cada uno con su vista previa.
 * Los diseños nuevos se agregan en [TemaApp] y aparecen aquí solos. Al tocar uno se aplica
 * al instante y queda guardado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AparienciaScreen(
    onVolver: () -> Unit,
    viewModel: AjustesViewModel = viewModel()
) {
    // Colores.tema es el tema aplicado en la app (se lee como estado: al cambiarlo, la lista se actualiza).
    val temaActual = Colores.tema

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.apariencia_titulo),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Colores.sobreBarra
                        )
                        Text(
                            text = stringResource(R.string.apariencia_subtitulo),
                            style = MaterialTheme.typography.bodySmall,
                            color = Colores.sobreBarraSecundario
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVolver, modifier = Modifier.testTag("volver_apariencia_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.comun_volver),
                            tint = Colores.iconoBarra
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Colores.barra)
            )
        },
        containerColor = Colores.fondoPantalla
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .selectableGroup()
                .testTag("lista_disenos")
        ) {
            items(TemaApp.entries, key = { it.name }) { tema ->
                TarjetaDiseno(
                    tema = tema,
                    enUso = tema == temaActual,
                    onUsar = { viewModel.cambiarTema(tema) }
                )
            }
        }
    }
}

/** Un diseño de la lista: vista previa con sus propios colores, nombre, descripción y estado. */
@Composable
private fun TarjetaDiseno(tema: TemaApp, enUso: Boolean, onUsar: () -> Unit) {
    val neon = Colores.esNeon
    val modificador = Modifier
        .fillMaxWidth()
        .testTag("diseno_${tema.name.lowercase()}")
    val contenido: @Composable () -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .selectable(selected = enUso, onClick = onUsar, role = Role.RadioButton)
                .padding(12.dp)
        ) {
            MiniaturaTema(
                tema = tema,
                modifier = Modifier
                    .width(112.dp)
                    .height(84.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(tema.titulo),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Colores.texto,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(tema.descripcion),
                    style = MaterialTheme.typography.bodySmall,
                    color = Colores.textoTenue
                )
                Spacer(Modifier.height(8.dp))
                EstadoDiseno(enUso)
            }
        }
    }

    if (neon) {
        // El que está en uso se enmarca en lima; los demás, en cian
        PanelNeon(
            modifier = modificador,
            cortes = CortesNeon.Grupo,
            colorBorde = if (enUso) Neon.lima else Neon.cian,
            colorAcento = if (enUso) Neon.lima else Neon.magenta,
            detalles = false
        ) { contenido() }
    } else {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Colores.superficie,
            border = BorderStroke(
                width = if (enUso) 2.dp else 1.dp,
                color = if (enUso) MaterialTheme.colorScheme.primary else Colores.borde
            ),
            modifier = modificador
        ) { contenido() }
    }
}

/** "En uso" con una marca, o "Tocar para usar" para los demás. */
@Composable
private fun EstadoDiseno(enUso: Boolean) {
    if (enUso) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.comun_en_uso),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    } else {
        Text(
            text = stringResource(R.string.apariencia_tocar_para_usar),
            style = MaterialTheme.typography.labelLarge,
            color = Colores.acentoTexto
        )
    }
}
