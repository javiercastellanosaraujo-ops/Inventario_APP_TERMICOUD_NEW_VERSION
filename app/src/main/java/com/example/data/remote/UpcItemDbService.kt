package com.example.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class UpcProductLookupResult(
    val found: Boolean,
    val title: String = "",
    val brand: String = "",
    val model: String = "",
    val category: String = "",
    val imageUrl: String? = null,
    val errorMessage: String? = null
)

object UpcItemDbService {
    private const val TAG = "UpcItemDbService"
    private const val BASE_URL = "https://api.upcitemdb.com/prod/trial/lookup?upc="

    /**
     * Consulta la API pública de UPCItemDB (Trial Endpoint gratuito).
     * Retorna el producto encontrado o un resultado no encontrado / error sin crashear.
     */
    suspend fun lookupBarcode(barcode: String): UpcProductLookupResult = withContext(Dispatchers.IO) {
        val cleanCode = barcode.trim()
        if (cleanCode.isBlank()) {
            return@withContext UpcProductLookupResult(found = false, errorMessage = "Código vacío")
        }

        var connection: HttpURLConnection? = null
        try {
            val url = URL("$BASE_URL$cleanCode")
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Termicoud-Inventory/1.0 (Android)")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                val json = JSONObject(responseText)
                val code = json.optString("code", "")
                val total = json.optInt("total", 0)
                val items = json.optJSONArray("items")

                if (code == "OK" && total > 0 && items != null && items.length() > 0) {
                    val firstItem = items.getJSONObject(0)
                    val title = firstItem.optString("title", "")
                    val brand = firstItem.optString("brand", "")
                    val model = firstItem.optString("model", "")
                    val category = firstItem.optString("category", "")
                    
                    var img: String? = null
                    val imagesArray = firstItem.optJSONArray("images")
                    if (imagesArray != null && imagesArray.length() > 0) {
                        img = imagesArray.optString(0)
                    }

                    return@withContext UpcProductLookupResult(
                        found = true,
                        title = title,
                        brand = brand,
                        model = model,
                        category = category,
                        imageUrl = img
                    )
                } else {
                    return@withContext UpcProductLookupResult(
                        found = false,
                        errorMessage = "Código no encontrado en base de datos global."
                    )
                }
            } else if (responseCode == 429) {
                Log.w(TAG, "Límite de consultas UPCItemDB trial alcanzado (HTTP 429)")
                return@withContext UpcProductLookupResult(
                    found = false,
                    errorMessage = "Límite de consultas gratuitas alcanzado. Completa los datos manualmente."
                )
            } else {
                Log.w(TAG, "Respuesta HTTP inesperada de UPCItemDB: $responseCode")
                return@withContext UpcProductLookupResult(
                    found = false,
                    errorMessage = "No se pudo consultar en línea (HTTP $responseCode)."
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error consultando UPCItemDB: ${e.message}")
            return@withContext UpcProductLookupResult(
                found = false,
                errorMessage = "Sin conexión a internet o servicio no disponible."
            )
        } finally {
            connection?.disconnect()
        }
    }
}
