package com.example.myapplication.data

import com.example.myapplication.R
import com.example.myapplication.utils.Idioma
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Acceso a la colección `inventario`.
 *
 * Firestore es la única fuente de verdad: su SDK ya mantiene una caché local en disco,
 * así que la lista sigue visible sin conexión. Los errores de escritura se devuelven
 * al ViewModel en lugar de ocultarse.
 */
class InventarioRepository {

    private val db = FirebaseFirestore.getInstance()
    private val coleccionInventario = db.collection("inventario")

    /**
     * Escucha en tiempo real los implementos deportivos.
     * @param incluirInactivos true para recibir también los dados de baja
     * (hace falta para no reutilizar sus códigos al generar uno nuevo).
     */
    fun obtenerInventarioFlow(incluirInactivos: Boolean = false): Flow<List<ItemDeportivo>> = callbackFlow {
        val registro = coleccionInventario.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val items = snapshot?.documents.orEmpty()
                .mapNotNull { doc -> doc.toObject(ItemDeportivo::class.java)?.copy(id = doc.id) }
                .filter { incluirInactivos || it.activo }
                .sortedWith(compareBy({ it.categoria }, { it.id }))
            trySend(items)
        }
        awaitClose { registro.remove() }
    }

    /**
     * RF-04: registra uno o varios artículos nuevos. Si alguno de los códigos ya existe
     * no se crea ninguno, para no sobrescribir un artículo por accidente.
     */
    suspend fun crearItems(items: List<ItemDeportivo>): Result<String> {
        if (items.isEmpty()) {
            return Result.failure(IllegalArgumentException(Idioma.texto(R.string.repo_inv_sin_articulos)))
        }
        val ahora = Timestamp.now()
        val nuevos = items.map { normalizarNuevo(it, ahora) }
        if (nuevos.any { it.id.isBlank() }) {
            return Result.failure(IllegalArgumentException(Idioma.texto(R.string.repo_inv_codigo_vacio)))
        }
        nuevos.firstOrNull { it.estado == EstadoItem.INCOMPLETO && !EstadoItem.admiteIncompleto(it.categoria) }?.let {
            return Result.failure(IllegalArgumentException(mensajeSinIncompleto(it.categoria)))
        }

        return try {
            db.runTransaction { tx ->
                val referencias = nuevos.map { coleccionInventario.document(it.id) }
                val existentes = referencias.map { tx.get(it) }
                existentes.firstOrNull { it.exists() }?.let { duplicado ->
                    throw reglaNegocio(Idioma.texto(R.string.repo_inv_codigo_duplicado, duplicado.id))
                }
                nuevos.forEachIndexed { indice, item -> tx.set(referencias[indice], item) }
                nuevos.size
            }.await()

            val mensaje = if (nuevos.size == 1) {
                Idioma.texto(R.string.repo_inv_registrado, nuevos.first().id)
            } else {
                Idioma.texto(R.string.repo_inv_registrados, nuevos.size, nuevos.first().id, nuevos.last().id)
            }
            Result.success(mensaje)
        } catch (e: Exception) {
            falla(e, Idioma.texto(R.string.repo_inv_error_registrar))
        }
    }

    /**
     * RF-05: edita un artículo existente. Solo se tocan los campos editables; el estado
     * PRESTADO y las cantidades prestadas los maneja exclusivamente el flujo de préstamos.
     */
    suspend fun actualizarItem(item: ItemDeportivo): Result<String> {
        val id = item.id.trim().uppercase()
        val referencia = coleccionInventario.document(id)

        return try {
            db.runTransaction { tx ->
                val snapshot = tx.get(referencia)
                val actual = snapshot.toObject(ItemDeportivo::class.java)
                    ?: throw reglaNegocio(Idioma.texto(R.string.comun_articulo_no_existe, id))

                val cambios = mutableMapOf<String, Any>(
                    "nombre" to item.nombre.trim(),
                    "categoria" to item.categoria.trim(),
                    "icono" to item.icono,
                    "descripcion" to item.descripcion.trim(),
                    "fechaActualizacion" to Timestamp.now()
                )

                if (actual.esPorCantidad) {
                    val comprometidas = actual.cantidadPrestada + actual.cantidadDanada + actual.cantidadExtraviada
                    val nuevoTotal = item.cantidadTotal
                    if (nuevoTotal < comprometidas || nuevoTotal < 1) {
                        throw reglaNegocio(Idioma.texto(R.string.repo_inv_cantidad_menor, comprometidas))
                    }
                    val nuevaDisponible = nuevoTotal - comprometidas
                    cambios["cantidadTotal"] = nuevoTotal
                    cambios["cantidadDisponible"] = nuevaDisponible
                    cambios["estado"] = EstadoItem.porCantidad(
                        nuevaDisponible, nuevoTotal, actual.cantidadDanada, actual.cantidadExtraviada
                    )
                } else {
                    val estaPrestado = actual.estado.equals(EstadoItem.PRESTADO, ignoreCase = true)
                    val pidePrestado = item.estado.equals(EstadoItem.PRESTADO, ignoreCase = true)
                    if (estaPrestado && !pidePrestado) {
                        throw reglaNegocio(Idioma.texto(R.string.repo_inv_unidad_prestada_cambiar, id))
                    }
                    if (!estaPrestado && pidePrestado) {
                        throw reglaNegocio(Idioma.texto(R.string.repo_inv_estado_prestado))
                    }
                    // Balones y petos no quedan incompletos (sí se conserva si ya lo estaba).
                    val pasaAIncompleto = item.estado.equals(EstadoItem.INCOMPLETO, ignoreCase = true) &&
                        !actual.estado.equals(EstadoItem.INCOMPLETO, ignoreCase = true)
                    if (pasaAIncompleto && !EstadoItem.admiteIncompleto(item.categoria)) {
                        throw reglaNegocio(mensajeSinIncompleto(item.categoria))
                    }
                    if (!estaPrestado) {
                        val disponible = item.estado.equals(EstadoItem.DISPONIBLE, ignoreCase = true)
                        cambios["estado"] = item.estado
                        cambios["cantidadDisponible"] = if (EstadoItem.esPrestable(item.estado)) 1 else 0
                        cambios["cantidadDanada"] =
                            if (item.estado.equals(EstadoItem.DANADO, ignoreCase = true)) 1 else 0
                        cambios["cantidadExtraviada"] =
                            if (item.estado.equals(EstadoItem.EXTRAVIADO, ignoreCase = true)) 1 else 0
                        if (disponible) cambios["observacion"] = ""
                    }
                }

                tx.update(referencia, cambios)
                id
            }.await()
            Result.success(Idioma.texto(R.string.repo_inv_actualizado, id))
        } catch (e: Exception) {
            falla(e, Idioma.texto(R.string.repo_inv_error_actualizar))
        }
    }

    /**
     * RF-05: da de baja un artículo (baja lógica). El registro se conserva para que el
     * historial de préstamos siga siendo consistente. No se permite si está prestado.
     */
    suspend fun darDeBaja(id: String): Result<String> {
        val idLimpio = id.trim().uppercase()
        val referencia = coleccionInventario.document(idLimpio)

        return try {
            db.runTransaction { tx ->
                val actual = tx.get(referencia).toObject(ItemDeportivo::class.java)
                    ?: throw reglaNegocio(Idioma.texto(R.string.comun_articulo_no_existe, idLimpio))
                if (actual.estaPrestado) {
                    throw reglaNegocio(Idioma.texto(R.string.repo_inv_baja_prestado, idLimpio))
                }
                tx.update(
                    referencia,
                    mapOf(
                        "activo" to false,
                        "fechaActualizacion" to Timestamp.now()
                    )
                )
                idLimpio
            }.await()
            Result.success(Idioma.texto(R.string.repo_inv_dado_de_baja, idLimpio))
        } catch (e: Exception) {
            falla(e, Idioma.texto(R.string.repo_inv_error_baja))
        }
    }

    /**
     * RF-07: registra una novedad con su observación: dañado, incompleto o extraviado.
     * - Unidad: la unidad pasa a ese estado ([cantidad] no se usa).
     * - Cantidad: [cantidad] piezas disponibles pasan a dañadas o extraviadas
     *   ("incompleto" no aplica a piezas sueltas como conos o petos).
     */
    suspend fun marcarNovedad(id: String, tipo: String, cantidad: Int, observacion: String): Result<String> {
        val idLimpio = id.trim().uppercase()
        val referencia = coleccionInventario.document(idLimpio)
        if (!EstadoItem.esNovedad(tipo)) {
            return Result.failure(IllegalArgumentException(Idioma.texto(R.string.repo_inv_tipo_invalido, tipo)))
        }

        return try {
            db.runTransaction { tx ->
                val actual = tx.get(referencia).toObject(ItemDeportivo::class.java)
                    ?: throw reglaNegocio(Idioma.texto(R.string.comun_articulo_no_existe, idLimpio))

                val cambios = mutableMapOf<String, Any>(
                    "observacion" to observacion.trim(),
                    "fechaActualizacion" to Timestamp.now()
                )

                if (actual.esPorCantidad) {
                    if (tipo == EstadoItem.INCOMPLETO) {
                        throw reglaNegocio(Idioma.texto(R.string.repo_inv_incompleto_cantidad))
                    }
                    if (cantidad < 1 || cantidad > actual.cantidadDisponible) {
                        throw reglaNegocio(Idioma.plural(R.plurals.repo_inv_solo_hay_disponibles, actual.cantidadDisponible, actual.cantidadDisponible))
                    }
                    val nuevaDisponible = actual.cantidadDisponible - cantidad
                    val nuevaDanada = actual.cantidadDanada + if (tipo == EstadoItem.DANADO) cantidad else 0
                    val nuevaExtraviada = actual.cantidadExtraviada + if (tipo == EstadoItem.EXTRAVIADO) cantidad else 0
                    cambios["cantidadDisponible"] = nuevaDisponible
                    cambios["cantidadDanada"] = nuevaDanada
                    cambios["cantidadExtraviada"] = nuevaExtraviada
                    cambios["estado"] = EstadoItem.porCantidad(nuevaDisponible, actual.cantidadTotal, nuevaDanada, nuevaExtraviada)
                } else {
                    if (actual.estado.equals(EstadoItem.PRESTADO, ignoreCase = true)) {
                        throw reglaNegocio(Idioma.texto(R.string.repo_inv_unidad_prestada_novedad, idLimpio))
                    }
                    if (tipo == EstadoItem.INCOMPLETO && !EstadoItem.admiteIncompleto(actual.categoria)) {
                        throw reglaNegocio(mensajeSinIncompleto(actual.categoria))
                    }
                    cambios["estado"] = tipo
                    // Un incompleto se sigue prestando; dañado y extraviado no.
                    cambios["cantidadDisponible"] = if (EstadoItem.esPrestable(tipo)) 1 else 0
                    cambios["cantidadDanada"] = if (tipo == EstadoItem.DANADO) 1 else 0
                    cambios["cantidadExtraviada"] = if (tipo == EstadoItem.EXTRAVIADO) 1 else 0
                }

                tx.update(referencia, cambios)
                idLimpio
            }.await()
            Result.success(Idioma.texto(R.string.repo_inv_marcado, idLimpio, EstadoItem.etiqueta(tipo).lowercase()))
        } catch (e: Exception) {
            falla(e, Idioma.texto(R.string.novedad_error_registrar))
        }
    }

    /**
     * Devuelve al inventario disponible lo que tenía novedad: la unidad reparada, completada
     * o encontrada; o, en control por cantidad, [cantidad] piezas del [tipo] indicado
     * (dañadas reparadas o extraviadas encontradas).
     */
    suspend fun resolverNovedad(id: String, tipo: String, cantidad: Int): Result<String> {
        val idLimpio = id.trim().uppercase()
        val referencia = coleccionInventario.document(idLimpio)

        return try {
            db.runTransaction { tx ->
                val actual = tx.get(referencia).toObject(ItemDeportivo::class.java)
                    ?: throw reglaNegocio(Idioma.texto(R.string.comun_articulo_no_existe, idLimpio))

                val cambios = mutableMapOf<String, Any>("fechaActualizacion" to Timestamp.now())

                if (actual.esPorCantidad) {
                    val registradas = if (tipo == EstadoItem.EXTRAVIADO) actual.cantidadExtraviada else actual.cantidadDanada
                    if (cantidad < 1 || cantidad > registradas) {
                        val mensaje = if (tipo == EstadoItem.EXTRAVIADO) R.plurals.repo_inv_solo_hay_extraviadas else R.plurals.repo_inv_solo_hay_danadas
                        throw reglaNegocio(Idioma.plural(mensaje, registradas, registradas))
                    }
                    val nuevaDanada = actual.cantidadDanada - if (tipo == EstadoItem.EXTRAVIADO) 0 else cantidad
                    val nuevaExtraviada = actual.cantidadExtraviada - if (tipo == EstadoItem.EXTRAVIADO) cantidad else 0
                    val nuevaDisponible = actual.cantidadDisponible + cantidad
                    cambios["cantidadDanada"] = nuevaDanada
                    cambios["cantidadExtraviada"] = nuevaExtraviada
                    cambios["cantidadDisponible"] = nuevaDisponible
                    cambios["estado"] = EstadoItem.porCantidad(nuevaDisponible, actual.cantidadTotal, nuevaDanada, nuevaExtraviada)
                    if (nuevaDanada == 0 && nuevaExtraviada == 0) cambios["observacion"] = ""
                } else {
                    if (actual.estado.equals(EstadoItem.PRESTADO, ignoreCase = true)) {
                        throw reglaNegocio(Idioma.texto(R.string.repo_inv_unidad_prestada, idLimpio))
                    }
                    cambios["estado"] = EstadoItem.DISPONIBLE
                    cambios["cantidadDisponible"] = 1
                    cambios["cantidadDanada"] = 0
                    cambios["cantidadExtraviada"] = 0
                    cambios["observacion"] = ""
                }

                tx.update(referencia, cambios)
                idLimpio
            }.await()
            Result.success(Idioma.texto(R.string.repo_inv_disponible, idLimpio))
        } catch (e: Exception) {
            falla(e, Idioma.texto(R.string.repo_inv_error_actualizar))
        }
    }

    /**
     * Control por cantidad: da por perdidas las piezas extraviadas y las descuenta del total.
     * (Una unidad extraviada se da de baja con el botón de baja.)
     */
    suspend fun descontarExtraviadas(id: String): Result<String> {
        val idLimpio = id.trim().uppercase()
        val referencia = coleccionInventario.document(idLimpio)

        return try {
            val descontadas = db.runTransaction { tx ->
                val actual = tx.get(referencia).toObject(ItemDeportivo::class.java)
                    ?: throw reglaNegocio(Idioma.texto(R.string.comun_articulo_no_existe, idLimpio))
                if (!actual.esPorCantidad || actual.cantidadExtraviada < 1) {
                    throw reglaNegocio(Idioma.texto(R.string.repo_inv_sin_extraviadas))
                }
                val nuevoTotal = actual.cantidadTotal - actual.cantidadExtraviada
                val cambios = mutableMapOf<String, Any>(
                    "cantidadTotal" to nuevoTotal,
                    "cantidadExtraviada" to 0,
                    "estado" to EstadoItem.porCantidad(actual.cantidadDisponible, nuevoTotal, actual.cantidadDanada, 0),
                    "fechaActualizacion" to Timestamp.now()
                )
                if (actual.cantidadDanada == 0) cambios["observacion"] = ""
                tx.update(referencia, cambios)
                actual.cantidadExtraviada
            }.await()
            Result.success(Idioma.plural(R.plurals.repo_inv_descontadas, descontadas, descontadas, idLimpio))
        } catch (e: Exception) {
            falla(e, Idioma.texto(R.string.repo_inv_error_descontar))
        }
    }

    /**
     * Carga un inventario de demostración. Solo lo ejecuta el monitor, a petición,
     * y únicamente si la colección está vacía.
     */
    suspend fun cargarDatosEjemplo(): Result<String> {
        return try {
            val hayDatos = !coleccionInventario.limit(1).get().await().isEmpty
            if (hayDatos) {
                return Result.failure(IllegalStateException(Idioma.texto(R.string.repo_inv_ya_tiene)))
            }
            val ahora = Timestamp.now()
            val lote = db.batch()
            val ejemplos = DatosEjemplo.inventario().map { normalizarNuevo(it, ahora) }
            ejemplos.forEach { item -> lote.set(coleccionInventario.document(item.id), item) }
            lote.commit().await()
            Result.success(Idioma.texto(R.string.repo_inv_ejemplo_cargado, ejemplos.size))
        } catch (e: Exception) {
            falla(e, Idioma.texto(R.string.repo_inv_error_ejemplo))
        }
    }

    /** Deja un artículo nuevo con cantidades y estado coherentes según su tipo de control. */
    private fun normalizarNuevo(item: ItemDeportivo, ahora: Timestamp): ItemDeportivo {
        val base = item.copy(
            id = item.id.trim().uppercase(),
            nombre = item.nombre.trim(),
            categoria = item.categoria.trim(),
            descripcion = item.descripcion.trim(),
            estudiantePrestamoId = "",
            estudiantePrestamoNombre = "",
            prestamoActivoId = "",
            activo = true,
            fechaCreacion = ahora,
            fechaActualizacion = ahora
        )
        return if (base.esPorCantidad) {
            val total = base.cantidadTotal.coerceAtLeast(1)
            base.copy(
                cantidadTotal = total,
                cantidadDisponible = total,
                cantidadDanada = 0,
                cantidadExtraviada = 0,
                estado = EstadoItem.DISPONIBLE
            )
        } else {
            // Un artículo nuevo nunca nace prestado.
            val estado = if (base.estado.equals(EstadoItem.PRESTADO, ignoreCase = true)) {
                EstadoItem.DISPONIBLE
            } else {
                base.estado
            }
            base.copy(
                estado = estado,
                cantidadTotal = 1,
                cantidadDisponible = if (EstadoItem.esPrestable(estado)) 1 else 0,
                cantidadDanada = if (estado.equals(EstadoItem.DANADO, ignoreCase = true)) 1 else 0,
                cantidadExtraviada = if (estado.equals(EstadoItem.EXTRAVIADO, ignoreCase = true)) 1 else 0
            )
        }
    }

    companion object {
        /** Mensaje cuando se intenta marcar incompleto un artículo de una sola pieza. */
        fun mensajeSinIncompleto(categoria: String): String =
            Idioma.texto(R.string.repo_inv_una_pieza, Categorias.etiqueta(categoria))

        /**
         * Determina el prefijo según categoría y nombre (BA-, RQ-, PT-, CN-, AJ-, etc.)
         */
        fun determinarPrefijo(categoria: String, nombre: String): String {
            val texto = "$categoria $nombre".uppercase()
            return when {
                texto.contains("FÚTBOL") || texto.contains("FUTBOL") ||
                texto.contains("BASKET") || texto.contains("BALONCESTO") ||
                texto.contains("VOLEIBOL") || texto.contains("VOLEY") ||
                texto.contains("BALÓN") || texto.contains("BALON") ||
                texto.contains("PELOTA") -> "BA"

                texto.contains("RAQUETA") || texto.contains("TENIS") ||
                texto.contains("PING") || texto.contains("MESA") ||
                texto.contains("BADMINTON") -> "RQ"

                texto.contains("PETO") || texto.contains("CHALECO") ||
                texto.contains("CAMISETA") -> "PT"

                texto.contains("CONO") || texto.contains("PLATILLO") ||
                texto.contains("ENTRENAMIENTO") -> "CN"

                texto.contains("AJEDREZ") || texto.contains("TABLERO") ||
                texto.contains("FICHAS") -> "AJ"

                texto.contains("LAZO") || texto.contains("CUERDA") ||
                texto.contains("SALTO") -> "LZ"

                texto.contains("COLCHONETA") || texto.contains("ESTERILLA") ||
                texto.contains("TAPETE") -> "CL"

                texto.contains("CRONÓMETRO") || texto.contains("CRONOMETRO") ||
                texto.contains("RELOJ") -> "CR"

                else -> {
                    val limpia = categoria.filter { it.isLetter() }.uppercase()
                    if (limpia.length >= 2) limpia.take(2) else "OT"
                }
            }
        }

        /**
         * Genera automáticamente el siguiente código secuencial disponible para la categoría dada.
         * Permite un offset para secuencias continuas en lotes de artículos.
         */
        fun generarSiguienteId(
            categoria: String,
            nombre: String,
            itemsExistentes: List<ItemDeportivo>,
            offset: Int = 0
        ): String {
            val prefijo = determinarPrefijo(categoria, nombre)
            val regex = Regex("""^${Regex.escape(prefijo)}-(\d+)$""")

            val numerosExistentes = itemsExistentes.mapNotNull { item ->
                regex.find(item.id.trim())?.groupValues?.get(1)?.toIntOrNull()
            }

            val baseNumero = if (numerosExistentes.isEmpty()) {
                1
            } else {
                (numerosExistentes.maxOrNull() ?: 0) + 1
            }

            val siguienteNumero = baseNumero + offset
            return "$prefijo-%03d".format(siguienteNumero)
        }

        /**
         * Genera una lista de códigos secuenciales para registrar N artículos individuales a la vez.
         */
        fun generarSecuenciaIds(
            categoria: String,
            nombre: String,
            cantidad: Int,
            itemsExistentes: List<ItemDeportivo>
        ): List<String> {
            val cant = cantidad.coerceAtLeast(1)
            return (0 until cant).map { offset ->
                generarSiguienteId(categoria, nombre, itemsExistentes, offset)
            }
        }

        /**
         * Asigna un ícono/emoji deportivo representativo según la categoría o nombre.
         */
        fun iconoRepresentativo(categoria: String, nombre: String = ""): String {
            val texto = "$categoria $nombre".lowercase()
            return when {
                texto.contains("fútbol") || texto.contains("futbol") -> "⚽"
                texto.contains("baloncesto") || texto.contains("basket") -> "🏀"
                texto.contains("voleibol") || texto.contains("voley") -> "🏐"
                texto.contains("balón") || texto.contains("balon") -> "⚽"
                texto.contains("tenis de mesa") || texto.contains("ping pong") -> "🏓"
                texto.contains("raqueta") || texto.contains("tenis") -> "🎾"
                texto.contains("peto") || texto.contains("chaleco") -> "🎽"
                texto.contains("cono") -> "🔶"
                texto.contains("ajedrez") -> "♟️"
                texto.contains("lazo") || texto.contains("cuerda") -> "🪢"
                texto.contains("cronómetro") || texto.contains("cronometro") -> "⏱️"
                texto.contains("colchoneta") -> "🧘"
                else -> "🏅"
            }
        }
    }
}
