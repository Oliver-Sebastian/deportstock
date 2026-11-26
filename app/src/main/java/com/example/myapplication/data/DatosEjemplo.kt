package com.example.myapplication.data

/**
 * Inventario de demostración. Ya no se siembra automáticamente: el monitor lo carga
 * desde la pantalla de inventario cuando la colección está vacía.
 */
object DatosEjemplo {

    fun inventario(): List<ItemDeportivo> =
        unidades("BA", 1, 2, "Balón de Fútbol Golty Tradicional #5", "Balones", "⚽", "Césped natural y sintético.") +
        unidades("BA", 3, 2, "Balón de Baloncesto Molten #7", "Balones", "🏀", "Cuero sintético para coliseo.") +
        unidades("BA", 5, 1, "Balón de Voleibol Mikasa MVA200", "Balones", "🏐", "Balón suave de 8 paneles.") +
        unidades("RQ", 1, 2, "Raqueta Tenis de Mesa Butterfly", "Raquetas", "🏓", "Goma roja y negra estándar.") +
        unidades("RQ", 3, 1, "Raqueta de Tenis de Campo Wilson", "Raquetas", "🎾", "Cuerdas tensadas y grip estándar.") +
        unidades("AJ", 1, 1, "Juego de Ajedrez Profesional", "Ajedrez", "♟️", "Tablero enrollable con 32 piezas.") +
        listOf(
            porCantidad("PT-001", 12, "Peto Deportivo Verde Neón", "Petos", "🎽", "Chaleco de entrenamiento transpirable."),
            porCantidad("CN-001", 20, "Cono de Entrenamiento 23cm", "Conos", "🔶", "Cono flexible de alta visibilidad.")
        )

    /** Genera [cantidad] unidades individuales con códigos consecutivos a partir de [desde]. */
    private fun unidades(
        prefijo: String,
        desde: Int,
        cantidad: Int,
        nombre: String,
        categoria: String,
        icono: String,
        descripcion: String
    ): List<ItemDeportivo> = (0 until cantidad).map { indice ->
        ItemDeportivo(
            id = "$prefijo-%03d".format(desde + indice),
            nombre = if (cantidad > 1) "$nombre (Unidad ${indice + 1})" else nombre,
            categoria = categoria,
            icono = icono,
            descripcion = descripcion,
            tipoControl = TipoControl.UNIDAD
        )
    }

    private fun porCantidad(
        id: String,
        total: Int,
        nombre: String,
        categoria: String,
        icono: String,
        descripcion: String
    ): ItemDeportivo = ItemDeportivo(
        id = id,
        nombre = nombre,
        categoria = categoria,
        icono = icono,
        descripcion = descripcion,
        tipoControl = TipoControl.CANTIDAD,
        cantidadTotal = total,
        cantidadDisponible = total
    )
}
