package com.example.bodegalider.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.bodegalider.data.ProductPhotos
import com.example.bodegalider.model.CloudPhoto
import com.example.bodegalider.model.StockStatus
import com.example.bodegalider.model.User
import com.example.bodegalider.ui.theme.statusColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Encabezado estándar de cada pestaña: título, subtítulo y acciones opcionales a la derecha. */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        actions()
    }
}

/** Par de colores (fondo, contenido) asociado a cada estado de stock. */
data class StatusPalette(val container: Color, val content: Color, val accent: Color)

@Composable
fun stockStatusPalette(status: StockStatus): StatusPalette {
    val scheme = MaterialTheme.colorScheme
    val extra = MaterialTheme.statusColors
    return when (status) {
        StockStatus.OUT_OF_STOCK -> StatusPalette(scheme.errorContainer, scheme.onErrorContainer, scheme.error)
        StockStatus.LOW -> StatusPalette(extra.warningContainer, extra.onWarningContainer, extra.warning)
        StockStatus.OK -> StatusPalette(extra.successContainer, extra.onSuccessContainer, extra.success)
    }
}

@Composable
fun StatusBadge(status: StockStatus, modifier: Modifier = Modifier) {
    val palette = stockStatusPalette(status)
    Pill(text = status.label, container = palette.container, content = palette.content, modifier = modifier)
}

@Composable
fun Pill(text: String, container: Color, content: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = content,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}

/**
 * Foto del producto. Carga en segundo plano la foto guardada en la nube, la del catálogo
 * o la recién elegida en este equipo; si no hay foto (o no se puede leer) muestra un ícono neutro.
 */
@Composable
fun ProductImage(
    imageUri: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp
) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, imageUri) {
        value = if (imageUri.isBlank()) null else loadThumbnail(context, imageUri)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.Inventory2,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(size * 0.45f)
            )
        }
    }
}

private suspend fun loadThumbnail(context: Context, uriString: String, targetPx: Int = 320): ImageBitmap? {
    val cloudPhoto = CloudPhoto.parse(uriString)
    if (cloudPhoto != null) {
        val bytes = ProductPhotos(context).load(cloudPhoto) ?: return null
        return withContext(Dispatchers.Default) {
            decodeSampled(targetPx) { options -> BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) }
        }
    }
    return withContext(Dispatchers.IO) {
        val uri = Uri.parse(uriString)
        decodeSampled(targetPx) { options ->
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        }
    }
}

/** Decodifica la imagen reducida a cerca de [targetPx] para no cargar fotos enteras en memoria. */
private fun decodeSampled(targetPx: Int, decode: (BitmapFactory.Options) -> Bitmap?): ImageBitmap? =
    runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        decode(bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= targetPx && bounds.outHeight / (sample * 2) >= targetPx) {
            sample *= 2
        }
        decode(BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
    }.getOrNull()

/** Avatar circular con iniciales y punto de estado en línea. */
@Composable
fun UserAvatar(user: User, modifier: Modifier = Modifier, size: Dp = 44.dp, showStatus: Boolean = true) {
    val status = MaterialTheme.statusColors
    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = user.initials,
                style = when {
                    size >= 56.dp -> MaterialTheme.typography.titleLarge
                    size >= 36.dp -> MaterialTheme.typography.titleSmall
                    else -> MaterialTheme.typography.labelSmall
                },
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
        }
        if (showStatus) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.3f)
                    .clip(CircleShape)
                    .background(if (user.isOnline) status.success else MaterialTheme.colorScheme.outline)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}

/** Indicador numérico compacto para los resúmenes (KPIs). */
@Composable
fun StatTile(
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val colors = if (selected) {
        CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.16f))
    } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    }
    val border = if (selected) BorderStroke(1.5.dp, accent) else null
    val content: @Composable ColumnScope.() -> Unit = {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = accent,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, colors = colors, border = border, shape = MaterialTheme.shapes.medium, content = content)
    } else {
        Card(modifier = modifier, colors = colors, border = border, shape = MaterialTheme.shapes.medium, content = content)
    }
}

/** Mensaje centrado para listas vacías o estados sin datos. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = tint.copy(alpha = 0.12f),
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(36.dp))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/** Título pequeño para separar secciones dentro de una pantalla o diálogo. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        trailing()
    }
}
