package com.example.bodegalider.ui.components

import android.app.Activity
import android.content.ContextWrapper
import android.view.View
import android.view.ViewParent
import android.view.Window
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider
import com.example.bodegalider.util.BARCODE_QUIET_ZONE
import com.example.bodegalider.util.encodeBarcode
import kotlin.math.floor

/**
 * Código de barras del producto con su número, para leerlo con una pistola desde la pantalla.
 * Siempre es negro sobre blanco (también en modo oscuro) y sube el brillo al máximo mientras se muestra.
 */
@Composable
fun ScannableBarcode(code: String, modifier: Modifier = Modifier) {
    val barcode = remember(code) { encodeBarcode(code) }
    FullBrightnessWhileVisible()

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (barcode != null) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .clipToBounds()
                ) {
                    // Ancho entero en píxeles por módulo para que las barras queden nítidas
                    val totalModules = barcode.modules.size + 2 * BARCODE_QUIET_ZONE
                    val moduleWidth = floor(size.width / totalModules).coerceAtLeast(1f)
                    val left = floor((size.width - moduleWidth * barcode.modules.size) / 2)
                    barcode.modules.forEachIndexed { index, isBar ->
                        if (isBar) {
                            drawRect(
                                color = Color.Black,
                                topLeft = Offset(left + index * moduleWidth, 0f),
                                size = Size(moduleWidth, size.height)
                            )
                        }
                    }
                }
            }
            Text(
                text = code,
                color = Color.Black,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                letterSpacing = 3.sp
            )
            Text(
                text = barcode?.format ?: "No se pudo generar el código de barras",
                color = Color.DarkGray,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun FullBrightnessWhileVisible() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.findWindow() ?: return@DisposableEffect onDispose {}
        val previous = window.attributes.screenBrightness
        window.setBrightness(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL)
        onDispose { window.setBrightness(previous) }
    }
}

private fun Window.setBrightness(value: Float) {
    attributes = attributes.apply { screenBrightness = value }
}

/** Ventana donde se dibuja la vista: la del diálogo si está dentro de uno, si no la de la Activity. */
private fun View.findWindow(): Window? {
    var current: ViewParent? = parent
    while (current != null) {
        if (current is DialogWindowProvider) return current.window
        current = current.parent
    }
    var ctx = context
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx.window
        ctx = ctx.baseContext
    }
    return null
}
