package com.example.myapplication

import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.data.PreferenciasRepository
import com.example.myapplication.ui.components.FondoApp
import com.example.myapplication.ui.navigation.AppNavigation
import com.example.myapplication.ui.theme.Colores
import com.example.myapplication.ui.theme.DeportStockTheme
import com.example.myapplication.ui.theme.ProveedorIdioma
import com.example.myapplication.utils.Idioma
import com.example.myapplication.utils.RecursosConIdioma
import com.example.myapplication.viewmodel.SesionUiState
import com.example.myapplication.viewmodel.SesionViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // Los diálogos, los menús desplegables y el calendario se dibujan en ventanas propias que
    // toman los textos de la actividad: con esto también salen en el idioma elegido en Ajustes.
    private val recursosConIdioma = RecursosConIdioma(this)

    override fun getResources(): Resources = recursosConIdioma.de(super.getResources())

    // Sesión de toda la app (la misma que usa AppNavigation): decide si al abrir se ve el logo.
    private val sesionViewModel: SesionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Pantalla de carga del sistema (Theme.MyApplication.Carga). Va antes de
        // super.onCreate, como pide core-splashscreen.
        val pantallaDeCarga = installSplashScreen()
        super.onCreate(savedInstanceState)
        // El tema y el idioma guardados se leen antes de dibujar: la app abre directamente con ellos.
        Colores.tema = PreferenciasRepository(this).tema()
        Idioma.iniciar(this)
        val sesion = sesionViewModel
        // El primer cuadro de la app ya muestra lo que corresponde: el logo si hay sesión
        // guardada (hasta que carga), o el ingreso si no. La pantalla del sistema se quita de
        // golpe, sin su animación de salida: con sesión, el logo sigue quieto en su lugar.
        pantallaDeCarga.setOnExitAnimationListener { vista -> vista.remove() }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) recordarPantallaDeCarga(sesion)
        enableEdgeToEdge()
        setContent {
            // Textos en el idioma elegido en Ajustes; al cambiarlo, la app se redibuja sin reiniciar.
            ProveedorIdioma {
                DeportStockTheme(tema = Colores.tema) {
                    // Fondo de la app: el color del tema, o el fondo HUD animado con los temas neón.
                    FondoApp {
                        AppNavigation(sesionViewModel = sesion)
                    }
                }
            }
        }
    }

    /**
     * Android 13 o superior: Android dibuja su pantalla de carga antes de que la app sepa si hay
     * sesión, así que se elige para la próxima apertura. Con sesión, el logo desde que se toca el
     * ícono; sin sesión (al cerrarla), solo el fondo negro.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun recordarPantallaDeCarga(sesion: SesionViewModel) {
        lifecycleScope.launch {
            sesion.estado
                .mapNotNull { estado ->
                    when (estado) {
                        is SesionUiState.Activa -> R.style.Theme_MyApplication_CargaConLogo
                        is SesionUiState.SinSesion -> R.style.Theme_MyApplication_Carga
                        is SesionUiState.Cargando -> null
                    }
                }
                .distinctUntilChanged()
                .collect { tema -> splashScreen.setSplashScreenTheme(tema) }
        }
    }
}