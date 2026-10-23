package com.example.bodegalider.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bodegalider.R
import com.example.bodegalider.data.InventoryRepository
import com.example.bodegalider.model.Product
import com.example.bodegalider.model.TaskItem
import com.example.bodegalider.model.User
import com.example.bodegalider.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

enum class AppTab {
    CATALOG,
    ALERTS,
    SCANNER,
    TEAM,
    NOTIFICATIONS,
    SETTINGS
}

enum class SortOption {
    OUT_OF_STOCK_FIRST,
    LOW_STOCK_FIRST,
    HIGH_STOCK_FIRST,
    ALPHABETICAL
}

class InventoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = InventoryRepository(application)

    private val registeredUsers = listOf(
        User("u_admin", "Roberto Supervisor", UserRole.ADMIN, "046", "Maipú", true, "Mañana", "Full Time", listOf("Maipú", "Pudahuel")),
        User("u1", "Carlos Reponedor", UserRole.USER, "046", "Maipú", true, "Mañana", "Full Time"),
        User("u2", "Ana Pérez", UserRole.USER, "048", "Maipú", true, "Tarde", "Part Time"),
        User("u3", "Juan Soto", UserRole.USER, "052", "Pudahuel", false, "Noche", "Backup"),
        User("u4", "María Gómez", UserRole.USER, "060", "Cerrillos", true, "Mañana", "Full Time"),
        User("u5", "Pedro Alarcón", UserRole.USER, "071", "Stgo Centro", false, "Tarde", "Backup"),
        User("u6", "Sofía Valdés", UserRole.USER, "046", "Maipú", true, "Noche", "Part Time")
    )

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _teamUsers = MutableStateFlow(registeredUsers.filter { it.role == UserRole.USER })
    val teamUsers: StateFlow<List<User>> = _teamUsers.asStateFlow()

    private val _tasks = MutableStateFlow(
        listOf(
            TaskItem("t1", "u1", "Carlos Reponedor", "046", "Priorizar rellenar góndola de Lácteos", false, System.currentTimeMillis()),
            TaskItem("t2", "u2", "Ana Pérez", "048", "Revisar quiebres en abarrotes y actualizar stock", false, System.currentTimeMillis()),
            TaskItem("t3", "u4", "María Gómez", "060", "Armar cabecera de pasillo principal", true, System.currentTimeMillis())
        )
    )
    val tasks: StateFlow<List<TaskItem>> = _tasks.asStateFlow()

    private val lastAlertTimestamps = mutableMapOf<String, Long>()
    private val _alertStateVersion = MutableStateFlow(0)
    val alertStateVersion: StateFlow<Int> = _alertStateVersion.asStateFlow()

    private val _currentTab = MutableStateFlow(AppTab.CATALOG)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _teamSearchQuery = MutableStateFlow("")
    val teamSearchQuery: StateFlow<String> = _teamSearchQuery.asStateFlow()

    private val _selectedCommuneFilter = MutableStateFlow("Todas")
    val selectedCommuneFilter: StateFlow<String> = _selectedCommuneFilter.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.OUT_OF_STOCK_FIRST)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct: StateFlow<Product?> = _selectedProduct.asStateFlow()

    private val _isAddProductDialogOpen = MutableStateFlow(false)
    val isAddProductDialogOpen: StateFlow<Boolean> = _isAddProductDialogOpen.asStateFlow()

    private val _scannedBarcodeForNew = MutableStateFlow("")
    val scannedBarcodeForNew: StateFlow<String> = _scannedBarcodeForNew.asStateFlow()

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    val products: StateFlow<List<Product>> = repository.products
    val isOnline: StateFlow<Boolean> = repository.isOnline

    init {
        cleanExpiredTasks()
    }

    private fun cleanExpiredTasks() {
        val oneWeekMillis = 7 * 24 * 60 * 60 * 1000L
        val currentTime = System.currentTimeMillis()
        _tasks.update { list ->
            list.filter { task -> currentTime - task.timestamp < oneWeekMillis }
        }
    }

    val filteredProducts: StateFlow<List<Product>> = combine(products, searchQuery, sortOption) { list, query, sort ->
        val filtered = if (query.isBlank()) list
        else list.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.barcode.contains(query) ||
                    it.brand.contains(query, ignoreCase = true) ||
                    it.aisle.contains(query, ignoreCase = true) ||
                    it.locationDetail.contains(query, ignoreCase = true)
        }

        when (sort) {
            SortOption.OUT_OF_STOCK_FIRST -> filtered.sortedWith(
                compareBy<Product> { !it.isOutOfStock && it.stockOnShelf > 0 }
                    .thenBy { it.stockOnShelf }
            )
            SortOption.LOW_STOCK_FIRST -> filtered.sortedBy { it.stockOnShelf }
            SortOption.HIGH_STOCK_FIRST -> filtered.sortedByDescending { it.stockOnShelf }
            SortOption.ALPHABETICAL -> filtered.sortedBy { it.name }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredTeamUsers: StateFlow<List<User>> = combine(currentUser, teamUsers, teamSearchQuery, selectedCommuneFilter) { admin, list, query, commune ->
        val allowedList = if (admin?.role == UserRole.ADMIN) {
            list.filter { worker -> admin.managedCommunes.contains(worker.commune) }
        } else {
            list
        }

        allowedList.filter { user ->
            val matchesQuery = query.isBlank() || user.name.contains(query, ignoreCase = true) || user.storeCode.contains(query)
            val matchesCommune = commune == "Todas" || user.commune.equals(commune, ignoreCase = true)
            matchesQuery && matchesCommune
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val outOfStockProducts: StateFlow<List<Product>> = products.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun isAlertOnCooldown(userId: String, productId: String): Boolean {
        val key = "${userId}_$productId"
        val lastTime = lastAlertTimestamps[key] ?: 0L
        val oneHourMillis = 60 * 60 * 1000L
        return System.currentTimeMillis() - lastTime < oneHourMillis
    }

    fun sendPriorityAlert(userId: String, userName: String, storeCode: String, product: Product): Pair<Boolean, Int> {
        val key = "${userId}_${product.id}"
        val lastTime = lastAlertTimestamps[key] ?: 0L
        val oneHourMillis = 60 * 60 * 1000L
        val currentTime = System.currentTimeMillis()
        val elapsed = currentTime - lastTime

        if (elapsed < oneHourMillis) {
            val remainingMinutes = ((oneHourMillis - elapsed) / (60 * 1000L)).toInt() + 1
            return Pair(false, remainingMinutes)
        }

        lastAlertTimestamps[key] = currentTime
        _alertStateVersion.update { it + 1 }
        val taskTitle = "🚨 ¡ALERTA DE PRIORIDAD! Rellenar ${product.brand} ${product.name} (${product.locationText})"
        assignTask(userId, userName, storeCode, taskTitle)
        return Pair(true, 0)
    }

    fun assignTask(userId: String, userName: String, storeCode: String, title: String) {
        val newTask = TaskItem(
            id = System.currentTimeMillis().toString(),
            userId = userId,
            userName = userName,
            storeCode = storeCode,
            title = title.trim(),
            isCompleted = false,
            timestamp = System.currentTimeMillis()
        )
        _tasks.update { listOf(newTask) + it }
    }

    fun completeTask(taskId: String) {
        _tasks.update { list ->
            list.map { task ->
                if (task.id == taskId) task.copy(isCompleted = !task.isCompleted) else task
            }
        }
    }

    fun login(name: String, role: UserRole, storeCode: String, commune: String, shift: String, contractType: String, managedCommunes: List<String> = emptyList()) {
        _currentUser.value = User(
            id = System.currentTimeMillis().toString(),
            name = name,
            role = role,
            storeCode = storeCode,
            commune = commune,
            isOnline = true,
            shift = shift,
            contractType = contractType,
            managedCommunes = managedCommunes
        )
    }

    fun loginWithUsername(username: String): Boolean {
        val clean = username.trim().lowercase()
        val found = when (clean) {
            "admin", "supervisor", "roberto" -> registeredUsers.find { it.role == UserRole.ADMIN }
            "carlos" -> registeredUsers.find { it.name.contains("Carlos", true) }
            "ana" -> registeredUsers.find { it.name.contains("Ana", true) }
            "juan" -> registeredUsers.find { it.name.contains("Juan", true) }
            "maria" -> registeredUsers.find { it.name.contains("María", true) }
            "pedro" -> registeredUsers.find { it.name.contains("Pedro", true) }
            "sofia" -> registeredUsers.find { it.name.contains("Sofía", true) }
            else -> registeredUsers.find { it.name.equals(clean, true) }
        }

        if (found != null) {
            _currentUser.value = found
            return true
        }
        return false
    }

    fun logout() {
        _currentUser.value = null
    }

    fun setTeamSearchQuery(query: String) {
        _teamSearchQuery.value = query
    }

    fun setSelectedCommuneFilter(commune: String) {
        _selectedCommuneFilter.value = commune
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun toggleDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
    }

    fun refreshNetworkStatus() {
        repository.refreshConnectivity()
    }

    fun setTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectProduct(product: Product?) {
        _selectedProduct.value = product
    }

    fun adjustStock(productId: String, delta: Int) {
        repository.adjustStock(productId, delta)
    }

    fun updateStock(productId: String, stock: Int, needed: Int, isQuiebre: Boolean, aisle: String, locationDetail: String) {
        repository.updateStock(productId, stock, needed, isQuiebre, aisle, locationDetail)
    }

    fun handleScannedBarcode(barcode: String): Product? {
        val existing = repository.getProductByBarcode(barcode)
        return existing
    }

    fun openAddProductWithBarcode(barcode: String) {
        _scannedBarcodeForNew.value = barcode
        _isAddProductDialogOpen.value = true
    }

    fun closeAddProductDialog() {
        _isAddProductDialogOpen.value = false
        _scannedBarcodeForNew.value = ""
    }

    fun addNewProduct(barcode: String, name: String, brand: String, stock: Int, needed: Int, imageUri: String, isQuiebre: Boolean, aisle: String, locationDetail: String) {
        val newProduct = Product(
            id = System.currentTimeMillis().toString(),
            barcode = barcode,
            name = name,
            brand = brand,
            imageResId = if (imageUri.isEmpty()) R.drawable.baked_goods_1 else 0,
            imageUri = imageUri,
            stockOnShelf = stock,
            quantityNeeded = needed,
            isOutOfStock = isQuiebre || stock <= 0,
            aisle = aisle.trim(),
            locationDetail = locationDetail.trim()
        )
        repository.addProduct(newProduct)
        closeAddProductDialog()
    }
}
