package com.example.myapplication.data

import android.util.Log
import com.example.myapplication.R
import com.example.myapplication.utils.Idioma
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.example.myapplication.utils.Telefonos
import kotlinx.coroutines.tasks.await

/** El correo de la cuenta todavía no se ha verificado. */
class CorreoNoVerificadoException(val correo: String) :
    Exception(Idioma.texto(R.string.auth_correo_no_verificado, correo))

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()

    /** Los correos de verificación y de recuperación de contraseña llegan en el idioma de la app. */
    private fun idiomaDeLosCorreos() = auth.setLanguageCode(Idioma.actual.codigo)
    private val db = FirebaseFirestore.getInstance()
    private val coleccionUsuarios = db.collection("usuarios")
    private val coleccionCodigos = db.collection("codigosEstudiante")

    /**
     * Inicia sesión y devuelve el perfil guardado en Firestore (de ahí sale el rol).
     * Si el perfil no se puede leer, el inicio de sesión falla: nunca se asume un rol.
     * En el primer ingreso con el correo verificado se confirma la cuenta (ver [confirmarCuenta]).
     */
    suspend fun login(correo: String, password: String): Result<Usuario> {
        return try {
            val resultado = auth.signInWithEmailAndPassword(correo.trim(), password).await()
            val user = resultado.user
                ?: return Result.failure(Exception(Idioma.texto(R.string.auth_error_usuario)))

            // Nos aseguramos de tener el estado de verificación más reciente
            user.reload().await()

            if (!user.isEmailVerified) {
                // No debe quedar autenticado hasta que confirme que el correo es suyo.
                // El reenvío del enlace es una acción aparte (botón en la pantalla de ingreso),
                // para no gastar el límite de envíos de Firebase en cada intento.
                auth.signOut()
                return Result.failure(CorreoNoVerificadoException(user.email ?: correo.trim()))
            }

            // Token nuevo: así las reglas de Firestore ven que el correo ya está verificado.
            user.getIdToken(true).await()

            Result.success(confirmarCuenta(cargarOCrearPerfil(user)))
        } catch (e: Exception) {
            auth.signOut()
            Result.failure(e)
        }
    }

    /**
     * Reenvía el correo de verificación. Firebase solo lo permite con la sesión iniciada,
     * por eso se ingresa un momento con las credenciales y se cierra la sesión al terminar.
     */
    suspend fun reenviarVerificacion(correo: String, password: String): Result<Unit> {
        return try {
            val user = auth.signInWithEmailAndPassword(correo.trim(), password).await().user
                ?: return Result.failure(Exception(Idioma.texto(R.string.auth_error_usuario)))
            user.reload().await()
            if (user.isEmailVerified) {
                auth.signOut()
                return Result.failure(Exception(Idioma.texto(R.string.auth_ya_verificado)))
            }
            idiomaDeLosCorreos()
            user.sendEmailVerification().await()
            auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            auth.signOut()
            Result.failure(e)
        }
    }

    /**
     * Hay una sesión guardada que [restaurarSesion] puede recuperar (cuenta con el correo
     * verificado). No usa la red: sirve para decidir, al abrir la app, si se muestra el logo.
     */
    fun haySesionGuardada(): Boolean = auth.currentUser?.isEmailVerified == true

    /**
     * Recupera la sesión guardada por Firebase Auth al abrir la app.
     * Devuelve null si no hay sesión válida.
     */
    suspend fun restaurarSesion(): Usuario? {
        val user = auth.currentUser ?: return null
        if (!user.isEmailVerified) {
            auth.signOut()
            return null
        }
        return try {
            confirmarCuenta(cargarOCrearPerfil(user))
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo restaurar la sesión: ${e.message}")
            null
        }
    }

    /**
     * RF-02: registro del estudiante.
     *
     * Se crea la cuenta y el perfil (sin verificar) y se envía el correo de verificación.
     * El código estudiantil se reserva recién cuando el estudiante entra con el correo
     * verificado: así, un correo mal escrito no deja el código bloqueado en una cuenta
     * que nadie puede usar, y en la búsqueda del monitor solo aparecen cuentas verificadas.
     */
    suspend fun registrarEstudiante(usuario: Usuario, password: String): Result<Unit> {
        return try {
            val resultado = auth.createUserWithEmailAndPassword(usuario.correo.trim(), password).await()
            val user = resultado.user
                ?: return Result.failure(Exception(Idioma.texto(R.string.auth_error_crear_usuario)))

            val perfil = usuario.copy(
                id = user.uid,
                nombre = usuario.nombre.trim(),
                codigo = usuario.codigo.trim(),
                programa = usuario.programa.trim(),
                telefono = Telefonos.normalizar(usuario.telefono),
                correo = usuario.correo.trim(),
                // Toda cuenta nace como estudiante; el permiso de monitor lo da monitores_autorizados
                esMonitor = false,
                modoActivo = ModoUso.ESTUDIANTE,
                bloqueado = false,
                motivoBloqueo = "",
                prestamoActivoId = "",
                verificado = false
            )

            try {
                // Aviso temprano: si el código ya pertenece a una cuenta verificada, las reglas
                // niegan la lectura de su reserva y no tiene sentido continuar.
                if (codigoReservadoPorOtro(perfil.codigo)) {
                    throw IllegalStateException(Idioma.texto(R.string.auth_codigo_registrado, perfil.codigo))
                }
                coleccionUsuarios.document(user.uid).set(perfil).await()
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo guardar el perfil; se revierte la cuenta", e)
                try {
                    user.delete().await()
                } catch (_: Exception) {
                }
                auth.signOut()
                val mensaje = when {
                    e is IllegalStateException -> e.message.orEmpty()
                    e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                        Idioma.texto(R.string.auth_perfil_reglas)
                    else -> Idioma.texto(R.string.auth_perfil_error)
                }
                return Result.failure(Exception(mensaje, e))
            }

            // El estudiante solo podrá iniciar sesión cuando abra el enlace de verificación.
            try {
                idiomaDeLosCorreos()
                user.sendEmailVerification().await()
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo enviar la verificación; puede pedir el reenvío al iniciar sesión", e)
            }
            auth.signOut()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** RF-03: envía el correo para restablecer la contraseña. */
    suspend fun recuperarPassword(correo: String): Result<Unit> {
        return try {
            idiomaDeLosCorreos()
            auth.sendPasswordResetEmail(correo.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun cerrarSesion() {
        auth.signOut()
    }

    /**
     * Cambio de contraseña desde Ajustes (monitor o estudiante).
     * Firebase exige un inicio de sesión reciente para cambiarla, así que primero se
     * confirma la contraseña actual (reautenticación) y luego se guarda la nueva.
     */
    suspend fun cambiarPassword(actual: String, nueva: String): Result<Unit> {
        val user = auth.currentUser
            ?: return Result.failure(Exception(Idioma.texto(R.string.auth_sesion_expiro)))
        val correo = user.email
            ?: return Result.failure(Exception(Idioma.texto(R.string.auth_sin_correo)))
        return try {
            user.reauthenticate(EmailAuthProvider.getCredential(correo, actual)).await()
            user.updatePassword(nueva).await()
            Result.success(Unit)
        } catch (e: FirebaseAuthWeakPasswordException) {
            // Va antes porque es un caso particular de credenciales inválidas.
            Result.failure(Exception(Idioma.texto(R.string.auth_contrasena_debil), e))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception(Idioma.texto(R.string.auth_contrasena_incorrecta), e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * true si el código ya está reservado por OTRA cuenta. Las reglas solo dejan leer la
     * reserva si no existe o si es propia, así que una lectura denegada significa "ocupado".
     */
    private suspend fun codigoReservadoPorOtro(codigo: String): Boolean {
        if (codigo.isBlank()) return false
        return try {
            coleccionCodigos.document(codigo).get().await()
            false
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) true else throw e
        }
    }

    /**
     * Primer ingreso con el correo verificado: reserva el código estudiantil (si nadie más
     * lo tiene) y marca el perfil como verificado, en una sola transacción. Vale también para
     * los monitores: su autorización puede ir por código, y las reglas exigen que sea suyo.
     * Los perfiles ya verificados no necesitan nada.
     */
    private suspend fun confirmarCuenta(perfil: Usuario): Usuario {
        if (perfil.verificado) return perfil

        val referenciaUsuario = coleccionUsuarios.document(perfil.id)
        try {
            if (perfil.codigo.isBlank()) {
                referenciaUsuario.update(mapOf<String, Any>("verificado" to true)).await()
            } else {
                val referenciaCodigo = coleccionCodigos.document(perfil.codigo)
                db.runTransaction { tx ->
                    // Si la reserva es de otra cuenta, esta lectura falla con PERMISSION_DENIED.
                    val reserva = tx.get(referenciaCodigo)
                    if (!reserva.exists()) {
                        tx.set(referenciaCodigo, mapOf("uid" to perfil.id))
                    }
                    tx.update(referenciaUsuario, mapOf<String, Any>("verificado" to true))
                    true
                }.await()
            }
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                throw Exception(Idioma.texto(R.string.auth_codigo_asociado, perfil.codigo), e)
            }
            throw e
        }
        return perfil.copy(verificado = true)
    }

    /**
     * Lee `usuarios/{uid}`. Si la cuenta viene de una versión anterior y no tiene perfil,
     * se crea uno mínimo de estudiante (el rol más restringido). Para que sea monitor,
     * Bienestar la agrega a `monitores_autorizados` desde Firebase Console.
     */
    private suspend fun cargarOCrearPerfil(user: FirebaseUser): Usuario {
        val referencia = coleccionUsuarios.document(user.uid)
        val documento = referencia.get().await()
        val existente = documento.toObject(Usuario::class.java)
        if (existente != null) {
            return existente.copy(id = user.uid)
        }

        val correo = user.email.orEmpty()
        val nuevo = Usuario(
            id = user.uid,
            nombre = user.displayName?.ifBlank { null }
                ?: correo.substringBefore("@").replace(".", " ").replaceFirstChar { it.uppercase() },
            correo = correo
        )
        referencia.set(nuevo).await()
        return nuevo
    }

    private companion object {
        const val TAG = "AuthRepository"
    }
}
