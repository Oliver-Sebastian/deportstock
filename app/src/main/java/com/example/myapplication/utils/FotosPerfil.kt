package com.example.myapplication.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.example.myapplication.R
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Base64

/**
 * Prepara la foto de perfil para guardarla en Firestore sin usar Cloud Storage for Firebase,
 * que desde 2024 exige el plan de pago Blaze; Firestore sí funciona en el plan gratuito.
 *
 * La foto se recorta al centro en un cuadrado, se reduce a [TAMANO] px y se comprime en
 * JPEG: queda en unos 10 a 30 KB, muy por debajo del límite de 1 MB de un documento.
 */
object FotosPerfil {

    const val TAMANO = 256
    private const val CALIDAD_JPEG = 80

    /** Lee una imagen elegida en la galería, la endereza y la deja lista para guardar. */
    fun desdeUri(contexto: Context, uri: Uri): String {
        val resolver = contexto.contentResolver

        // 1. Solo las dimensiones, para no cargar en memoria una foto de 12 MP completa.
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, limites) }
        val ladoMenor = minOf(limites.outWidth, limites.outHeight)
        if (ladoMenor <= 0) throw IOException(Idioma.texto(R.string.foto_error_leer))
        var muestreo = 1
        while (ladoMenor / (muestreo * 2) >= TAMANO) muestreo *= 2

        // 2. La imagen reducida.
        val opciones = BitmapFactory.Options().apply { inSampleSize = muestreo }
        val imagen = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }
            ?: throw IOException(Idioma.texto(R.string.foto_error_leer))

        // 3. Las fotos de la cámara guardan la orientación aparte (EXIF): se aplica aquí.
        val grados = try {
            resolver.openInputStream(uri)?.use { gradosDeRotacion(ExifInterface(it)) } ?: 0
        } catch (e: Exception) {
            0
        }
        return desdeBitmap(rotar(imagen, grados))
    }

    /** Recorta, reduce y comprime. También sirve para la foto tomada con la cámara. */
    fun desdeBitmap(imagen: Bitmap): String {
        val lado = minOf(imagen.width, imagen.height)
        val cuadrada = Bitmap.createBitmap(imagen, (imagen.width - lado) / 2, (imagen.height - lado) / 2, lado, lado)
        val reducida = Bitmap.createScaledBitmap(cuadrada, TAMANO, TAMANO, true)
        val bytes = ByteArrayOutputStream().use { salida ->
            reducida.compress(Bitmap.CompressFormat.JPEG, CALIDAD_JPEG, salida)
            salida.toByteArray()
        }
        return Base64.getEncoder().encodeToString(bytes)
    }

    /** Convierte lo guardado en Firestore en una imagen; null si el texto está dañado. */
    fun decodificar(base64: String): Bitmap? = try {
        val bytes = Base64.getDecoder().decode(base64)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: IllegalArgumentException) {
        null
    }

    private fun gradosDeRotacion(exif: ExifInterface): Int =
        when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }

    private fun rotar(imagen: Bitmap, grados: Int): Bitmap {
        if (grados == 0) return imagen
        val matriz = Matrix().apply { postRotate(grados.toFloat()) }
        return Bitmap.createBitmap(imagen, 0, 0, imagen.width, imagen.height, matriz, true)
    }
}
