package com.example.myapplication.ui.screens.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.data.Usuario
import com.example.myapplication.ui.components.CampoPassword
import com.example.myapplication.ui.components.EstiloNeon
import com.example.myapplication.ui.components.TarjetaElevadaApp
import com.example.myapplication.ui.components.VeloNeon
import com.example.myapplication.ui.components.estiloBrillo
import com.example.myapplication.ui.components.filtroFotoNeon
import com.example.myapplication.ui.theme.formaApp
import com.example.myapplication.viewmodel.LoginUiState
import com.example.myapplication.viewmodel.LoginViewModel
import com.example.myapplication.data.TemaApp
import com.example.myapplication.ui.components.LogoUdenarCompleto
import com.example.myapplication.ui.theme.Colores

@Composable
fun LoginScreen(
    onLoginExitoso: (Usuario) -> Unit,
    onIrARegistro: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    var correo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val estado by viewModel.uiState.collectAsState()

    LaunchedEffect(estado) {
        val actual = estado
        if (actual is LoginUiState.Exito) onLoginExitoso(actual.usuario)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. IMAGEN DE FONDO
        Image(
            painter = painterResource(id = R.drawable.fondo_login),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            colorFilter = filtroFotoNeon()
        )
        // Con el tema neón la foto queda teñida de cian y magenta
        VeloNeon()

        // 2. DEGRADADO SOBRE LA IMAGEN
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

        // 3. TARJETA DE LOGIN CENTRADA. Se desplaza si no cabe (por ejemplo, con el teléfono
        // girado); si cabe, queda centrada como siempre.
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TarjetaElevadaApp(
                    estiloNeon = EstiloNeon.DESTACADA,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    shape = formaApp(24.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "DEPORTSTOCK",
                            // En neón el nombre brilla
                            style = if (Colores.esNeon) {
                                MaterialTheme.typography.headlineMedium.merge(estiloBrillo(MaterialTheme.colorScheme.primary, 6.dp))
                            } else {
                                MaterialTheme.typography.headlineMedium
                            },
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 28.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.ingreso_bienvenido),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(32.dp))

                        OutlinedTextField(
                            value = correo, onValueChange = { correo = it },
                            label = { Text(stringResource(R.string.comun_correo)) },
                            singleLine = true,
                            shape = formaApp(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("correo_login_input")
                        )
                        Spacer(Modifier.height(12.dp))

                        CampoPassword(
                            valor = password,
                            onValorChange = { password = it },
                            etiqueta = stringResource(R.string.comun_contrasena),
                            etiquetaPrueba = "password_login_input",
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(16.dp))

                        if (estado is LoginUiState.Error) {
                            Text(
                                (estado as LoginUiState.Error).mensaje,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(Modifier.height(8.dp))
                        }

                        // Correo sin verificar: se explica qué hacer y se ofrece reenviar el enlace
                        (estado as? LoginUiState.CorreoNoVerificado)?.let { sinVerificar ->
                            Text(
                                stringResource(R.string.ingreso_correo_sin_verificar, sinVerificar.correo),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.testTag("correo_no_verificado_text")
                            )
                            TextButton(
                                onClick = { viewModel.reenviarVerificacion(correo, password) },
                                modifier = Modifier.testTag("reenviar_verificacion_button")
                            ) {
                                Text(stringResource(R.string.ingreso_reenviar), fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(4.dp))
                        }

                        (estado as? LoginUiState.VerificacionReenviada)?.let { reenviada ->
                            Text(
                                stringResource(R.string.ingreso_reenviado, reenviada.correo),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.testTag("verificacion_reenviada_text")
                            )
                            Spacer(Modifier.height(8.dp))
                        }

                        // RF-03: confirmación del correo de recuperación de contraseña
                        (estado as? LoginUiState.RecuperacionEnviada)?.let { recuperacion ->
                            Text(
                                stringResource(R.string.ingreso_recuperacion, recuperacion.correo),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.testTag("recuperacion_enviada_text")
                            )
                            Spacer(Modifier.height(8.dp))
                        }

                        Button(
                            onClick = { viewModel.login(correo, password) },
                            enabled = estado !is LoginUiState.Cargando,
                            shape = formaApp(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("login_button")
                        ) {
                            if (estado is LoginUiState.Cargando) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(stringResource(R.string.ingreso_ingresar), fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // RF-03: recuperar la contraseña con el correo escrito arriba
                        TextButton(
                            onClick = { viewModel.recuperarPassword(correo) },
                            enabled = estado !is LoginUiState.Cargando,
                            modifier = Modifier.testTag("recuperar_password_button")
                        ) {
                            Text(
                                stringResource(R.string.ingreso_olvidaste),
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        TextButton(
                            onClick = {
                                viewModel.reiniciarEstado()
                                onIrARegistro()
                            },
                            modifier = Modifier.testTag("ir_a_registro_button")
                        ) {
                            Text(
                                stringResource(R.string.ingreso_registrate),
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Tema Institucional: logo de la universidad debajo de la tarjeta de ingreso.
                if (Colores.tema == TemaApp.INSTITUCIONAL) {
                    Spacer(Modifier.height(20.dp))
                    LogoUdenarCompleto(modifier = Modifier.width(140.dp))
                }
            }
        }
    }
}
