package com.example.bodegalider.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import com.example.bodegalider.model.Product
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class InventoryRepository(context: Context) {

    private val appContext = context.applicationContext
    private val db = FirebaseFirestore.getInstance()
    private val productsCollection = db.collection("products")
    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private var hasSyncedCatalog = false
    private val listenerRegistration: ListenerRegistration

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOnline.value = true
        }

        override fun onLost(network: Network) {
            _isOnline.value = false
        }
    }

    init {
        // Escucha en tiempo real de Firestore (sincronización multiusuario en la nube).
        // Firestore mantiene caché local, así que los cambios hechos sin señal se envían al reconectar.
        listenerRegistration = productsCollection.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Error al escuchar productos", error)
                _isLoading.value = false
                return@addSnapshotListener
            }
            if (snapshot == null) return@addSnapshotListener

            val current = snapshot.documents.mapNotNull { it.toProduct() }
            // Solo con datos del servidor: con la caché vacía se crearían productos que ya existen en la nube
            if (!hasSyncedCatalog && !snapshot.metadata.isFromCache) {
                hasSyncedCatalog = true
                syncCatalog(current)
            }

            _products.value = current
            _isLoading.value = false
        }

        refreshConnectivity()
        try {
            connectivityManager?.registerDefaultNetworkCallback(networkCallback)
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo registrar el monitor de red", e)
        }
    }

    private fun DocumentSnapshot.toProduct(): Product? {
        val data = data ?: return null
        return Product(
            id = id,
            barcode = data["barcode"] as? String ?: "",
            name = data["name"] as? String ?: "",
            brand = data["brand"] as? String ?: "",
            imageUri = data["imageUri"] as? String ?: "",
            stockOnShelf = (data["stockOnShelf"] as? Number)?.toInt() ?: 0,
            quantityNeeded = (data["quantityNeeded"] as? Number)?.toInt() ?: 0,
            // "outOfStock" es la clave que generaba la serialización automática en versiones anteriores
            isOutOfStock = (data["isOutOfStock"] ?: data["outOfStock"]) as? Boolean ?: false,
            aisle = data["aisle"] as? String ?: "",
            locationDetail = data["locationDetail"] as? String ?: ""
        )
    }

    private fun Product.toFirestoreMap(): Map<String, Any> = mapOf(
        "barcode" to barcode,
        "name" to name,
        "brand" to brand,
        "imageUri" to imageUri,
        "stockOnShelf" to stockOnShelf,
        "quantityNeeded" to quantityNeeded,
        "isOutOfStock" to isOutOfStock,
        "aisle" to aisle,
        "locationDetail" to locationDetail
    )

    /**
     * Agrega a Firestore los productos del catálogo P&G que aún no están registrados (por código de barras)
     * y pone la foto del catálogo a los que ya existían sin foto. No toca stock ni ubicación ya cargados.
     */
    private fun syncCatalog(existing: List<Product>) {
        val byBarcode = existing.associateBy { it.barcode }
        val batch = db.batch()
        var pending = 0
        PgCatalog.items.forEach { item ->
            val imageUri = "android.resource://${appContext.packageName}/drawable/${item.imageName}"
            val current = byBarcode[item.barcode]
            if (current == null) {
                val product = Product(
                    id = "pg_${item.barcode}",
                    barcode = item.barcode,
                    name = item.name,
                    brand = item.brand,
                    imageUri = imageUri,
                    stockOnShelf = PgCatalog.INITIAL_STOCK,
                    locationDetail = item.locationDetail
                )
                batch.set(productsCollection.document(product.id), product.toFirestoreMap())
                pending++
            } else if (current.imageUri.isBlank()) {
                batch.update(productsCollection.document(current.id), "imageUri", imageUri)
                pending++
            }
        }
        if (pending > 0) batch.commit()
    }

    /** Vuelve a consultar el estado de la red y devuelve si hay internet. */
    fun refreshConnectivity(): Boolean {
        val cm = connectivityManager
        val online = if (cm == null) {
            true
        } else {
            try {
                val caps = cm.getNetworkCapabilities(cm.activeNetwork)
                caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            } catch (e: SecurityException) {
                true
            }
        }
        _isOnline.value = online
        return online
    }

    fun updateProduct(product: Product) {
        val sanitized = product.copy(
            stockOnShelf = product.stockOnShelf.coerceAtLeast(0),
            quantityNeeded = product.quantityNeeded.coerceAtLeast(0),
            aisle = product.aisle.trim(),
            locationDetail = product.locationDetail.trim()
        )
        productsCollection.document(product.id).set(sanitized.toFirestoreMap())
    }

    fun adjustStock(productId: String, delta: Int) {
        val current = _products.value.find { it.id == productId } ?: return
        val newStock = (current.stockOnShelf + delta).coerceAtLeast(0)
        val updated = current.copy(
            stockOnShelf = newStock,
            // Cada unidad repuesta se descuenta de lo que falta por rellenar (y al revés al corregir)
            quantityNeeded = (current.quantityNeeded - (newStock - current.stockOnShelf)).coerceAtLeast(0),
            isOutOfStock = if (newStock == 0) true else if (delta > 0) false else current.isOutOfStock
        )
        // Actualización optimista: la UI responde al instante aunque no haya señal
        _products.value = _products.value.map { if (it.id == productId) updated else it }
        productsCollection.document(productId).update(
            mapOf(
                "stockOnShelf" to updated.stockOnShelf,
                "quantityNeeded" to updated.quantityNeeded,
                "isOutOfStock" to updated.isOutOfStock
            )
        )
    }

    fun updateImageUri(productId: String, imageUri: String) {
        productsCollection.document(productId).update("imageUri", imageUri)
    }

    fun addProduct(product: Product) {
        productsCollection.document(product.id).set(product.toFirestoreMap())
    }

    fun close() {
        listenerRegistration.remove()
        try {
            connectivityManager?.unregisterNetworkCallback(networkCallback)
        } catch (_: Exception) {
        }
    }

    private companion object {
        const val TAG = "InventoryRepository"
    }
}
