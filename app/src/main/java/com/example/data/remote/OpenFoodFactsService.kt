package com.example.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

data class OpenFoodFactsResult(
    val found: Boolean,
    val productName: String = "",
    val brand: String = "",
    val categories: String = "",
    val imageUrl: String? = null,
    val errorMessage: String? = null
)

object OpenFoodFactsService {
    private const val TAG = "OpenFoodFactsService"
    private const val BASE_URL = "https://world.openfoodfacts.org/api/v2/product/"

    /**
     * Consulta la API pública y gratuita de Open Food Facts:
     * GET https://world.openfoodfacts.org/api/v2/product/{codigo}.json
     */
    suspend fun lookupBarcode(barcode: String): OpenFoodFactsResult = withContext(Dispatchers.IO) {
        val cleanCode = barcode.trim()
        if (cleanCode.isBlank()) {
            return@withContext OpenFoodFactsResult(found = false, errorMessage = "Código de barras vacío")
        }

        var connection: HttpURLConnection? = null
        try {
            val url = URL("$BASE_URL$cleanCode.json")
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Termicoud-InventoryApp/2.0 (Android; support@termicoud.local)")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                val json = JSONObject(responseText)
                val status = json.optInt("status", 0)
                val productObj = json.optJSONObject("product")

                if (status == 1 && productObj != null) {
                    // Try spanish name first, then generic product_name, generic_name, etc.
                    val nameEs = productObj.optString("product_name_es", "").trim()
                    val name = productObj.optString("product_name", "").trim()
                    val genericName = productObj.optString("generic_name", "").trim()
                    val genericNameEs = productObj.optString("generic_name_es", "").trim()
                    
                    val chosenName = when {
                        nameEs.isNotBlank() -> nameEs
                        name.isNotBlank() -> name
                        genericNameEs.isNotBlank() -> genericNameEs
                        genericName.isNotBlank() -> genericName
                        else -> ""
                    }

                    val brand = productObj.optString("brands", "").trim()
                    val categories = productObj.optString("categories", "").trim()
                    val imageUrl = productObj.optString("image_url", "").ifBlank { null }

                    if (chosenName.isNotBlank()) {
                        val fullName = if (brand.isNotBlank() && !chosenName.contains(brand, ignoreCase = true)) {
                            "$chosenName ($brand)"
                        } else {
                            chosenName
                        }
                        return@withContext OpenFoodFactsResult(
                            found = true,
                            productName = fullName,
                            brand = brand,
                            categories = categories,
                            imageUrl = imageUrl
                        )
                    }
                }

                // If not found in Open Food Facts, try secondary fallback UPCItemDb
                val upcFallback = UpcItemDbService.lookupBarcode(cleanCode)
                if (upcFallback.found && upcFallback.title.isNotBlank()) {
                    return@withContext OpenFoodFactsResult(
                        found = true,
                        productName = upcFallback.title,
                        brand = upcFallback.brand,
                        categories = upcFallback.category,
                        imageUrl = upcFallback.imageUrl
                    )
                }

                return@withContext OpenFoodFactsResult(
                    found = false,
                    errorMessage = "Producto no encontrado en Open Food Facts"
                )
            } else if (responseCode == 404) {
                // Try secondary fallback UPCItemDb
                val upcFallback = UpcItemDbService.lookupBarcode(cleanCode)
                if (upcFallback.found && upcFallback.title.isNotBlank()) {
                    return@withContext OpenFoodFactsResult(
                        found = true,
                        productName = upcFallback.title,
                        brand = upcFallback.brand,
                        categories = upcFallback.category,
                        imageUrl = upcFallback.imageUrl
                    )
                }

                return@withContext OpenFoodFactsResult(
                    found = false,
                    errorMessage = "Código no registrado en base de datos abierta"
                )
            } else {
                Log.w(TAG, "Open Food Facts HTTP code: $responseCode")
                return@withContext OpenFoodFactsResult(
                    found = false,
                    errorMessage = "Error HTTP $responseCode al consultar Open Food Facts"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error consultando Open Food Facts: ${e.message}", e)
            return@withContext OpenFoodFactsResult(
                found = false,
                errorMessage = "Sin conexión o servicio no disponible (${e.localizedMessage})"
            )
        } finally {
            connection?.disconnect()
        }
    }
}
