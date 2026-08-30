package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.config.AppConfig
import com.example.data.local.AppPreferencesRepository
import com.example.util.InvoicePdfGenerator
import com.example.data.model.AppUser
import com.example.data.model.CartItem
import com.example.data.model.Combo
import com.example.data.model.ComboComponente
import com.example.data.model.GananciasMes
import com.example.data.model.Movimiento
import com.example.data.model.PriceMode
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.data.model.StockFilter
import com.example.data.model.TipoMovimiento
import com.example.data.model.UserSession
import com.example.data.model.UsuarioGanancia
import com.example.data.remote.GananciasApiService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.example.data.local.TermicoudDatabase
import com.example.data.local.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class InventoryViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val preferencesRepo = AppPreferencesRepository(application)
    private val localDb = TermicoudDatabase.getDatabase(application)
    private val productDao = localDb.productDao()
    private val saleDao = localDb.saleDao()

    // Firestore listener registrations
    private var productsListener: ListenerRegistration? = null
    private var salesListener: ListenerRegistration? = null
    private var movementsListener: ListenerRegistration? = null
    private var combosListener: ListenerRegistration? = null
    private var rateDocListener: ListenerRegistration? = null
    private var userDocListener: ListenerRegistration? = null
    private var allUsersListener: ListenerRegistration? = null
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    companion object {
        const val ADMIN_EMAIL = AppConfig.ADMIN_EMAIL
        const val UMBRAL_STOCK_BAJO = 5
    }

    // Helper to get strictly verified lowercase auth email matching Firestore Security Rules
    private fun getCurrentAuthEmail(): String {
        return (auth.currentUser?.email ?: _currentUser.value?.email ?: "").lowercase().trim()
    }

    // Session & User Approval State
    private val _currentUser = MutableStateFlow<UserSession?>(null)
    val currentUser: StateFlow<UserSession?> = _currentUser.asStateFlow()

    private val _appUser = MutableStateFlow<AppUser?>(null)
    val appUser: StateFlow<AppUser?> = _appUser.asStateFlow()

    private val _allUsers = MutableStateFlow<List<AppUser>>(emptyList())
    val allUsers: StateFlow<List<AppUser>> = _allUsers.asStateFlow()

    private val _isCheckingUserApproval = MutableStateFlow(false)
    val isCheckingUserApproval: StateFlow<Boolean> = _isCheckingUserApproval.asStateFlow()

    val isUserApproved: StateFlow<Boolean> = _appUser.map { user ->
        user != null && user.estado.equals("aprobado", ignoreCase = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isCurrentUserAdmin: StateFlow<Boolean> = _appUser.map { user ->
        user != null && (user.rol.equals("admin", ignoreCase = true) || AppConfig.isUserAdmin(user.email))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // User preferences & settings
    val activeUser: StateFlow<String> = preferencesRepo.activeUser
    val exchangeRate: StateFlow<Double> = preferencesRepo.exchangeRate
    val isUserSelected: StateFlow<Boolean> = preferencesRepo.isUserSelected
    val backendUrl: StateFlow<String> = preferencesRepo.backendUrl

    private val _tasaActualizada = MutableStateFlow<String?>(null)
    val tasaActualizada: StateFlow<String?> = _tasaActualizada.asStateFlow()

    private val _tasaUsuario = MutableStateFlow<String?>(null)
    val tasaUsuario: StateFlow<String?> = _tasaUsuario.asStateFlow()

    // Navigation & Tab state (0: Inicio, 1: Inventario, 2: Salida, 3: Entrada, 4: Combos, 5: Ganancias, 6: Tasa)
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Combos Section State
    private val _combos = MutableStateFlow<List<Combo>>(emptyList())
    val combos: StateFlow<List<Combo>> = _combos.asStateFlow()

    private val _isLoadingCombos = MutableStateFlow(false)
    val isLoadingCombos: StateFlow<Boolean> = _isLoadingCombos.asStateFlow()

    // Ganancias Section State
    private val _gananciasActuales = MutableStateFlow<GananciasMes?>(null)
    val gananciasActuales: StateFlow<GananciasMes?> = _gananciasActuales.asStateFlow()

    private val _historialMeses = MutableStateFlow<List<String>>(emptyList())
    val historialMeses: StateFlow<List<String>> = _historialMeses.asStateFlow()

    private val _gananciasMesArchivado = MutableStateFlow<GananciasMes?>(null)
    val gananciasMesArchivado: StateFlow<GananciasMes?> = _gananciasMesArchivado.asStateFlow()

    private val _selectedArchivedMonth = MutableStateFlow<String?>(null)
    val selectedArchivedMonth: StateFlow<String?> = _selectedArchivedMonth.asStateFlow()

    private val _isLoadingGanancias = MutableStateFlow(false)
    val isLoadingGanancias: StateFlow<Boolean> = _isLoadingGanancias.asStateFlow()

    // Search & Filter state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Todos")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _stockFilter = MutableStateFlow(StockFilter.TODOS)
    val stockFilter: StateFlow<StockFilter> = _stockFilter.asStateFlow()

    fun setStockFilter(filter: StockFilter) {
        _stockFilter.value = filter
    }

    // Real-time Firestore StateFlows
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _salesHistory = MutableStateFlow<List<Sale>>(emptyList())
    val salesHistory: StateFlow<List<Sale>> = _salesHistory.asStateFlow()

    private val _movimientos = MutableStateFlow<List<Movimiento>>(emptyList())
    val movimientos: StateFlow<List<Movimiento>> = _movimientos.asStateFlow()

    // Filtered products flow based on category, search query, and stock level with 300ms debounce
    @OptIn(FlowPreview::class)
    val filteredProducts: StateFlow<List<Product>> = combine(
        products,
        _searchQuery.debounce { query -> if (query.isBlank()) 0L else 300L },
        _selectedCategory,
        _stockFilter
    ) { allProducts, query, category, filter ->
        allProducts.filter { product ->
            val matchesCategory = (category == "Todos" || product.catalogo.equals(category, ignoreCase = true))
            val matchesQuery = query.isBlank() ||
                    product.producto.contains(query, ignoreCase = true) ||
                    product.catalogo.contains(query, ignoreCase = true) ||
                    product.marca.contains(query, ignoreCase = true) ||
                    product.codigo.contains(query, ignoreCase = true) ||
                    product.codigoBarras.contains(query, ignoreCase = true)

            val matchesStock = when (filter) {
                StockFilter.TODOS -> true
                StockFilter.AGOTADOS -> product.cantidad <= 0
                StockFilter.STOCK_BAJO -> product.cantidad in 1..UMBRAL_STOCK_BAJO
            }
            matchesCategory && matchesQuery && matchesStock
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Unique Categories List
    val categories: StateFlow<List<String>> = products.combine(MutableStateFlow(Unit)) { allProducts, _ ->
        val list = mutableListOf("Todos")
        val unique = allProducts.map { it.catalogo }.filter { it.isNotBlank() }.distinct().sorted()
        list.addAll(unique)
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Todos"))

    // Cart for Salida / Venta Rápida
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    // UI Status & BottomSheets
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _selectedProductForBottomSheet = MutableStateFlow<Product?>(null)
    val selectedProductForBottomSheet: StateFlow<Product?> = _selectedProductForBottomSheet.asStateFlow()

    private val _completedSale = MutableStateFlow<Sale?>(null)
    val completedSale: StateFlow<Sale?> = _completedSale.asStateFlow()

    init {
        initAuthListener()
        fetchExchangeRateFromBackend()
        // Carga inicial ultra-rápida desde Room Database local (soporte offline e instant startup)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                productDao.getAllProducts().collect { localEntities ->
                    if (_products.value.isEmpty() && localEntities.isNotEmpty()) {
                        val localList = localEntities.map { it.toDomain() }.sortedWith(compareBy({ it.catalogo }, { it.producto }))
                        _products.value = localList
                    }
                }
            } catch (e: Exception) {
                Log.w("InventoryViewModel", "Aviso cargando cache local Room: ${e.message}")
            }
        }
    }

    private fun listenToSharedExchangeRate() {
        rateDocListener?.remove()
        rateDocListener = firestore.collection("configuracion").document("tasa_bcv")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("InventoryViewModel", "Aviso escuchando tasa_bcv en Firestore (${error.message}). Usando tasa configurada o por defecto.")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val tasa = (snapshot.getDouble("tasa") ?: (snapshot.get("tasa") as? Number)?.toDouble()) ?: 0.0
                    val actualizada = snapshot.getString("actualizada") ?: snapshot.getString("fecha")
                    val usuario = snapshot.getString("usuario") ?: snapshot.getString("usuarioNombre")
                    if (tasa > 0.0) {
                        preferencesRepo.setExchangeRate(tasa)
                        _tasaActualizada.value = actualizada
                        _tasaUsuario.value = usuario
                    }
                }
            }
    }

    fun fetchExchangeRateFromBackend() {
        val url = preferencesRepo.backendUrl.value.trim()
        viewModelScope.launch {
            if (url.isNotBlank()) {
                val result = GananciasApiService.obtenerTasa(url)
                result.onSuccess { data ->
                    if (data.tasa > 0.0) {
                        preferencesRepo.setExchangeRate(data.tasa)
                        _tasaActualizada.value = data.actualizada
                        _tasaUsuario.value = data.usuario

                        // Sync to Firestore shared config only if authenticated and approved
                        if (_appUser.value?.estado.equals("aprobado", ignoreCase = true)) {
                            try {
                                val updateMap = mutableMapOf<String, Any>(
                                    "tasa" to data.tasa,
                                    "timestamp" to System.currentTimeMillis()
                                )
                                data.actualizada?.let { updateMap["actualizada"] = it }
                                data.usuario?.let { updateMap["usuario"] = it }
                                firestore.collection("configuracion").document("tasa_bcv").set(updateMap, SetOptions.merge())
                            } catch (e: Exception) {
                                Log.w("InventoryViewModel", "No se pudo sincronizar tasa a Firestore: ${e.message}")
                            }
                        }
                    }
                }.onFailure { e ->
                    Log.w("InventoryViewModel", "Error obteniendo tasa de backend: ${e.message}")
                }
            }
        }
    }

    private fun initAuthListener() {
        val initialUser = auth.currentUser
        if (initialUser != null) {
            val session = UserSession(
                uid = initialUser.uid,
                email = initialUser.email ?: "",
                displayName = initialUser.displayName ?: initialUser.email ?: "Operador",
                photoUrl = initialUser.photoUrl?.toString()
            )
            _currentUser.value = session
            preferencesRepo.setActiveUser(session.displayName)
            syncUserProfile(initialUser.email ?: "", initialUser.displayName ?: "Operador")
        }

        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                val session = UserSession(
                    uid = user.uid,
                    email = user.email ?: "",
                    displayName = user.displayName ?: user.email ?: "Operador",
                    photoUrl = user.photoUrl?.toString()
                )
                _currentUser.value = session
                preferencesRepo.setActiveUser(session.displayName)
                syncUserProfile(user.email ?: "", user.displayName ?: "Operador")
            } else {
                _currentUser.value = null
                _appUser.value = null
                _allUsers.value = emptyList()
                stopAllListeners()
                _products.value = emptyList()
                _salesHistory.value = emptyList()
                _movimientos.value = emptyList()
            }
        }
        auth.addAuthStateListener(authStateListener!!)
    }

    fun syncUserProfile(email: String, displayName: String) {
        if (email.isBlank()) return
        val normalizedEmail = email.lowercase().trim()
        val isAdminEmail = AppConfig.isUserAdmin(normalizedEmail)

        _isCheckingUserApproval.value = true

        // Clean previous doc listener if any
        userDocListener?.remove()

        val userDocRef = firestore.collection("usuarios").document(normalizedEmail)

        // Real-time listener for current user document
        userDocListener = userDocRef.addSnapshotListener { snapshot, error ->
            _isCheckingUserApproval.value = false
            if (error != null) {
                Log.w("InventoryViewModel", "Aviso leyendo estado de usuario (${error.message}).")
                if (_appUser.value == null) {
                    val fallbackUser = AppUser(
                        email = normalizedEmail,
                        nombre = displayName,
                        estado = if (isAdminEmail) "aprobado" else "pendiente",
                        fechaSolicitud = System.currentTimeMillis(),
                        fechaAprobacion = if (isAdminEmail) System.currentTimeMillis() else null,
                        aprobadoPorEmail = if (isAdminEmail) "Sistema" else null,
                        rol = if (isAdminEmail) "admin" else "operador"
                    )
                    _appUser.value = fallbackUser
                    if (isAdminEmail) {
                        startFirestoreDataListeners()
                        listenToAllUsers()
                    } else {
                        stopFirestoreDataListeners()
                    }
                }
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val rawEstado = snapshot.getString("estado") ?: if (isAdminEmail) "aprobado" else "pendiente"
                val estado = if (isAdminEmail) "aprobado" else rawEstado
                val user = AppUser(
                    email = snapshot.getString("email") ?: normalizedEmail,
                    nombre = snapshot.getString("nombre") ?: displayName,
                    estado = estado,
                    fechaSolicitud = snapshot.getLong("fechaSolicitud") ?: System.currentTimeMillis(),
                    fechaAprobacion = if (isAdminEmail) (snapshot.getLong("fechaAprobacion") ?: System.currentTimeMillis()) else snapshot.getLong("fechaAprobacion"),
                    aprobadoPorEmail = if (isAdminEmail) (snapshot.getString("aprobadoPorEmail") ?: "Sistema") else snapshot.getString("aprobadoPorEmail"),
                    rol = if (isAdminEmail) "admin" else (snapshot.getString("rol") ?: "operador")
                )
                _appUser.value = user

                // If user is approved, start syncing Firestore data
                if (user.estado.equals("aprobado", ignoreCase = true)) {
                    startFirestoreDataListeners()
                    if (user.rol.equals("admin", ignoreCase = true) || isAdminEmail) {
                        listenToAllUsers()
                    }
                } else {
                    stopFirestoreDataListeners()
                }
            } else {
                // First-time login: create record in "usuarios"
                val initialEstado = if (isAdminEmail) "aprobado" else "pendiente"
                val initialRol = if (isAdminEmail) "admin" else "operador"
                val now = System.currentTimeMillis()

                val newUserMap = mutableMapOf<String, Any?>(
                    "email" to normalizedEmail,
                    "nombre" to displayName,
                    "estado" to initialEstado,
                    "fechaSolicitud" to now,
                    "rol" to initialRol
                )

                if (isAdminEmail) {
                    newUserMap["fechaAprobacion"] = now
                    newUserMap["aprobadoPorEmail"] = "Sistema"
                }

                userDocRef.set(newUserMap.filterValues { it != null }, com.google.firebase.firestore.SetOptions.merge()).addOnSuccessListener {
                    _appUser.value = AppUser(
                        email = normalizedEmail,
                        nombre = displayName,
                        estado = initialEstado,
                        fechaSolicitud = now,
                        fechaAprobacion = if (isAdminEmail) now else null,
                        aprobadoPorEmail = if (isAdminEmail) "Sistema" else null,
                        rol = initialRol
                    )

                    if (initialEstado == "aprobado") {
                        startFirestoreDataListeners()
                        if (isAdminEmail) {
                            listenToAllUsers()
                        }
                    } else {
                        stopFirestoreDataListeners()
                    }
                }.addOnFailureListener { e ->
                    Log.w("InventoryViewModel", "Aviso registrando usuario: ${e.message}.")
                    _appUser.value = AppUser(
                        email = normalizedEmail,
                        nombre = displayName,
                        estado = if (isAdminEmail) "aprobado" else "pendiente",
                        fechaSolicitud = now,
                        fechaAprobacion = if (isAdminEmail) now else null,
                        aprobadoPorEmail = if (isAdminEmail) "Sistema" else null,
                        rol = initialRol
                    )
                    if (isAdminEmail) {
                        startFirestoreDataListeners()
                    } else {
                        stopFirestoreDataListeners()
                    }
                    _isCheckingUserApproval.value = false
                }
            }
        }
    }

    fun ensureAllUsersLoaded() {
        if (allUsersListener != null) return
        // No requerir orderBy para evitar fallos por índices faltantes en Firestore
        allUsersListener = firestore.collection("usuarios")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("InventoryViewModel", "Aviso escuchando usuarios (${error.message})")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val userList = snapshot.documents.mapNotNull { doc ->
                        try {
                            AppUser(
                                email = doc.getString("email") ?: doc.id,
                                nombre = doc.getString("nombre") ?: "",
                                estado = doc.getString("estado") ?: "pendiente",
                                fechaSolicitud = doc.getLong("fechaSolicitud") ?: System.currentTimeMillis(),
                                fechaAprobacion = doc.getLong("fechaAprobacion"),
                                aprobadoPorEmail = doc.getString("aprobadoPorEmail"),
                                rol = doc.getString("rol") ?: "operador"
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }.sortedByDescending { it.fechaSolicitud }
                    _allUsers.value = userList
                }
            }
    }

    private fun listenToAllUsers() {
        ensureAllUsersLoaded()
    }

    fun approveUser(targetEmail: String) {
        val adminEmail = _currentUser.value?.email ?: ADMIN_EMAIL
        val normalized = targetEmail.lowercase().trim()

        viewModelScope.launch {
            try {
                firestore.collection("usuarios").document(normalized)
                    .set(
                        mapOf(
                            "email" to normalized,
                            "estado" to "aprobado",
                            "fechaAprobacion" to System.currentTimeMillis(),
                            "aprobadoPorEmail" to adminEmail
                        ),
                        com.google.firebase.firestore.SetOptions.merge()
                    ).await()
                _successMessage.value = "Usuario $normalized aprobado con éxito"
            } catch (e: Exception) {
                Log.e("InventoryViewModel", "Error aprobando usuario: ${e.message}")
                _errorMessage.value = "Error al aprobar usuario: ${e.localizedMessage}"
            }
        }
    }

    fun rejectUser(targetEmail: String) {
        val normalized = targetEmail.lowercase().trim()

        viewModelScope.launch {
            try {
                firestore.collection("usuarios").document(normalized)
                    .set(
                        mapOf(
                            "email" to normalized,
                            "estado" to "rechazado"
                        ),
                        com.google.firebase.firestore.SetOptions.merge()
                    ).await()
                _successMessage.value = "Acceso revocado para $normalized"
            } catch (e: Exception) {
                Log.e("InventoryViewModel", "Error rechazando usuario: ${e.message}")
                _errorMessage.value = "Error al actualizar usuario: ${e.localizedMessage}"
            }
        }
    }

    fun preApproveOperator(email: String, name: String) {
        val adminEmail = _currentUser.value?.email ?: ADMIN_EMAIL
        val normalized = email.lowercase().trim()
        if (normalized.isBlank()) return

        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val userData = mapOf(
                    "email" to normalized,
                    "nombre" to name.ifBlank { "Operador" },
                    "estado" to "aprobado",
                    "rol" to "operador",
                    "fechaSolicitud" to now,
                    "fechaAprobacion" to now,
                    "aprobadoPorEmail" to adminEmail
                )
                firestore.collection("usuarios").document(normalized)
                    .set(userData, com.google.firebase.firestore.SetOptions.merge())
                    .await()
                _successMessage.value = "Operador $normalized autorizado y pre-aprobado"
            } catch (e: Exception) {
                Log.e("InventoryViewModel", "Error pre-aprobando operador: ${e.message}")
                _errorMessage.value = "Error al autorizar operador: ${e.localizedMessage}"
            }
        }
    }

    fun refreshUserStatus() {
        val email = _currentUser.value?.email ?: auth.currentUser?.email ?: return
        val name = _currentUser.value?.displayName ?: auth.currentUser?.displayName ?: "Operador"
        syncUserProfile(email, name)
    }

    fun signOut() {
        auth.signOut()
        _currentUser.value = null
        _appUser.value = null
        _allUsers.value = emptyList()
        stopAllListeners()
        _products.value = emptyList()
        _salesHistory.value = emptyList()
        _movimientos.value = emptyList()
    }

    private fun Any?.toSafeInt(default: Int = 0): Int {
        return when (this) {
            is Number -> this.toInt()
            is String -> {
                val clean = this.trim()
                clean.toIntOrNull() ?: clean.toDoubleOrNull()?.toInt() ?: default
            }
            else -> default
        }
    }

    private fun Any?.toSafeDouble(default: Double = 0.0): Double {
        return when (this) {
            is Number -> this.toDouble()
            is String -> {
                val clean = this.trim().replace(",", ".")
                clean.toDoubleOrNull() ?: default
            }
            else -> default
        }
    }

    private fun Any?.toSafeString(default: String = ""): String {
        return when (this) {
            null -> default
            is String -> this.trim()
            else -> this.toString().trim()
        }
    }

    private fun startFirestoreDataListeners() {
        if (productsListener != null) return // Already active

        _isSyncing.value = true
        listenToSharedExchangeRate()

        // 1. Real-time Products listener (Core catalog)
        productsListener = firestore.collection("productos")
            .addSnapshotListener { snapshot, error ->
                _isSyncing.value = false
                if (error != null) {
                    Log.w("InventoryViewModel", "Aviso escuchando productos en Firestore (${error.message}). Usando base de datos local.")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    if (snapshot.isEmpty) {
                        // Seed initial inventory if first time empty
                        seedInitialProductsIfEmpty()
                    } else {
                        val productList = snapshot.documents.mapIndexedNotNull { index, doc ->
                            try {
                                val data = doc.data ?: emptyMap<String, Any>()
                                val id = doc.id.ifBlank { "prod_${index + 1}" }

                                val rawFila = data["fila"] ?: data["row"] ?: (index + 1)
                                val fila = rawFila.toSafeInt(default = index + 1)

                                val producto = (data["producto"] ?: data["Producto"] ?: data["nombre"] ?: data["Nombre"]
                                    ?: data["name"] ?: data["descripcion"] ?: data["description"] ?: "").toSafeString()

                                if (producto.isBlank()) {
                                    return@mapIndexedNotNull null
                                }

                                val cantidad = (data["cantidad"] ?: data["Cantidad"] ?: data["stock"] ?: data["Stock"]
                                    ?: data["qty"] ?: 0).toSafeInt(default = 0)

                                val precioUsd = (data["precioUsd"] ?: data["PrecioUsd"] ?: data["precio"] ?: data["Precio"]
                                    ?: data["price"] ?: data["precio_usd"] ?: 0.0).toSafeDouble(default = 0.0)

                                val precioCompra = when {
                                    data.containsKey("precioCompra") -> (data["precioCompra"] ?: 0.0).toSafeDouble()
                                    data.containsKey("precio_compra") -> (data["precio_compra"] ?: 0.0).toSafeDouble()
                                    data.containsKey("costo") -> (data["costo"] ?: 0.0).toSafeDouble()
                                    data.containsKey("cost") -> (data["cost"] ?: 0.0).toSafeDouble()
                                    else -> 0.0
                                }

                                val precioMayor = when {
                                    data.containsKey("precioMayor") -> (data["precioMayor"] ?: 0.0).toSafeDouble().let { if (it > 0.0) it else null }
                                    data.containsKey("precio_mayor") -> (data["precio_mayor"] ?: 0.0).toSafeDouble().let { if (it > 0.0) it else null }
                                    else -> null
                                }
                                val cantidadMinimaMayor = when {
                                    data.containsKey("cantidadMinimaMayor") -> (data["cantidadMinimaMayor"] ?: 0).toSafeInt().let { if (it > 0) it else null }
                                    data.containsKey("cantidad_minima_mayor") -> (data["cantidad_minima_mayor"] ?: 0).toSafeInt().let { if (it > 0) it else null }
                                    else -> null
                                }

                                val catalogo = (data["catalogo"] ?: data["Catalogo"] ?: data["categoria"] ?: data["Categoria"]
                                    ?: data["category"] ?: "General").toSafeString(default = "General")

                                val codigo = (data["codigo"] ?: data["Codigo"] ?: data["code"] ?: data["sku"] ?: "").toSafeString()
                                val codigoBarras = (data["codigoBarras"] ?: data["barcode"] ?: data["codigo_barras"] ?: data["codBarras"] ?: "").toSafeString()
                                val marca = (data["marca"] ?: data["Marca"] ?: data["brand"] ?: "").toSafeString()
                                val modelo = (data["modelo"] ?: data["Modelo"] ?: data["model"] ?: "").toSafeString()
                                val ubicacion = (data["ubicacion"] ?: data["Ubicacion"] ?: data["location"] ?: "").toSafeString()
                                val minStock = (data["minStock"] ?: data["min_stock"] ?: data["stockMinimo"] ?: 5).toSafeInt(default = 5)

                                Product(
                                    fila = fila,
                                    id = id,
                                    producto = producto,
                                    cantidad = cantidad,
                                    precioUsd = precioUsd,
                                    precioCompra = precioCompra,
                                    precioMayor = precioMayor,
                                    cantidadMinimaMayor = cantidadMinimaMayor,
                                    catalogo = catalogo,
                                    codigo = codigo,
                                    codigoBarras = codigoBarras,
                                    marca = marca,
                                    modelo = modelo,
                                    ubicacion = ubicacion,
                                    minStock = minStock
                                )
                            } catch (e: Throwable) {
                                Log.e("InventoryViewModel", "Error parseando producto ${doc.id}: ${e.message}", e)
                                null
                            }
                        }.sortedWith(compareBy({ it.catalogo }, { it.producto }))

                        _products.value = productList
                        // Sincronizar cache persistente en Room Database
                        viewModelScope.launch(Dispatchers.IO) {
                            try {
                                productDao.insertOrUpdateAll(productList.map { it.toEntity() })
                            } catch (e: Exception) {
                                Log.w("InventoryViewModel", "Aviso sincronizando Room: ${e.message}")
                            }
                        }
                    }
                }
            }

        // Lazy pre-fetch recent sales for Dashboard
        ensureSalesLoaded(limit = 20)
    }

    /**
     * Lazy Listener for Sales: attached only when needed (Dashboard preview, Historial, Ganancias)
     */
    fun ensureSalesLoaded(limit: Long = 100) {
        if (salesListener != null) return

        salesListener = firestore.collection("ventas")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("InventoryViewModel", "Aviso escuchando ventas (${error.message})")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val currentProducts = _products.value
                    val salesList = snapshot.documents.mapNotNull { doc ->
                        try {
                            val itemsRaw = doc.get("items") as? List<Map<String, Any>> ?: emptyList()
                            val items = itemsRaw.map { m ->
                                val prodName = (m["producto"] as? String) ?: (m["nombre"] as? String) ?: (m["productoNombre"] as? String) ?: ""
                                val tipo = (m["tipo"] as? String) ?: "producto"
                                val precio = (m["precioUsd"] as? Number)?.toDouble() ?: (m["precio_usd"] as? Number)?.toDouble() ?: (m["precio"] as? Number)?.toDouble() ?: 0.0
                                val precioCompraRaw = (m["precioCompra"] as? Number)?.toDouble() ?: (m["precio_compra"] as? Number)?.toDouble() ?: (m["costo"] as? Number)?.toDouble() ?: 0.0
                                val cant = (m["cantidad"] as? Number)?.toInt() ?: (m["qty"] as? Number)?.toInt() ?: 0
                                val fila = (m["fila"] as? Number)?.toInt() ?: 0
                                
                                val compRaw = m["componentes"] as? List<Map<String, Any>> ?: emptyList()
                                val componentes = compRaw.map { c ->
                                    ComboComponente(
                                        fila = (c["fila"] as? Number)?.toInt() ?: 0,
                                        nombre = (c["nombre"] as? String) ?: "",
                                        cantidadPorCombo = (c["cantidadPorCombo"] as? Number)?.toInt() ?: 1,
                                        precioCompraUnitario = (c["precioCompraUnitario"] as? Number)?.toDouble() ?: 0.0,
                                        precioVentaUnitario = (c["precioVentaUnitario"] as? Number)?.toDouble() ?: 0.0
                                    )
                                }

                                val effectivePrecioCompra = when {
                                    precioCompraRaw > 0.0 -> precioCompraRaw
                                    tipo == "combo" && componentes.isNotEmpty() -> componentes.sumOf { it.precioCompraUnitario * it.cantidadPorCombo }
                                    else -> {
                                        val matched = currentProducts.find { p ->
                                            p.producto.equals(prodName, ignoreCase = true) || (p.codigo.isNotBlank() && p.codigo.equals(prodName, ignoreCase = true))
                                        }
                                        matched?.precioCompra ?: 0.0
                                    }
                                }

                                SaleItem(
                                    fila = fila,
                                    producto = prodName,
                                    cantidad = cant,
                                    precioUsd = precio,
                                    precioCompra = effectivePrecioCompra,
                                    tipo = tipo,
                                    componentes = componentes
                                )
                            }

                            val sellerName = doc.getString("usuario") ?: doc.getString("usuarioNombre") ?: doc.getString("vendedor") ?: "Operador"
                            val clientName = doc.getString("clienteNombre") ?: doc.getString("cliente") ?: ""
                            val clientCed = doc.getString("clienteCedula") ?: doc.getString("cedula") ?: ""
                            val payMethod = doc.getString("metodoPago") ?: ""
                            val folio = doc.getString("folio") ?: doc.id.takeLast(6).uppercase()

                            Sale(
                                id = doc.id,
                                folio = folio,
                                clienteNombre = clientName,
                                clienteCedula = clientCed,
                                metodoPago = payMethod,
                                usuario = sellerName,
                                usuarioEmail = doc.getString("usuarioEmail") ?: "",
                                timestamp = doc.getLong("timestamp") ?: doc.getLong("fecha") ?: System.currentTimeMillis(),
                                totalUsd = (doc.getDouble("totalUsd") ?: doc.getDouble("total_usd") ?: (doc.get("totalUsd") as? Number)?.toDouble() ?: (doc.get("total_usd") as? Number)?.toDouble()) ?: 0.0,
                                totalBs = (doc.getDouble("totalBs") ?: doc.getDouble("total_bs") ?: (doc.get("totalBs") as? Number)?.toDouble() ?: (doc.get("total_bs") as? Number)?.toDouble()) ?: 0.0,
                                tasaBcv = (doc.getDouble("tasaBcv") ?: doc.getDouble("tasa_bcv") ?: (doc.get("tasaBcv") as? Number)?.toDouble()) ?: 0.0,
                                items = items,
                                esReversado = doc.getBoolean("esReversado") ?: false,
                                fechaReverso = doc.getLong("fechaReverso"),
                                reversadoPorNombre = doc.getString("reversadoPorNombre"),
                                reversadoPorEmail = doc.getString("reversadoPorEmail")
                            )
                        } catch (e: Exception) {
                            Log.e("InventoryViewModel", "Error parseando venta ${doc.id}: ${e.message}")
                            null
                        }
                    }
                    _salesHistory.value = salesList
                    val localCalc = computeLocalGananciasCurrentMonth()
                    if (_gananciasActuales.value == null || (_gananciasActuales.value?.usuarios?.isEmpty() == true && localCalc.usuarios.isNotEmpty())) {
                        _gananciasActuales.value = localCalc
                    }
                }
            }
    }

    /**
     * Lazy Listener for Movements: attached only when navigating to Movements / Full History
     */
    fun ensureMovementsLoaded(limit: Long = 150) {
        if (movementsListener != null) return

        movementsListener = firestore.collection("movimientos")
            .orderBy("fecha", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("InventoryViewModel", "Aviso escuchando movimientos (${error.message})")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val movs = snapshot.documents.mapNotNull { doc ->
                        try {
                            val tipoStr = doc.getString("tipo") ?: "ENTRADA"
                            val tipoEnum = try {
                                TipoMovimiento.valueOf(tipoStr)
                            } catch (e: Exception) {
                                TipoMovimiento.ENTRADA
                            }

                            Movimiento(
                                id = doc.id,
                                productoId = doc.getString("productoId") ?: "",
                                productoFila = doc.getLong("productoFila")?.toInt() ?: 0,
                                productoNombre = doc.getString("productoNombre") ?: "",
                                tipo = tipoEnum,
                                cantidad = doc.getLong("cantidad")?.toInt() ?: 0,
                                fecha = doc.getLong("fecha") ?: System.currentTimeMillis(),
                                motivo = doc.getString("motivo") ?: "",
                                precioUnitarioUsd = (doc.getDouble("precioUnitarioUsd") ?: (doc.get("precioUnitarioUsd") as? Number)?.toDouble()) ?: 0.0,
                                usuarioEmail = doc.getString("usuarioEmail") ?: "",
                                usuarioNombre = doc.getString("usuarioNombre") ?: "",
                                esReversado = doc.getBoolean("esReversado") ?: false,
                                fechaReverso = doc.getLong("fechaReverso"),
                                reversadoPorEmail = doc.getString("reversadoPorEmail")
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    _movimientos.value = movs
                }
            }
    }

    /**
     * Lazy Listener for Combos: attached only when needed (Salida with combos or Combos screen)
     */
    fun ensureCombosLoaded() {
        if (combosListener != null) return

        combosListener = firestore.collection("combos")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("InventoryViewModel", "Aviso escuchando combos en Firestore (${error.message})")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val comboList = snapshot.documents.mapNotNull { doc ->
                        try {
                            val fila = (doc.getLong("fila") ?: (doc.get("fila") as? Number)?.toLong())?.toInt() ?: 0
                            val nombre = doc.getString("nombre") ?: doc.getString("producto") ?: ""
                            if (nombre.isBlank()) return@mapNotNull null
                            val precioUsd = (doc.getDouble("precioUsd") ?: doc.getDouble("precio_usd") ?: (doc.get("precioUsd") as? Number)?.toDouble()) ?: 0.0
                            val categoria = doc.getString("categoria") ?: "Combos"

                            val componentesRaw = doc.get("componentes") as? List<Map<String, Any>> ?: emptyList()
                            val costoTotalDoc = (doc.getDouble("costoTotal") ?: doc.getDouble("costo_total") ?: (doc.get("costoTotal") as? Number)?.toDouble()) ?: 0.0

                            val componentesList = componentesRaw.map { comp ->
                                val compFila = (comp["fila"] as? Number)?.toInt() ?: 0
                                val compNombre = comp["nombre"] as? String ?: ""
                                val cantPorCombo = (comp["cantidadPorCombo"] as? Number)?.toInt()
                                    ?: (comp["cantidad"] as? Number)?.toInt()
                                    ?: (comp["cantidad_por_combo"] as? Number)?.toInt() ?: 1
                                val stockDisp = (comp["stockDisponible"] as? Number)?.toInt()
                                    ?: (comp["stock"] as? Number)?.toInt()
                                    ?: (comp["stock_disponible"] as? Number)?.toInt() ?: 0
                                val matchingProd = _products.value.find { it.fila == compFila }
                                val precioCompraUnitario = (comp["precioCompraUnitario"] as? Number)?.toDouble()
                                    ?: (comp["precioCompra"] as? Number)?.toDouble()
                                    ?: (comp["costo"] as? Number)?.toDouble()
                                    ?: matchingProd?.precioCompra ?: 0.0
                                val precioVentaUnitario = (comp["precioVentaUnitario"] as? Number)?.toDouble()
                                    ?: (comp["precioUsd"] as? Number)?.toDouble()
                                    ?: matchingProd?.precioUsd ?: 0.0

                                ComboComponente(
                                    fila = compFila,
                                    nombre = compNombre,
                                    cantidadPorCombo = cantPorCombo,
                                    stockDisponible = stockDisp,
                                    precioCompraUnitario = precioCompraUnitario,
                                    precioVentaUnitario = precioVentaUnitario
                                )
                            }

                            // Compute availability based on current product inventory or backend value
                            val backendDisponibles = (doc.getLong("disponibles") ?: (doc.get("disponibles") as? Number)?.toLong())?.toInt()
                            val calculatedDisponibles = if (componentesList.isEmpty()) 0 else {
                                componentesList.minOfOrNull { comp ->
                                    val currentProd = _products.value.find { it.fila == comp.fila }
                                    val currentStock = currentProd?.cantidad ?: comp.stockDisponible
                                    if (comp.cantidadPorCombo > 0) currentStock / comp.cantidadPorCombo else 0
                                } ?: 0
                            }

                            val finalDisponibles = backendDisponibles ?: calculatedDisponibles
                            val finalCosto = if (costoTotalDoc > 0) costoTotalDoc else componentesList.sumOf { it.precioCompraUnitario * it.cantidadPorCombo }

                            Combo(
                                fila = fila,
                                id = doc.id,
                                nombre = nombre,
                                precioUsd = precioUsd,
                                categoria = categoria,
                                componentes = componentesList,
                                disponibles = finalDisponibles,
                                costoTotal = finalCosto
                            )
                        } catch (e: Exception) {
                            Log.e("InventoryViewModel", "Error parseando combo ${doc.id}: ${e.message}")
                            null
                        }
                    }.sortedBy { it.nombre }

                    _combos.value = comboList
                }
            }
    }

    private fun seedInitialProductsIfEmpty() {
        viewModelScope.launch {
            try {
                val demoProducts = listOf(
                    Product(fila = 2, producto = "Filtro de Aceite PH6607", cantidad = 15, precioUsd = 8.50, catalogo = "Filtros", codigo = "FIL-001", marca = "FRAM", modelo = "PH6607"),
                    Product(fila = 3, producto = "Bujía Iridium BKR6EIX", cantidad = 32, precioUsd = 4.20, catalogo = "Encendido", codigo = "BUJ-002", marca = "NGK", modelo = "BKR6EIX"),
                    Product(fila = 4, producto = "Pastillas de Freno BP902", cantidad = 6, precioUsd = 25.00, catalogo = "Frenos", codigo = "FRE-003", marca = "BOSCH", modelo = "BP902"),
                    Product(fila = 5, producto = "Aceite 20W50 Mineral 1L", cantidad = 24, precioUsd = 6.00, catalogo = "Lubricantes", codigo = "LUB-004", marca = "SHELL", modelo = "Helix HX3"),
                    Product(fila = 6, producto = "Refrigerante 50/50 1 Galón", cantidad = 8, precioUsd = 12.00, catalogo = "Refrigeración", codigo = "REF-005", marca = "PRESTONE", modelo = "AF2100"),
                    Product(fila = 7, producto = "Bomba de Agua WP-142", cantidad = 4, precioUsd = 38.50, catalogo = "Motor", codigo = "MOT-006", marca = "GMB", modelo = "WP-142")
                )

                val batch = firestore.batch()
                demoProducts.forEach { prod ->
                    val docRef = firestore.collection("productos").document("prod_${prod.fila}")
                    batch.set(docRef, prod)
                }
                batch.commit().await()
                Log.d("InventoryViewModel", "Productos iniciales cargados en Firestore con éxito")
            } catch (e: Exception) {
                Log.e("InventoryViewModel", "Error sembrando productos iniciales: ${e.message}")
            }
        }
    }

    fun setUserSession(session: UserSession) {
        _currentUser.value = session
        preferencesRepo.setActiveUser(session.displayName)
        syncUserProfile(session.email, session.displayName)
    }

    fun updateUserProfile(newName: String, newEmail: String? = null) {
        val current = _currentUser.value ?: return
        val updatedEmail = newEmail?.trim()?.ifBlank { current.email } ?: current.email
        val updatedName = newName.trim().ifBlank { current.displayName }
        val updatedSession = current.copy(displayName = updatedName, email = updatedEmail)
        _currentUser.value = updatedSession
        preferencesRepo.setActiveUser(updatedName)
        syncUserProfile(updatedEmail, updatedName)
        _successMessage.value = "Perfil actualizado correctamente"
    }

    fun selectTab(tabIndex: Int) {
        val isAdmin = isCurrentUserAdmin.value
        val effectiveTab = if (tabIndex == 5 && !isAdmin) 0 else tabIndex
        _selectedTab.value = effectiveTab
        when (effectiveTab) {
            0 -> {
                ensureSalesLoaded(limit = 20)
            }
            1, 6 -> {
                // Tab 1 (Inventario), Tab 6 (Tasa)
            }
            2 -> {
                ensureCombosLoaded()
            }
            3 -> {
                // Entrada
            }
            4 -> {
                ensureCombosLoaded()
                fetchCombos()
            }
            5 -> {
                // Ganancias: fetch data and ensure sales are loaded (only for admin)
                if (isAdmin) {
                    ensureSalesLoaded(limit = 100)
                    fetchGanancias()
                    fetchHistorialMeses()
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setActiveUser(user: String) {
        preferencesRepo.setActiveUser(user)
    }

    fun setExchangeRate(rate: Double) {
        if (rate <= 0.0) return
        val userSession = _currentUser.value
        val userName = userSession?.displayName?.ifBlank { null }
            ?: auth.currentUser?.displayName?.ifBlank { null }
            ?: _appUser.value?.nombre?.ifBlank { null }
            ?: activeUser.value.ifBlank { null }
            ?: "Operador"

        val nowFormatted = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.format(java.util.Date())

        // 1. Immediate local update so UI reflects instantaneously
        preferencesRepo.setExchangeRate(rate)
        _tasaActualizada.value = nowFormatted
        _tasaUsuario.value = userName

        // 2. Broadcast via Firestore to all active app instances
        viewModelScope.launch {
            try {
                firestore.collection("configuracion").document("tasa_bcv").set(
                    mapOf(
                        "tasa" to rate,
                        "actualizada" to nowFormatted,
                        "fecha" to nowFormatted,
                        "usuario" to userName,
                        "usuarioEmail" to getCurrentAuthEmail(),
                        "timestamp" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                ).await()
            } catch (e: Exception) {
                Log.w("InventoryViewModel", "Error guardando tasa en Firestore: ${e.message}")
            }

            // 3. Save to backend Google Sheet (guardar_tasa endpoint)
            val url = preferencesRepo.backendUrl.value.trim()
            if (url.isNotBlank()) {
                try {
                    GananciasApiService.guardarTasa(url, rate, userName)
                } catch (e: Exception) {
                    Log.w("InventoryViewModel", "Error guardando tasa en backend: ${e.message}")
                }
            }

            _successMessage.value = "Tasa de cambio actualizada: Bs ${String.format(java.util.Locale.US, "%.2f", rate)}"
        }
    }

    fun syncInventoryCatalog() {
        viewModelScope.launch {
            _isSyncing.value = true
            _errorMessage.value = null
            try {
                // Query Firestore collection to ensure full refresh (Firebase is single source of truth)
                val snapshot = firestore.collection("productos").get().await()
                if (snapshot.isEmpty) {
                    seedInitialProductsIfEmpty()
                } else {
                    val productList = snapshot.documents.mapIndexedNotNull { index, doc ->
                        try {
                            val data = doc.data ?: emptyMap<String, Any>()
                            val id = doc.id.ifBlank { "prod_${index + 1}" }
                            val rawFila = data["fila"] ?: data["row"] ?: (index + 1)
                            val fila = rawFila.toSafeInt(default = index + 1)
                            val producto = (data["producto"] ?: data["Producto"] ?: data["nombre"] ?: data["Nombre"]
                                ?: data["name"] ?: data["descripcion"] ?: data["description"] ?: "").toSafeString()
                            if (producto.isBlank()) return@mapIndexedNotNull null

                            val cantidad = (data["cantidad"] ?: data["Cantidad"] ?: data["stock"] ?: data["Stock"] ?: 0).toSafeInt()
                            val precioUsd = (data["precioUsd"] ?: data["PrecioUsd"] ?: data["precio_usd"] ?: data["precio"] ?: data["Precio"] ?: 0.0).toSafeDouble()
                            val precioMayor = when {
                                data.containsKey("precioMayor") -> (data["precioMayor"] ?: 0.0).toSafeDouble().let { if (it > 0.0) it else null }
                                data.containsKey("precio_mayor") -> (data["precio_mayor"] ?: 0.0).toSafeDouble().let { if (it > 0.0) it else null }
                                else -> null
                            }
                            val cantidadMinimaMayor = when {
                                data.containsKey("cantidadMinimaMayor") -> (data["cantidadMinimaMayor"] ?: 0).toSafeInt().let { if (it > 0) it else null }
                                data.containsKey("cantidad_minima_mayor") -> (data["cantidad_minima_mayor"] ?: 0).toSafeInt().let { if (it > 0) it else null }
                                else -> null
                            }
                            val catalogo = (data["catalogo"] ?: data["Catalogo"] ?: data["categoria"] ?: data["Categoria"] ?: "General").toSafeString(default = "General").ifBlank { "General" }
                            val codigo = (data["codigo"] ?: data["Codigo"] ?: data["code"] ?: data["sku"] ?: "").toSafeString()
                            val codigoBarras = (data["codigoBarras"] ?: data["codigo_barras"] ?: data["barcode"] ?: data["codBarras"] ?: "").toSafeString()
                            val marca = (data["marca"] ?: data["Marca"] ?: data["brand"] ?: "").toSafeString()
                            val modelo = (data["modelo"] ?: data["Modelo"] ?: data["model"] ?: "").toSafeString()
                            val ubicacion = (data["ubicacion"] ?: data["Ubicacion"] ?: data["location"] ?: "").toSafeString()
                            val minStock = (data["minStock"] ?: data["min_stock"] ?: data["stockMinimo"] ?: 5).toSafeInt(default = 5)

                            Product(
                                fila = fila,
                                id = id,
                                producto = producto,
                                cantidad = cantidad,
                                precioUsd = precioUsd,
                                precioMayor = precioMayor,
                                cantidadMinimaMayor = cantidadMinimaMayor,
                                catalogo = catalogo,
                                codigo = codigo,
                                codigoBarras = codigoBarras,
                                marca = marca,
                                modelo = modelo,
                                ubicacion = ubicacion,
                                minStock = minStock
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }.sortedWith(compareBy({ it.catalogo }, { it.producto }))
                    _products.value = productList
                }

                _successMessage.value = "Inventario actualizado"
            } catch (e: Exception) {
                _errorMessage.value = "Error al actualizar inventario: ${e.localizedMessage}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun syncFromRemote() {
        syncInventoryCatalog()
    }

    // BottomSheet controls
    fun openProductBottomSheet(product: Product) {
        _selectedProductForBottomSheet.value = product
    }

    fun dismissBottomSheet() {
        _selectedProductForBottomSheet.value = null
    }

    // Cart actions (Salida / Venta rápida)
    fun addToCart(product: Product, quantityToAdd: Int = 1, priceMode: PriceMode = PriceMode.AUTOMATICO) {
        if (quantityToAdd <= 0) return
        val currentList = _cart.value.toMutableList()
        val existingIndex = currentList.indexOfFirst {
            !it.isCombo && (
                (it.product.id.isNotBlank() && it.product.id == product.id) ||
                (product.fila > 0 && it.product.fila == product.fila) ||
                (it.product.producto.equals(product.producto, ignoreCase = true))
            )
        }

        if (existingIndex >= 0) {
            val existingItem = currentList[existingIndex]
            val updatedQty = existingItem.cantidadSelected + quantityToAdd
            currentList[existingIndex] = existingItem.copy(
                cantidadSelected = updatedQty,
                product = product,
                priceMode = priceMode
            )
        } else {
            currentList.add(CartItem(product = product, cantidadSelected = quantityToAdd, isCombo = false, priceMode = priceMode))
        }
        _cart.value = currentList
        _successMessage.value = "Agregado a la venta: ${product.producto}"
    }

    fun updateCartItemPriceMode(fila: Int, mode: PriceMode) {
        val currentList = _cart.value.toMutableList()
        val idx = currentList.indexOfFirst { !it.isCombo && it.product.fila == fila }
        if (idx >= 0) {
            currentList[idx] = currentList[idx].copy(priceMode = mode)
            _cart.value = currentList
        }
    }

    fun updateCartItemPriceMode(item: CartItem, mode: PriceMode) {
        val currentList = _cart.value.toMutableList()
        val idx = currentList.indexOfFirst {
            if (item.isCombo) {
                it.isCombo && (it.combo?.fila == item.combo?.fila || it.combo?.id == item.combo?.id)
            } else {
                !it.isCombo && (it.product.id == item.product.id || it.product.fila == item.product.fila)
            }
        }
        if (idx >= 0) {
            currentList[idx] = currentList[idx].copy(priceMode = mode)
            _cart.value = currentList
        }
    }

    fun addComboToCart(combo: Combo, quantityToAdd: Int = 1) {
        if (quantityToAdd <= 0) return
        val currentList = _cart.value.toMutableList()
        val existingIndex = currentList.indexOfFirst {
            it.isCombo && it.combo != null && (
                it.combo.fila == combo.fila ||
                it.combo.id == combo.id ||
                it.combo.nombre.equals(combo.nombre, ignoreCase = true)
            )
        }

        if (existingIndex >= 0) {
            val existingItem = currentList[existingIndex]
            val maxDisp = combo.disponibles
            val updatedQty = if (maxDisp > 0) {
                (existingItem.cantidadSelected + quantityToAdd).coerceAtMost(maxDisp)
            } else {
                existingItem.cantidadSelected + quantityToAdd
            }
            currentList[existingIndex] = existingItem.copy(cantidadSelected = updatedQty, combo = combo)
        } else {
            currentList.add(CartItem(cantidadSelected = quantityToAdd, isCombo = true, combo = combo))
        }
        _cart.value = currentList
        _successMessage.value = "Combo agregado a la venta: ${combo.nombre}"
    }

    fun updateCartQuantity(fila: Int, newQuantity: Int) {
        val currentList = _cart.value.toMutableList()
        val idx = currentList.indexOfFirst { it.itemFila == fila }
        if (idx >= 0) {
            if (newQuantity <= 0) {
                currentList.removeAt(idx)
            } else {
                currentList[idx] = currentList[idx].copy(cantidadSelected = newQuantity)
            }
            _cart.value = currentList
        }
    }

    fun updateCartItemQuantity(item: CartItem, newQuantity: Int) {
        val currentList = _cart.value.toMutableList()
        val idx = currentList.indexOfFirst {
            if (item.isCombo) {
                it.isCombo && (it.combo?.fila == item.combo?.fila || it.combo?.id == item.combo?.id)
            } else {
                !it.isCombo && (it.product.id == item.product.id || it.product.fila == item.product.fila)
            }
        }
        if (idx >= 0) {
            if (newQuantity <= 0) {
                currentList.removeAt(idx)
            } else {
                currentList[idx] = currentList[idx].copy(cantidadSelected = newQuantity)
            }
            _cart.value = currentList
        }
    }

    fun removeCartItem(item: CartItem) {
        _cart.value = _cart.value.filterNot {
            if (item.isCombo) {
                it.isCombo && (it.combo?.fila == item.combo?.fila || it.combo?.id == item.combo?.id)
            } else {
                !it.isCombo && (it.product.id == item.product.id || it.product.fila == item.product.fila)
            }
        }
    }

    fun updateCartProductQuantity(product: Product, newQuantity: Int) {
        val currentList = _cart.value.toMutableList()
        val idx = currentList.indexOfFirst {
            !it.isCombo && (
                (it.product.id.isNotBlank() && it.product.id == product.id) ||
                (product.fila > 0 && it.product.fila == product.fila) ||
                (it.product.producto.equals(product.producto, ignoreCase = true))
            )
        }
        if (idx >= 0) {
            if (newQuantity <= 0) {
                currentList.removeAt(idx)
            } else {
                currentList[idx] = currentList[idx].copy(cantidadSelected = newQuantity)
            }
            _cart.value = currentList
        }
    }

    fun removeFromCart(fila: Int) {
        _cart.value = _cart.value.filter { it.itemFila != fila }
    }

    fun removeProductFromCart(product: Product) {
        _cart.value = _cart.value.filterNot {
            !it.isCombo && (
                (it.product.id.isNotBlank() && it.product.id == product.id) ||
                (product.fila > 0 && it.product.fila == product.fila) ||
                (it.product.producto.equals(product.producto, ignoreCase = true))
            )
        }
    }

    fun clearCart() {
        _cart.value = emptyList()
    }

    private var isProcessingSale = false

    // 1. CONFIRM SALE: Transactional stock deduction in Firestore
    fun confirmSale(clienteNombre: String = "Cliente Mostrador", clienteCedula: String = "", metodoPago: String = "Efectivo") {
        if (isProcessingSale) return
        val currentCart = _cart.value
        if (currentCart.isEmpty()) return

        isProcessingSale = true
        _isSyncing.value = true
        _errorMessage.value = null

        val userSession = _currentUser.value
        val userEmail = getCurrentAuthEmail()
        val userName = userSession?.displayName ?: auth.currentUser?.displayName ?: activeUser.value

        val rate = exchangeRate.value
        val totalUsd = currentCart.sumOf { it.subtotalUsd }
        val totalBs = totalUsd * rate

        viewModelScope.launch {
            try {
                val saleId = "sale_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"

                // Run Firestore transaction to atomically update stock for all purchased products and combo components
                firestore.runTransaction { transaction ->
                    // Map of DocumentReference to stock to deduct
                    val deductionsMap = mutableMapOf<DocumentReference, Int>()
                    val movementsList = mutableListOf<Map<String, Any>>()

                    for (item in currentCart) {
                        if (item.isCombo && item.combo != null) {
                            // Combo: deduct from each component
                            for (comp in item.combo.componentes) {
                                val docRef = firestore.collection("productos").document("prod_${comp.fila}")
                                val qtyToDeduct = comp.cantidadPorCombo * item.cantidadSelected

                                val currentAccum = deductionsMap[docRef] ?: 0
                                deductionsMap[docRef] = currentAccum + qtyToDeduct

                                val movId = "mov_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
                                movementsList.add(
                                    mapOf(
                                        "id" to movId,
                                        "productoFila" to comp.fila,
                                        "productoNombre" to comp.nombre,
                                        "tipo" to "SALIDA",
                                        "cantidad" to qtyToDeduct,
                                        "fecha" to System.currentTimeMillis(),
                                        "motivo" to "Salida por Venta Combo '${item.combo.nombre}' ($saleId) - $clienteNombre",
                                        "precioUnitarioUsd" to item.combo.precioUsd,
                                        "usuarioEmail" to userEmail,
                                        "usuarioNombre" to userName,
                                        "esReversado" to false
                                    )
                                )
                            }
                        } else {
                            // Regular Product: deduct directly
                            val docRef = if (item.product.id.isNotBlank()) {
                                firestore.collection("productos").document(item.product.id)
                            } else {
                                firestore.collection("productos").document("prod_${item.product.fila}")
                            }

                            val currentAccum = deductionsMap[docRef] ?: 0
                            deductionsMap[docRef] = currentAccum + item.cantidadSelected

                            val movId = "mov_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
                            movementsList.add(
                                mapOf(
                                    "id" to movId,
                                    "productoId" to item.product.id,
                                    "productoFila" to item.product.fila,
                                    "productoNombre" to item.product.producto,
                                    "tipo" to "SALIDA",
                                    "cantidad" to item.cantidadSelected,
                                    "fecha" to System.currentTimeMillis(),
                                    "motivo" to "Salida por Venta ($saleId) - $clienteNombre",
                                    "precioUnitarioUsd" to item.product.precioUsd,
                                    "usuarioEmail" to userEmail,
                                    "usuarioNombre" to userName,
                                    "esReversado" to false
                                )
                            )
                        }
                    }

                    // Read current stock for all affected document references
                    val updates = mutableListOf<Pair<DocumentReference, Int>>()
                    for ((docRef, deductQty) in deductionsMap) {
                        val snapshot = transaction.get(docRef)
                        if (snapshot.exists()) {
                            val currentStock = (snapshot.getLong("cantidad") ?: snapshot.getLong("stock") ?: 0L).toInt()
                            val newStock = (currentStock - deductQty).coerceAtLeast(0)
                            updates.add(Pair(docRef, newStock))
                        }
                    }

                    // Apply stock updates
                    for ((docRef, newStock) in updates) {
                        transaction.update(docRef, "cantidad", newStock)
                    }

                    // Write Sale Record to Firestore
                    val saleDocRef = firestore.collection("ventas").document(saleId)
                    val saleData = mapOf(
                        "id" to saleId,
                        "usuario" to userName,
                        "usuarioNombre" to userName,
                        "usuarioEmail" to userEmail,
                        "timestamp" to System.currentTimeMillis(),
                        "totalUsd" to totalUsd,
                        "totalBs" to totalBs,
                        "tasaBcv" to rate,
                        "clienteNombre" to clienteNombre,
                        "clienteCedula" to clienteCedula,
                        "metodoPago" to metodoPago,
                        "items" to currentCart.map { item ->
                            if (item.isCombo && item.combo != null) {
                                val comboCosto = item.combo.costoCalculado
                                mapOf(
                                    "tipo" to "combo",
                                    "fila" to item.combo.fila,
                                    "producto" to item.combo.nombre,
                                    "nombre" to item.combo.nombre,
                                    "cantidad" to item.cantidadSelected,
                                    "precioUsd" to item.combo.precioUsd,
                                    "precioCompra" to comboCosto,
                                    "componentes" to item.combo.componentes.map {
                                        mapOf(
                                            "fila" to it.fila,
                                            "nombre" to it.nombre,
                                            "cantidadPorCombo" to it.cantidadPorCombo,
                                            "precioCompraUnitario" to it.precioCompraUnitario,
                                            "precioVentaUnitario" to it.precioVentaUnitario
                                        )
                                    }
                                )
                            } else {
                                mapOf(
                                    "tipo" to "producto",
                                    "fila" to item.product.fila,
                                    "producto" to item.product.producto,
                                    "nombre" to item.product.producto,
                                    "cantidad" to item.cantidadSelected,
                                    "precioUsd" to item.precioUnitarioAplicado,
                                    "precioCompra" to item.product.precioCompra
                                )
                            }
                        },
                        "esReversado" to false
                    )
                    transaction.set(saleDocRef, saleData)

                    // Write Movimiento Record for each item
                    for (movData in movementsList) {
                        val movId = movData["id"] as String
                        val movDocRef = firestore.collection("movimientos").document(movId)
                        transaction.set(movDocRef, movData)
                    }
                }.await()

                val saleCompleted = Sale(
                    id = saleId,
                    usuario = userName,
                    usuarioEmail = userEmail,
                    timestamp = System.currentTimeMillis(),
                    totalUsd = totalUsd,
                    totalBs = totalBs,
                    tasaBcv = rate,
                    items = currentCart.map { item ->
                        if (item.isCombo && item.combo != null) {
                            SaleItem(
                                fila = item.combo.fila,
                                producto = item.combo.nombre,
                                cantidad = item.cantidadSelected,
                                precioUsd = item.combo.precioUsd,
                                precioCompra = item.combo.costoCalculado,
                                tipo = "combo",
                                componentes = item.combo.componentes
                            )
                        } else {
                            SaleItem(
                                fila = item.product.fila,
                                producto = item.product.producto,
                                cantidad = item.cantidadSelected,
                                precioUsd = item.precioUnitarioAplicado,
                                precioCompra = item.product.precioCompra,
                                tipo = "producto"
                            )
                        }
                    }
                )

                // Auto-upload Nota de Entrega PDF to Google Drive Folder in Background if configured
                val driveUrl = preferencesRepo.backendUrl.value.trim().ifBlank { AppConfig.GOOGLE_DRIVE_FOLDER_WEBHOOK_URL.trim() }
                if (driveUrl.isNotBlank()) {
                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val pdfFile = InvoicePdfGenerator.generateInvoicePdf(getApplication(), saleCompleted)
                            if (pdfFile != null && pdfFile.exists()) {
                                val bytes = pdfFile.readBytes()
                                val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                                val mesAnioStr = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(java.util.Date(saleCompleted.timestamp))
                                val uploadResult = GananciasApiService.uploadInvoiceToDrive(
                                    url = driveUrl,
                                    saleId = saleCompleted.id,
                                    folio = saleCompleted.folio,
                                    cliente = saleCompleted.clienteNombre,
                                    pdfBase64 = base64,
                                    totalUsd = saleCompleted.totalUsd,
                                    usuario = saleCompleted.usuario,
                                    mesAnio = mesAnioStr
                                )
                                if (uploadResult.isSuccess) {
                                    Log.i("InventoryViewModel", "Nota de Entrega enviada a Google Drive con éxito")
                                } else {
                                    Log.w("InventoryViewModel", "No se pudo subir PDF a Drive: ${uploadResult.exceptionOrNull()?.message}")
                                }
                            }
                        } catch (e: Exception) {
                            Log.w("InventoryViewModel", "Error en subida de PDF a Drive: ${e.message}")
                        }
                    }
                }

                clearCart()
                _completedSale.value = saleCompleted
                _successMessage.value = "Venta registrada con éxito"

                // Invalidate & force refresh Inventario and Ganancias
                syncInventoryCatalog()
                fetchCombos()
                fetchGanancias()
                fetchHistorialMeses()
            } catch (e: Exception) {
                Log.e("InventoryViewModel", "Error procesando venta: ${e.message}", e)
                _errorMessage.value = "Error al procesar venta: ${e.localizedMessage}"
            } finally {
                _isSyncing.value = false
                isProcessingSale = false
            }
        }
    }

    fun dismissCompletedSale() {
        _completedSale.value = null
        _selectedTab.value = 0 // Return to Inicio
    }

    // 2. UPDATE PRODUCT STOCK, PRICE & BARCODE (Transaction)
    fun updateProductStockPriceAndBarcode(
        product: Product,
        newCantidad: Int,
        newPrecioUsd: Double,
        newCodigoBarras: String = product.codigoBarras,
        newPrecioMayor: Double? = product.precioMayor,
        newCantidadMinimaMayor: Int? = product.cantidadMinimaMayor,
        newPrecioCompra: Double? = product.precioCompra
    ) {
        val userSession = _currentUser.value
        val userEmail = getCurrentAuthEmail()
        val userName = userSession?.displayName ?: auth.currentUser?.displayName ?: activeUser.value

        viewModelScope.launch {
            _isSyncing.value = true
            _errorMessage.value = null
            try {
                val docRef = if (product.id.isNotBlank()) {
                    firestore.collection("productos").document(product.id)
                } else {
                    firestore.collection("productos").document("prod_${product.fila}")
                }

                val updateMap = mutableMapOf<String, Any?>(
                    "cantidad" to newCantidad,
                    "precioUsd" to newPrecioUsd,
                    "codigoBarras" to newCodigoBarras.trim()
                )
                if (newPrecioCompra != null && newPrecioCompra >= 0) {
                    updateMap["precioCompra"] = newPrecioCompra
                    updateMap["costo"] = newPrecioCompra
                }
                if (newPrecioMayor != null && newPrecioMayor > 0) {
                    updateMap["precioMayor"] = newPrecioMayor
                } else {
                    updateMap["precioMayor"] = null
                }
                if (newCantidadMinimaMayor != null && newCantidadMinimaMayor > 0) {
                    updateMap["cantidadMinimaMayor"] = newCantidadMinimaMayor
                } else {
                    updateMap["cantidadMinimaMayor"] = null
                }

                firestore.runTransaction { transaction ->
                    val snapshot = transaction.get(docRef)
                    val oldStock = (snapshot.getLong("cantidad") ?: product.cantidad.toLong()).toInt()
                    val oldPrice = (snapshot.getDouble("precioUsd") ?: product.precioUsd)

                    transaction.update(docRef, updateMap)

                    // Log movement if stock or price changed
                    val diffStock = newCantidad - oldStock
                    if (diffStock != 0 || newPrecioUsd != oldPrice) {
                        val movId = "mov_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
                        val movDocRef = firestore.collection("movimientos").document(movId)

                        val tipoMov = if (newPrecioUsd != oldPrice && diffStock == 0) "CAMBIO_PRECIO" else if (diffStock >= 0) "ENTRADA" else "SALIDA"
                        val motivo = if (newPrecioUsd != oldPrice) {
                            "Ajuste precio ${oldPrice} -> ${newPrecioUsd}. Stock: $newCantidad"
                        } else {
                            "Ajuste manual de inventario (${if (diffStock >= 0) "+$diffStock" else "$diffStock"})"
                        }

                        val movData = mapOf(
                            "id" to movId,
                            "productoId" to product.id,
                            "productoFila" to product.fila,
                            "productoNombre" to product.producto,
                            "tipo" to tipoMov,
                            "cantidad" to Math.abs(diffStock),
                            "fecha" to System.currentTimeMillis(),
                            "motivo" to motivo,
                            "precioUnitarioUsd" to newPrecioUsd,
                            "usuarioEmail" to userEmail,
                            "usuarioNombre" to userName,
                            "esReversado" to false
                        )
                        transaction.set(movDocRef, movData)
                    }
                }.await()

                // Sync with Google Apps Script backend if configured
                val backendUrlStr = preferencesRepo.backendUrl.value.trim()
                if (backendUrlStr.isNotBlank() && product.fila > 0) {
                    try {
                        GananciasApiService.actualizarProductoBackend(
                            backendUrl = backendUrlStr,
                            fila = product.fila,
                            cantidad = newCantidad,
                            precioUsd = newPrecioUsd,
                            precioCompra = newPrecioCompra,
                            precioMayor = newPrecioMayor,
                            cantidadMinimaMayor = newCantidadMinimaMayor
                        )
                    } catch (e: Exception) {
                        Log.w("InventoryViewModel", "Error actualizando producto en backend: ${e.message}")
                    }
                }

                dismissBottomSheet()
                _successMessage.value = "Producto ${product.producto} actualizado"
            } catch (e: Exception) {
                Log.e("InventoryViewModel", "Error actualizando producto: ${e.message}", e)
                _errorMessage.value = "Error al actualizar producto: ${e.localizedMessage}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    // Keep backwards compatible signature
    fun updateProductStockAndPrice(fila: Int, newCantidad: Int, newPrecioUsd: Double) {
        val currentProduct = products.value.find { it.fila == fila } ?: return
        updateProductStockPriceAndBarcode(currentProduct, newCantidad, newPrecioUsd, currentProduct.codigoBarras, currentProduct.precioMayor, currentProduct.cantidadMinimaMayor)
    }

    // 3. REGISTRAR ENTRADA (Atomic Transaction)
    fun addStockToProduct(product: Product, quantityToAdd: Int, motivo: String = "Entrada de Stock / Reposición") {
        if (quantityToAdd <= 0) return
        val userSession = _currentUser.value
        val userEmail = getCurrentAuthEmail()
        val userName = userSession?.displayName ?: auth.currentUser?.displayName ?: activeUser.value

        viewModelScope.launch {
            _isSyncing.value = true
            _errorMessage.value = null
            try {
                val docRef = if (product.id.isNotBlank()) {
                    firestore.collection("productos").document(product.id)
                } else {
                    firestore.collection("productos").document("prod_${product.fila}")
                }

                firestore.runTransaction { transaction ->
                    val snapshot = transaction.get(docRef)
                    val currentStock = (snapshot.getLong("cantidad") ?: product.cantidad.toLong()).toInt()
                    val newStock = currentStock + quantityToAdd

                    transaction.update(docRef, "cantidad", newStock)

                    val movId = "mov_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
                    val movDocRef = firestore.collection("movimientos").document(movId)
                    val movData = mapOf(
                        "id" to movId,
                        "productoId" to product.id,
                        "productoFila" to product.fila,
                        "productoNombre" to product.producto,
                        "tipo" to "ENTRADA",
                        "cantidad" to quantityToAdd,
                        "fecha" to System.currentTimeMillis(),
                        "motivo" to motivo,
                        "precioUnitarioUsd" to product.precioUsd,
                        "usuarioEmail" to userEmail,
                        "usuarioNombre" to userName,
                        "esReversado" to false
                    )
                    transaction.set(movDocRef, movData)
                }.await()

                _successMessage.value = "Stock agregado (+ $quantityToAdd) a ${product.producto} en Firestore"
            } catch (e: Exception) {
                Log.e("InventoryViewModel", "Error registrando entrada: ${e.message}", e)
                _errorMessage.value = "Error al agregar stock: ${e.localizedMessage}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    // 3.1 REGISTRAR SALIDA DIRECTA / ESCANEO RÁPIDO (Atomic Transaction)
    fun deductStockFromProduct(product: Product, quantityToDeduct: Int, motivo: String = "Salida rápida por escaneo") {
        if (quantityToDeduct <= 0) return
        val userSession = _currentUser.value
        val userEmail = getCurrentAuthEmail()
        val userName = userSession?.displayName ?: auth.currentUser?.displayName ?: activeUser.value

        viewModelScope.launch {
            _isSyncing.value = true
            _errorMessage.value = null
            try {
                val docRef = if (product.id.isNotBlank()) {
                    firestore.collection("productos").document(product.id)
                } else {
                    firestore.collection("productos").document("prod_${product.fila}")
                }

                firestore.runTransaction { transaction ->
                    val snapshot = transaction.get(docRef)
                    val currentStock = (snapshot.getLong("cantidad") ?: product.cantidad.toLong()).toInt()
                    val newStock = (currentStock - quantityToDeduct).coerceAtLeast(0)

                    transaction.update(docRef, "cantidad", newStock)

                    val movId = "mov_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
                    val movDocRef = firestore.collection("movimientos").document(movId)
                    val movData = mapOf(
                        "id" to movId,
                        "productoId" to product.id,
                        "productoFila" to product.fila,
                        "productoNombre" to product.producto,
                        "tipo" to "SALIDA",
                        "cantidad" to quantityToDeduct,
                        "fecha" to System.currentTimeMillis(),
                        "motivo" to motivo,
                        "precioUnitarioUsd" to product.precioUsd,
                        "usuarioEmail" to userEmail,
                        "usuarioNombre" to userName,
                        "esReversado" to false
                    )
                    transaction.set(movDocRef, movData)
                }.await()

                _successMessage.value = "Salida registrada (- $quantityToDeduct) para ${product.producto}"
            } catch (e: Exception) {
                Log.e("InventoryViewModel", "Error registrando salida: ${e.message}", e)
                _errorMessage.value = "Error al registrar salida: ${e.localizedMessage}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    // 3.2 REGISTRAR LOTE DE ESCANEO RÁPIDO (BATCH ATOMIC TRANSACTION)
    fun processQuickScanBatch(
        isEntrada: Boolean,
        items: List<Pair<Product, Int>>
    ) {
        if (items.isEmpty()) return
        val userSession = _currentUser.value
        val userEmail = getCurrentAuthEmail()
        val userName = userSession?.displayName ?: auth.currentUser?.displayName ?: activeUser.value
        val tipoMovimiento = if (isEntrada) "ENTRADA" else "SALIDA"
        val batchTag = UUID.randomUUID().toString().take(6)

        viewModelScope.launch {
            _isSyncing.value = true
            _errorMessage.value = null
            try {
                firestore.runTransaction { transaction ->
                    for ((product, qty) in items) {
                        if (qty <= 0) continue

                        val docRef = if (product.id.isNotBlank()) {
                            firestore.collection("productos").document(product.id)
                        } else {
                            firestore.collection("productos").document("prod_${product.fila}")
                        }

                        val snapshot = transaction.get(docRef)
                        val currentStock = (snapshot.getLong("cantidad") ?: product.cantidad.toLong()).toInt()
                        val newStock = if (isEntrada) {
                            currentStock + qty
                        } else {
                            (currentStock - qty).coerceAtLeast(0)
                        }

                        transaction.update(docRef, "cantidad", newStock)

                        val movId = "mov_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
                        val movDocRef = firestore.collection("movimientos").document(movId)
                        val movData = mapOf(
                            "id" to movId,
                            "productoId" to product.id,
                            "productoFila" to product.fila,
                            "productoNombre" to product.producto,
                            "tipo" to tipoMovimiento,
                            "cantidad" to qty,
                            "fecha" to System.currentTimeMillis(),
                            "motivo" to if (isEntrada) "Reposición Lote Escáner ($batchTag)" else "Despacho Lote Escáner ($batchTag)",
                            "precioUnitarioUsd" to product.precioUsd,
                            "usuarioEmail" to userEmail,
                            "usuarioNombre" to userName,
                            "esReversado" to false
                        )
                        transaction.set(movDocRef, movData)
                    }
                }.await()

                val totalUnits = items.sumOf { it.second }
                _successMessage.value = if (isEntrada) {
                    "Lote de entrada procesado: +$totalUnits un. en ${items.size} productos"
                } else {
                    "Lote de salida procesado: -$totalUnits un. en ${items.size} productos"
                }
            } catch (e: Exception) {
                Log.e("InventoryViewModel", "Error procesando lote de escaneo: ${e.message}", e)
                _errorMessage.value = "Error al procesar lote: ${e.localizedMessage}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    // 4. CREATE NEW PRODUCT DIRECTLY IN FIRESTORE (WITH BARCODE SUPPORT)
    fun createNewProduct(
        producto: String,
        cantidad: Int,
        precioUsd: Double,
        categoria: String,
        codigoBarras: String = "",
        precioMayor: Double? = null,
        cantidadMinimaMayor: Int? = null,
        precioCompra: Double = 0.0
    ) {
        if (producto.isBlank()) return
        val userSession = _currentUser.value
        val userEmail = getCurrentAuthEmail()
        val userName = userSession?.displayName ?: auth.currentUser?.displayName ?: activeUser.value

        viewModelScope.launch {
            _isSyncing.value = true
            _errorMessage.value = null
            try {
                val nextFila = (_products.value.maxOfOrNull { it.fila } ?: 1) + 1
                val prodId = "prod_$nextFila"

                val newProd = Product(
                    fila = nextFila,
                    id = prodId,
                    producto = producto.trim(),
                    cantidad = cantidad.coerceAtLeast(0),
                    precioUsd = precioUsd.coerceAtLeast(0.0),
                    precioCompra = precioCompra.coerceAtLeast(0.0),
                    precioMayor = if (precioMayor != null && precioMayor > 0) precioMayor else null,
                    cantidadMinimaMayor = if (cantidadMinimaMayor != null && cantidadMinimaMayor > 0) cantidadMinimaMayor else null,
                    catalogo = categoria.ifBlank { "General" }.trim(),
                    codigoBarras = codigoBarras.trim()
                )

                firestore.runTransaction { transaction ->
                    val prodRef = firestore.collection("productos").document(prodId)
                    transaction.set(prodRef, newProd)

                    if (cantidad > 0) {
                        val movId = "mov_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
                        val movRef = firestore.collection("movimientos").document(movId)
                        val movData = mapOf(
                            "id" to movId,
                            "productoId" to prodId,
                            "productoFila" to nextFila,
                            "productoNombre" to producto.trim(),
                            "tipo" to "ENTRADA",
                            "cantidad" to cantidad,
                            "fecha" to System.currentTimeMillis(),
                            "motivo" to "Carga inicial de nuevo producto",
                            "precioUnitarioUsd" to precioUsd,
                            "usuarioEmail" to userEmail,
                            "usuarioNombre" to userName,
                            "esReversado" to false
                        )
                        transaction.set(movRef, movData)
                    }
                }.await()

                _successMessage.value = "Nuevo producto registrado: $producto"

                // Optional backend Google Apps Script sync
                val backendUrlStr = preferencesRepo.backendUrl.value.trim()
                if (backendUrlStr.isNotBlank()) {
                    GananciasApiService.agregarProductoBackend(
                        backendUrl = backendUrlStr,
                        producto = producto,
                        cantidad = cantidad,
                        precioUsd = precioUsd,
                        precioCompra = precioCompra,
                        precioMayor = precioMayor,
                        cantidadMinimaMayor = cantidadMinimaMayor,
                        catalogo = categoria,
                        codigoBarra = codigoBarras
                    )
                }
            } catch (e: Exception) {
                Log.e("InventoryViewModel", "Error creando producto: ${e.message}", e)
                _errorMessage.value = "Error al crear producto: ${e.localizedMessage}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    // 4.1 AGREGAR ALIAS DE CÓDIGO DE BARRAS A PRODUCTO EXISTENTE
    fun addBarcodeAliasToProduct(
        product: Product,
        newBarcode: String,
        onSuccess: () -> Unit = {}
    ) {
        val cleanBarcode = newBarcode.trim()
        if (cleanBarcode.isBlank()) return

        val userSession = _currentUser.value
        val userEmail = getCurrentAuthEmail()
        val userName = userSession?.displayName ?: auth.currentUser?.displayName ?: activeUser.value

        viewModelScope.launch {
            _isSyncing.value = true
            _errorMessage.value = null
            try {
                val docRef = if (product.id.isNotBlank()) {
                    firestore.collection("productos").document(product.id)
                } else {
                    firestore.collection("productos").document("prod_${product.fila}")
                }

                val currentBarcodes = product.codigoBarras.trim()
                val updatedBarcodes = if (currentBarcodes.isBlank()) {
                    cleanBarcode
                } else {
                    val list = currentBarcodes.split(",", ";", "\n").map { it.trim() }.filter { it.isNotBlank() }
                    if (!list.contains(cleanBarcode)) {
                        "$currentBarcodes, $cleanBarcode"
                    } else {
                        currentBarcodes
                    }
                }

                firestore.runTransaction { transaction ->
                    transaction.update(docRef, "codigoBarras", updatedBarcodes)
                }.await()

                // Call backend if configured: POST {URL} con { accion: "agregar_alias_codigo", fila, codigo_barra }
                val backendUrlStr = preferencesRepo.backendUrl.value.trim()
                if (backendUrlStr.isNotBlank()) {
                    GananciasApiService.agregarAliasCodigo(
                        backendUrl = backendUrlStr,
                        fila = product.fila,
                        codigoBarra = cleanBarcode
                    )
                }

                _successMessage.value = "Código '$cleanBarcode' agregado como alias a '${product.producto}'"
                onSuccess()
            } catch (e: Exception) {
                Log.e("InventoryViewModel", "Error agregando alias: ${e.message}", e)
                _errorMessage.value = "Error al asociar código de barras: ${e.localizedMessage}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    // 5. REVERSAR VENTA (Atomic Transaction)
    fun reversarVenta(ventaId: String) {
        val userSession = _currentUser.value
        val userEmail = getCurrentAuthEmail()
        val userName = userSession?.displayName ?: auth.currentUser?.displayName ?: activeUser.value

        viewModelScope.launch {
            _isSyncing.value = true
            _errorMessage.value = null
            try {
                val saleRef = firestore.collection("ventas").document(ventaId)

                firestore.runTransaction { transaction ->
                    val saleSnapshot = transaction.get(saleRef)
                    val esReversado = saleSnapshot.getBoolean("esReversado") ?: false
                    if (esReversado) {
                        throw IllegalStateException("Esta venta ya fue reversada previamente.")
                    }

                    val itemsRaw = saleSnapshot.get("items") as? List<Map<String, Any>> ?: emptyList()

                    // Restore stock for all items (handling both single products and combos)
                    for (m in itemsRaw) {
                        val tipo = m["tipo"] as? String ?: "producto"
                        val cant = (m["cantidad"] as? Number)?.toInt() ?: 1
                        val prodName = (m["producto"] as? String) ?: (m["nombre"] as? String) ?: ""
                        val precioUsd = (m["precioUsd"] as? Number)?.toDouble() ?: 0.0

                        if (tipo == "combo" || m.containsKey("componentes")) {
                            // Combo: restore stock for each component
                            val componentesRaw = m["componentes"] as? List<Map<String, Any>> ?: emptyList()
                            for (comp in componentesRaw) {
                                val compFila = (comp["fila"] as? Number)?.toInt() ?: 0
                                val compNombre = comp["nombre"] as? String ?: ""
                                val cantPorCombo = (comp["cantidadPorCombo"] as? Number)?.toInt()
                                    ?: (comp["cantidad"] as? Number)?.toInt()
                                    ?: 1
                                val qtyToRestore = cantPorCombo * cant

                                if (compFila > 0) {
                                    val prodRef = firestore.collection("productos").document("prod_$compFila")
                                    val prodSnap = transaction.get(prodRef)
                                    if (prodSnap.exists()) {
                                        val currentStock = (prodSnap.getLong("cantidad") ?: 0L).toInt()
                                        transaction.update(prodRef, "cantidad", currentStock + qtyToRestore)
                                    }

                                    // Write restoration movement for component
                                    val movId = "mov_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
                                    val movDocRef = firestore.collection("movimientos").document(movId)
                                    val movData = mapOf(
                                        "id" to movId,
                                        "productoFila" to compFila,
                                        "productoNombre" to compNombre.ifBlank { "Componente de $prodName" },
                                        "tipo" to TipoMovimiento.REVERSO.name,
                                        "cantidad" to qtyToRestore,
                                        "fecha" to System.currentTimeMillis(),
                                        "motivo" to "REVERSO DE VENTA COMBO '$prodName' (${ventaId.take(8)}) por $userName",
                                        "precioUnitarioUsd" to precioUsd,
                                        "usuarioEmail" to userEmail,
                                        "usuarioNombre" to userName,
                                        "esReversado" to false
                                    )
                                    transaction.set(movDocRef, movData)
                                }
                            }
                        } else {
                            // Regular Product
                            val fila = (m["fila"] as? Number)?.toInt() ?: 0
                            val prodId = m["productoId"] as? String ?: ""
                            val prodRef = if (prodId.isNotBlank()) {
                                firestore.collection("productos").document(prodId)
                            } else {
                                firestore.collection("productos").document("prod_$fila")
                            }

                            val prodSnap = transaction.get(prodRef)
                            if (prodSnap.exists()) {
                                val currentStock = (prodSnap.getLong("cantidad") ?: 0L).toInt()
                                transaction.update(prodRef, "cantidad", currentStock + cant)
                            }

                            // Write restoration movement
                            val movId = "mov_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
                            val movDocRef = firestore.collection("movimientos").document(movId)
                            val movData = mapOf(
                                "id" to movId,
                                "productoId" to prodId,
                                "productoFila" to fila,
                                "productoNombre" to prodName,
                                "tipo" to TipoMovimiento.REVERSO.name,
                                "cantidad" to cant,
                                "fecha" to System.currentTimeMillis(),
                                "motivo" to "REVERSO DE VENTA (${ventaId.take(8)}) por $userName",
                                "precioUnitarioUsd" to precioUsd,
                                "usuarioEmail" to userEmail,
                                "usuarioNombre" to userName,
                                "esReversado" to false
                            )
                            transaction.set(movDocRef, movData)
                        }
                    }

                    // Mark sale as reversado
                    transaction.update(saleRef, mapOf(
                        "esReversado" to true,
                        "fechaReverso" to System.currentTimeMillis(),
                        "reversadoPorNombre" to userName,
                        "reversadoPorEmail" to userEmail
                    ))
                }.await()

                _successMessage.value = "Venta reversada y stock restaurado en Firestore"
            } catch (e: Exception) {
                Log.e("InventoryViewModel", "Error al reversar venta: ${e.message}", e)
                _errorMessage.value = "Error al reversar venta: ${e.localizedMessage}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    // 6. REVERSAR MOVIMIENTO INDIVIDUAL (Atomic Transaction)
    fun reversarMovimiento(movimientoId: String) {
        val userSession = _currentUser.value
        val userEmail = getCurrentAuthEmail()

        viewModelScope.launch {
            _isSyncing.value = true
            _errorMessage.value = null
            try {
                val movRef = firestore.collection("movimientos").document(movimientoId)

                firestore.runTransaction { transaction ->
                    val movSnap = transaction.get(movRef)
                    val esReversado = movSnap.getBoolean("esReversado") ?: false
                    if (esReversado) {
                        throw IllegalStateException("Este movimiento ya fue reversado.")
                    }

                    val tipo = movSnap.getString("tipo") ?: "ENTRADA"
                    val cantidad = (movSnap.getLong("cantidad") ?: 0L).toInt()
                    val fila = (movSnap.getLong("productoFila") ?: 0L).toInt()
                    val prodId = movSnap.getString("productoId") ?: ""

                    if (tipo != "CAMBIO_PRECIO" && cantidad > 0) {
                        val prodRef = if (prodId.isNotBlank()) {
                            firestore.collection("productos").document(prodId)
                        } else {
                            firestore.collection("productos").document("prod_$fila")
                        }
                        val prodSnap = transaction.get(prodRef)
                        if (prodSnap.exists()) {
                            val currentStock = (prodSnap.getLong("cantidad") ?: 0L).toInt()
                            val newStock = if (tipo == "SALIDA") {
                                currentStock + cantidad
                            } else {
                                (currentStock - cantidad).coerceAtLeast(0)
                            }
                            transaction.update(prodRef, "cantidad", newStock)
                        }
                    }

                    transaction.update(movRef, mapOf(
                        "esReversado" to true,
                        "fechaReverso" to System.currentTimeMillis(),
                        "reversadoPorEmail" to userEmail
                    ))
                }.await()

                _successMessage.value = "Movimiento reversado con éxito"
            } catch (e: Exception) {
                Log.e("InventoryViewModel", "Error reversando movimiento: ${e.message}", e)
                _errorMessage.value = "Error al reversar movimiento: ${e.localizedMessage}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    // 7. DELETE PRODUCT (Admin Only)
    fun deleteProduct(product: Product) {
        if (!isCurrentUserAdmin.value) {
            _errorMessage.value = "Solo los administradores pueden eliminar productos."
            return
        }
        viewModelScope.launch {
            _isSyncing.value = true
            _errorMessage.value = null
            try {
                val docRef = if (product.id.isNotBlank()) {
                    firestore.collection("productos").document(product.id)
                } else {
                    firestore.collection("productos").document("prod_${product.fila}")
                }
                docRef.delete().await()
                dismissBottomSheet()
                _successMessage.value = "Producto '${product.producto}' eliminado del catálogo"
            } catch (e: Exception) {
                Log.e("InventoryViewModel", "Error eliminando producto: ${e.message}", e)
                _errorMessage.value = "Error al eliminar producto: ${e.localizedMessage}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    // 8. ONLINE BARCODE LOOKUP (OPEN FOOD FACTS + UPCITEMDB)
    fun lookupBarcodeOnline(barcode: String, onResult: (com.example.data.remote.UpcProductLookupResult) -> Unit) {
        viewModelScope.launch {
            _isSyncing.value = true
            val result = com.example.data.remote.UpcItemDbService.lookupBarcode(barcode)
            _isSyncing.value = false
            onResult(result)
        }
    }

    fun lookupOpenFoodFacts(barcode: String, onResult: (com.example.data.remote.OpenFoodFactsResult) -> Unit) {
        viewModelScope.launch {
            _isSyncing.value = true
            val result = com.example.data.remote.OpenFoodFactsService.lookupBarcode(barcode)
            _isSyncing.value = false
            onResult(result)
        }
    }

    // 9. GANANCIAS & HISTORIAL METHODS
    fun setBackendUrl(url: String) {
        preferencesRepo.setBackendUrl(url)
    }

    fun fetchGanancias() {
        val url = preferencesRepo.backendUrl.value.trim()
        val localCalc = computeLocalGananciasCurrentMonth()
        viewModelScope.launch {
            _isLoadingGanancias.value = true
            if (url.isNotBlank()) {
                val result = GananciasApiService.getGananciasActuales(url)
                result.onSuccess { remoteData ->
                    val finalData = if (remoteData.usuarios.isNotEmpty() && remoteData.totalUsd > 0.0) {
                        remoteData
                    } else if (localCalc.usuarios.isNotEmpty() && localCalc.totalUsd > 0.0) {
                        localCalc
                    } else {
                        remoteData
                    }
                    _gananciasActuales.value = finalData
                }.onFailure { e ->
                    Log.w("InventoryViewModel", "Error consultando endpoint de ganancias: ${e.message}. Usando cálculo local.", e)
                    _gananciasActuales.value = localCalc
                }
            } else {
                _gananciasActuales.value = localCalc
            }
            _isLoadingGanancias.value = false
        }
    }

    fun fetchHistorialMeses() {
        val url = preferencesRepo.backendUrl.value.trim()
        val localMonths = computeLocalPastMonths()
        viewModelScope.launch {
            _isLoadingGanancias.value = true
            if (url.isNotBlank()) {
                val result = GananciasApiService.getHistorialMeses(url)
                result.onSuccess { list ->
                    val merged = (list + localMonths).distinct().sortedDescending()
                    _historialMeses.value = if (merged.isNotEmpty()) merged else list
                }.onFailure { e ->
                    Log.w("InventoryViewModel", "Error obteniendo historial_meses de backend: ${e.message}")
                    _historialMeses.value = localMonths
                }
            } else {
                _historialMeses.value = localMonths
            }
            _isLoadingGanancias.value = false
        }
    }

    fun selectArchivedMonth(mesKey: String) {
        _selectedArchivedMonth.value = mesKey
        val url = preferencesRepo.backendUrl.value.trim()
        val localMonthCalc = computeLocalGananciasForMonthKey(mesKey)
        viewModelScope.launch {
            _isLoadingGanancias.value = true
            if (url.isNotBlank()) {
                val result = GananciasApiService.getGananciasMesArchivado(url, mesKey)
                result.onSuccess { remoteData ->
                    val finalData = if (remoteData.usuarios.isNotEmpty() && remoteData.totalUsd > 0.0) {
                        remoteData
                    } else if (localMonthCalc.usuarios.isNotEmpty() && localMonthCalc.totalUsd > 0.0) {
                        localMonthCalc
                    } else {
                        remoteData
                    }
                    _gananciasMesArchivado.value = finalData
                }.onFailure { e ->
                    Log.w("InventoryViewModel", "Error obteniendo mes archivado: ${e.message}")
                    _gananciasMesArchivado.value = localMonthCalc
                }
            } else {
                _gananciasMesArchivado.value = localMonthCalc
            }
            _isLoadingGanancias.value = false
        }
    }

    fun clearSelectedArchivedMonth() {
        _selectedArchivedMonth.value = null
        _gananciasMesArchivado.value = null
    }

    fun computeLocalGananciasCurrentMonth(): GananciasMes {
        val sales = _salesHistory.value.filter { !it.esReversado }
        val now = java.util.Calendar.getInstance()
        val currentYear = now.get(java.util.Calendar.YEAR)
        val currentMonth = now.get(java.util.Calendar.MONTH) // 0-indexed

        val monthSales = sales.filter { sale ->
            if (sale.timestamp <= 0L) true else {
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = sale.timestamp }
                cal.get(java.util.Calendar.YEAR) == currentYear && cal.get(java.util.Calendar.MONTH) == currentMonth
            }
        }

        val currentRate = exchangeRate.value.coerceAtLeast(1.0)
        val byUser = monthSales.groupBy { 
            it.usuario.trim().ifBlank { it.usuarioEmail.trim() }.ifBlank { "Operador" } 
        }
        val userList = byUser.map { (user, userSales) ->
            val totalVentas = userSales.size
            val totalUnidades = userSales.sumOf { sale ->
                if (sale.items.isNotEmpty()) sale.items.sumOf { it.cantidad } else 1
            }
            val totalUsd = userSales.sumOf { it.totalUsd }
            val totalBs = userSales.sumOf {
                if (it.totalBs > 0) it.totalBs else it.totalUsd * (if (it.tasaBcv > 0) it.tasaBcv else currentRate)
            }
            val totalCosto = userSales.sumOf { it.costoTotalUsd }
            val gananciaNeta = (totalUsd - totalCosto).coerceAtLeast(0.0)
            val margen = if (totalUsd > 0) (gananciaNeta / totalUsd) * 100.0 else 0.0

            UsuarioGanancia(
                usuario = user,
                ventas = totalVentas,
                unidades = totalUnidades,
                totalUsd = totalUsd,
                totalBs = totalBs,
                totalCostoUsd = totalCosto,
                gananciaNetaUsd = gananciaNeta,
                margenPorcentaje = margen
            )
        }.sortedByDescending { it.totalUsd }

        val monthTotalUsd = userList.sumOf { it.totalUsd }
        val monthTotalBs = userList.sumOf { it.totalBs }
        val monthTotalCosto = userList.sumOf { it.totalCostoUsd }
        val monthGananciaNeta = (monthTotalUsd - monthTotalCosto).coerceAtLeast(0.0)
        val monthMargen = if (monthTotalUsd > 0) (monthGananciaNeta / monthTotalUsd) * 100.0 else 0.0
        val monthFormattedKey = String.format(java.util.Locale.US, "Ventas_%04d-%02d", currentYear, currentMonth + 1)

        return GananciasMes(
            mes = monthFormattedKey,
            usuarios = userList,
            totalUsd = monthTotalUsd,
            totalBs = monthTotalBs,
            totalCostoUsd = monthTotalCosto,
            gananciaNetaUsd = monthGananciaNeta,
            margenPorcentaje = monthMargen,
            isArchived = false
        )
    }

    private fun computeLocalPastMonths(): List<String> {
        val sales = _salesHistory.value.filter { !it.esReversado }
        val now = java.util.Calendar.getInstance()
        val currentYear = now.get(java.util.Calendar.YEAR)
        val currentMonth = now.get(java.util.Calendar.MONTH)

        val distinctMonths = mutableSetOf<String>()
        sales.forEach { sale ->
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = sale.timestamp }
            val y = cal.get(java.util.Calendar.YEAR)
            val m = cal.get(java.util.Calendar.MONTH)
            // If it's before the current month
            if (y < currentYear || (y == currentYear && m < currentMonth)) {
                distinctMonths.add(String.format(java.util.Locale.US, "Ventas_%04d-%02d", y, m + 1))
            }
        }
        return distinctMonths.sortedDescending()
    }

    private fun computeLocalGananciasForMonthKey(mesKey: String): GananciasMes {
        val clean = mesKey.removePrefix("Ventas_").removePrefix("ventas_").trim()
        val parts = clean.split("-")
        if (parts.size != 2) {
            return GananciasMes(mes = mesKey, usuarios = emptyList(), totalUsd = 0.0, totalBs = 0.0, totalCostoUsd = 0.0, gananciaNetaUsd = 0.0, margenPorcentaje = 0.0, isArchived = true)
        }
        val targetYear = parts[0].toIntOrNull() ?: 0
        val targetMonth = (parts[1].toIntOrNull() ?: 1) - 1

        val sales = _salesHistory.value.filter { !it.esReversado }
        val monthSales = sales.filter { sale ->
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = sale.timestamp }
            cal.get(java.util.Calendar.YEAR) == targetYear && cal.get(java.util.Calendar.MONTH) == targetMonth
        }

        val currentRate = exchangeRate.value.coerceAtLeast(1.0)
        val byUser = monthSales.groupBy { it.usuario.ifBlank { it.usuarioEmail }.ifBlank { "Operador" } }
        val userList = byUser.map { (user, userSales) ->
            val totalVentas = userSales.size
            val totalUnidades = userSales.sumOf { sale ->
                if (sale.items.isNotEmpty()) sale.items.sumOf { it.cantidad } else 1
            }
            val totalUsd = userSales.sumOf { it.totalUsd }
            val totalBs = userSales.sumOf {
                if (it.totalBs > 0) it.totalBs else it.totalUsd * (if (it.tasaBcv > 0) it.tasaBcv else currentRate)
            }
            val totalCosto = userSales.sumOf { it.costoTotalUsd }
            val gananciaNeta = (totalUsd - totalCosto).coerceAtLeast(0.0)
            val margen = if (totalUsd > 0) (gananciaNeta / totalUsd) * 100.0 else 0.0

            UsuarioGanancia(
                usuario = user,
                ventas = totalVentas,
                unidades = totalUnidades,
                totalUsd = totalUsd,
                totalBs = totalBs,
                totalCostoUsd = totalCosto,
                gananciaNetaUsd = gananciaNeta,
                margenPorcentaje = margen
            )
        }.sortedByDescending { it.totalUsd }

        val monthTotalUsd = userList.sumOf { it.totalUsd }
        val monthTotalBs = userList.sumOf { it.totalBs }
        val monthTotalCosto = userList.sumOf { it.totalCostoUsd }
        val monthGananciaNeta = (monthTotalUsd - monthTotalCosto).coerceAtLeast(0.0)
        val monthMargen = if (monthTotalUsd > 0) (monthGananciaNeta / monthTotalUsd) * 100.0 else 0.0

        return GananciasMes(
            mes = mesKey,
            usuarios = userList,
            totalUsd = monthTotalUsd,
            totalBs = monthTotalBs,
            totalCostoUsd = monthTotalCosto,
            gananciaNetaUsd = monthGananciaNeta,
            margenPorcentaje = monthMargen,
            isArchived = true
        )
    }

    // 10. COMBOS CRUD & ENDPOINT INTEGRATION
    fun fetchCombos() {
        val url = preferencesRepo.backendUrl.value.trim()
        viewModelScope.launch {
            _isLoadingCombos.value = true
            if (url.isNotBlank()) {
                val result = GananciasApiService.listarCombos(url)
                result.onSuccess { remoteCombos ->
                    if (remoteCombos.isNotEmpty()) {
                        _combos.value = remoteCombos
                        // Cache/sync to Firestore
                        try {
                            val batch = firestore.batch()
                            remoteCombos.forEach { combo ->
                                val docRef = firestore.collection("combos").document(combo.id.ifBlank { "combo_${combo.fila}" })
                                batch.set(docRef, mapOf(
                                    "fila" to combo.fila,
                                    "id" to (combo.id.ifBlank { "combo_${combo.fila}" }),
                                    "nombre" to combo.nombre,
                                    "precioUsd" to combo.precioUsd,
                                    "categoria" to combo.categoria,
                                    "disponibles" to combo.disponibles,
                                    "componentes" to combo.componentes.map {
                                        mapOf(
                                            "fila" to it.fila,
                                            "nombre" to it.nombre,
                                            "cantidadPorCombo" to it.cantidadPorCombo,
                                            "stockDisponible" to it.stockDisponible
                                        )
                                    }
                                ))
                            }
                            batch.commit().await()
                        } catch (e: Exception) {
                            Log.w("InventoryViewModel", "Error sincronizando combos a Firestore: ${e.message}")
                        }
                    }
                }.onFailure { e ->
                    Log.w("InventoryViewModel", "Error obteniendo combos del backend: ${e.message}. Usando Firestore.")
                }
            }
            _isLoadingCombos.value = false
        }
    }

    fun crearCombo(
        nombre: String,
        precioUsd: Double,
        categoria: String,
        componentes: List<Pair<Product, Int>>,
        onSuccess: () -> Unit = {}
    ) {
        val trimmedNombre = nombre.trim()
        if (trimmedNombre.isBlank()) {
            _errorMessage.value = "El nombre del combo no puede estar vacío"
            return
        }
        if (precioUsd <= 0.0) {
            _errorMessage.value = "El precio debe ser mayor a 0"
            return
        }
        if (componentes.isEmpty()) {
            _errorMessage.value = "Debes agregar al menos un componente al combo"
            return
        }

        viewModelScope.launch {
            _isLoadingCombos.value = true
            _errorMessage.value = null
            try {
                val nextFila = (_combos.value.maxOfOrNull { it.fila } ?: 0) + 1
                val comboId = "combo_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"

                val componentesList = componentes.map { (prod, qty) ->
                    ComboComponente(
                        fila = prod.fila,
                        nombre = prod.producto,
                        cantidadPorCombo = qty,
                        stockDisponible = prod.cantidad,
                        precioCompraUnitario = prod.precioCompra,
                        precioVentaUnitario = prod.precioUsd
                    )
                }

                val costoTotal = componentesList.sumOf { it.precioCompraUnitario * it.cantidadPorCombo }

                val disponibles = if (componentesList.isEmpty()) 0 else {
                    componentesList.minOfOrNull { comp ->
                        if (comp.cantidadPorCombo > 0) comp.stockDisponible / comp.cantidadPorCombo else 0
                    } ?: 0
                }

                val nuevoCombo = Combo(
                    fila = nextFila,
                    id = comboId,
                    nombre = trimmedNombre,
                    precioUsd = precioUsd,
                    categoria = categoria.ifBlank { "Combos" },
                    componentes = componentesList,
                    disponibles = disponibles,
                    costoTotal = costoTotal
                )

                // Save to Firestore
                val comboDocRef = firestore.collection("combos").document(comboId)
                comboDocRef.set(mapOf(
                    "fila" to nextFila,
                    "id" to comboId,
                    "nombre" to trimmedNombre,
                    "precioUsd" to precioUsd,
                    "categoria" to categoria.ifBlank { "Combos" },
                    "disponibles" to disponibles,
                    "costoTotal" to costoTotal,
                    "componentes" to componentesList.map {
                        mapOf(
                            "fila" to it.fila,
                            "nombre" to it.nombre,
                            "cantidadPorCombo" to it.cantidadPorCombo,
                            "stockDisponible" to it.stockDisponible,
                            "precioCompraUnitario" to it.precioCompraUnitario,
                            "precioVentaUnitario" to it.precioVentaUnitario
                        )
                    }
                )).await()

                // Call backend API if configured
                val url = preferencesRepo.backendUrl.value.trim()
                if (url.isNotBlank()) {
                    GananciasApiService.crearCombo(
                        backendUrl = url,
                        nombre = trimmedNombre,
                        precioUsd = precioUsd,
                        categoria = categoria.ifBlank { "Combos" },
                        componentes = componentes.map { Pair(it.first.fila, it.second) }
                    )
                }

                _successMessage.value = "Combo '$trimmedNombre' creado con éxito"
                onSuccess()
            } catch (e: Exception) {
                Log.e("InventoryViewModel", "Error creando combo: ${e.message}", e)
                _errorMessage.value = "Error al crear combo: ${e.localizedMessage}"
            } finally {
                _isLoadingCombos.value = false
            }
        }
    }

    fun eliminarCombo(combo: Combo, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoadingCombos.value = true
            _errorMessage.value = null
            try {
                // Delete from Firestore
                val docRef = if (combo.id.isNotBlank()) {
                    firestore.collection("combos").document(combo.id)
                } else {
                    firestore.collection("combos").document("combo_${combo.fila}")
                }
                docRef.delete().await()

                // Remove from cart if present
                _cart.value = _cart.value.filterNot {
                    it.isCombo && (it.combo?.fila == combo.fila || it.combo?.id == combo.id)
                }

                // Delete from backend if configured
                val url = preferencesRepo.backendUrl.value.trim()
                if (url.isNotBlank()) {
                    GananciasApiService.eliminarCombo(url, combo.fila)
                }

                _successMessage.value = "Combo '${combo.nombre}' eliminado"
                onSuccess()
            } catch (e: Exception) {
                Log.e("InventoryViewModel", "Error eliminando combo: ${e.message}", e)
                _errorMessage.value = "Error al eliminar combo: ${e.localizedMessage}"
            } finally {
                _isLoadingCombos.value = false
            }
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    fun dismissSuccess() {
        _successMessage.value = null
    }

    private fun stopFirestoreDataListeners() {
        productsListener?.remove()
        productsListener = null
        salesListener?.remove()
        salesListener = null
        movementsListener?.remove()
        movementsListener = null
        combosListener?.remove()
        combosListener = null
        rateDocListener?.remove()
        rateDocListener = null
    }

    private fun stopAllListeners() {
        stopFirestoreDataListeners()
        userDocListener?.remove()
        userDocListener = null
        allUsersListener?.remove()
        allUsersListener = null
    }

    override fun onCleared() {
        super.onCleared()
        stopAllListeners()
        authStateListener?.let { auth.removeAuthStateListener(it) }
    }
}