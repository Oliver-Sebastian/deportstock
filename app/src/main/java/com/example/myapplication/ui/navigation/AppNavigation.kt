package com.example.myapplication.ui.navigation

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.R
import com.example.myapplication.data.Usuario
import com.example.myapplication.notificaciones.ProgramadorRecordatorios
import com.example.myapplication.ui.screens.HomeScreen
import com.example.myapplication.ui.screens.ajustes.AjustesScreen
import com.example.myapplication.ui.screens.ajustes.AparienciaScreen
import com.example.myapplication.ui.screens.estudiante.EstudianteHomeScreen
import com.example.myapplication.ui.screens.horario.HorarioAdminScreen
import com.example.myapplication.ui.screens.inventario.InventarioScreen
import com.example.myapplication.ui.screens.login.LoginScreen
import com.example.myapplication.ui.screens.login.RegistroScreen
import com.example.myapplication.ui.screens.prestamo.RegistrarDevolucionScreen
import com.example.myapplication.ui.screens.prestamo.RegistrarPrestamoScreen
import com.example.myapplication.ui.screens.reportes.ReportesScreen
import com.example.myapplication.utils.Idioma
import com.example.myapplication.viewmodel.SesionUiState
import com.example.myapplication.viewmodel.SesionViewModel

object Rutas {
    const val INICIO = "inicio"
    const val LOGIN = "login"
    const val REGISTRO = "registro"
    const val HOME_MONITOR = "home"
    const val HOME_ESTUDIANTE = "home_estudiante"
    const val REGISTRAR_PRESTAMO = "registrar_prestamo"
    const val REGISTRAR_DEVOLUCION = "registrar_devolucion"
    const val INVENTARIO = "inventario"
    const val HORARIO = "horario"
    const val REPORTES = "reportes"
    const val AJUSTES = "ajustes"
    const val APARIENCIA = "apariencia"
}

/**
 * Navegación de la app. RF-01: tras iniciar sesión cada usuario llega a la interfaz de su rol.
 * Un monitor con permiso vigente elige en Ajustes si usa la app como monitor o como estudiante;
 * las rutas del monitor solo se abren en modo monitor: si no, se vuelve al inicio del estudiante.
 */
