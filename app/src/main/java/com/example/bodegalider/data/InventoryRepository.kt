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

    private var hasSeeded = false
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
        listenerRegistration = productsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Error al escuchar productos", error)
                _isLoading.value = false
                return@addSnapshotListener
            }
            if (snapshot == null) return@addSnapshotListener

            if (snapshot.isEmpty && !hasSeeded) {
                hasSeeded = true
                seedInitialData()
                return@addSnapshotListener
            }

            _products.value = snapshot.documents.mapNotNull { it.toProduct() }
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

    private fun seedInitialData() {
        val initial = listOf(
            Product(id = "1", barcode = "7801610001234", name = "Leche Entera 1L", brand = "Colun",
                stockOnShelf = 3, quantityNeeded = 10, aisle = "Pasillo 2", locationDetail = "Cartel 5"),
            Product(id = "2", barcode = "7802340005678", name = "Yogurt Batido Frutilla 125g", brand = "Soprole",
                stockOnShelf = 0, quantityNeeded = 15, isOutOfStock = true, aisle = "Pasillo 2", locationDetail = "Cabecera Norte"),
            Product(id = "3", barcode = "7804560009012", name = "Atún en Aceite Lata 160g", brand = "Gourmet",
                stockOnShelf = 12, quantityNeeded = 4, aisle = "Pasillo 5", locationDetail = "Cartel 12"),
            Product(id = "4", barcode = "7807890003456", name = "Arroz Grado 1 1kg", brand = "Tucapel",
                stockOnShelf = 1, quantityNeeded = 20, aisle = "Pasillo 4", locationDetail = "Isla Central"),
            Product(id = "5", barcode = "7809990001111", name = "Fideos Spaghetti N°5 400g", brand = "Lucchetti",
                stockOnShelf = 0, quantityNeeded = 12, isOutOfStock = true, aisle = "Pasillo 4", locationDetail = "Cartel 8")
        )
        val batch = db.batch()
        initial.forEach { batch.set(productsCollection.document(it.id), it.toFirestoreMap()) }
        batch.commit()
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
