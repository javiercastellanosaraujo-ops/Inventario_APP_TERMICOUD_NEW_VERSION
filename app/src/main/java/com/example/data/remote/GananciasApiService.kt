package com.example.data.remote

import android.util.Log
import com.example.data.model.CartItem
import com.example.data.model.Combo
import com.example.data.model.ComboComponente
import com.example.data.model.GananciasMes
import com.example.data.model.Product
import com.example.data.model.UsuarioGanancia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object GananciasApiService {
    private const val TAG = "GananciasApiService"

    /**
     * Helper to perform HTTP GET following redirects (essential for Google Apps Script Web Apps).
     */
    private fun executeHttpGet(urlString: String, maxRedirects: Int = 5): String {
        var currentUrl = urlString
        var redirects = 0

        while (redirects < maxRedirects) {
            val url = URL(currentUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Termicoud-Inventory/1.0 (Android)")
            }

            val responseCode = connection.responseCode

            if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
                responseCode == 307 || responseCode == 308
            ) {
                val newUrl = connection.getHeaderField("Location")
                connection.disconnect()
                if (newUrl != null) {
                    currentUrl = newUrl
                    redirects++
                    continue
                }
            }

            if (responseCode in 200..299) {
                return connection.inputStream.bufferedReader().use(BufferedReader::readText)
            } else {
                val errorBody = try {
                    connection.errorStream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
                } catch (e: Exception) {
                    ""
                }
                throw Exception("HTTP $responseCode: $errorBody")
            }
        }
        throw Exception("Demasiadas redirecciones HTTP")
    }

    private fun executeHttpPost(urlString: String, jsonBody: JSONObject): String {
        val url = URL(urlString)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 15000
            doOutput = true
            instanceFollowRedirects = true
            setRequestProperty("Content-Type", "text/plain;charset=utf-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Termicoud-Inventory/1.0 (Android)")
        }

        connection.outputStream.use { os ->
            val input = jsonBody.toString().toByteArray(Charsets.UTF_8)
            os.write(input, 0, input.size)
        }

        val responseCode = connection.responseCode
        if (responseCode in 200..299) {
            return connection.inputStream.bufferedReader().use(BufferedReader::readText)
        } else if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
            responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
            responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
            responseCode == 307 || responseCode == 308
        ) {
            val redirectedUrl = connection.getHeaderField("Location")
            connection.disconnect()
            if (redirectedUrl != null) {
                return executeHttpGet(redirectedUrl)
            }
        }

        val errorBody = try {
            connection.errorStream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
        } catch (e: Exception) {
            ""
        }
        throw Exception("HTTP $responseCode: $errorBody")
    }

    /**
     * Data class for shared exchange rate info.
     */
    data class TasaInfo(
        val tasa: Double,
        val actualizada: String? = null,
        val usuario: String? = null
    )

    /**
     * GET {URL}?accion=obtener_tasa
     * Retorna { ok: true, tasa: number, actualizada: "2026-08-20T10:30:00.000Z", usuario: "..." }
     */
    suspend fun obtenerTasa(backendUrl: String): Result<TasaInfo> = withContext(Dispatchers.IO) {
        val cleanUrl = backendUrl.trim()
        if (cleanUrl.isBlank()) {
            return@withContext Result.failure(Exception("URL del backend no configurada"))
        }

        try {
            val separator = if (cleanUrl.contains("?")) "&" else "?"
            val fullUrl = "$cleanUrl${separator}accion=obtener_tasa"
            val responseText = executeHttpGet(fullUrl)
            val json = JSONObject(responseText)

            val tasa = when {
                json.has("tasa") -> json.optDouble("tasa", 0.0)
                json.has("tasa_bcv") -> json.optDouble("tasa_bcv", 0.0)
                json.has("tasaBcv") -> json.optDouble("tasaBcv", 0.0)
                json.has("rate") -> json.optDouble("rate", 0.0)
                else -> 0.0
            }

            val actualizada = when {
                json.has("actualizada") -> json.optString("actualizada", "").ifBlank { null }
                json.has("fecha") -> json.optString("fecha", "").ifBlank { null }
                json.has("updated_at") -> json.optString("updated_at", "").ifBlank { null }
                else -> null
            }

            val usuario = when {
                json.has("usuario") -> json.optString("usuario", "").ifBlank { null }
                json.has("usuarioNombre") -> json.optString("usuarioNombre", "").ifBlank { null }
                json.has("user") -> json.optString("user", "").ifBlank { null }
                else -> null
            }

            if (tasa > 0.0) {
                Result.success(TasaInfo(tasa = tasa, actualizada = actualizada, usuario = usuario))
            } else {
                Result.failure(Exception("Tasa no válida en respuesta: $responseText"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo tasa de backend: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * POST {URL} con { accion: "guardar_tasa", tasa: number, usuario: "..." }
     */
    suspend fun guardarTasa(
        backendUrl: String,
        tasa: Double,
        usuario: String = "Operador"
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val cleanUrl = backendUrl.trim()
        if (cleanUrl.isBlank()) {
            return@withContext Result.success(true)
        }

        try {
            val payload = JSONObject().apply {
                put("accion", "guardar_tasa")
                put("tasa", tasa)
                put("tasa_bcv", tasa)
                put("tasaBcv", tasa)
                put("usuario", usuario.ifBlank { "Operador" })
                put("fecha", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }.format(java.util.Date()))
            }
            val response = executeHttpPost(cleanUrl, payload)
            Log.d(TAG, "Respuesta guardar_tasa: $response")
            Result.success(true)
        } catch (e: Exception) {
            Log.w(TAG, "Error guardando tasa en backend (${e.message})", e)
            Result.failure(e)
        }
    }

    /**
     * GET {URL}?accion=listar
     * Obtiene la lista completa de productos del inventario desde el backend.
     */
    suspend fun listarProductos(backendUrl: String): Result<List<Product>> = withContext(Dispatchers.IO) {
        val cleanUrl = backendUrl.trim()
        if (cleanUrl.isBlank()) {
            return@withContext Result.failure(Exception("URL del backend no configurada"))
        }

        try {
            val separator = if (cleanUrl.contains("?")) "&" else "?"
            val fullUrl = "$cleanUrl${separator}accion=listar"
            val responseText = executeHttpGet(fullUrl)
            val json = JSONObject(responseText)

            val productosList = mutableListOf<Product>()
            val productosArray = when {
                json.has("productos") -> json.optJSONArray("productos")
                json.has("data") -> json.optJSONArray("data")
                json.has("items") -> json.optJSONArray("items")
                else -> null
            }

            if (productosArray != null) {
                for (i in 0 until productosArray.length()) {
                    val pObj = productosArray.optJSONObject(i) ?: continue
                    val fila = pObj.optInt("fila", pObj.optInt("row", i + 1))
                    val id = pObj.optString("id", "prod_$fila").ifBlank { "prod_$fila" }
                    val nombre = (pObj.optString("producto", pObj.optString("Producto",
                        pObj.optString("nombre", pObj.optString("Nombre", ""))))).trim()

                    if (nombre.isBlank()) continue

                    val cantidad = pObj.optInt("cantidad", pObj.optInt("Cantidad",
                        pObj.optInt("stock", pObj.optInt("Stock", 0))))

                    val precioUsd = when {
                        pObj.has("precioUsd") -> pObj.optDouble("precioUsd", 0.0)
                        pObj.has("PrecioUsd") -> pObj.optDouble("PrecioUsd", 0.0)
                        pObj.has("precio_usd") -> pObj.optDouble("precio_usd", 0.0)
                        pObj.has("precio") -> pObj.optDouble("precio", 0.0)
                        pObj.has("Precio") -> pObj.optDouble("Precio", 0.0)
                        pObj.has("price") -> pObj.optDouble("price", 0.0)
                        else -> 0.0
                    }

                    val precioMayor = when {
                        pObj.has("precio_mayor") && !pObj.isNull("precio_mayor") -> pObj.optDouble("precio_mayor").let { if (it > 0) it else null }
                        pObj.has("precioMayor") && !pObj.isNull("precioMayor") -> pObj.optDouble("precioMayor").let { if (it > 0) it else null }
                        pObj.has("Precio Mayor USD") && !pObj.isNull("Precio Mayor USD") -> pObj.optDouble("Precio Mayor USD").let { if (it > 0) it else null }
                        pObj.has("Precio Mayor") && !pObj.isNull("Precio Mayor") -> pObj.optDouble("Precio Mayor").let { if (it > 0) it else null }
                        pObj.has("precio_mayor_usd") && !pObj.isNull("precio_mayor_usd") -> pObj.optDouble("precio_mayor_usd").let { if (it > 0) it else null }
                        else -> null
                    }

                    val cantidadMinimaMayor = when {
                        pObj.has("cantidad_minima_mayor") && !pObj.isNull("cantidad_minima_mayor") -> pObj.optInt("cantidad_minima_mayor").let { if (it > 0) it else null }
                        pObj.has("cantidadMinimaMayor") && !pObj.isNull("cantidadMinimaMayor") -> pObj.optInt("cantidadMinimaMayor").let { if (it > 0) it else null }
                        pObj.has("Cant. Mínima Mayor") && !pObj.isNull("Cant. Mínima Mayor") -> pObj.optInt("Cant. Mínima Mayor").let { if (it > 0) it else null }
                        pObj.has("cant_minima_mayor") && !pObj.isNull("cant_minima_mayor") -> pObj.optInt("cant_minima_mayor").let { if (it > 0) it else null }
                        pObj.has("min_mayor") && !pObj.isNull("min_mayor") -> pObj.optInt("min_mayor").let { if (it > 0) it else null }
                        else -> null
                    }

                    val catalogo = (pObj.optString("catalogo", pObj.optString("Catalogo",
                        pObj.optString("categoria", pObj.optString("Categoria", "General"))))).trim().ifBlank { "General" }

                    val codigo = pObj.optString("codigo", pObj.optString("Codigo",
                        pObj.optString("code", pObj.optString("sku", "")))).trim()

                    val codigoBarras = pObj.optString("codigoBarras", pObj.optString("barcode",
                        pObj.optString("codigo_barras", pObj.optString("codBarras", "")))).trim()

                    val marca = pObj.optString("marca", pObj.optString("Marca",
                        pObj.optString("brand", ""))).trim()

                    val modelo = pObj.optString("modelo", pObj.optString("Modelo",
                        pObj.optString("model", ""))).trim()

                    val ubicacion = pObj.optString("ubicacion", pObj.optString("Ubicacion",
                        pObj.optString("location", ""))).trim()

                    val minStock = pObj.optInt("minStock", pObj.optInt("min_stock",
                        pObj.optInt("stockMinimo", 5)))

                    productosList.add(
                        Product(
                            fila = fila,
                            id = id,
                            producto = nombre,
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
                    )
                }
            }

            Result.success(productosList)
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo productos de backend: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * POST {URL} con { accion: "agregar_alias_codigo", fila: fila, codigo_barra: codigo }
     */
    suspend fun agregarAliasCodigo(backendUrl: String, fila: Int, codigoBarra: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val cleanUrl = backendUrl.trim()
        if (cleanUrl.isBlank()) {
            return@withContext Result.success(true) // No backend URL configured, Firestore was updated locally
        }

        try {
            val payload = JSONObject().apply {
                put("accion", "agregar_alias_codigo")
                put("fila", fila)
                put("codigo_barra", codigoBarra.trim())
                put("codigo", codigoBarra.trim())
            }
            val response = executeHttpPost(cleanUrl, payload)
            Log.d(TAG, "Respuesta agregar_alias_codigo: $response")
            Result.success(true)
        } catch (e: Exception) {
            Log.w(TAG, "Error enviando alias a backend (${e.message}), continuando con Firestore", e)
            Result.failure(e)
        }
    }

    /**
     * POST {URL} con { accion: "agregar_producto", ... }
     */
    suspend fun agregarProductoBackend(
        backendUrl: String,
        producto: String,
        cantidad: Int,
        precioUsd: Double,
        precioMayor: Double? = null,
        cantidadMinimaMayor: Int? = null,
        catalogo: String,
        codigoBarra: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val cleanUrl = backendUrl.trim()
        if (cleanUrl.isBlank()) {
            return@withContext Result.success(true)
        }

        try {
            val payload = JSONObject().apply {
                put("accion", "agregar_producto")
                put("producto", producto.trim())
                put("cantidad", cantidad)
                put("precio", precioUsd)
                put("precio_usd", precioUsd)
                put("precioUsd", precioUsd)
                if (precioMayor != null && precioMayor > 0) {
                    put("precio_mayor", precioMayor)
                } else {
                    put("precio_mayor", JSONObject.NULL)
                }
                if (cantidadMinimaMayor != null && cantidadMinimaMayor > 0) {
                    put("cantidad_minima_mayor", cantidadMinimaMayor)
                } else {
                    put("cantidad_minima_mayor", JSONObject.NULL)
                }
                put("catalogo", catalogo.trim())
                put("codigo_barra", codigoBarra.trim())
            }
            val response = executeHttpPost(cleanUrl, payload)
            Log.d(TAG, "Respuesta agregar_producto: $response")
            Result.success(true)
        } catch (e: Exception) {
            Log.w(TAG, "Error enviando producto a backend (${e.message}), continuando con Firestore", e)
            Result.failure(e)
        }
    }

    /**
     * POST {URL} con { accion: "actualizar_producto", fila, cantidad, precio_usd, precio_mayor, cantidad_minima_mayor }
     */
    suspend fun actualizarProductoBackend(
        backendUrl: String,
        fila: Int,
        cantidad: Int? = null,
        precioUsd: Double? = null,
        precioMayor: Double? = null,
        cantidadMinimaMayor: Int? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val cleanUrl = backendUrl.trim()
        if (cleanUrl.isBlank() || fila <= 0) {
            return@withContext Result.success(true)
        }

        try {
            val payload = JSONObject().apply {
                put("accion", "actualizar_producto")
                put("fila", fila)
                if (cantidad != null) put("cantidad", cantidad)
                if (precioUsd != null) {
                    put("precio_usd", precioUsd)
                    put("precioUsd", precioUsd)
                    put("precio", precioUsd)
                }
                if (precioMayor != null && precioMayor > 0) {
                    put("precio_mayor", precioMayor)
                } else if (precioMayor != null) {
                    put("precio_mayor", JSONObject.NULL)
                }
                if (cantidadMinimaMayor != null && cantidadMinimaMayor > 0) {
                    put("cantidad_minima_mayor", cantidadMinimaMayor)
                } else if (cantidadMinimaMayor != null) {
                    put("cantidad_minima_mayor", JSONObject.NULL)
                }
            }
            val response = executeHttpPost(cleanUrl, payload)
            Log.d(TAG, "Respuesta actualizar_producto: $response")
            Result.success(true)
        } catch (e: Exception) {
            Log.w(TAG, "Error actualizando producto en backend (${e.message})", e)
            Result.failure(e)
        }
    }

    /**
     * GET {URL}?accion=ganancias
     * Obtiene las ganancias del mes en curso.
     */
    suspend fun getGananciasActuales(backendUrl: String): Result<GananciasMes> = withContext(Dispatchers.IO) {
        val cleanUrl = backendUrl.trim()
        if (cleanUrl.isBlank()) {
            return@withContext Result.failure(Exception("URL del backend no configurada"))
        }

        try {
            val separator = if (cleanUrl.contains("?")) "&" else "?"
            val fullUrl = "$cleanUrl${separator}accion=ganancias"
            val responseText = executeHttpGet(fullUrl)
            val json = JSONObject(responseText)

            if (!json.optBoolean("ok", false) && !json.has("ganancias")) {
                val errorMsg = json.optString("error", "Error desconocido del backend")
                return@withContext Result.failure(Exception(errorMsg))
            }

            val gananciasObj = json.optJSONObject("ganancias") ?: json
            val parsed = parseGananciasMesJson(gananciasObj, isArchived = false)
            Result.success(parsed)
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo ganancias actuales: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * GET {URL}?accion=historial_meses
     * Obtiene la lista de meses archivados/cerrados.
     */
    suspend fun getHistorialMeses(backendUrl: String): Result<List<String>> = withContext(Dispatchers.IO) {
        val cleanUrl = backendUrl.trim()
        if (cleanUrl.isBlank()) {
            return@withContext Result.failure(Exception("URL del backend no configurada"))
        }

        try {
            val separator = if (cleanUrl.contains("?")) "&" else "?"
            val fullUrl = "$cleanUrl${separator}accion=historial_meses"
            val responseText = executeHttpGet(fullUrl)
            val json = JSONObject(responseText)

            val mesesList = mutableListOf<String>()
            val mesesArray = json.optJSONArray("meses")
            if (mesesArray != null) {
                for (i in 0 until mesesArray.length()) {
                    val m = mesesArray.optString(i, "").trim()
                    if (m.isNotBlank()) {
                        mesesList.add(m)
                    }
                }
            }

            Result.success(mesesList)
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo historial de meses: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * GET {URL}?accion=ganancias_mes&mes={mes}
     * Obtiene las ganancias de un mes archivado específico.
     */
    suspend fun getGananciasMesArchivado(backendUrl: String, mesKey: String): Result<GananciasMes> = withContext(Dispatchers.IO) {
        val cleanUrl = backendUrl.trim()
        if (cleanUrl.isBlank()) {
            return@withContext Result.failure(Exception("URL del backend no configurada"))
        }

        try {
            val separator = if (cleanUrl.contains("?")) "&" else "?"
            val encodedMes = URLEncoder.encode(mesKey, "UTF-8")
            val fullUrl = "$cleanUrl${separator}accion=ganancias_mes&mes=$encodedMes"
            val responseText = executeHttpGet(fullUrl)
            val json = JSONObject(responseText)

            if (!json.optBoolean("ok", false) && !json.has("ganancias")) {
                val errorMsg = json.optString("error", "Error consultando mes archivado")
                return@withContext Result.failure(Exception(errorMsg))
            }

            val gananciasObj = json.optJSONObject("ganancias") ?: json
            val parsed = parseGananciasMesJson(gananciasObj, isArchived = true).copy(
                mes = if (gananciasObj.optString("mes").isNotBlank()) gananciasObj.optString("mes") else mesKey
            )
            Result.success(parsed)
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo ganancias del mes $mesKey: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun parseGananciasMesJson(obj: JSONObject, isArchived: Boolean): GananciasMes {
        val mes = obj.optString("mes", "")
        val totalUsd = obj.optDouble("total_usd", obj.optDouble("totalUsd", 0.0))
        val totalBs = obj.optDouble("total_bs", obj.optDouble("totalBs", 0.0))

        val usuariosList = mutableListOf<UsuarioGanancia>()
        val usuariosArray = obj.optJSONArray("usuarios")
        if (usuariosArray != null) {
            for (i in 0 until usuariosArray.length()) {
                val uObj = usuariosArray.optJSONObject(i) ?: continue
                val usuario = uObj.optString("usuario", uObj.optString("nombre", "Operador")).trim()
                val ventas = uObj.optInt("ventas", uObj.optInt("cantidad_ventas", 0))
                val unidades = uObj.optInt("unidades", uObj.optInt("cantidad_unidades", 0))
                val uTotalUsd = uObj.optDouble("total_usd", uObj.optDouble("totalUsd", 0.0))
                val uTotalBs = uObj.optDouble("total_bs", uObj.optDouble("totalBs", 0.0))

                usuariosList.add(
                    UsuarioGanancia(
                        usuario = usuario,
                        ventas = ventas,
                        unidades = unidades,
                        totalUsd = uTotalUsd,
                        totalBs = uTotalBs
                    )
                )
            }
        }

        // Order descending by totalUsd
        val sortedUsers = usuariosList.sortedByDescending { it.totalUsd }

        return GananciasMes(
            mes = mes,
            usuarios = sortedUsers,
            totalUsd = totalUsd,
            totalBs = totalBs,
            isArchived = isArchived
        )
    }

    /**
     * GET {URL}?accion=listar_combos
     * Retorna { ok: true, combos: [ { fila, nombre, precio_usd, categoria, componentes: [ { fila, nombre, cantidad_por_combo, stock_disponible } ], disponibles } ] }
     */
    suspend fun listarCombos(backendUrl: String): Result<List<Combo>> = withContext(Dispatchers.IO) {
        val cleanUrl = backendUrl.trim()
        if (cleanUrl.isBlank()) {
            return@withContext Result.failure(Exception("URL del backend no configurada"))
        }

        try {
            val separator = if (cleanUrl.contains("?")) "&" else "?"
            val fullUrl = "$cleanUrl${separator}accion=listar_combos"
            val responseText = executeHttpGet(fullUrl)
            val json = JSONObject(responseText)

            val combosList = mutableListOf<Combo>()
            val combosArray = json.optJSONArray("combos")
            if (combosArray != null) {
                for (i in 0 until combosArray.length()) {
                    val cObj = combosArray.optJSONObject(i) ?: continue
                    val fila = cObj.optInt("fila", 0)
                    val nombre = cObj.optString("nombre", "").trim()
                    val precioUsd = cObj.optDouble("precio_usd", cObj.optDouble("precioUsd", 0.0))
                    val categoria = cObj.optString("categoria", "Combos").trim()
                    val disponibles = cObj.optInt("disponibles", 0)

                    val componentesList = mutableListOf<ComboComponente>()
                    val compArray = cObj.optJSONArray("componentes")
                    if (compArray != null) {
                        for (j in 0 until compArray.length()) {
                            val compObj = compArray.optJSONObject(j) ?: continue
                            val compFila = compObj.optInt("fila", 0)
                            val compNombre = compObj.optString("nombre", "").trim()
                            val cantPorCombo = compObj.optInt("cantidad_por_combo", compObj.optInt("cantidad", 1))
                            val stockDisp = compObj.optInt("stock_disponible", compObj.optInt("stock", 0))

                            componentesList.add(
                                ComboComponente(
                                    fila = compFila,
                                    nombre = compNombre,
                                    cantidadPorCombo = cantPorCombo,
                                    stockDisponible = stockDisp
                                )
                            )
                        }
                    }

                    combosList.add(
                        Combo(
                            fila = fila,
                            id = "combo_$fila",
                            nombre = nombre,
                            precioUsd = precioUsd,
                            categoria = if (categoria.isNotBlank()) categoria else "Combos",
                            componentes = componentesList,
                            disponibles = disponibles
                        )
                    )
                }
            }

            Result.success(combosList)
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo lista de combos: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * POST {URL} body: { accion: "crear_combo", nombre, precio_usd, categoria, componentes: [ { fila, cantidad } ] }
     */
    suspend fun crearCombo(
        backendUrl: String,
        nombre: String,
        precioUsd: Double,
        categoria: String,
        componentes: List<Pair<Int, Int>>
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val cleanUrl = backendUrl.trim()
        if (cleanUrl.isBlank()) {
            return@withContext Result.success(true)
        }

        try {
            val compArray = JSONArray()
            componentes.forEach { (fila, cantidad) ->
                val cObj = JSONObject().apply {
                    put("fila", fila)
                    put("cantidad", cantidad)
                }
                compArray.put(cObj)
            }

            val payload = JSONObject().apply {
                put("accion", "crear_combo")
                put("nombre", nombre.trim())
                put("precio_usd", precioUsd)
                put("precio", precioUsd)
                put("categoria", categoria.trim())
                put("componentes", compArray)
            }

            val response = executeHttpPost(cleanUrl, payload)
            Log.d(TAG, "Respuesta crear_combo: $response")
            Result.success(true)
        } catch (e: Exception) {
            Log.w(TAG, "Error creando combo en backend (${e.message})", e)
            Result.failure(e)
        }
    }

    /**
     * POST {URL} body: { accion: "eliminar_combo", fila }
     */
    suspend fun eliminarCombo(
        backendUrl: String,
        fila: Int
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val cleanUrl = backendUrl.trim()
        if (cleanUrl.isBlank()) {
            return@withContext Result.success(true)
        }

        try {
            val payload = JSONObject().apply {
                put("accion", "eliminar_combo")
                put("fila", fila)
            }

            val response = executeHttpPost(cleanUrl, payload)
            Log.d(TAG, "Respuesta eliminar_combo: $response")
            Result.success(true)
        } catch (e: Exception) {
            Log.w(TAG, "Error eliminando combo en backend (${e.message})", e)
            Result.failure(e)
        }
    }

    /**
     * POST {URL}
     * Content-Type: text/plain;charset=utf-8
     * Body:
     * {
     *   "accion": "registrar_venta",
     *   "usuario": "<nombre del usuario activo>",
     *   "total_usd": <total en USD del carrito completo>,
     *   "total_bs": <total en Bs del carrito completo>,
     *   "items": [
     *     {
     *       "tipo": "producto",           // o "combo" si es un combo
     *       "fila": <número de fila del producto o combo>,
     *       "cantidad": <cantidad vendida>,
     *       "nombre": "<nombre>",
     *       "precio_usd": <precio unitario efectivo, normal o mayor>
     *     }
     *   ]
     * }
     */
    suspend fun registrarVentaBackend(
        backendUrl: String,
        totalUsd: Double,
        totalBs: Double,
        items: List<CartItem>,
        usuario: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val cleanUrl = backendUrl.trim()
        if (cleanUrl.isBlank()) {
            return@withContext Result.success(true)
        }

        try {
            val itemsArray = JSONArray()
            for (item in items) {
                val itemObj = JSONObject().apply {
                    if (item.isCombo && item.combo != null) {
                        put("tipo", "combo")
                        put("fila", item.combo.fila)
                        put("cantidad", item.cantidadSelected)
                        put("nombre", item.combo.nombre)
                        put("precio_usd", item.combo.precioUsd)
                    } else {
                        put("tipo", "producto")
                        put("fila", item.product.fila)
                        put("cantidad", item.cantidadSelected)
                        put("nombre", item.product.producto)
                        put("precio_usd", item.precioUnitarioAplicado)
                    }
                }
                itemsArray.put(itemObj)
            }

            val cleanUsuario = usuario.trim().ifBlank { "Operador" }
            val payload = JSONObject().apply {
                put("accion", "registrar_venta")
                put("usuario", cleanUsuario)
                put("total_usd", totalUsd)
                put("total_bs", totalBs)
                put("items", itemsArray)
            }

            val response = executeHttpPost(cleanUrl, payload)
            Log.d(TAG, "Respuesta registrar_venta: $response")

            val json = try { JSONObject(response) } catch (e: Exception) { null }
            if (json != null && json.has("ok") && !json.optBoolean("ok", true)) {
                val errMsg = json.optString("error", json.optString("mensaje", "Error registrando venta en backend"))
                return@withContext Result.failure(Exception(errMsg))
            }
            Result.success(true)
        } catch (e: Exception) {
            Log.w(TAG, "Error registrando venta en backend (${e.message})", e)
            Result.failure(e)
        }
    }

    /**
     * Sube silenciosamente el PDF de la Nota de Entrega a la carpeta de Google Drive del Administrador.
     */
    suspend fun uploadInvoiceToDrive(
        url: String,
        saleId: String,
        folio: String,
        cliente: String,
        pdfBase64: String,
        totalUsd: Double,
        usuario: String,
        mesAnio: String = ""
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank()) return@withContext Result.failure(Exception("URL vacía"))

        try {
            val fileName = "Nota_Entrega_${folio.ifBlank { saleId }}.pdf"
            val payload = JSONObject().apply {
                put("accion", "guardar_nota_drive")
                put("folio", folio)
                put("cliente", cliente)
                put("usuario", usuario)
                put("total_usd", totalUsd)
                put("mes_anio", mesAnio)
                put("archivo_nombre", fileName)
                put("archivo_base64", pdfBase64)
            }

            val response = executeHttpPost(cleanUrl, payload)
            Log.d(TAG, "Respuesta guardar_nota_drive: $response")
            Result.success(true)
        } catch (e: Exception) {
            Log.w(TAG, "Error subiendo PDF a Drive: ${e.message}", e)
            Result.failure(e)
        }
    }
}
