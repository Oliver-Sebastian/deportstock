package com.example.myapplication.ui.screens.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.data.ProgramasAcademicos
import com.example.myapplication.ui.components.CampoPassword
import com.example.myapplication.ui.components.CampoTelefono
import com.example.myapplication.ui.components.Desplegable
import com.example.myapplication.ui.components.EstiloNeon
import com.example.myapplication.ui.components.TarjetaElevadaApp
import com.example.myapplication.ui.components.VeloNeon
import com.example.myapplication.ui.components.filtroFotoNeon
import com.example.myapplication.ui.theme.formaApp
import com.example.myapplication.utils.Idioma
import com.example.myapplication.utils.ValidacionPassword
import com.example.myapplication.viewmodel.LoginUiState
import com.example.myapplication.viewmodel.LoginViewModel

@Composable
fun RegistroScreen(
    onVolverALogin: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    var nombre by remember { mutableStateOf("") }
    var codigo by remember { mutableStateOf("") }
    var programaSeleccionado by remember { mutableStateOf("") }
    var programaOtro by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var confirmarCorreo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var repetirPassword by remember { mutableStateOf("") }
    val estado by viewModel.uiState.collectAsState()

    val pendienteVerificacion = estado as? LoginUiState.RegistroPendienteVerificacion
    // Con "Otro programa" se usa lo que el estudiante escriba.
    val programa = if (programaSeleccionado == ProgramasAcademicos.OTRO) programaOtro else programaSeleccionado

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

        // 2. DEGRADADO
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

        // 3. TARJETA DE REGISTRO
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
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
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (pendienteVerificacion != null) {
                        Text(
                            stringResource(R.string.registro_revisa_correo),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.registro_enlace_enviado, pendienteVerificacion.correo),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            stringResource(R.string.registro_no_llega),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = {
                                viewModel.reiniciarEstado()
                                onVolverALogin()
                            },
                            shape = formaApp(12.dp),
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("ir_a_login_button")
                        ) {
                            Text(stringResource(R.string.registro_ir_ingreso), fontWeight = FontWeight.Bold)
                        }
                        return@Column
                    }

                    Text(
                        stringResource(R.string.registro_crear_cuenta),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    )
                    Text(
                        stringResource(R.string.registro_subtitulo),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(24.dp))

                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text(stringResource(R.string.comun_nombre_completo)) },
                        singleLine = true,
                        shape = formaApp(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("nombre_input")
                    )
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = codigo,
                        onValueChange = { input ->
                            val filtrado = input.filter { it.isLetterOrDigit() }
                            if (filtrado.length <= 15) codigo = filtrado
                        },
                        label = { Text(stringResource(R.string.comun_codigo_estudiantil)) },
                        singleLine = true,
                        shape = formaApp(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("codigo_input")
                    )
                    Spacer(Modifier.height(8.dp))

                    Desplegable(
                        etiqueta = stringResource(R.string.comun_programa_academico),
                        opciones = ProgramasAcademicos.opciones,
                        seleccion = programaSeleccionado,
                        onSeleccion = { programaSeleccionado = it },
                        placeholder = stringResource(R.string.comun_selecciona_programa),
                        etiquetaPrueba = "programa_input",
                        textoOpcion = ProgramasAcademicos::etiqueta,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (programaSeleccionado == ProgramasAcademicos.OTRO) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = programaOtro,
                            onValueChange = { if (it.length <= 60) programaOtro = it },
                            label = { Text(stringResource(R.string.comun_cual_programa)) },
                            singleLine = true,
                            shape = formaApp(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("programa_otro_input")
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    CampoTelefono(
                        valor = telefono,
                        onValorChange = { telefono = it },
                        etiquetaPrueba = "telefono_input",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = correo,
                        onValueChange = { correo = it.trim() },
                        label = { Text(stringResource(R.string.registro_correo)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = formaApp(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("correo_input")
                    )
                    Spacer(Modifier.height(8.dp))

                    val correosNoCoinciden = confirmarCorreo.isNotEmpty() &&
                        !correo.equals(confirmarCorreo, ignoreCase = true)
                    OutlinedTextField(
                        value = confirmarCorreo,
                        onValueChange = { confirmarCorreo = it.trim() },
                        label = { Text(stringResource(R.string.registro_confirmar_correo)) },
                        singleLine = true,
                        isError = correosNoCoinciden,
                        supportingText = {
                            Text(
                                if (correosNoCoinciden) stringResource(R.string.registro_correo_no_coincide)
                                else stringResource(R.string.registro_correo_ayuda)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = formaApp(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("confirmar_correo_input")
                    )
                    Spacer(Modifier.height(8.dp))

                    CampoPassword(
                        valor = password,
                        onValorChange = { password = it },
                        etiqueta = stringResource(R.string.comun_contrasena),
                        esError = password.isNotEmpty() && !ValidacionPassword.esSegura(password),
                        textoApoyo = {
                            // Lista de requisitos que se va marcando mientras escribe
                            Column {
                                ValidacionPassword.requisitos.forEach { requisito ->
                                    val cumple = requisito.cumple(password)
                                    Text(
                                        text = (if (cumple) "✓ " else "• ") + requisito.descripcion,
                                        color = if (cumple) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        etiquetaPrueba = "password_input",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))

                    val passwordsNoCoinciden = repetirPassword.isNotEmpty() && password != repetirPassword
                    CampoPassword(
                        valor = repetirPassword,
                        onValorChange = { repetirPassword = it },
                        etiqueta = stringResource(R.string.registro_repetir_contrasena),
                        esError = passwordsNoCoinciden,
                        textoApoyo = if (passwordsNoCoinciden) {
                            { Text(Idioma.texto(R.string.comun_no_coinciden)) }
                        } else {
                            null
                        },
                        etiquetaPrueba = "repetir_password_input",
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

                    Button(
                        onClick = {
                            viewModel.registrar(
                                nombre = nombre,
                                codigo = codigo,
                                programa = programa,
                                telefono = telefono,
                                correo = correo,
                                password = password,
                                repetirPassword = repetirPassword,
                                confirmarCorreo = confirmarCorreo
                            )
                        },
                        enabled = estado !is LoginUiState.Cargando,
                        shape = formaApp(12.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("registrar_button")
                    ) {
                        if (estado is LoginUiState.Cargando) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(stringResource(R.string.registro_registrarme), fontWeight = FontWeight.Bold)
                        }
                    }

                    TextButton(onClick = onVolverALogin) {
                        Text(
                            stringResource(R.string.registro_ya_tengo_cuenta),
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
