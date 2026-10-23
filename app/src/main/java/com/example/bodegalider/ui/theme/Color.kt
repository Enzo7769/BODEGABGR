package com.example.bodegalider.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Paleta de marca: azul corporativo + amarillo de acento
val BrandBlue = Color(0xFF0071CE)
val BrandBlueDark = Color(0xFF004C91)
val BrandYellow = Color(0xFFFFC220)

// Esquema claro
val LightPrimary = Color(0xFF0061B2)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFD4E3FF)
val LightOnPrimaryContainer = Color(0xFF001C3A)
val LightSecondary = Color(0xFF735B00)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFFFDF90)
val LightOnSecondaryContainer = Color(0xFF241A00)
val LightBackground = Color(0xFFF6F8FC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE0E6F0)
val LightOnSurface = Color(0xFF1A1C20)
val LightOnSurfaceVariant = Color(0xFF43474F)
val LightOutline = Color(0xFF737780)
val LightOutlineVariant = Color(0xFFC3C7D0)

// Esquema oscuro
val DarkPrimary = Color(0xFFA5C8FF)
val DarkOnPrimary = Color(0xFF00315F)
val DarkPrimaryContainer = Color(0xFF004786)
val DarkOnPrimaryContainer = Color(0xFFD4E3FF)
val DarkSecondary = Color(0xFFF1C14A)
val DarkOnSecondary = Color(0xFF3D2E00)
val DarkSecondaryContainer = Color(0xFF574400)
val DarkOnSecondaryContainer = Color(0xFFFFDF90)
val DarkBackground = Color(0xFF111318)
val DarkSurface = Color(0xFF181B21)
val DarkSurfaceVariant = Color(0xFF2B2F37)
val DarkOnSurface = Color(0xFFE2E2E9)
val DarkOnSurfaceVariant = Color(0xFFC3C6CF)
val DarkOutline = Color(0xFF8D9199)
val DarkOutlineVariant = Color(0xFF43474E)

/**
 * Colores semánticos de estado (éxito / advertencia) que Material 3 no trae.
 * El rojo de "sin stock" usa los colores error del esquema.
 */
@Immutable
data class StatusColors(
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color
)

val LightStatusColors = StatusColors(
    success = Color(0xFF1B7F3B),
    successContainer = Color(0xFFD4F5DC),
    onSuccessContainer = Color(0xFF00391A),
    warning = Color(0xFFB25E00),
    warningContainer = Color(0xFFFFE3C2),
    onWarningContainer = Color(0xFF3D1F00)
)

val DarkStatusColors = StatusColors(
    success = Color(0xFF7DDB97),
    successContainer = Color(0xFF0E4D26),
    onSuccessContainer = Color(0xFFD4F5DC),
    warning = Color(0xFFFFB870),
    warningContainer = Color(0xFF5A3200),
    onWarningContainer = Color(0xFFFFE3C2)
)
