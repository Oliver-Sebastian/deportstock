package com.example.myapplication.data

import com.example.myapplication.R
import com.example.myapplication.utils.Fechas
import com.example.myapplication.utils.Idioma
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Acceso a la colección `prestamos`.
 *
 * Préstamo y devolución se ejecutan como transacciones de Firestore: o se guardan
 * todos los cambios (préstamo, inventario y estudiante) o no se guarda ninguno.
 * Así dos monitores no pueden prestar la misma unidad al mismo tiempo.
 */
class PrestamosRepository {

    private val db = FirebaseFirestore.getInstance()
    private val coleccionPrestamos = db.collection("prestamos")
    private val coleccionInventario = db.collection("inventario")
    private val coleccionUsuarios = db.collection("usuarios")
    private val configuracionGeneral = db.collection("configuracion").document("general")

    /** Todos los préstamos en tiempo real, del más reciente al más antiguo (solo monitores). */
    fun obtenerPrestamosFlow(): Flow<List<Prestamo>> = escuchar(coleccionPrestamos)

    /** RF-16: préstamos de un estudiante. Las reglas de Firestore solo le permiten leer los suyos. */
    fun obtenerPrestamosDeEstudianteFlow(estudianteId: String): Flow<List<Prestamo>> =
        escuchar(coleccionPrestamos.whereEqualTo("estudianteId", estudianteId))

    /** RF-19: préstamo sin devolver del estudiante (consulta puntual para el recordatorio), o null. */
    suspend fun obtenerPrestamoActivoDe(estudianteId: String): Prestamo? {
        val snapshot = coleccionPrestamos.whereEqualTo("estudianteId", estudianteId).get().await()
        return snapshot.documents
            .mapNotNull { doc -> doc.toObject(Prestamo::class.java)?.copy(id = doc.id) }
            .firstOrNull { it.estaActivo }
    }

    /**
     * RF-19 para el monitor: cuántos préstamos pasaron su fecha límite sin devolverse.
     * La sede maneja pocos préstamos, así que se leen todos y se filtra en el teléfono
     * (también cubre registros antiguos con el estado escrito en minúsculas).
     */
    suspend fun contarPrestamosVencidos(ahora: Date = Date()): Int {
        val snapshot = coleccionPrestamos.get().await()
        return snapshot.documents
            .mapNotNull { doc -> doc.toObject(Prestamo::class.java) }
            .count { it.estaVencido(ahora) }
    }

