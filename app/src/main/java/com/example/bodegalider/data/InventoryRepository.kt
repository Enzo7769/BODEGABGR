package com.example.bodegalider.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.bodegalider.R
import com.example.bodegalider.model.Product
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class InventoryRepository(private val context: Context? = null) {

    private val db = FirebaseFirestore.getInstance()
    private val productsCollection = db.collection("products")

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        // Escucha en tiempo real de Firestore (Sincronización multiusuario en la nube)
        productsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                return@addSnapshotListener
            }

            if (snapshot != null && !snapshot.isEmpty) {
                val list = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    Product(
                        id = doc.id,
                        barcode = data["barcode"] as? String ?: "",
                        name = data["name"] as? String ?: "",
                        brand = data["brand"] as? String ?: "",
                        imageResId = (data["imageResId"] as? Long)?.toInt() ?: R.drawable.baked_goods_1,
                        imageUri = data["imageUri"] as? String ?: "",
                        stockOnShelf = (data["stockOnShelf"] as? Long)?.toInt() ?: 0,
                        quantityNeeded = (data["quantityNeeded"] as? Long)?.toInt() ?: 10,
                        isOutOfStock = data["isOutOfStock"] as? Boolean ?: false,
                        aisle = data["aisle"] as? String ?: "",
                        locationDetail = data["locationDetail"] as? String ?: ""
                    )
                }
                _products.value = list
            } else {
                seedInitialData()
            }
        }
        refreshConnectivity()
    }

    private fun seedInitialData() {
        val initial = listOf(
            Product("1", "7801610001234", "Leche Entera 1L", "Colun", R.drawable.baked_goods_1, "", 3, 10, false, "Pasillo 2", "Cartel 5"),
            Product("2", "7802340005678", "Yogurt Batido Fresa 125g", "Soprole", R.drawable.baked_goods_2, "", 0, 15, true, "Pasillo 2", "Cabecera Norte"),
            Product("3", "7804560009012", "Atún en Aceite Lata 160g", "Gourmet", R.drawable.baked_goods_3, "", 12, 4, false, "Pasillo 5", "Cartel 12"),
            Product("4", "7807890003456", "Arroz Grado 1 1kg", "Tucapel", R.drawable.baked_goods_1, "", 1, 20, false, "Pasillo 4", "Isla Central"),
            Product("5", "7809990001111", "Dante Fideos Spaghetti 400g", "Lucchetti", R.drawable.baked_goods_2, "", 0, 12, true, "Pasillo 4", "Cartel 8")
        )
        for (p in initial) {
            productsCollection.document(p.id).set(p)
        }
    }

    fun refreshConnectivity() {
        if (context == null) {
            _isOnline.value = true
            return
        }
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val net = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(net)
        _isOnline.value = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    fun updateStock(productId: String, newStock: Int, newNeeded: Int, isQuiebre: Boolean, aisle: String, locationDetail: String) {
        val updates = mapOf(
            "stockOnShelf" to newStock.coerceAtLeast(0),
            "quantityNeeded" to newNeeded.coerceAtLeast(0),
            "isOutOfStock" to (isQuiebre || newStock <= 0),
            "aisle" to aisle.trim(),
            "locationDetail" to locationDetail.trim()
        )
        productsCollection.document(productId).update(updates)
    }

    fun adjustStock(productId: String, delta: Int) {
        val current = _products.value.find { it.id == productId } ?: return
        val newStock = (current.stockOnShelf + delta).coerceAtLeast(0)
        val outOfStock = if (newStock == 0) true else current.isOutOfStock
        val updates = mapOf(
            "stockOnShelf" to newStock,
            "isOutOfStock" to outOfStock
        )
        productsCollection.document(productId).update(updates)
    }

    fun addProduct(product: Product) {
        productsCollection.document(product.id).set(product)
    }

    fun getProductByBarcode(barcode: String): Product? {
        return _products.value.find { it.barcode == barcode }
    }
}
