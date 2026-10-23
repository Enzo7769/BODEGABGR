package com.example.bodegalider.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.bodegalider.model.Product
import com.example.bodegalider.ui.components.ProductImage
import com.example.bodegalider.ui.components.StatusBadge
import com.example.bodegalider.ui.viewmodel.InventoryViewModel
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun ScannerScreen(viewModel: InventoryViewModel) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val products by viewModel.products.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Código leído (por cámara o a mano). El producto se busca en vivo para reflejar cambios de stock.
    var scannedCode by remember { mutableStateOf<String?>(null) }
    val scanPaused = remember { AtomicBoolean(false) }
    val scannedProduct = scannedCode?.let { code -> products.find { it.barcode == code } }

    val onCodeDetected: (String) -> Unit = { code ->
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        scannedCode = code.trim()
    }
    val resumeScanning = {
        scannedCode = null
        scanPaused.set(false)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (hasCameraPermission) Color.Black else MaterialTheme.colorScheme.background)
    ) {
        if (hasCameraPermission) {
            CameraPreview(
                scanPaused = scanPaused,
                onCodeDetected = onCodeDetected,
                modifier = Modifier.fillMaxSize()
            )
            ScanFrame(modifier = Modifier.align(Alignment.Center))
        } else {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp)
                    .padding(bottom = 160.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Activa la cámara para escanear",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "También puedes escribir el código de barras más abajo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Permitir cámara")
                }
            }
        }

        val code = scannedCode
        when {
            code == null -> ManualEntryCard(
                showHint = hasCameraPermission,
                onSearch = { value ->
                    scanPaused.set(true)
                    scannedCode = value
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            scannedProduct != null -> ScanResultCard(
                product = scannedProduct,
                onIncrement = { viewModel.adjustStock(scannedProduct.id, 1) },
                onDecrement = { viewModel.adjustStock(scannedProduct.id, -1) },
                onEdit = { viewModel.selectProduct(scannedProduct) },
                onScanAgain = resumeScanning,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            else -> UnknownCodeCard(
                code = code,
                onRegister = { viewModel.openAddProduct(code) },
                onScanAgain = resumeScanning,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun CameraPreview(
    scanPaused: AtomicBoolean,
    onCodeDetected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnCodeDetected by rememberUpdatedState(onCodeDetected)
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var torchOn by remember { mutableStateOf(false) }

    // La cámara se enciende al entrar a la pestaña y se libera al salir
    DisposableEffect(lifecycleOwner) {
        val executor = Executors.newSingleThreadExecutor()
        val scanner = BarcodeScanning.getClient()
        val session = CameraSession(context, lifecycleOwner, previewView, executor, scanner, scanPaused) { code ->
            currentOnCodeDetected(code)
        }
        session.start { camera = it }
        onDispose {
            session.stop()
            camera = null
            torchOn = false
        }
    }

    Box(modifier = modifier) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        Surface(
            color = Color.Black.copy(alpha = 0.55f),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Apunta al código de barras", color = Color.White, style = MaterialTheme.typography.bodyMedium)
            }
        }

        val cam = camera
        if (cam != null && cam.cameraInfo.hasFlashUnit()) {
            FilledIconButton(
                onClick = {
                    torchOn = !torchOn
                    cam.cameraControl.enableTorch(torchOn)
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (torchOn) MaterialTheme.colorScheme.secondaryContainer else Color.Black.copy(alpha = 0.55f),
                    contentColor = if (torchOn) MaterialTheme.colorScheme.onSecondaryContainer else Color.White
                ),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
                    .size(48.dp)
            ) {
                Icon(
                    if (torchOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                    contentDescription = if (torchOn) "Apagar linterna" else "Encender linterna"
                )
            }
        }
    }
}

/** Encapsula la configuración de CameraX + ML Kit para poder liberarla de forma segura. */
private class CameraSession(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView,
    private val executor: ExecutorService,
    private val scanner: BarcodeScanner,
    private val scanPaused: AtomicBoolean,
    private val onCodeDetected: (String) -> Unit
) {
    private var provider: ProcessCameraProvider? = null
    @Volatile
    private var stopped = false

    fun start(onCameraReady: (Camera) -> Unit) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (stopped) return@addListener
            try {
                val cameraProvider = future.get()
                provider = cameraProvider
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor) { proxy -> analyze(proxy) }

                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
                onCameraReady(camera)
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo iniciar la cámara", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun analyze(proxy: ImageProxy) {
        val mediaImage = proxy.image
        if (mediaImage == null || scanPaused.get() || stopped) {
            proxy.close()
            return
        }
        val image = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val value = barcodes.firstNotNullOfOrNull { barcode -> barcode.rawValue?.takeIf { it.isNotBlank() } }
                // compareAndSet evita procesar el mismo código varias veces seguidas
                if (value != null && !stopped && scanPaused.compareAndSet(false, true)) {
                    onCodeDetected(value)
                }
            }
            .addOnCompleteListener { proxy.close() }
    }

    fun stop() {
        stopped = true
        provider?.unbindAll()
        scanner.close()
        executor.shutdown()
    }

    private companion object {
        const val TAG = "ScannerScreen"
    }
}

@Composable
private fun ScanFrame(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .padding(bottom = 120.dp)
            .size(width = 280.dp, height = 170.dp),
        color = Color.Transparent,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(3.dp, Color.White.copy(alpha = 0.9f))
    ) {}
}

@Composable
private fun ManualEntryCard(showHint: Boolean, onSearch: (String) -> Unit, modifier: Modifier = Modifier) {
    var input by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val submit = {
        val clean = input.trim()
        if (clean.isNotEmpty()) {
            focusManager.clearFocus()
            onSearch(clean)
            input = ""
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = if (showHint) "¿No lee el código? Ingrésalo a mano" else "Ingresar código de barras",
                style = MaterialTheme.typography.titleSmall
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.filter(Char::isLetterOrDigit).take(32) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ej: 7801610001234") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { submit() })
                )
                FilledIconButton(
                    onClick = submit,
                    enabled = input.isNotBlank(),
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Buscar código")
                }
            }
        }
    }
}

@Composable
private fun ScanResultCard(
    product: Product,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onEdit: () -> Unit,
    onScanAgain: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ProductImage(imageUri = product.imageUri, contentDescription = product.name, size = 64.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.brand.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = product.locationText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                StatusBadge(status = product.stockStatus, modifier = Modifier.align(Alignment.Top))
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Unidades en góndola",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            StockStepper(value = product.stockOnShelf, onDecrement = onDecrement, onIncrement = onIncrement, large = true)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (product.quantityNeeded > 0) "Faltan ${product.quantityNeeded} un. por reponer" else "Góndola completa",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Editar")
                }
                Button(onClick = onScanAgain, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Escanear otro")
                }
            }
        }
    }
}

@Composable
private fun UnknownCodeCard(
    code: String,
    onRegister: () -> Unit,
    onScanAgain: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.SearchOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Producto no registrado", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Código $code",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onScanAgain, modifier = Modifier.weight(1f)) {
                    Text("Escanear otro")
                }
                Button(onClick = onRegister, modifier = Modifier.weight(1f)) {
                    Text("Registrar")
                }
            }
        }
    }
}