    private fun escuchar(consulta: Query): Flow<List<Prestamo>> = callbackFlow {
        val registro = consulta.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val prestamos = snapshot?.documents.orEmpty()
                .mapNotNull { doc -> doc.toObject(Prestamo::class.java)?.copy(id = doc.id) }
                .sortedByDescending { it.fechaPrestamo?.seconds ?: 0L }
            trySend(prestamos)
        }
        awaitClose { registro.remove() }
    }

    /**
     * RF-09, RF-10 y RF-12: registra un préstamo de uno o varios artículos.
     *
     * Dentro de la transacción se verifica que:
     * - la fecha límite no supere el fin de semestre (regla de negocio 2);
     * - el estudiante no esté bloqueado ni tenga otro préstamo sin devolver;
     * - cada artículo siga disponible en la cantidad pedida.
     */
    suspend fun registrarPrestamo(
        estudiante: Usuario,
        monitor: Usuario?,
        seleccion: List<SeleccionPrestamo>,
        fechaLimite: Date,
        observaciones: String = ""
    ): Result<String> {
        if (estudiante.id.isBlank()) {
            return Result.failure(IllegalArgumentException(Idioma.texto(R.string.repo_pre_selecciona_estudiante)))
        }
        // Un monitor no se registra préstamos a sí mismo (también lo impiden las reglas de Firestore)
        if (estudiante.id == (monitor?.id ?: uidActual())) {
            return Result.failure(IllegalArgumentException(Idioma.texto(R.string.repo_pre_prestamo_propio)))
        }
        // Si un artículo llegara repetido, se suma en una sola línea.
        val lineas = seleccion
            .groupBy { it.item.id }
            .map { (_, repetidos) -> SeleccionPrestamo(repetidos.first().item, repetidos.sumOf { it.cantidad }) }
        if (lineas.isEmpty() || lineas.any { it.cantidad < 1 }) {
            return Result.failure(IllegalArgumentException(Idioma.texto(R.string.repo_pre_agrega_articulo)))
        }

        val referenciaPrestamo = coleccionPrestamos.document()
        val referenciaEstudiante = coleccionUsuarios.document(estudiante.id)

        return try {
            db.runTransaction { tx ->
                // --- Lecturas (en una transacción van todas antes de las escrituras) ---
                val configuracion = tx.get(configuracionGeneral).toObject(ConfiguracionGeneral::class.java)
                val estudianteActual = tx.get(referenciaEstudiante).toObject(Usuario::class.java)
                    ?: throw reglaNegocio(Idioma.texto(R.string.repo_pre_estudiante_no_registrado))
                val referenciasItems = lineas.map { coleccionInventario.document(it.item.id) }
                val snapshotsItems = referenciasItems.map { tx.get(it) }

                // --- Validaciones ---
                val ahora = Date()
                val finSemestre = configuracion?.fechaFinSemestre?.toDate()
                    ?: throw reglaNegocio(Idioma.texto(R.string.repo_pre_configura_semestre))
                if (fechaLimite.before(ahora)) {
                    throw reglaNegocio(Idioma.texto(R.string.repo_pre_fecha_posterior))
                }
                if (fechaLimite.after(finSemestre)) {
                    throw reglaNegocio(Idioma.texto(R.string.repo_pre_fecha_supera, Fechas.corta(finSemestre)))
                }
                if (estudianteActual.bloqueado) {
                    // El motivo guardado en Firestore está en español: el mensaje sale del idioma de la app
                    throw reglaNegocio(Idioma.texto(R.string.repo_pre_estudiante_bloqueado))
                }
                if (estudianteActual.prestamoActivoId.isNotBlank()) {
                    throw reglaNegocio(Idioma.texto(R.string.repo_pre_prestamo_activo))
                }

                val itemsActuales = snapshotsItems.mapIndexed { indice, snapshot ->
                    val linea = lineas[indice]
                    val actual = snapshot.toObject(ItemDeportivo::class.java)
                        ?: throw reglaNegocio(Idioma.texto(R.string.comun_articulo_no_existe, linea.item.id))
                    if (!actual.activo) {
                        throw reglaNegocio(Idioma.texto(R.string.repo_pre_articulo_baja, linea.item.id))
                    }
                    if (actual.esPorCantidad) {
                        if (actual.cantidadDisponible < linea.cantidad) {
                            throw reglaNegocio(
                                Idioma.texto(R.string.repo_pre_solo_quedan, actual.cantidadDisponible, actual.nombre, linea.cantidad)
                            )
                        }
                    } else if (!EstadoItem.esPrestable(actual.estado)) {
                        throw reglaNegocio(
                            Idioma.texto(R.string.repo_pre_unidad_no_disponible, linea.item.id, EstadoItem.etiqueta(actual.estado).lowercase())
                        )
                    }
                    actual
                }

                // --- Escrituras ---
                val marca = Timestamp.now()
                val itemsPrestamo = itemsActuales.mapIndexed { indice, actual ->
                    val linea = lineas[indice]
                    val cantidad = if (actual.esPorCantidad) linea.cantidad else 1

                    if (actual.esPorCantidad) {
                        val nuevaDisponible = actual.cantidadDisponible - cantidad
                        tx.update(
                            referenciasItems[indice],
                            mapOf(
                                "cantidadDisponible" to nuevaDisponible,
                                "estado" to EstadoItem.porCantidad(nuevaDisponible, actual.cantidadTotal, actual.cantidadDanada),
                                "fechaActualizacion" to marca
                            )
                        )
                    } else {
                        // No se guarda el nombre del estudiante en el artículo: el catálogo lo leen
                        // todos los usuarios y un estudiante no debe ver préstamos ajenos.
                        tx.update(
                            referenciasItems[indice],
                            mapOf(
                                "estado" to EstadoItem.PRESTADO,
                                "cantidadDisponible" to 0,
                                "prestamoActivoId" to referenciaPrestamo.id,
                                "estudiantePrestamoId" to "",
                                "estudiantePrestamoNombre" to "",
                                "fechaActualizacion" to marca
                            )
                        )
                    }

                    // Si la unidad sale incompleta, se anota para no culpar al estudiante al devolverla.
                    val incompleta = actual.esIncompleto
                    ItemPrestamo(
                        itemId = linea.item.id,
                        nombre = actual.nombre,
                        tipoControl = actual.tipoControl,
                        cantidad = cantidad,
                        estadoAlPrestar = if (incompleta) EstadoItem.INCOMPLETO else "",
                        observacionAlPrestar = if (incompleta) actual.observacion else ""
                    )
                }

                val prestamo = Prestamo(
                    id = referenciaPrestamo.id,
                    estudianteId = estudiante.id,
                    codigoEstudiante = estudianteActual.codigo,
                    nombreEstudiante = estudianteActual.nombre,
                    programaEstudiante = estudianteActual.programa,
                    telefono = estudianteActual.telefono,   // copia para reportes; las pantallas usan el actual
                    monitorId = monitor?.id.orEmpty(),
                    monitorNombre = monitor?.nombre.orEmpty(),
                    items = itemsPrestamo,
                    // Campos de resumen que ya usaban las pantallas existentes:
                    idItem = itemsPrestamo.first().itemId,
                    itemsCodigos = itemsPrestamo.map { it.itemId },
                    itemsNombres = itemsPrestamo.map { describir(it) },
                    cantidadTotalItems = itemsPrestamo.sumOf { it.cantidad },
                    fechaPrestamo = marca,
                    fechaEntregaEsperada = Timestamp(fechaLimite),
                    estadoDevolucion = EstadoPrestamo.PENDIENTE,
                    observaciones = observaciones.trim(),
                    carnetRetenido = true,
                    documentoRetenido = true
                )
                tx.set(referenciaPrestamo, prestamo)
                tx.update(referenciaEstudiante, mapOf("prestamoActivoId" to referenciaPrestamo.id))

                referenciaPrestamo.id
            }.await()

            Result.success(Idioma.texto(R.string.repo_pre_registrado, Fechas.corta(fechaLimite)))
        } catch (e: Exception) {
            falla(e, Idioma.texto(R.string.repo_pre_error_registrar))
        }
    }

    /**
     * RF-11: registra la devolución indicando el estado de cada artículo.
     *
     * - Todo en buen estado: los artículos vuelven a estar disponibles y se libera el carnet.
     * - Con novedades (dañado, incompleto o extraviado): el artículo queda en ese estado con su
     *   observación, el carnet sigue retenido y el estudiante queda bloqueado hasta que un
     *   monitor resuelva el caso (regla de negocio 4).
     *
     * @param resultados solo hace falta incluir los artículos con novedad.
     */
    suspend fun registrarDevolucion(
        prestamoId: String,
        resultados: List<ResultadoDevolucion>,
        observaciones: String = "",
        monitor: Usuario? = null
    ): Result<String> {
        val referenciaPrestamo = coleccionPrestamos.document(prestamoId)
        val novedadesPorItem = resultados.associateBy { it.itemId }

        return try {
            val huboNovedades = db.runTransaction { tx ->
                // --- Lecturas ---
                val snapshotPrestamo = tx.get(referenciaPrestamo)
                val prestamo = snapshotPrestamo.toObject(Prestamo::class.java)?.copy(id = snapshotPrestamo.id)
                    ?: throw reglaNegocio(Idioma.texto(R.string.repo_pre_no_encontrado))
                if (!prestamo.estaActivo) {
                    throw reglaNegocio(Idioma.texto(R.string.repo_pre_ya_devuelto))
                }
                if (prestamo.estudianteId == (monitor?.id ?: uidActual())) {
                    throw reglaNegocio(Idioma.texto(R.string.repo_pre_devolucion_propia))
                }
                val lineas = prestamo.itemsEfectivos
                val snapshotsItems = lineas.map { tx.get(coleccionInventario.document(it.itemId)) }
                val referenciaEstudiante = prestamo.estudianteId
                    .takeIf { it.isNotBlank() }
                    ?.let { coleccionUsuarios.document(it) }
                val existeEstudiante = referenciaEstudiante?.let { tx.get(it).exists() } ?: false

                // --- Escrituras ---
                val marca = Timestamp.now()
                val lineasDevueltas = lineas.mapIndexed { indice, linea ->
                    val resultado = novedadesPorItem[linea.itemId]
                    val observacionItem = resultado?.observacion.orEmpty().trim()
                    val snapshot = snapshotsItems[indice]
                    val actual = snapshot.toObject(ItemDeportivo::class.java)
                    val porCantidad = actual?.esPorCantidad ?: (linea.tipoControl == TipoControl.CANTIDAD)

                    if (porCantidad) {
                        // Piezas que regresan dañadas o que no regresan (extraviadas)
                        val danadas = (resultado?.cantidadDanada ?: 0).coerceIn(0, linea.cantidad)
                        val extraviadas = (resultado?.cantidadExtraviada ?: 0).coerceIn(0, linea.cantidad - danadas)

                        // Un artículo eliminado con una versión anterior de la app ya no tiene documento.
                        if (actual != null) {
                            val nuevaDanada = actual.cantidadDanada + danadas
                            val nuevaExtraviada = actual.cantidadExtraviada + extraviadas
                            val nuevaDisponible = (actual.cantidadDisponible + linea.cantidad - danadas - extraviadas)
                                .coerceAtMost(actual.cantidadTotal - nuevaDanada - nuevaExtraviada)
                                .coerceAtLeast(0)
                            val cambios = mutableMapOf<String, Any>(
                                "fechaActualizacion" to marca,
                                "cantidadDisponible" to nuevaDisponible,
                                "cantidadDanada" to nuevaDanada,
                                "cantidadExtraviada" to nuevaExtraviada,
                                "estado" to EstadoItem.porCantidad(
                                    nuevaDisponible, actual.cantidadTotal, nuevaDanada, nuevaExtraviada
                                )
                            )
                            if (danadas + extraviadas > 0) cambios["observacion"] = observacionItem
                            tx.update(snapshot.reference, cambios)
                        }

                        linea.copy(
                            cantidadDanada = danadas,
                            cantidadExtraviada = extraviadas,
                            estadoDevolucion = when {
                                danadas > 0 -> EstadoItemPrestamo.DANADO
                                extraviadas > 0 -> EstadoItemPrestamo.EXTRAVIADO
                                else -> EstadoItemPrestamo.BUEN_ESTADO
                            },
                            observacion = observacionItem
                        )
                    } else {
                        // Unidad: buen estado, dañada, incompleta o extraviada
                        val novedad = resultado?.estado?.takeIf { it in EstadoItemPrestamo.NOVEDADES }
                        // Un balón o un peto no regresa "incompleto" (salvo que ya hubiera salido así).
                        if (novedad == EstadoItemPrestamo.INCOMPLETO && actual != null &&
                            !linea.seEntregoIncompleto && !EstadoItem.admiteIncompleto(actual.categoria)
                        ) {
                            throw reglaNegocio(InventarioRepository.mensajeSinIncompleto(actual.categoria))
                        }

                        if (actual != null) {
                            // Sin novedad, la unidad vuelve como se prestó: disponible, o incompleta
                            // con la misma observación si ya había salido incompleta.
                            val estadoNuevo = novedad?.let { EstadoItemPrestamo.estadoInventario(it) }
                                ?: if (linea.seEntregoIncompleto) EstadoItem.INCOMPLETO else EstadoItem.DISPONIBLE
                            val observacionNueva = when {
                                novedad != null -> observacionItem
                                linea.seEntregoIncompleto -> linea.observacionAlPrestar.ifBlank { actual.observacion }
                                else -> ""
                            }
                            tx.update(
                                snapshot.reference,
                                mapOf(
                                    "fechaActualizacion" to marca,
                                    "estado" to estadoNuevo,
                                    "cantidadDisponible" to if (EstadoItem.esPrestable(estadoNuevo)) 1 else 0,
                                    "cantidadDanada" to if (estadoNuevo == EstadoItem.DANADO) 1 else 0,
                                    "cantidadExtraviada" to if (estadoNuevo == EstadoItem.EXTRAVIADO) 1 else 0,
                                    "observacion" to observacionNueva,
                                    "prestamoActivoId" to "",
                                    "estudiantePrestamoId" to "",
                                    "estudiantePrestamoNombre" to ""
                                )
                            )
                        }

                        linea.copy(
                            cantidadDanada = if (novedad == EstadoItemPrestamo.DANADO) 1 else 0,
                            cantidadExtraviada = if (novedad == EstadoItemPrestamo.EXTRAVIADO) 1 else 0,
                            estadoDevolucion = novedad ?: EstadoItemPrestamo.BUEN_ESTADO,
                            observacion = if (novedad == null) "" else observacionItem
                        )
                    }
                }

                val hayNovedades = lineasDevueltas.any { it.tieneNovedad }
                val notas = listOf(prestamo.observaciones, observaciones.trim())
                    .filter { it.isNotBlank() }
                    .joinToString(" | ")

                tx.set(
                    referenciaPrestamo,
                    prestamo.copy(
                        items = lineasDevueltas,
                        estadoDevolucion = EstadoPrestamo.DEVUELTO,
                        fechaEntregaReal = marca,
                        observaciones = notas,
                        danoPendiente = hayNovedades,
                        // El carnet solo se libera si todo regresó en buen estado.
                        carnetRetenido = hayNovedades,
                        documentoRetenido = hayNovedades,
                        monitorDevolucionId = monitor?.id.orEmpty()
                    )
                )

                if (referenciaEstudiante != null && existeEstudiante) {
                    val cambiosEstudiante = mutableMapOf<String, Any>("prestamoActivoId" to "")
                    if (hayNovedades) {
                        cambiosEstudiante["bloqueado"] = true
                        cambiosEstudiante["motivoBloqueo"] =
                            "Devolución con novedades (daño, faltante o pérdida) del ${Fechas.corta(marca.toDate())} " +
                                "pendiente de resolver"
                    }
                    tx.update(referenciaEstudiante, cambiosEstudiante)
                }

                hayNovedades
            }.await()

            Result.success(
                if (huboNovedades) {
                    Idioma.texto(R.string.repo_pre_devolucion_novedades)
                } else {
                    Idioma.texto(R.string.repo_pre_devolucion_ok)
                }
            )
        } catch (e: Exception) {
            falla(e, Idioma.texto(R.string.repo_pre_error_devolucion))
        }
    }

    /**
     * Cierra un caso de novedad (el estudiante repuso, pagó o encontró el artículo): libera
     * el carnet y levanta el bloqueo. El artículo se gestiona aparte, desde el inventario.
     */
    suspend fun resolverDano(prestamoId: String, nota: String = ""): Result<String> {
        val referenciaPrestamo = coleccionPrestamos.document(prestamoId)

        return try {
            db.runTransaction { tx ->
                val snapshotPrestamo = tx.get(referenciaPrestamo)
                val prestamo = snapshotPrestamo.toObject(Prestamo::class.java)?.copy(id = snapshotPrestamo.id)
                    ?: throw reglaNegocio(Idioma.texto(R.string.repo_pre_no_encontrado))
                if (!prestamo.danoPendiente) {
                    throw reglaNegocio(Idioma.texto(R.string.repo_pre_sin_novedades))
                }
                if (prestamo.estudianteId == uidActual()) {
                    throw reglaNegocio(Idioma.texto(R.string.repo_pre_devolucion_propia))
                }
                val referenciaEstudiante = prestamo.estudianteId
                    .takeIf { it.isNotBlank() }
                    ?.let { coleccionUsuarios.document(it) }
                val existeEstudiante = referenciaEstudiante?.let { tx.get(it).exists() } ?: false

                val notas = listOf(prestamo.observaciones, nota.trim().takeIf { it.isNotBlank() }?.let { "Novedad resuelta: $it" }.orEmpty())
                    .filter { it.isNotBlank() }
                    .joinToString(" | ")

                tx.set(
                    referenciaPrestamo,
                    prestamo.copy(
                        danoPendiente = false,
                        carnetRetenido = false,
                        documentoRetenido = false,
                        observaciones = notas
                    )
                )
                if (referenciaEstudiante != null && existeEstudiante) {
                    tx.update(
                        referenciaEstudiante,
                        mapOf(
                            "bloqueado" to false,
                            "motivoBloqueo" to ""
                        )
                    )
                }
                prestamoId
            }.await()
            Result.success(Idioma.texto(R.string.repo_pre_novedad_resuelta))
        } catch (e: Exception) {
            falla(e, Idioma.texto(R.string.repo_pre_error_resolver))
        }
    }

    /** Uid de quien usa la app (el monitor que registra el préstamo o la devolución). */
    private fun uidActual(): String? = FirebaseAuth.getInstance().currentUser?.uid

    private fun describir(item: ItemPrestamo): String =
        if (item.cantidad > 1) "${item.itemId} - ${item.nombre} (x${item.cantidad})"
        else "${item.itemId} - ${item.nombre}"
}
