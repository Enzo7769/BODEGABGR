package com.example.bodegalider.util

import com.google.zxing.oned.Code128Writer
import com.google.zxing.oned.EAN13Writer
import com.google.zxing.oned.EAN8Writer
import com.google.zxing.oned.OneDimensionalCodeWriter

/** Código de barras listo para dibujar: cada elemento de [modules] es una barra (true) o un espacio (false). */
class EncodedBarcode(val format: String, val modules: BooleanArray)

/** Módulos en blanco a cada lado; los lectores necesitan este margen para detectar el código. */
const val BARCODE_QUIET_ZONE = 10

/**
 * Genera las barras del código del producto. Usa el mismo formato que trae el envase
 * (EAN-13, UPC-A o EAN-8) cuando el número es válido para ese formato, y Code 128 en otro caso.
 * Devuelve null si el código está vacío o no se puede codificar.
 */
fun encodeBarcode(code: String): EncodedBarcode? {
    val value = code.trim()
    if (value.isEmpty()) return null

    // Formato, writer y texto a codificar. Un UPC-A son las mismas barras que un EAN-13 que empieza con 0.
    val candidates = buildList<Triple<String, OneDimensionalCodeWriter, String>> {
        if (value.all(Char::isDigit)) {
            when (value.length) {
                13 -> add(Triple("EAN-13", EAN13Writer(), value))
                12 -> add(Triple("UPC-A", EAN13Writer(), "0$value"))
                8 -> add(Triple("EAN-8", EAN8Writer(), value))
            }
        }
        add(Triple("Code 128", Code128Writer(), value))
    }

    for ((format, writer, contents) in candidates) {
        // Los writers de EAN/UPC rechazan un dígito verificador incorrecto; en ese caso se usa Code 128
        val modules = runCatching { writer.encode(contents) }.getOrNull() ?: continue
        return EncodedBarcode(format, modules)
    }
    return null
}
