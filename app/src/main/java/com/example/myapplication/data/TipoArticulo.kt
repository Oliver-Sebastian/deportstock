package com.example.myapplication.data

import java.text.Normalizer

/**
 * Tipo de implemento, para mostrar su foto en lugar del emoji (ver `ui/components/ImagenArticulo.kt`).
 *
 * Se reconoce en este orden:
 * 1. por el nombre ("Balón de Baloncesto Molten #7", "Raqueta Tenis de Mesa Butterfly"...);
 * 2. por el emoji guardado en `icono` ("⚽", "🏀"...), para nombres sin el deporte ("Balón Golty");
 * 3. por la categoría (Balones, Raquetas, Petos, Conos, Ajedrez).
 *
 * Los datos guardados en Firestore no cambian: el campo `icono` sigue igual. Si no se reconoce
 * (por ejemplo, un artículo nuevo de "Otros"), la app muestra el emoji guardado, como antes.
 */
enum class TipoArticulo {
    BALON_FUTBOL,
    BALON_BALONCESTO,
    BALON_VOLEIBOL,
    RAQUETA_TENIS_MESA,
    RAQUETA_TENIS_CAMPO,
    PETO,
    CONO,
    AJEDREZ,
    CUERDA,
    CRONOMETRO,
    COLCHONETA;

    companion object {

        fun de(nombre: String, categoria: String = "", icono: String = ""): TipoArticulo? =
            porNombre(nombre) ?: porIcono(icono) ?: porNombre(categoria) ?: deCategoria(categoria)

        /** Foto de cada categoría de base (filtros y formulario del inventario); "Otros" no tiene. */
        fun deCategoria(categoria: String): TipoArticulo? = when (categoria) {
            "Balones" -> BALON_FUTBOL
            "Raquetas" -> RAQUETA_TENIS_MESA
            "Petos" -> PETO
            "Conos" -> CONO
            "Ajedrez" -> AJEDREZ
            else -> null
        }

        private fun porNombre(texto: String): TipoArticulo? {
            val t = normalizar(texto)
            if (t.isBlank()) return null
            val palabras = t.split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }.toSet()
            fun palabra(vararg claves: String) = claves.any { it in palabras }
            return when {
                "futbol" in t || "soccer" in t || "futsal" in t -> BALON_FUTBOL
                "baloncesto" in t || "basket" in t || "basquet" in t -> BALON_BALONCESTO
                "volei" in t || "voley" in t || "volley" in t -> BALON_VOLEIBOL
                "tenis de mesa" in t || "ping pong" in t || "pingpong" in t || "pimpon" in t ||
                    (palabra("raqueta", "raquetas", "paleta", "paletas") && palabra("mesa")) -> RAQUETA_TENIS_MESA
                "tenis" in t -> RAQUETA_TENIS_CAMPO
                palabra("peto", "petos", "chaleco", "chalecos") -> PETO
                palabra("cono", "conos") -> CONO
                "ajedrez" in t -> AJEDREZ
                palabra("cuerda", "cuerdas", "lazo", "lazos", "soga", "sogas") || "saltar" in t -> CUERDA
                "cronometro" in t -> CRONOMETRO
                "colchoneta" in t -> COLCHONETA
                else -> null
            }
        }

        private fun porIcono(icono: String): TipoArticulo? = when {
            "⚽" in icono -> BALON_FUTBOL
            "🏀" in icono -> BALON_BALONCESTO
            "🏐" in icono -> BALON_VOLEIBOL
            "🏓" in icono -> RAQUETA_TENIS_MESA
            "🎾" in icono || "🏸" in icono -> RAQUETA_TENIS_CAMPO
            "🎽" in icono -> PETO
            "🔶" in icono -> CONO
            "♟" in icono -> AJEDREZ
            "🪢" in icono -> CUERDA
            "⏱" in icono -> CRONOMETRO
            "🧘" in icono -> COLCHONETA
            else -> null
        }

        /** Minúsculas y sin tildes: "Balón de Fútbol" → "balon de futbol". */
        private fun normalizar(texto: String): String =
            Normalizer.normalize(texto.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
    }
}