@Composable
fun AppNavigation(sesionViewModel: SesionViewModel = viewModel()) {
    val navController = rememberNavController()
    val contexto = LocalContext.current.applicationContext
    val sesion by sesionViewModel.estado.collectAsState()
    val foto by sesionViewModel.foto.collectAsState()
    val periodoMonitor by sesionViewModel.periodoMonitor.collectAsState()

    val irALogin: () -> Unit = { navController.reemplazarTodoPor(Rutas.LOGIN) }
    val irAInicioDe: (Usuario) -> Unit = { usuario ->
        navController.reemplazarTodoPor(if (usuario.enModoMonitor) Rutas.HOME_MONITOR else Rutas.HOME_ESTUDIANTE)
    }
    // Al cerrar sesión el estado pasa a SinSesion y la ruta protegida redirige al login.
    val cerrarSesion: () -> Unit = {
        ProgramadorRecordatorios.cancelar(contexto)
        sesionViewModel.cerrarSesion()
    }

    // Cambio de modo en Ajustes, o permiso de monitor que Bienestar activa o termina: aviso y,
    // si corresponde, al inicio del modo nuevo (sin cerrar sesión).
    LaunchedEffect(sesionViewModel) {
        sesionViewModel.avisos.collect { aviso ->
            Toast.makeText(contexto, Idioma.texto(aviso.mensaje), Toast.LENGTH_LONG).show()
            val actual = sesionViewModel.estado.value
            if (aviso.irAlInicio && actual is SesionUiState.Activa) irAInicioDe(actual.usuario)
        }
    }

    // Sin sesión guardada la app abre directo en el ingreso: el logo es solo para quien ya
    // inició sesión. Se decide una vez, al crear la navegación.
    val inicio = remember {
        if (sesionViewModel.estado.value is SesionUiState.SinSesion) Rutas.LOGIN else Rutas.INICIO
    }

    NavHost(navController = navController, startDestination = inicio) {

        // Pantalla de carga con sesión (al abrir la app con la sesión guardada, o justo después
        // de ingresar): el logo hasta saber a qué inicio ir y si es monitor.
        composable(Rutas.INICIO) {
            LaunchedEffect(sesion) {
                when (val actual = sesion) {
                    is SesionUiState.Activa -> irAInicioDe(actual.usuario)
                    is SesionUiState.SinSesion -> irALogin()
                    is SesionUiState.Cargando -> Unit
                }
            }
            PantallaLogo()
        }

        composable(Rutas.LOGIN) {
            LoginScreen(
                onLoginExitoso = { usuario ->
                    // La pantalla de arranque espera a saber si tiene permiso de monitor
                    sesionViewModel.iniciar(usuario)
                    navController.reemplazarTodoPor(Rutas.INICIO)
                },
                onIrARegistro = { navController.navigate(Rutas.REGISTRO) }
            )
        }

        composable(Rutas.REGISTRO) {
            RegistroScreen(
                onVolverALogin = { navController.popBackStack() }
            )
        }

        // ------------------------- Interfaz del monitor -------------------------
        composable(Rutas.HOME_MONITOR) {
            RutaProtegida(sesion, soloMonitor = true, irALogin, irAInicioDe) { usuario ->
                HomeScreen(
                    usuario = usuario,
                    onRegistrarPrestamo = { navController.navigate(Rutas.REGISTRAR_PRESTAMO) },
                    onRegistrarDevolucion = { navController.navigate(Rutas.REGISTRAR_DEVOLUCION) },
                    onMirarInventario = { navController.navigate(Rutas.INVENTARIO) },
                    onGestionarHorario = { navController.navigate(Rutas.HORARIO) },
                    onMirarReporte = { navController.navigate(Rutas.REPORTES) },
                    onAbrirAjustes = { navController.navigate(Rutas.AJUSTES) },
                    foto = foto
                )
            }
        }

        composable(Rutas.REGISTRAR_PRESTAMO) {
            RutaProtegida(sesion, soloMonitor = true, irALogin, irAInicioDe) {
                RegistrarPrestamoScreen(
                    onVolver = { navController.popBackStack() },
                    onConfigurarSemestre = { navController.navigate(Rutas.HORARIO) }
                )
            }
        }

        composable(Rutas.REGISTRAR_DEVOLUCION) {
            RutaProtegida(sesion, soloMonitor = true, irALogin, irAInicioDe) {
                RegistrarDevolucionScreen(
                    onVolver = { navController.popBackStack() }
                )
            }
        }

        composable(Rutas.INVENTARIO) {
            RutaProtegida(sesion, soloMonitor = true, irALogin, irAInicioDe) {
                InventarioScreen(
                    onVolver = { navController.popBackStack() }
                )
            }
        }

        composable(Rutas.HORARIO) {
            RutaProtegida(sesion, soloMonitor = true, irALogin, irAInicioDe) {
                HorarioAdminScreen(
                    onVolver = { navController.popBackStack() }
                )
            }
        }

        composable(Rutas.REPORTES) {
            RutaProtegida(sesion, soloMonitor = true, irALogin, irAInicioDe) {
                ReportesScreen(
                    onVolver = { navController.popBackStack() }
                )
            }
        }

        // ------------------------ Interfaz del estudiante ------------------------
        composable(Rutas.HOME_ESTUDIANTE) {
            RutaProtegida(sesion, soloMonitor = false, irALogin, irAInicioDe) { usuario ->
                EstudianteHomeScreen(
                    usuario = usuario,
                    onAbrirAjustes = { navController.navigate(Rutas.AJUSTES) }
                )
            }
        }

        // ------------------------- Común a los dos roles -------------------------
        // Ajustes: datos de la cuenta, contraseña, tema, notificaciones y cierre de sesión.
        composable(Rutas.AJUSTES) {
            RutaProtegida(sesion, soloMonitor = false, irALogin, irAInicioDe) { usuario ->
                AjustesScreen(
                    usuario = usuario,
                    onVolver = { navController.popBackStack() },
                    onCerrarSesion = cerrarSesion,
                    foto = foto,
                    onVerMasDisenos = { navController.navigate(Rutas.APARIENCIA) { launchSingleTop = true } },
                    periodoMonitor = periodoMonitor,
                    onCambiarModo = sesionViewModel::cambiarModo
                )
            }
        }

        // Lista de todos los diseños (Ajustes > Apariencia > Ver más diseños).
        composable(Rutas.APARIENCIA) {
            RutaProtegida(sesion, soloMonitor = false, irALogin, irAInicioDe) {
                AparienciaScreen(onVolver = { navController.popBackStack() })
            }
        }
    }
}

/** Navega a [ruta] dejándola como única pantalla de la pila. */
private fun NavHostController.reemplazarTodoPor(ruta: String) {
    navigate(ruta) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

/**
 * Muestra [contenido] solo si hay sesión y el modo lo permite.
 * Sin sesión redirige al login; fuera del modo monitor, una ruta de monitor lleva al inicio.
 */
@Composable
private fun RutaProtegida(
    sesion: SesionUiState,
    soloMonitor: Boolean,
    onSinSesion: () -> Unit,
    onSinPermiso: (Usuario) -> Unit,
    contenido: @Composable (Usuario) -> Unit
) {
    when (sesion) {
        is SesionUiState.Cargando -> PantallaLogo()
        is SesionUiState.SinSesion -> {
            // Un instante, mientras se pasa al ingreso: sin nada encima del fondo
            LaunchedEffect(Unit) { onSinSesion() }
        }
        is SesionUiState.Activa -> {
            val usuario = sesion.usuario
            if (soloMonitor && !usuario.enModoMonitor) {
                LaunchedEffect(Unit) { onSinPermiso(usuario) }
            } else {
                contenido(usuario)
            }
        }
    }
}

/**
 * El logo sobre negro, igual que la pantalla de carga del sistema (mismo tamaño y posición): así
 * el paso de esa pantalla a la app no se nota y no hace falta un círculo de carga.
 */
@Composable
private fun PantallaLogo() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.fondo_logo)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.logo_carga),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier.size(288.dp)
        )
    }
}
