package com.example.bodegalider.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bodegalider.data.InventoryRepository
import com.example.bodegalider.model.InventoryStats
import com.example.bodegalider.model.Product
import com.example.bodegalider.model.TaskItem
import com.example.bodegalider.model.User
import com.example.bodegalider.model.UserRole
import com.example.bodegalider.util.normalizeForSearch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.util.UUID

enum class AppTab {
    CATALOG,
    ALERTS,
    SCANNER,
    TEAM,
    NOTIFICATIONS,
    SETTINGS
}

enum class SortOption(val label: String) {
    OUT_OF_STOCK_FIRST("Críticos primero"),
    LOW_STOCK_FIRST("Menor stock"),
    HIGH_STOCK_FIRST("Mayor stock"),
    ALPHABETICAL("Nombre (A-Z)"),
    LOCATION("Pasillo")
}

enum class StockFilter(val label: String) {
    ALL("Todos"),
    OUT_OF_STOCK("Sin stock"),
    LOW("Stock bajo")
}

sealed interface AlertResult {
    data object Sent : AlertResult
    data class OnCooldown(val remainingMinutes: Int) : AlertResult
}

class InventoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = InventoryRepository(application)
    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Usuarios de demostración (en producción vendrían de un backend de autenticación)
    private val registeredUsers = listOf(
        User("u_admin", "Roberto Supervisor", UserRole.ADMIN, "046", "Maipú", true, "Mañana", "Full Time", listOf("Maipú", "Pudahuel"), "admin"),
        User("u1", "Carlos Reponedor", UserRole.USER, "046", "Maipú", true, "Mañana", "Full Time", username = "carlos"),
        User("u2", "Ana Pérez", UserRole.USER, "048", "Maipú", true, "Tarde", "Part Time", username = "ana"),
        User("u3", "Juan Soto", UserRole.USER, "052", "Pudahuel", false, "Noche", "Backup", username = "juan"),
        User("u4", "María Gómez", UserRole.USER, "060", "Cerrillos", true, "Mañana", "Full Time", username = "maria"),
        User("u5", "Pedro Alarcón", UserRole.USER, "071", "Stgo Centro", false, "Tarde", "Backup", username = "pedro"),
        User("u6", "Sofía Valdés", UserRole.USER, "046", "Maipú", true, "Noche", "Part Time", username = "sofia")
    )

    /** Usuarios que se muestran como acceso rápido en la pantalla de inicio de sesión. */
    val demoUsers: List<User> = registeredUsers

    private val _currentUser = MutableStateFlow(
        prefs.getString(KEY_USER_ID, null)?.let { id -> registeredUsers.find { it.id == id } }
    )
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val teamUsers = MutableStateFlow(registeredUsers.filter { it.role == UserRole.USER })

    private val _tasks = MutableStateFlow(
        listOf(
            TaskItem("t1", "u1", "Carlos Reponedor", "046", "Priorizar rellenar góndola de lácteos", isPriority = true),
            TaskItem("t2", "u2", "Ana Pérez", "048", "Revisar quiebres en abarrotes y actualizar stock"),
            TaskItem("t3", "u4", "María Gómez", "060", "Armar cabecera de pasillo principal", isCompleted = true)
        )
    )
    val tasks: StateFlow<List<TaskItem>> = _tasks.asStateFlow()

    private val lastAlertTimestamps = mutableMapOf<String, Long>()
    private val _alertStateVersion = MutableStateFlow(0)
    val alertStateVersion: StateFlow<Int> = _alertStateVersion.asStateFlow()

    private val _currentTab = MutableStateFlow(defaultTabFor(_currentUser.value))
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _stockFilter = MutableStateFlow(StockFilter.ALL)
    val stockFilter: StateFlow<StockFilter> = _stockFilter.asStateFlow()

    private val _teamSearchQuery = MutableStateFlow("")
    val teamSearchQuery: StateFlow<String> = _teamSearchQuery.asStateFlow()

    private val _selectedCommuneFilter = MutableStateFlow(ALL_COMMUNES)
    val selectedCommuneFilter: StateFlow<String> = _selectedCommuneFilter.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.OUT_OF_STOCK_FIRST)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _selectedProductId = MutableStateFlow<String?>(null)

    private val _isAddProductDialogOpen = MutableStateFlow(false)
    val isAddProductDialogOpen: StateFlow<Boolean> = _isAddProductDialogOpen.asStateFlow()

    private val _scannedBarcodeForNew = MutableStateFlow("")
    val scannedBarcodeForNew: StateFlow<String> = _scannedBarcodeForNew.asStateFlow()

    private val _isDarkMode = MutableStateFlow(
        if (prefs.contains(KEY_DARK_MODE)) prefs.getBoolean(KEY_DARK_MODE, false) else isSystemDark(application)
    )
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    /** Mensajes cortos para mostrar en un Snackbar. */
    val messages: Flow<String> = _messages.receiveAsFlow()

    val products: StateFlow<List<Product>> = repository.products
    val isLoading: StateFlow<Boolean> = repository.isLoading
    val isOnline: StateFlow<Boolean> = repository.isOnline

    val selectedProduct: StateFlow<Product?> = combine(products, _selectedProductId) { list, id ->
        id?.let { selected -> list.find { it.id == selected } }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val inventoryStats: StateFlow<InventoryStats> = products
        .map { InventoryStats.from(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InventoryStats())

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

    val filteredProducts: StateFlow<List<Product>> =
        combine(products, searchQuery, sortOption, stockFilter) { list, query, sort, filter ->
            val needle = query.normalizeForSearch()
            val filtered = list.filter { product ->
                val matchesFilter = when (filter) {
                    StockFilter.ALL -> true
                    StockFilter.OUT_OF_STOCK -> product.isQuiebre
                    StockFilter.LOW -> product.isLowStock
                }
                val matchesQuery = needle.isBlank() ||
                    product.name.normalizeForSearch().contains(needle) ||
                    product.brand.normalizeForSearch().contains(needle) ||
                    product.barcode.contains(needle) ||
                    product.aisle.normalizeForSearch().contains(needle) ||
                    product.locationDetail.normalizeForSearch().contains(needle)
                matchesFilter && matchesQuery
            }
            sortProducts(filtered, sort)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Productos que necesitan atención, del más crítico al menos crítico. */
    val restockProducts: StateFlow<List<Product>> = products
        .map { list -> sortProducts(list.filter { it.needsRestock }, SortOption.OUT_OF_STOCK_FIRST) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Reponedores bajo el cargo del supervisor actual (sin filtros de búsqueda). */
    val managedWorkers: StateFlow<List<User>> = combine(currentUser, teamUsers) { admin, list ->
        if (admin?.role == UserRole.ADMIN) list.filter { it.commune in admin.managedCommunes } else list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableCommunes: StateFlow<List<String>> = managedWorkers
        .map { workers -> listOf(ALL_COMMUNES) + workers.map { it.commune }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf(ALL_COMMUNES))

    val filteredTeamUsers: StateFlow<List<User>> =
        combine(managedWorkers, teamSearchQuery, selectedCommuneFilter) { list, query, commune ->
            val needle = query.normalizeForSearch()
            list.filter { user ->
                val matchesQuery = needle.isBlank() ||
                    user.name.normalizeForSearch().contains(needle) ||
                    user.storeCode.contains(needle)
                val matchesCommune = commune == ALL_COMMUNES || user.commune.equals(commune, ignoreCase = true)
                matchesQuery && matchesCommune
            }.sortedWith(compareByDescending<User> { it.isOnline }.thenBy { it.name })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Tareas visibles para el usuario actual: las propias o, para el supervisor, las de su equipo. */
    val visibleTasks: StateFlow<List<TaskItem>> = combine(currentUser, tasks, managedWorkers) { user, list, workers ->
        val visible = when {
            user == null -> emptyList()
            user.role == UserRole.ADMIN -> {
                val ids = workers.map { it.id }.toSet()
                list.filter { it.userId in ids }
            }
            user.role == UserRole.SUPER_ADMIN -> list
            else -> list.filter { it.userId == user.id }
        }
        visible.sortedWith(compareBy<TaskItem> { it.isCompleted }.thenByDescending { it.timestamp })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun sortProducts(list: List<Product>, sort: SortOption): List<Product> = when (sort) {
        SortOption.OUT_OF_STOCK_FIRST -> list.sortedWith(
            compareBy<Product> { it.stockStatus.ordinal }
                .thenByDescending { it.quantityNeeded }
                .thenBy { it.stockOnShelf }
        )
        SortOption.LOW_STOCK_FIRST -> list.sortedBy { it.stockOnShelf }
        SortOption.HIGH_STOCK_FIRST -> list.sortedByDescending { it.stockOnShelf }
        SortOption.ALPHABETICAL -> list.sortedBy { it.name.normalizeForSearch() }
        SortOption.LOCATION -> list.sortedWith(
            compareBy<Product> { it.aisle.isBlank() }
                .thenBy { it.aisle.filter(Char::isDigit).toIntOrNull() ?: Int.MAX_VALUE }
                .thenBy { it.aisle }
                .thenBy { it.locationDetail }
        )
    }

    fun isAlertOnCooldown(userId: String, productId: String): Boolean {
        val lastTime = lastAlertTimestamps["${userId}_$productId"] ?: 0L
        return System.currentTimeMillis() - lastTime < ALERT_COOLDOWN_MILLIS
    }

    fun sendPriorityAlert(worker: User, product: Product): AlertResult {
        val key = "${worker.id}_${product.id}"
        val currentTime = System.currentTimeMillis()
        val elapsed = currentTime - (lastAlertTimestamps[key] ?: 0L)

        if (elapsed < ALERT_COOLDOWN_MILLIS) {
            val remainingMinutes = ((ALERT_COOLDOWN_MILLIS - elapsed) / 60_000L).toInt() + 1
            return AlertResult.OnCooldown(remainingMinutes)
        }

        lastAlertTimestamps[key] = currentTime
        _alertStateVersion.update { it + 1 }
        val taskTitle = "Reponer ${product.displayName} · ${product.locationText}" +
            if (product.quantityNeeded > 0) " (${product.quantityNeeded} un.)" else ""
        assignTask(worker, taskTitle, isPriority = true)
        return AlertResult.Sent
    }

    fun assignTask(worker: User, title: String, isPriority: Boolean = false) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        val newTask = TaskItem(
            id = UUID.randomUUID().toString(),
            userId = worker.id,
            userName = worker.name,
            storeCode = worker.storeCode,
            title = clean,
            isPriority = isPriority
        )
        _tasks.update { listOf(newTask) + it }
    }

    fun toggleTaskCompleted(taskId: String) {
        var completedNow = false
        _tasks.update { list ->
            list.map { task ->
                if (task.id == taskId) {
                    completedNow = !task.isCompleted
                    task.copy(isCompleted = !task.isCompleted)
                } else {
                    task
                }
            }
        }
        if (completedNow) postMessage("Tarea marcada como completada")
    }

    /** Busca un usuario por su nombre de usuario o nombre (sin importar tildes ni mayúsculas). */
    fun loginWithUsername(username: String): Boolean {
        val clean = username.normalizeForSearch()
        if (clean.isEmpty()) return false
        val found = registeredUsers.find { user ->
            user.username == clean ||
                user.name.normalizeForSearch() == clean ||
                user.name.normalizeForSearch().substringBefore(" ") == clean
        } ?: if (clean == "supervisor" || clean == "roberto") registeredUsers.find { it.role == UserRole.ADMIN } else null

        if (found != null) {
            loginAs(found)
            return true
        }
        return false
    }

    fun loginAs(user: User) {
        _currentUser.value = user
        _currentTab.value = defaultTabFor(user)
        prefs.edit().putString(KEY_USER_ID, user.id).apply()
    }

    fun logout() {
        _currentUser.value = null
        _searchQuery.value = ""
        _teamSearchQuery.value = ""
        _selectedCommuneFilter.value = ALL_COMMUNES
        _stockFilter.value = StockFilter.ALL
        _selectedProductId.value = null
        closeAddProductDialog()
        prefs.edit().remove(KEY_USER_ID).apply()
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

    fun setStockFilter(filter: StockFilter) {
        _stockFilter.value = filter
    }

    fun toggleDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }

    fun refreshNetworkStatus() {
        val online = repository.refreshConnectivity()
        postMessage(
            if (online) "Conectado: los cambios se sincronizan con la nube"
            else "Sin conexión: los cambios quedan guardados en el equipo"
        )
    }

    fun setTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectProduct(product: Product?) {
        _selectedProductId.value = product?.id
    }

    fun adjustStock(productId: String, delta: Int) {
        repository.adjustStock(productId, delta)
    }

    fun updateProduct(product: Product) {
        repository.updateProduct(product)
        postMessage("Cambios guardados")
    }

    fun findProductByBarcode(barcode: String): Product? = repository.getProductByBarcode(barcode)

    fun isBarcodeTaken(barcode: String, exceptProductId: String? = null): Boolean =
        products.value.any { it.barcode == barcode.trim() && it.id != exceptProductId }

    fun openAddProduct(barcode: String = "") {
        _scannedBarcodeForNew.value = barcode
        _isAddProductDialogOpen.value = true
    }

    fun closeAddProductDialog() {
        _isAddProductDialogOpen.value = false
        _scannedBarcodeForNew.value = ""
    }

    fun addNewProduct(product: Product) {
        val newProduct = product.copy(
            id = UUID.randomUUID().toString(),
            barcode = product.barcode.trim(),
            name = product.name.trim(),
            brand = product.brand.trim(),
            isOutOfStock = product.isOutOfStock || product.stockOnShelf <= 0,
            aisle = product.aisle.trim(),
            locationDetail = product.locationDetail.trim()
        )
        repository.addProduct(newProduct)
        closeAddProductDialog()
        postMessage("Producto \"${newProduct.name}\" registrado")
    }

    fun postMessage(message: String) {
        _messages.trySend(message)
    }

    override fun onCleared() {
        repository.close()
        super.onCleared()
    }

    private fun defaultTabFor(user: User?): AppTab =
        if (user?.isSupervisor == true) AppTab.TEAM else AppTab.CATALOG

    private fun isSystemDark(application: Application): Boolean =
        (application.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    companion object {
        const val ALL_COMMUNES = "Todas"
        private const val PREFS_NAME = "bodega_lider_prefs"
        private const val KEY_USER_ID = "current_user_id"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val ALERT_COOLDOWN_MILLIS = 60 * 60 * 1000L
    }
}
